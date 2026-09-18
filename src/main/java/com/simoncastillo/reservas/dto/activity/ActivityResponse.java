package com.simoncastillo.reservas.dto.activity;

import com.simoncastillo.reservas.entity.Activity;
import com.simoncastillo.reservas.entity.ActivityStatus;

import java.time.LocalDateTime;

public record ActivityResponse(
        Long id,
        String name,
        String sport,
        LocalDateTime startAt,
        Integer durationMinutes,
        Integer capacity,
        int reservedSpots,
        int availableSpots,
        ActivityStatus status
) {
    public static ActivityResponse of(Activity activity, int reservedSpots) {
        int available = Math.max(0, activity.getCapacity() - reservedSpots);
        return new ActivityResponse(
                activity.getId(),
                activity.getName(),
                activity.getSport(),
                activity.getStartAt(),
                activity.getDurationMinutes(),
                activity.getCapacity(),
                reservedSpots,
                available,
                activity.getStatus()
        );
    }
}
