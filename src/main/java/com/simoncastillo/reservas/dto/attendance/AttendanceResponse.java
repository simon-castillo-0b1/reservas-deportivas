package com.simoncastillo.reservas.dto.attendance;

import com.simoncastillo.reservas.entity.Attendance;
import com.simoncastillo.reservas.entity.AttendanceStatus;

import java.time.LocalDateTime;

public record AttendanceResponse(
        Long id,
        Long reservationId,
        AttendanceStatus status,
        LocalDateTime registeredAt
) {
    public static AttendanceResponse fromEntity(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getReservation().getId(),
                attendance.getStatus(),
                attendance.getRegisteredAt()
        );
    }
}
