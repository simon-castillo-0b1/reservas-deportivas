package com.simoncastillo.reservas.dto.attendance;

import com.simoncastillo.reservas.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAttendanceRequest(
        @NotNull(message = "El estado de asistencia es obligatorio")
        AttendanceStatus status
) {
}
