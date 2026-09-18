package com.simoncastillo.reservas.service;

import com.simoncastillo.reservas.dto.activity.ActivityResponse;
import com.simoncastillo.reservas.dto.activity.CreateActivityRequest;
import com.simoncastillo.reservas.dto.activity.UpdateActivityRequest;
import com.simoncastillo.reservas.entity.Activity;
import com.simoncastillo.reservas.entity.ActivityStatus;
import com.simoncastillo.reservas.entity.ReservationStatus;
import com.simoncastillo.reservas.exception.BusinessRuleViolationException;
import com.simoncastillo.reservas.exception.ResourceNotFoundException;
import com.simoncastillo.reservas.repository.ActivityRepository;
import com.simoncastillo.reservas.repository.ActivitySpecifications;
import com.simoncastillo.reservas.repository.ReservationRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ReservationRepository reservationRepository;

    public ActivityService(ActivityRepository activityRepository, ReservationRepository reservationRepository) {
        this.activityRepository = activityRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public ActivityResponse createActivity(CreateActivityRequest request) {
        if (request.startAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleViolationException("La fecha de inicio debe ser futura.");
        }

        Activity activity = new Activity(
                request.name(),
                request.sport(),
                request.startAt(),
                request.durationMinutes(),
                request.capacity()
        );

        Activity saved = activityRepository.save(activity);
        return ActivityResponse.of(saved, 0);
    }

    public ActivityResponse getActivityById(Long id) {
        Activity activity = findActivityById(id);
        long reservationCount = reservationRepository.countByActivityIdAndStatus(id, ReservationStatus.CONFIRMADA);
        return ActivityResponse.of(activity, (int) reservationCount);
    }

    public List<ActivityResponse> getActivities(String sport, ActivityStatus status, LocalDateTime from, LocalDateTime to) {
        Specification<Activity> spec = ActivitySpecifications.withFilters(sport, status, from, to);
        List<Activity> activities = activityRepository.findAll(spec);

        return activities.stream()
                .map(activity -> {
                    long reservationCount = reservationRepository.countByActivityIdAndStatus(activity.getId(), ReservationStatus.CONFIRMADA);
                    return ActivityResponse.of(activity, (int) reservationCount);
                })
                .toList();
    }

    @Transactional
    public ActivityResponse updateActivity(Long id, UpdateActivityRequest request) {
        Activity activity = findActivityById(id);

        if (activity.getStatus() != ActivityStatus.PROGRAMADA || activity.getStartAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleViolationException("No se puede modificar una actividad que ya ha comenzado o no está programada");
        }

        long reservedCount = reservationRepository.countByActivityIdAndStatus(id, ReservationStatus.CONFIRMADA);
        if (request.capacity() < reservedCount) {
            throw new BusinessRuleViolationException(
                    "La nueva capacidad (" + request.capacity() + ") no puede ser menor que las reservas confirmadas actuales (" + reservedCount + ")"
            );
        }

        activity.setName(request.name());
        activity.setSport(request.sport());
        activity.setStartAt(request.startAt());
        activity.setDurationMinutes(request.durationMinutes());
        activity.setCapacity(request.capacity());

        Activity updated = activityRepository.save(activity);
        return ActivityResponse.of(updated, (int) reservedCount);
    }

    @Transactional
    public void cancelActivity(Long id) {
        Activity activity = findActivityById(id);

        if (activity.getStatus() == ActivityStatus.CANCELADA) {
            return;
        }

        if (activity.getStartAt().isBefore(LocalDateTime.now()) || activity.getStatus() == ActivityStatus.FINALIZADA) {
            throw new BusinessRuleViolationException("No se puede cancelar una actividad que ya ha comenzado o finalizado");
        }

        activity.setStatus(ActivityStatus.CANCELADA);
        activityRepository.save(activity);
    }

    private Activity findActivityById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con ID: " + id));
    }
}