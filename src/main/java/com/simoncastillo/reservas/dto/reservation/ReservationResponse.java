package com.simoncastillo.reservas.dto.reservation;

import com.simoncastillo.reservas.entity.Reservation;
import com.simoncastillo.reservas.entity.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long userId,
        Long activityId,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt
) {
    public static ReservationResponse fromEntity(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getActivity().getId(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getCancelledAt()
        );
    }
}
