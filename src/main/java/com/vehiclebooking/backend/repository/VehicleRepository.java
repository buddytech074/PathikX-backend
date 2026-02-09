package com.vehiclebooking.backend.repository;

import com.vehiclebooking.backend.model.Vehicle;
import com.vehiclebooking.backend.model.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByType(VehicleType type);

    List<Vehicle> findByOwnerId(Long ownerId);

    List<Vehicle> findByOwnerIdOrAssignedDriverId(Long ownerId, Long assignedDriverId);

    boolean existsByAssignedDriverId(Long driverId);

    // Simple distance calculation (in real world uses PostGIS or Haversine formula
    // in code/DB)
    @Query(value = "SELECT * FROM vehicles v WHERE v.is_available = true AND ST_Distance_Sphere(point(v.longitude, v.latitude), point(:lng, :lat)) <= :radius", nativeQuery = true)
    List<Vehicle> findNearbyVehicles(@Param("lat") double lat, @Param("lng") double lng,
            @Param("radius") double radius);

    List<Vehicle> findByIsAvailableTrue();
}
