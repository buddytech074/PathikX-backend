package com.vehiclebooking.backend.service.impl;

import com.vehiclebooking.backend.dto.BookingDto;
import com.vehiclebooking.backend.dto.NotificationMessage;
import com.vehiclebooking.backend.exception.ResourceNotFoundException;
import com.vehiclebooking.backend.model.Booking;
import com.vehiclebooking.backend.model.User;
import com.vehiclebooking.backend.model.Vehicle;
import com.vehiclebooking.backend.model.enums.BookingStatus;
import com.vehiclebooking.backend.model.enums.VehicleType;
import com.vehiclebooking.backend.repository.BookingRepository;
import com.vehiclebooking.backend.repository.UserRepository;
import com.vehiclebooking.backend.repository.VehicleRepository;
import com.vehiclebooking.backend.repository.TransactionRepository;
import com.vehiclebooking.backend.model.Transaction;
import com.vehiclebooking.backend.model.enums.PaymentStatus;
import com.vehiclebooking.backend.service.BookingService;
import com.vehiclebooking.backend.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

        private final BookingRepository bookingRepository;
        private final UserRepository userRepository;
        private final VehicleRepository vehicleRepository;
        private final TransactionRepository transactionRepository;
        private final SimpMessagingTemplate messagingTemplate;
        private final PricingService pricingService;
        private final com.vehiclebooking.backend.service.DriverLocationService driverLocationService;
        private final com.vehiclebooking.backend.repository.PassengerVerificationRepository passengerVerificationRepository;

        @Override
        public Booking createBooking(Long userId, BookingDto bookingDto) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                // 1. Handle Wedding Fleet Bookings (Consolidated Request)
                if (bookingDto.getIsWedding() != null && bookingDto.getIsWedding()) {
                        return createWeddingFleetBooking(user, bookingDto);
                }

                Vehicle vehicle = null;

                // If specific vehicle is requested
                if (bookingDto.getVehicleId() != null) {
                        vehicle = vehicleRepository.findById(bookingDto.getVehicleId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

                        // Overlap Check for Future Bookings
                        if (bookingDto.getStartDateTime() != null && bookingDto.getEndDateTime() != null) {
                                List<Booking> conflicts = bookingRepository.findOverlappingBookings(
                                                vehicle.getId(),
                                                List.of(BookingStatus.PENDING, BookingStatus.ACCEPTED),
                                                bookingDto.getStartDateTime(),
                                                bookingDto.getEndDateTime());
                                if (!conflicts.isEmpty()) {
                                        throw new RuntimeException("Vehicle is already booked for this time slot");
                                }
                        }

                        if (!vehicle.isAvailable()) {
                                throw new RuntimeException("Vehicle is currently offline (Driver inactive)");
                        }
                } else if (bookingDto.getVehicleType() == null) {
                        throw new RuntimeException("Either Vehicle ID or Vehicle Type must be provided");
                }

                // Calculate distance and pricing
                double estimatedDistance = pricingService.calculateDistance(
                                bookingDto.getPickupLat(),
                                bookingDto.getPickupLng(),
                                bookingDto.getDropLat(),
                                bookingDto.getDropLng());

                int passengerCount = bookingDto.getPassengerCount() != null ? bookingDto.getPassengerCount() : 1;
                VehicleType vehicleType = bookingDto.getVehicleType() != null ? bookingDto.getVehicleType()
                                : (vehicle != null ? vehicle.getType() : null);

                // Determine Trip Type & Duration for Pricing
                // Rule: Future bookings (>1 hour) are ALWAYS Round Trip
                // Rule: Duration > 5 hours implies Rental Pricing
                com.vehiclebooking.backend.model.enums.TripType tripType = bookingDto.getTripType();
                long durationHours = 0;

                if (bookingDto.getStartDateTime() != null) {
                        if (bookingDto.getStartDateTime().isAfter(java.time.LocalDateTime.now().plusHours(1))) {
                                tripType = com.vehiclebooking.backend.model.enums.TripType.ROUND_TRIP;
                        }

                        if (bookingDto.getEndDateTime() != null) {
                                durationHours = java.time.Duration.between(
                                                bookingDto.getStartDateTime(),
                                                bookingDto.getEndDateTime()).toHours();
                        }
                }

                int stopCount = bookingDto.getStops() != null ? bookingDto.getStops().size() : 0;

                // Calculate price estimates based on detailed logic
                BigDecimal baseFare = pricingService.calculateFare(
                                estimatedDistance,
                                passengerCount,
                                vehicleType,
                                tripType,
                                durationHours,
                                stopCount);

                double minEstimateValue = baseFare.doubleValue() * 0.8; // -20%
                double maxEstimateValue = baseFare.doubleValue() * 1.4; // +40% (Buffer for traffic/route)

                // Apply minimum fare based on vehicle type
                double minimumFare = (vehicleType == VehicleType.AUTO) ? 30.0 : 50.0;
                minEstimateValue = Math.max(minEstimateValue, minimumFare);

                BigDecimal minBase = BigDecimal.valueOf(minEstimateValue);
                BigDecimal maxBase = BigDecimal.valueOf(maxEstimateValue);

                BigDecimal minPlatformCharge = pricingService.calculatePlatformCharge(minBase);
                BigDecimal maxPlatformCharge = pricingService.calculatePlatformCharge(maxBase);

                BigDecimal minEstimate = minBase.add(minPlatformCharge).setScale(2, RoundingMode.HALF_UP);
                BigDecimal maxEstimate = maxBase.add(maxPlatformCharge).setScale(2, RoundingMode.HALF_UP);

                Booking booking = Booking.builder()
                                .user(user)
                                .isWedding(false)
                                .vehicle(vehicle) // Can be null now
                                .vehicleType(bookingDto.getVehicleType() != null ? bookingDto.getVehicleType()
                                                : (vehicle != null ? vehicle.getType() : null))
                                .pickupLocation(bookingDto.getPickupLocation())
                                .dropLocation(bookingDto.getDropLocation())
                                .pickupLat(bookingDto.getPickupLat())
                                .pickupLng(bookingDto.getPickupLng())
                                .dropLat(bookingDto.getDropLat())
                                .dropLng(bookingDto.getDropLng())
                                .passengerCount(passengerCount)
                                .estimatedDistance(estimatedDistance)
                                .minEstimatedPrice(minEstimate)
                                .maxEstimatedPrice(maxEstimate)
                                .startDateTime(bookingDto.getStartDateTime())
                                .endDateTime(bookingDto.getEndDateTime())
                                .status(BookingStatus.PENDING)
                                .tripType(tripType) // Use the calculated tripType (enforced Round Trip)
                                .isShared(bookingDto.getIsShared())
                                .platformCharge(maxPlatformCharge) // Store the calculated platform charge
                                .build();

                // Save stops if provided
                if (bookingDto.getStops() != null && !bookingDto.getStops().isEmpty()) {
                        booking.setStops(bookingDto.getStops().stream()
                                        .map(s -> com.vehiclebooking.backend.model.BookingStop.builder()
                                                        .booking(booking)
                                                        .location(s.getLocation())
                                                        .latitude(s.getLatitude())
                                                        .longitude(s.getLongitude())
                                                        .stopOrder(s.getStopOrder())
                                                        .build())
                                        .collect(java.util.stream.Collectors.toList()));
                }

                // Determine if payment is required (prebook vs immediate)
                // Save first to get the ID, then update payment logic
                Booking savedBooking = bookingRepository.save(booking);

                if (savedBooking.isPrebook()) {
                        // Prebook ride: payment required upfront
                        savedBooking.setPaymentRequired(true);
                        savedBooking.setPaymentCompleted(false);
                        savedBooking.setStatus(BookingStatus.PENDING_PAYMENT);

                        System.out.println("💰 PREBOOK RIDE - Payment Required: ₹" + savedBooking.getPlatformCharge());
                        // Notification deferred until payment completion
                } else {
                        // Immediate ride: no upfront payment
                        savedBooking.setPaymentRequired(false);
                        savedBooking.setPaymentCompleted(false);
                        savedBooking.setStatus(BookingStatus.PENDING);
                        savedBooking.setPlatformCharge(BigDecimal.ZERO); // No platform charge for immediate

                        System.out.println("⚡ IMMEDIATE RIDE - No upfront payment required");
                        // Send notification immediately
                        notifyDriversNewBooking(savedBooking);
                }

                savedBooking = bookingRepository.save(savedBooking);
                return savedBooking;
        }

        private Booking createWeddingFleetBooking(User user, BookingDto bookingDto) {
                // 1. Create Parent Booking
                Booking parent = Booking.builder()
                                .user(user)
                                .isWedding(true)
                                .pickupLocation(bookingDto.getPickupLocation())
                                .dropLocation(bookingDto.getDropLocation())
                                .pickupLat(bookingDto.getPickupLat())
                                .pickupLng(bookingDto.getPickupLng())
                                .dropLat(bookingDto.getDropLat())
                                .dropLng(bookingDto.getDropLng())
                                .startDateTime(bookingDto.getStartDateTime())
                                .endDateTime(bookingDto.getEndDateTime())
                                .status(BookingStatus.PENDING)
                                .tripType(com.vehiclebooking.backend.model.enums.TripType.ROUND_TRIP)
                                .quantity(bookingDto.getFleet().values().stream().reduce(0, Integer::sum))
                                .paymentRequired(true) // Wedding always prebook/payment required
                                .paymentCompleted(false)
                                .platformCharge(BigDecimal.ZERO) // Calculated later or per sub-booking
                                .build();

                final Booking savedParent = bookingRepository.save(parent);
                BigDecimal totalPlatformCharge = BigDecimal.ZERO;

                // 2. Create Sub-Bookings
                for (Map.Entry<VehicleType, Integer> entry : bookingDto.getFleet().entrySet()) {
                        VehicleType type = entry.getKey();
                        int qty = entry.getValue();

                        for (int i = 0; i < qty; i++) {
                                // Sub-booking specific estimate
                                BigDecimal subBase = pricingService.calculateFare(
                                                pricingService.calculateDistance(bookingDto.getPickupLat(),
                                                                bookingDto.getPickupLng(), bookingDto.getDropLat(),
                                                                bookingDto.getDropLng()),
                                                1, type, parent.getTripType(), 8, 0); // Assume 8h for wedding, 0
                                                                                      // stops

                                BigDecimal subPlatform = pricingService.calculatePlatformCharge(subBase);
                                totalPlatformCharge = totalPlatformCharge.add(subPlatform);

                                BigDecimal subMin = subBase.multiply(new BigDecimal("0.8"));
                                BigDecimal subMax = subBase.multiply(new BigDecimal("1.2"));
                                BigDecimal subMinTotal = subMin.add(pricingService.calculatePlatformCharge(subMin));
                                BigDecimal subMaxTotal = subMax.add(pricingService.calculatePlatformCharge(subMax));

                                Booking sub = Booking.builder()
                                                .user(user)
                                                .parentBooking(savedParent)
                                                .isWedding(true)
                                                .vehicleType(type)
                                                .pickupLocation(bookingDto.getPickupLocation())
                                                .dropLocation(bookingDto.getDropLocation())
                                                .pickupLat(bookingDto.getPickupLat())
                                                .pickupLng(bookingDto.getPickupLng())
                                                .dropLat(bookingDto.getDropLat())
                                                .dropLng(bookingDto.getDropLng())
                                                .startDateTime(bookingDto.getStartDateTime())
                                                .endDateTime(bookingDto.getEndDateTime())
                                                .status(BookingStatus.PENDING_PAYMENT)
                                                .tripType(parent.getTripType())
                                                .minEstimatedPrice(subMinTotal.setScale(2, RoundingMode.HALF_UP))
                                                .maxEstimatedPrice(subMaxTotal.setScale(2, RoundingMode.HALF_UP))
                                                .platformCharge(subPlatform)
                                                .paymentRequired(true)
                                                .paymentCompleted(false)
                                                .build();
                                bookingRepository.save(sub);
                        }
                }

                savedParent.setPlatformCharge(totalPlatformCharge);
                savedParent.setStatus(BookingStatus.PENDING_PAYMENT);
                return bookingRepository.save(savedParent);
        }

        @Override
        public void notifyDriversNewBooking(Booking savedBooking) {
                // If this is a parent wedding booking, notify for each sub-booking separately
                if (savedBooking.getIsWedding() != null && savedBooking.getIsWedding()
                                && savedBooking.getParentBooking() == null) {
                        List<Booking> subs = bookingRepository.findByParentBookingId(savedBooking.getId());
                        System.out.println("💒 WEDDING PARENT NOTIFICATION: Notifying for " + subs.size()
                                        + " sub-bookings");
                        for (Booking sub : subs) {
                                notifyDriversNewBooking(sub);
                        }
                        return;
                }

                System.out.println("========================================");
                System.out.println("📨 SENDING WEBSOCKET NOTIFICATION");
                System.out.println("Booking ID: " + savedBooking.getId());
                System.out.println("Vehicle Type: " + savedBooking.getVehicleType());
                System.out.println("========================================");

                // Get all available vehicles of this type
                List<Vehicle> availableVehicles = vehicleRepository.findByType(savedBooking.getVehicleType())
                                .stream()
                                .filter(Vehicle::isAvailable)
                                .toList();

                for (Vehicle vehicle : availableVehicles) {
                        // If there's an assigned driver, notify them. Otherwise notify the owner.
                        Long driverId = (vehicle.getAssignedDriver() != null)
                                        ? vehicle.getAssignedDriver().getId()
                                        : vehicle.getOwner().getId();

                        // SIMPLIFIED FLOW: Only notify drivers with NO active bookings
                        List<Booking> activeBookings = getActiveBookingsForDriver(driverId);
                        if (!activeBookings.isEmpty()) {
                                System.out.println("⏭️  Skipping driver " + driverId
                                                + " - already has " + activeBookings.size() + " active booking(s)");
                                continue;
                        }

                        // Directional logic: Don't notify if driver has "crossed" the pickup
                        com.vehiclebooking.backend.model.DriverLocation loc = driverLocationService
                                        .getDriverLocation(driverId);
                        if (loc != null && loc.getHeading() != null) {
                                double dist = pricingService.calculateDistance(
                                                loc.getLatitude(), loc.getLongitude(),
                                                savedBooking.getPickupLat(), savedBooking.getPickupLng());

                                // If driver is within 500m and moving away from pickup, skip
                                if (dist < 0.5) {
                                        double angleToPickup = calculateAngle(loc.getLatitude(), loc.getLongitude(),
                                                        savedBooking.getPickupLat(), savedBooking.getPickupLng());
                                        double heading = loc.getHeading();
                                        double diff = Math.abs(heading - angleToPickup);
                                        if (diff > 180)
                                                diff = 360 - diff;

                                        // If driver is within a moderate range and moving ALMOST opposite, skip
                                        if (dist > 0.3 && diff > 150) { // Moving away (>150 degree difference)
                                                System.out.println("⏩ Skipping driver " + driverId
                                                                + " - heading is directly away from pickup (" + diff
                                                                + " deg)");
                                                continue;
                                        } else {
                                                System.out.println("✅ Driver " + driverId
                                                                + " heading check passed (diff: " + diff
                                                                + " deg, dist: " + dist + " km)");
                                        }
                                }
                        }

                        System.out.println("📣 NOTIFYING DRIVER " + driverId + " for booking " + savedBooking.getId());
                        messagingTemplate.convertAndSend("/topic/driver/" + driverId + "/bookings",
                                        NotificationMessage.builder()
                                                        .type("NEW_BOOKING")
                                                        .bookingId(savedBooking.getId())
                                                        .message("New booking request for "
                                                                        + savedBooking.getVehicleType())
                                                        .data(savedBooking)
                                                        .build());
                }

                // Also broadcast for backward compatibility or general listeners
                // messagingTemplate.convertAndSend("/topic/bookings", notification);
        }

        private double calculateAngle(double lat1, double lon1, double lat2, double lon2) {
                double dLon = Math.toRadians(lon2 - lon1);
                double y = Math.sin(dLon) * Math.cos(Math.toRadians(lat2));
                double x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) -
                                Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(dLon);
                double brng = Math.atan2(y, x);
                return (Math.toDegrees(brng) + 360) % 360;
        }

        // ... existing methods ...

        @Override
        public List<Booking> getDriverBookings(Long driverId) {
                return bookingRepository.findByDriverId(driverId);
        }

        @Override
        public Booking acceptBooking(Long bookingId) {
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
                booking.setStatus(BookingStatus.ACCEPTED);
                return bookingRepository.save(booking);
        }

        @Override
        public Booking completeBooking(Long bookingId) {
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

                // Ensure booking is in RIDE_STARTED state
                if (booking.getStatus() != BookingStatus.RIDE_STARTED) {
                        throw new RuntimeException(
                                        "Cannot complete ride. Booking must be in RIDE_STARTED state. Current status: "
                                                        + booking.getStatus());
                }

                booking.setStatus(BookingStatus.COMPLETED);

                // Recalculate distance to ensure accuracy (handling same-location testing)
                double actualDistance = pricingService.calculateDistance(
                                booking.getPickupLat(), booking.getPickupLng(),
                                booking.getDropLat(), booking.getDropLng());

                System.out.println("========================================");
                System.out.println("💰 FARE CALCULATION DEBUG");
                System.out.println("Booking ID: " + booking.getId());
                System.out.println("Pickup: (" + booking.getPickupLat() + ", " + booking.getPickupLng() + ")");
                System.out.println("Drop: (" + booking.getDropLat() + ", " + booking.getDropLng() + ")");
                System.out.println("Calculated Distance: " + actualDistance + " km");
                System.out.println("Trip Type: " + booking.getTripType());
                System.out.println("Vehicle Type: " + booking.getVehicleType());
                System.out.println("Passenger Count: " + booking.getPassengerCount());
                System.out.println("Is Shared: " + booking.getIsShared());
                System.out.println("Vehicle Price Per Km: ₹" + booking.getVehicle().getPricePerKm());
                System.out.println("Initial Estimate: ₹" + booking.getMinEstimatedPrice() + " - ₹"
                                + booking.getMaxEstimatedPrice());

                // Recalculate based on actual distance and correct pricing logic
                BigDecimal vehicleCharge = pricingService.calculateActualPrice(
                                actualDistance,
                                booking.getPassengerCount(),
                                booking.getVehicle().getPricePerKm(),
                                booking.getTripType(),
                                booking.getVehicleType(),
                                booking.getIsShared());

                System.out.println("Calculated Vehicle Charge: ₹" + vehicleCharge);

                // If daily/hourly, add logic here. Using simple Km logic for now.
                if (vehicleCharge.compareTo(BigDecimal.ZERO) == 0 && booking.getVehicle().getPricePerDay() != null) {
                        vehicleCharge = booking.getVehicle().getPricePerDay(); // fallback to 1 day charge
                        System.out.println("Using daily rate fallback: ₹" + vehicleCharge);
                }

                // Recalculate Platform Charge based on Actual Vehicle Charge
                BigDecimal platformCharge = pricingService.calculatePlatformCharge(vehicleCharge);
                booking.setPlatformCharge(platformCharge);

                booking.setRemainingAmount(vehicleCharge);
                booking.setTotalAmount(platformCharge.add(vehicleCharge));

                System.out.println("Platform Charge (15%): ₹" + platformCharge);
                System.out.println("Total Amount: ₹" + platformCharge.add(vehicleCharge));
                System.out.println("========================================");

                // PLATFORM COMMISSION DEBIT (15% from Driver Wallet)
                BigDecimal commission = platformCharge;
                User driver = booking.getVehicle().getOwner();

                if (driver.getWalletBalance().compareTo(commission) < 0) {
                        System.err.println("⚠️ CRITICAL: Driver " + driver.getFullName()
                                        + " has insufficient balance at completion!");
                }

                // Deduct from wallet
                driver.setWalletBalance(driver.getWalletBalance().subtract(commission));
                userRepository.save(driver);

                // Record transaction
                Transaction debitTx = Transaction.builder()
                                .user(driver)
                                .booking(booking)
                                .amount(commission)
                                .type("DRIVER")
                                .transactionCategory("DEBIT")
                                .status(PaymentStatus.SUCCESS)
                                .description("Platform Commission (15%) for Booking #" + booking.getId())
                                .build();
                transactionRepository.save(debitTx);

                Booking completedBooking = bookingRepository.save(booking);

                System.out.println("========================================");
                System.out.println("✅ RIDE COMPLETED");
                System.out.println("Booking ID: " + completedBooking.getId());
                System.out.println("Customer: " + completedBooking.getUser().getFullName());
                System.out.println("Total Amount: " + completedBooking.getTotalAmount());
                System.out.println("========================================");

                // Notify customer that ride is completed
                NotificationMessage notification = NotificationMessage.builder()
                                .type("RIDE_COMPLETED")
                                .bookingId(completedBooking.getId())
                                .message("Your ride has been completed!")
                                .data(completedBooking)
                                .build();

                messagingTemplate.convertAndSend("/topic/booking/" + completedBooking.getId(), notification);
                System.out.println("✅ Ride completion notification sent to /topic/booking/" + completedBooking.getId());

                // Notify driver via WebSocket for real-time dashboard update
                if (completedBooking.getVehicle() != null) {
                        Long driverId = completedBooking.getVehicle().getOwner().getId();
                        NotificationMessage driverNotification = NotificationMessage.builder()
                                        .type("DRIVER_RIDE_COMPLETED")
                                        .bookingId(completedBooking.getId())
                                        .message("Ride completed for booking #" + completedBooking.getId())
                                        .data(completedBooking)
                                        .build();
                        messagingTemplate.convertAndSend("/topic/driver/" + driverId + "/updates", driverNotification);
                        System.out.println("✅ Driver notification sent to /topic/driver/" + driverId + "/updates");
                }

                return completedBooking;
        }

        @Override
        public List<Booking> getUserBookings(Long userId) {
                return bookingRepository.findByUserId(userId);
        }

        @Override
        public Booking getBookingById(Long bookingId) {
                return bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Booking not found with ID: " + bookingId));
        }

        @Override
        public List<Booking> getPendingBookingsForDriver(Long driverId) {
                // Get driver's vehicle to know their vehicle type and capacity
                Vehicle driverVehicle = vehicleRepository.findByOwnerIdOrAssignedDriverId(driverId, driverId)
                                .stream()
                                .findFirst()
                                .orElse(null);

                if (driverVehicle == null) {
                        return List.of();
                }

                // Get all PENDING bookings that match driver's vehicle type AND capacity
                return bookingRepository.findAll().stream()
                                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                                .filter(b -> b.getVehicle() == null) // Not yet assigned to a specific vehicle
                                .filter(b -> b.getParentBooking() == null || b.getIsWedding()) // Wedding sub-bookings
                                                                                               // are allowed
                                .filter(b -> b.getVehicleType() == driverVehicle.getType())
                                .filter(b -> b.getPassengerCount() <= driverVehicle.getCapacity()) // Check capacity
                                .toList();
        }

        @Override
        public Booking acceptBookingByDriver(Long bookingId, Long driverId) {
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

                // Get driver's vehicle that matches the booking's vehicle type AND has
                // sufficient capacity
                Vehicle driverVehicle = vehicleRepository.findByOwnerIdOrAssignedDriverId(driverId, driverId)
                                .stream()
                                .filter(Vehicle::isAvailable)
                                .filter(vehicle -> vehicle.getType() == booking.getVehicleType())
                                .filter(vehicle -> vehicle.getCapacity() >= booking.getPassengerCount()) // Check
                                                                                                         // capacity
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException(
                                                "No available " + booking.getVehicleType()
                                                                + " vehicle with capacity for " +
                                                                booking.getPassengerCount() + " passengers found"));

                // SIMPLIFIED FLOW: Only one booking at a time per driver
                List<Booking> currentRides = getActiveBookingsForDriver(driverId);
                if (!currentRides.isEmpty()) {
                        throw new RuntimeException("Please complete your current ride before accepting a new one.");
                }

                // Calculate actual price based on vehicle's price per km and sharing status
                BigDecimal actualPrice = pricingService.calculateActualPrice(
                                booking.getEstimatedDistance(),
                                booking.getPassengerCount(),
                                driverVehicle.getPricePerKm(),
                                booking.getTripType(),
                                booking.getVehicleType(),
                                booking.getIsShared());

                // Calculate platform charge and total amount (User pays = Driver Fare +
                // Platform Charge)
                BigDecimal platformCharge = pricingService.calculatePlatformCharge(actualPrice);
                BigDecimal totalAmount = actualPrice.add(platformCharge);
                BigDecimal remainingAmount = actualPrice; // Driver keeps the actual vehicle fare

                // Assign vehicle, set actual price, and update status
                booking.setVehicle(driverVehicle);
                booking.setActualPrice(actualPrice); // Driver Fare
                booking.setTotalAmount(totalAmount); // User Pays
                booking.setPlatformCharge(platformCharge);
                booking.setRemainingAmount(remainingAmount);

                // Wallet Logic: Check Eligibility (Only for non-wedding/non-prebook rides)
                // Drivers must have at least 15% of max estimate in wallet to accept
                if (booking.getIsWedding() == null || !booking.getIsWedding()) {
                        BigDecimal requiredBalance = booking.getMaxEstimatedPrice().multiply(new BigDecimal("0.15"));
                        User driver = driverVehicle.getOwner();
                        if (driver.getWalletBalance().compareTo(requiredBalance) < 0) {
                                throw new RuntimeException(
                                                "Insufficient wallet balance. You need at least ₹"
                                                                + requiredBalance.setScale(0, RoundingMode.HALF_UP)
                                                                + " to accept this ride.");
                        }
                        System.out.println("💳 WALLET CHECK - Driver " + driver.getFullName()
                                        + " is eligible (Balance: ₹" + driver.getWalletBalance() + ")");
                }

                booking.setStatus(BookingStatus.ACCEPTED);

                Booking savedBooking = bookingRepository.save(booking);

                // Create passenger verification record for the primary passenger
                // For shared rides, this will track individual passenger pickups/drops
                // Use the existing booking OTP for compatibility with frontend
                com.vehiclebooking.backend.model.PassengerVerification verification = com.vehiclebooking.backend.model.PassengerVerification
                                .builder()
                                .booking(savedBooking)
                                .passenger(savedBooking.getUser())
                                .pickupOtp(savedBooking.getRideOtp()) // Use existing booking OTP
                                .dropOtp(savedBooking.getRideOtp()) // Use same OTP for pickup and drop
                                .pickupVerified(false)
                                .dropVerified(false)
                                .build();
                passengerVerificationRepository.save(verification);

                System.out.println("✅ Created passenger verification record for passenger: "
                                + savedBooking.getUser().getFullName());
                System.out.println("✅ Using booking OTP: " + savedBooking.getRideOtp());

                // If this is a wedding sub-booking, check if parent should be updated
                if (savedBooking.getParentBooking() != null) {
                        checkWeddingParentStatus(savedBooking.getParentBooking());
                }

                // Notify customer that booking was accepted
                System.out.println("========================================");
                System.out.println("📨 SENDING BOOKING ACCEPTED NOTIFICATION");
                System.out.println("Booking ID: " + savedBooking.getId());
                System.out.println("Customer ID: " + savedBooking.getUser().getId());
                System.out.println("Driver: " + driverVehicle.getOwner().getFullName());
                System.out.println("Vehicle: " + driverVehicle.getModel() + " (Capacity: " + driverVehicle.getCapacity()
                                + ")");
                System.out.println("Actual Price: ₹" + savedBooking.getActualPrice());
                System.out.println("Platform Charge: ₹" + savedBooking.getPlatformCharge());
                System.out.println("Driver receives: ₹" + savedBooking.getRemainingAmount());
                System.out.println("========================================");

                NotificationMessage notification = NotificationMessage.builder()
                                .type("BOOKING_ACCEPTED")
                                .bookingId(savedBooking.getId())
                                .message("Your " + (savedBooking.getIsWedding() ? "wedding sub-booking" : "booking")
                                                + " has been accepted!")
                                .data(savedBooking)
                                .build();

                // For wedding, notify parent topic too
                if (savedBooking.getParentBooking() != null) {
                        messagingTemplate.convertAndSend("/topic/booking/" + savedBooking.getParentBooking().getId(),
                                        notification);
                }
                messagingTemplate.convertAndSend("/topic/booking/" + savedBooking.getId(), notification);

                // Notify the driver who accepted via WebSocket for dashboard update
                NotificationMessage driverUpdateNotification = NotificationMessage.builder()
                                .type("DRIVER_BOOKING_ACCEPTED")
                                .bookingId(savedBooking.getId())
                                .message("You have successfully accepted the booking")
                                .data(savedBooking)
                                .build();

                messagingTemplate.convertAndSend("/topic/driver/" + driverId + "/updates", driverUpdateNotification);
                System.out.println("✅ Sent dashboard update to driver " + driverId);

                // Notify all OTHER drivers that this booking is no longer available
                NotificationMessage driverNotification = NotificationMessage.builder()
                                .type("BOOKING_NO_LONGER_AVAILABLE")
                                .bookingId(savedBooking.getId())
                                .message("This booking has been accepted by another driver")
                                .build();

                messagingTemplate.convertAndSend("/topic/bookings", driverNotification);
                return savedBooking;
        }

        private void checkWeddingParentStatus(Booking parent) {
                List<Booking> subs = bookingRepository.findByParentBookingId(parent.getId());

                boolean allAccepted = subs.stream().allMatch(b -> b.getStatus() == BookingStatus.ACCEPTED
                                || b.getStatus() == BookingStatus.COMPLETED);

                if (allAccepted) {
                        parent.setStatus(BookingStatus.ACCEPTED);
                        bookingRepository.save(parent);

                        NotificationMessage msg = NotificationMessage.builder()
                                        .type("WEDDING_FLEET_FULL")
                                        .bookingId(parent.getId())
                                        .message("All vehicles in your wedding fleet have been booked!")
                                        .data(parent)
                                        .build();
                        messagingTemplate.convertAndSend("/topic/booking/" + parent.getId(), msg);
                }
        }

        @Override
        public Booking rejectBooking(Long bookingId) {
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

                booking.setStatus(BookingStatus.CANCELLED);
                return bookingRepository.save(booking);
        }

        @Override
        public Booking cancelAcceptedBooking(Long bookingId, Long driverId) {
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

                // Verify booking is in ACCEPTED status
                if (booking.getStatus() != BookingStatus.ACCEPTED) {
                        throw new RuntimeException("Cannot cancel. Booking is not in ACCEPTED status.");
                }

                // Verify the driver cancelling is the one who accepted
                if (booking.getVehicle() == null ||
                                !booking.getVehicle().getOwner().getId().equals(driverId)) {
                        throw new RuntimeException("Only the driver who accepted can cancel this booking");
                }

                System.out.println("========================================");
                System.out.println("🔄 DRIVER CANCELLING ACCEPTED BOOKING");
                System.out.println("Booking ID: " + booking.getId());
                System.out.println("Driver ID: " + driverId);
                System.out.println("========================================");

                // Reset booking to PENDING status
                booking.setStatus(BookingStatus.PENDING);
                booking.setVehicle(null);
                booking.setActualPrice(null);
                booking.setTotalAmount(null);
                booking.setPlatformCharge(null);
                booking.setRemainingAmount(null);

                Booking savedBooking = bookingRepository.save(booking);

                // Re-notify all drivers EXCEPT the one who cancelled
                NotificationMessage reNotification = NotificationMessage.builder()
                                .type("NEW_BOOKING")
                                .bookingId(savedBooking.getId())
                                .excludeDriverId(driverId)
                                .message("Booking available again")
                                .data(savedBooking)
                                .build();

                messagingTemplate.convertAndSend("/topic/bookings", reNotification);
                System.out.println("✅ Re-notified all drivers (excluding driver ID: " + driverId + ")");

                // If wedding, update parent status back to PENDING if it was ACCEPTED
                if (savedBooking.getParentBooking() != null) {
                        Booking parent = savedBooking.getParentBooking();
                        if (parent.getStatus() == BookingStatus.ACCEPTED) {
                                parent.setStatus(BookingStatus.PENDING);
                                bookingRepository.save(parent);
                        }

                        NotificationMessage weddingNotify = NotificationMessage.builder()
                                        .type("WEDDING_SUB_CANCELLED")
                                        .bookingId(parent.getId())
                                        .message("A driver cancelled one vehicle in your fleet. Finding a replacement...")
                                        .data(savedBooking)
                                        .build();
                        messagingTemplate.convertAndSend("/topic/booking/" + parent.getId(), weddingNotify);
                }

                // Also notify customer that booking was cancelled by driver
                NotificationMessage customerNotification = NotificationMessage.builder()
                                .type("BOOKING_CANCELLED_BY_DRIVER")
                                .bookingId(savedBooking.getId())
                                .message("Driver cancelled the booking. Finding another driver...")
                                .data(savedBooking)
                                .build();

                messagingTemplate.convertAndSend("/topic/booking/" + savedBooking.getId(), customerNotification);
                System.out.println("✅ Notified customer about cancellation");

                return savedBooking;
        }

        @Override
        public Booking startRide(Long bookingId, String otpInput) {
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

                // Ensure booking is in ACCEPTED state
                if (booking.getStatus() != BookingStatus.ACCEPTED) {
                        throw new RuntimeException("Cannot start ride. Booking must be in ACCEPTED state.");
                }

                // Check if this is a shared ride with passenger verifications
                List<com.vehiclebooking.backend.model.PassengerVerification> verifications = passengerVerificationRepository
                                .findByBookingId(bookingId);

                if (!verifications.isEmpty()) {
                        // This is a shared ride - use passenger verification system
                        System.out.println("========================================");
                        System.out.println("📋 SHARED RIDE - Using Passenger Verification");
                        System.out.println("Booking ID: " + booking.getId());
                        System.out.println("Total Passengers: " + verifications.size());
                        System.out.println("========================================");

                        // Verify pickup for the primary passenger of this booking
                        return verifyPickupOtp(bookingId, booking.getUser().getId(), otpInput);
                }

                // Legacy flow: Single passenger, non-shared ride
                // Verify OTP
                if (otpInput == null || !otpInput.equals(booking.getRideOtp())) {
                        System.out.println("========================================");
                        System.out.println("❌ OTP VERIFICATION FAILED");
                        System.out.println("Booking ID: " + booking.getId());
                        System.out.println("Expected OTP: " + booking.getRideOtp());
                        System.out.println("Received OTP: " + otpInput);
                        System.out.println("========================================");
                        throw new RuntimeException("Invalid OTP. Please check the OTP with the customer.");
                }

                System.out.println("========================================");
                System.out.println("🚗 RIDE STARTED - OTP VERIFIED (Legacy Single Passenger)");
                System.out.println("Booking ID: " + booking.getId());
                System.out.println("Customer: " + booking.getUser().getFullName());
                System.out.println("OTP: " + booking.getRideOtp());
                System.out.println("========================================");

                // Update status to RIDE_STARTED
                booking.setStatus(BookingStatus.RIDE_STARTED);
                booking = bookingRepository.save(booking);

                // Notify customer that ride has started
                NotificationMessage notification = NotificationMessage.builder()
                                .type("RIDE_STARTED")
                                .bookingId(booking.getId())
                                .message("Your ride has started!")
                                .data(booking)
                                .build();

                messagingTemplate.convertAndSend("/topic/booking/" + booking.getId(), notification);
                System.out.println("✅ Ride started notification sent to /topic/booking/" + booking.getId());

                // Notify driver via WebSocket for real-time dashboard update
                if (booking.getVehicle() != null) {
                        Long driverId = booking.getVehicle().getOwner().getId();
                        NotificationMessage driverNotification = NotificationMessage.builder()
                                        .type("DRIVER_RIDE_STARTED")
                                        .bookingId(booking.getId())
                                        .message("Ride started for booking #" + booking.getId())
                                        .data(booking)
                                        .build();
                        messagingTemplate.convertAndSend("/topic/driver/" + driverId + "/updates", driverNotification);
                        System.out.println("✅ Driver notification sent to /topic/driver/" + driverId + "/updates");
                }

                return booking;
        }

        @Override
        public Booking getActiveBookingForUser(Long userId) {
                // Find active booking (PENDING, PENDING_PAYMENT, ACCEPTED, or RIDE_STARTED)
                List<Booking> bookings = bookingRepository.findByUserId(userId);
                return bookings.stream()
                                .filter(b -> b.getStatus() == BookingStatus.ACCEPTED ||
                                                b.getStatus() == BookingStatus.PENDING ||
                                                b.getStatus() == BookingStatus.PENDING_PAYMENT ||
                                                b.getStatus() == BookingStatus.RIDE_STARTED)
                                .findFirst()
                                .orElse(null);
        }

        @Override
        public List<Booking> getActiveBookingsForDriver(Long driverId) {
                return bookingRepository.findAll().stream()
                                .filter(b -> b.getStatus() == BookingStatus.ACCEPTED ||
                                                b.getStatus() == BookingStatus.RIDE_STARTED)
                                .filter(b -> b.getVehicle() != null)
                                .filter(b -> (b.getVehicle().getOwner() != null
                                                && b.getVehicle().getOwner().getId().equals(driverId)) ||
                                                (b.getVehicle().getAssignedDriver() != null && b.getVehicle()
                                                                .getAssignedDriver().getId().equals(driverId)))
                                .toList();
        }

        @Override
        public List<com.vehiclebooking.backend.dto.DriverTaskDto> getDriverTasks(Long driverId) {
                List<Booking> activeBookings = getActiveBookingsForDriver(driverId);
                List<com.vehiclebooking.backend.dto.DriverTaskDto> tasks = new java.util.ArrayList<>();

                for (Booking b : activeBookings) {
                        // Task 1: Pickup
                        tasks.add(com.vehiclebooking.backend.dto.DriverTaskDto.builder()
                                        .type("PICKUP")
                                        .location(b.getPickupLocation())
                                        .latitude(b.getPickupLat())
                                        .longitude(b.getPickupLng())
                                        .bookingId(b.getId())
                                        .customerName(b.getUser().getFullName())
                                        .customerPhone(b.getUser().getPhoneNumber())
                                        .otp(b.getRideOtp())
                                        .status("PENDING")
                                        .build());

                        // Support for drops to passengers (BookingStops)
                        if (b.getStops() != null) {
                                for (com.vehiclebooking.backend.model.BookingStop stop : b.getStops()) {
                                        tasks.add(com.vehiclebooking.backend.dto.DriverTaskDto.builder()
                                                        .type("DROP")
                                                        .location(stop.getLocation())
                                                        .latitude(stop.getLatitude())
                                                        .longitude(stop.getLongitude())
                                                        .bookingId(b.getId())
                                                        .customerName(b.getUser().getFullName() + " (Pass)")
                                                        .status("PENDING")
                                                        .build());
                                }
                        }

                        // Task: Destination Drop
                        tasks.add(com.vehiclebooking.backend.dto.DriverTaskDto.builder()
                                        .type("DROP")
                                        .location(b.getDropLocation())
                                        .latitude(b.getDropLat())
                                        .longitude(b.getDropLng())
                                        .bookingId(b.getId())
                                        .customerName(b.getUser().getFullName())
                                        .status("PENDING")
                                        .build());
                }
                return tasks;
        }

        @Override
        public Booking cancelBooking(Long bookingId) {
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

                // Allow cancellation for PENDING, PENDING_PAYMENT, and ACCEPTED bookings
                if (booking.getStatus() != BookingStatus.PENDING &&
                                booking.getStatus() != BookingStatus.PENDING_PAYMENT &&
                                booking.getStatus() != BookingStatus.ACCEPTED) {
                        throw new IllegalStateException(
                                        "Booking cannot be cancelled in current status: " + booking.getStatus());
                }

                booking.setStatus(BookingStatus.CANCELLED);
                Booking cancelledBooking = bookingRepository.save(booking);

                // If this is a wedding parent booking, cancel all sub-bookings
                if (booking.getIsWedding() != null && booking.getIsWedding() && booking.getParentBooking() == null) {
                        List<Booking> subs = bookingRepository.findByParentBookingId(booking.getId());
                        for (Booking sub : subs) {
                                sub.setStatus(BookingStatus.CANCELLED);
                                bookingRepository.save(sub);
                                // Also notify for each sub-booking
                                messagingTemplate.convertAndSend("/topic/booking/" + sub.getId(),
                                                Map.of("type", "BOOKING_CANCELLED", "data", sub));
                        }
                }

                // Process refund if payment was completed (70% refund of platform charge)
                BigDecimal refundAmount = BigDecimal.ZERO;
                if (booking.getPaymentCompleted() != null && booking.getPaymentCompleted()) {
                        refundAmount = booking.getPlatformCharge()
                                        .multiply(new BigDecimal("0.70"))
                                        .setScale(2, RoundingMode.HALF_UP);

                        System.out.println("💰 REFUND INITIATED");
                        System.out.println("Platform Charge: ₹" + booking.getPlatformCharge());
                        System.out.println("Refund Amount (70%): ₹" + refundAmount);
                        System.out.println(
                                        "NOTE: Refund will be processed to original payment method within 5-7 business days");

                        // TODO: Integrate with Razorpay refund API
                        // razorpayClient.payments.refund(paymentId, refundAmount);
                }

                System.out.println("========================================");
                System.out.println("❌ BOOKING CANCELLED");
                System.out.println("Booking ID: " + bookingId);
                System.out.println("Customer: " + booking.getUser().getFullName());
                if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                        System.out.println("Refund: ₹" + refundAmount);
                }
                System.out.println("========================================");

                // Notify via WebSocket
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "BOOKING_CANCELLED");
                notification.put("data", cancelledBooking);
                notification.put("refundAmount", refundAmount);
                messagingTemplate.convertAndSend("/topic/booking/" + bookingId, notification);

                // Notify assigned driver via private topic
                if (cancelledBooking.getVehicle() != null) {
                        Long driverId = null;
                        if (cancelledBooking.getVehicle().getAssignedDriver() != null) {
                                driverId = cancelledBooking.getVehicle().getAssignedDriver().getId();
                        } else if (cancelledBooking.getVehicle().getOwner() != null) {
                                driverId = cancelledBooking.getVehicle().getOwner().getId();
                        }

                        if (driverId != null) {
                                System.out.println("📨 NOTIFYING DRIVER: " + driverId + " of CANCELLATION");
                                messagingTemplate.convertAndSend("/topic/driver/" + driverId + "/updates",
                                                notification);
                        }
                }

                return cancelledBooking;
        }

        @Override
        public List<Booking> getSubBookings(Long parentId) {
                return bookingRepository.findByParentBookingId(parentId);
        }

        @Override
        public Booking verifyPickupOtp(Long bookingId, Long passengerId, String otpInput) {
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

                // Find the passenger verification record
                com.vehiclebooking.backend.model.PassengerVerification verification = passengerVerificationRepository
                                .findByBookingIdAndPassengerId(bookingId, passengerId)
                                .orElseThrow(() -> new RuntimeException("Passenger verification record not found"));

                // Verify pickup OTP
                if (otpInput == null || !otpInput.equals(verification.getPickupOtp())) {
                        System.out.println("========================================");
                        System.out.println("❌ PICKUP OTP VERIFICATION FAILED");
                        System.out.println("Booking ID: " + bookingId);
                        System.out.println("Passenger ID: " + passengerId);
                        System.out.println("Expected OTP: " + verification.getPickupOtp());
                        System.out.println("Received OTP: " + otpInput);
                        System.out.println("========================================");
                        throw new RuntimeException("Invalid pickup OTP for this passenger.");
                }

                System.out.println("========================================");
                System.out.println("✅ PASSENGER PICKED UP - OTP VERIFIED");
                System.out.println("Booking ID: " + bookingId);
                System.out.println("Passenger: " + verification.getPassenger().getFullName());
                System.out.println("========================================");

                // Mark pickup as verified
                verification.setPickupVerified(true);
                verification.setPickupVerifiedAt(java.time.LocalDateTime.now());
                passengerVerificationRepository.save(verification);

                // Determine all related bookings (for shared routes)
                List<Booking> relatedBookings = new java.util.ArrayList<>();

                if (booking.getParentBooking() != null) {
                        // This is a sub-booking, get all siblings
                        relatedBookings = booking.getParentBooking().getSubBookings();
                        System.out.println("📋 Shared Route Detected - Parent Booking ID: "
                                        + booking.getParentBooking().getId());
                        System.out.println("📋 Total Related Bookings: " + relatedBookings.size());
                } else if (!booking.getSubBookings().isEmpty()) {
                        // This is a parent booking with sub-bookings
                        relatedBookings = booking.getSubBookings();
                        relatedBookings.add(booking); // Include parent itself
                        System.out.println("📋 Parent Booking with Sub-Bookings");
                        System.out.println("📋 Total Related Bookings: " + relatedBookings.size());
                } else {
                        // Single standalone booking
                        relatedBookings.add(booking);
                        System.out.println("📋 Standalone Booking (No Shared Route)");
                }

                // Check if ALL passengers across ALL related bookings have been picked up
                boolean allPickedUp = true;
                int totalPassengers = 0;
                int pickedUpPassengers = 0;

                for (Booking relatedBooking : relatedBookings) {
                        List<com.vehiclebooking.backend.model.PassengerVerification> bookingVerifications = passengerVerificationRepository
                                        .findByBookingId(relatedBooking.getId());

                        totalPassengers += bookingVerifications.size();
                        pickedUpPassengers += (int) bookingVerifications.stream()
                                        .filter(com.vehiclebooking.backend.model.PassengerVerification::getPickupVerified)
                                        .count();

                        // If any passenger in any booking is not picked up, set flag to false
                        if (!bookingVerifications.stream()
                                        .allMatch(com.vehiclebooking.backend.model.PassengerVerification::getPickupVerified)) {
                                allPickedUp = false;
                        }
                }

                System.out.println("========================================");
                System.out.println("📊 PICKUP PROGRESS");
                System.out.println("Picked Up: " + pickedUpPassengers + "/" + totalPassengers + " passengers");
                System.out.println("All Passengers Picked Up: " + allPickedUp);
                System.out.println("========================================");

                // If all passengers across all bookings are picked up, start the ride
                if (allPickedUp) {
                        System.out.println("🚗 ALL PASSENGERS PICKED UP - STARTING RIDE");

                        // Update ALL related bookings to RIDE_STARTED
                        for (Booking relatedBooking : relatedBookings) {
                                if (relatedBooking.getStatus() == BookingStatus.ACCEPTED) {
                                        relatedBooking.setStatus(BookingStatus.RIDE_STARTED);
                                        Booking savedBooking = bookingRepository.save(relatedBooking);

                                        // Notify customer for this booking
                                        NotificationMessage customerNotification = NotificationMessage.builder()
                                                        .type("RIDE_STARTED")
                                                        .bookingId(savedBooking.getId())
                                                        .message("All passengers picked up. Ride started!")
                                                        .data(savedBooking)
                                                        .build();

                                        messagingTemplate.convertAndSend("/topic/booking/" + savedBooking.getId(),
                                                        customerNotification);
                                        System.out.println("✅ Notified customer for booking #" + savedBooking.getId());
                                }
                        }

                        // Notify driver (only once)
                        if (booking.getVehicle() != null) {
                                Long driverId = booking.getVehicle().getOwner().getId();
                                NotificationMessage driverNotification = NotificationMessage.builder()
                                                .type("DRIVER_RIDE_STARTED")
                                                .bookingId(booking.getId())
                                                .message("All passengers picked up. Ride started!")
                                                .data(booking)
                                                .build();

                                messagingTemplate.convertAndSend("/topic/driver/" + driverId + "/updates",
                                                driverNotification);
                                System.out.println("✅ Driver notification sent");
                        }
                }

                return bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        }

        @Override
        public Booking verifyDropOtp(Long bookingId, Long passengerId, String otpInput) {
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

                // Find the passenger verification record
                com.vehiclebooking.backend.model.PassengerVerification verification = passengerVerificationRepository
                                .findByBookingIdAndPassengerId(bookingId, passengerId)
                                .orElseThrow(() -> new RuntimeException("Passenger verification record not found"));

                // Verify drop OTP
                if (otpInput == null || !otpInput.equals(verification.getDropOtp())) {
                        System.out.println("========================================");
                        System.out.println("❌ DROP OTP VERIFICATION FAILED");
                        System.out.println("Booking ID: " + bookingId);
                        System.out.println("Passenger ID: " + passengerId);
                        System.out.println("Expected OTP: " + verification.getDropOtp());
                        System.out.println("Received OTP: " + otpInput);
                        System.out.println("========================================");
                        throw new RuntimeException("Invalid drop OTP for this passenger.");
                }

                System.out.println("========================================");
                System.out.println("✅ PASSENGER DROPPED - OTP VERIFIED");
                System.out.println("Booking ID: " + bookingId);
                System.out.println("Passenger: " + verification.getPassenger().getFullName());
                System.out.println("========================================");

                // Mark drop as verified
                verification.setDropVerified(true);
                verification.setDropVerifiedAt(java.time.LocalDateTime.now());
                passengerVerificationRepository.save(verification);

                // Determine all related bookings (for shared routes)
                List<Booking> relatedBookings = new java.util.ArrayList<>();

                if (booking.getParentBooking() != null) {
                        // This is a sub-booking, get all siblings
                        relatedBookings = booking.getParentBooking().getSubBookings();
                        System.out.println("📋 Shared Route Detected - Parent Booking ID: "
                                        + booking.getParentBooking().getId());
                        System.out.println("📋 Total Related Bookings: " + relatedBookings.size());
                } else if (!booking.getSubBookings().isEmpty()) {
                        // This is a parent booking with sub-bookings
                        relatedBookings = booking.getSubBookings();
                        relatedBookings.add(booking); // Include parent itself
                        System.out.println("📋 Parent Booking with Sub-Bookings");
                        System.out.println("📋 Total Related Bookings: " + relatedBookings.size());
                } else {
                        // Single standalone booking
                        relatedBookings.add(booking);
                        System.out.println("📋 Standalone Booking (No Shared Route)");
                }

                // Check if ALL passengers across ALL related bookings have been dropped
                boolean allDropped = true;
                int totalPassengers = 0;
                int droppedPassengers = 0;

                for (Booking relatedBooking : relatedBookings) {
                        List<com.vehiclebooking.backend.model.PassengerVerification> bookingVerifications = passengerVerificationRepository
                                        .findByBookingId(relatedBooking.getId());

                        totalPassengers += bookingVerifications.size();
                        droppedPassengers += (int) bookingVerifications.stream()
                                        .filter(com.vehiclebooking.backend.model.PassengerVerification::getDropVerified)
                                        .count();

                        // If any passenger in any booking is not dropped, set flag to false
                        if (!bookingVerifications.stream()
                                        .allMatch(com.vehiclebooking.backend.model.PassengerVerification::getDropVerified)) {
                                allDropped = false;
                        }
                }

                System.out.println("========================================");
                System.out.println("📊 DROP PROGRESS");
                System.out.println("Dropped: " + droppedPassengers + "/" + totalPassengers + " passengers");
                System.out.println("All Passengers Dropped: " + allDropped);
                System.out.println("========================================");

                // If all passengers across all bookings are dropped, complete ALL bookings
                if (allDropped) {
                        System.out.println("🏁 ALL PASSENGERS DROPPED - COMPLETING RIDE");

                        // Complete ALL related bookings
                        for (Booking relatedBooking : relatedBookings) {
                                if (relatedBooking.getStatus() == BookingStatus.RIDE_STARTED) {
                                        completeBooking(relatedBooking.getId());
                                        System.out.println("✅ Completed booking #" + relatedBooking.getId());
                                }
                        }
                }

                return bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        }

        @Override
        public List<com.vehiclebooking.backend.dto.PassengerVerificationDto> getPassengerVerifications(Long bookingId) {
                List<com.vehiclebooking.backend.model.PassengerVerification> verifications = passengerVerificationRepository
                                .findByBookingId(bookingId);

                return verifications.stream()
                                .map(v -> com.vehiclebooking.backend.dto.PassengerVerificationDto.builder()
                                                .id(v.getId())
                                                .bookingId(v.getBooking().getId())
                                                .passengerId(v.getPassenger().getId())
                                                .passengerName(v.getPassenger().getFullName())
                                                .passengerPhone(v.getPassenger().getPhoneNumber())
                                                .pickupOtp(v.getPickupOtp())
                                                .dropOtp(v.getDropOtp())
                                                .pickupVerified(v.getPickupVerified())
                                                .dropVerified(v.getDropVerified())
                                                .pickupLocation(v.getBooking().getPickupLocation())
                                                .dropLocation(v.getBooking().getDropLocation())
                                                .build())
                                .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public com.vehiclebooking.backend.dto.DriverEarningsDto getDriverEarnings(Long driverId) {
                // Get driver user to access wallet balance
                com.vehiclebooking.backend.model.User driver = userRepository.findById(driverId)
                                .orElseThrow(() -> new com.vehiclebooking.backend.exception.ResourceNotFoundException(
                                                "Driver not found"));

                // Calculate start and end of today (in system timezone)
                java.time.LocalDateTime startOfToday = java.time.LocalDate.now().atStartOfDay();
                java.time.LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

                // Calculate start of current month
                java.time.LocalDateTime startOfMonth = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
                java.time.LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);

                // Get earnings from repository
                java.math.BigDecimal dailyEarnings = bookingRepository.calculateDriverEarnings(
                                driverId, startOfToday, startOfTomorrow);
                java.math.BigDecimal monthlyEarnings = bookingRepository.calculateDriverEarnings(
                                driverId, startOfMonth, startOfNextMonth);

                System.out.println("========================================");
                System.out.println("💰 DRIVER EARNINGS CALCULATED");
                System.out.println("Driver ID: " + driverId);
                System.out.println("Today's Earnings: ₹" + dailyEarnings);
                System.out.println("Monthly Earnings: ₹" + monthlyEarnings);
                System.out.println("Wallet Balance: ₹" + driver.getWalletBalance());
                System.out.println("========================================");

                return com.vehiclebooking.backend.dto.DriverEarningsDto.builder()
                                .dailyEarnings(dailyEarnings)
                                .monthlyEarnings(monthlyEarnings)
                                .walletBalance(driver.getWalletBalance())
                                .period("TODAY") // Default to showing today
                                .build();
        }
}
