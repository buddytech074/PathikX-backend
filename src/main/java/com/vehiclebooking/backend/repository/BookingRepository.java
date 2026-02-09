package com.vehiclebooking.backend.repository;

import com.vehiclebooking.backend.model.Booking;
import com.vehiclebooking.backend.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
        @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.subBookings WHERE b.user.id = :userId")
        List<Booking> findByUserId(@Param("userId") Long userId);

        @Query("SELECT b FROM Booking b WHERE b.parentBooking.id = :parentId")
        List<Booking> findByParentBookingId(@Param("parentId") Long parentId);

        @Query("SELECT b FROM Booking b WHERE b.vehicle.owner.id = :driverId")
        List<Booking> findByDriverId(@Param("driverId") Long driverId);

        @Query("SELECT b FROM Booking b WHERE b.vehicle.id = :vehicleId " +
                        "AND b.status IN (:statuses) " +
                        "AND (b.startDateTime < :end AND b.endDateTime > :start)")
        List<Booking> findOverlappingBookings(@Param("vehicleId") Long vehicleId,
                        @Param("statuses") List<BookingStatus> statuses,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT DISTINCT b.vehicle.id FROM Booking b WHERE b.status IN (:statuses) " +
                        "AND (b.startDateTime < :end AND b.endDateTime > :start)")
        List<Long> findBusyVehicleIds(@Param("statuses") List<BookingStatus> statuses,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT SUM(b.totalAmount) FROM Booking b WHERE b.status = :status")
        java.math.BigDecimal sumTotalAmountByStatus(@Param("status") BookingStatus status);

        // Calculate driver earnings (remainingAmount) for a date range
        @Query("SELECT COALESCE(SUM(b.remainingAmount), 0) FROM Booking b " +
                        "WHERE b.vehicle.owner.id = :driverId " +
                        "AND b.status = 'COMPLETED' " +
                        "AND b.endDateTime >= :startDate " +
                        "AND b.endDateTime < :endDate")
        java.math.BigDecimal calculateDriverEarnings(@Param("driverId") Long driverId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);
}
