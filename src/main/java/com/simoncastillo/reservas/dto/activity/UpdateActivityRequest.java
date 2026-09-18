package com.simoncastillo.reservas.dto.activity;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateActivityRequest(
        @NotBlank(message = "El nombre de la actividad es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @NotBlank(message = "El deporte es obligatorio")
        @Size(max = 50, message = "El deporte no puede superar los 50 caracteres")
        String sport,

        @NotNull(message = "La fecha y hora de inicio es obligatoria")
        @Future(message = "La fecha y hora de inicio debe ser futura")
        LocalDateTime startAt,

        @NotNull(message = "La duración es obligatoria")
        @Positive(message = "La duración debe ser un número entero positivo")
        Integer durationMinutes,

        @NotNull(message = "La capacidad es obligatoria")
        @Positive(message = "La capacidad debe ser un número entero positivo")
        Integer capacity
) {
}
