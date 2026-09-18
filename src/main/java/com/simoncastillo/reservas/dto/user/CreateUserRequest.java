package com.simoncastillo.reservas.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;


public record CreateUserRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 50, message = "El apellido no puede superar los 50 caracteres")
        String lastName,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato de email es inválido")
        @Size(max = 150, message = "El email no puede superar los 150 caracteres")
        String email
) {
}
