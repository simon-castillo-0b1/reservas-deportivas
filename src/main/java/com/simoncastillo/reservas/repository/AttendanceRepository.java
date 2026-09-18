package com.simoncastillo.reservas.repository;

import com.simoncastillo.reservas.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByReservationId(Long reservationId);

    List<Attendance> findByReservationActivityId(Long activityId);

    List<Attendance> findByReservationUserId(Long userId);
}
