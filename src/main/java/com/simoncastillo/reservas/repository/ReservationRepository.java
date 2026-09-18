package com.simoncastillo.reservas.repository;

import com.simoncastillo.reservas.entity.Reservation;
import com.simoncastillo.reservas.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByUserIdAndActivityId(Long userId, Long activityId);

    long countByActivityIdAndStatus(Long activityId, ReservationStatus status);

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByActivityId(Long activityId);
}
