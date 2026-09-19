package com.simoncastillo.reservas.dto.reservation;

import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(
        @NotNull(message = "El ID de usuario es obligatorio")
        Long userId,

        @NotNull(message = "El ID de la actividad es obligatorio")
        Long activityId
) {
}
