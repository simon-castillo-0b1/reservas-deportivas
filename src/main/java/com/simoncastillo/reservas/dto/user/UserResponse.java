package com.simoncastillo.reservas.dto.user;

import com.simoncastillo.reservas.entity.User;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }
}
