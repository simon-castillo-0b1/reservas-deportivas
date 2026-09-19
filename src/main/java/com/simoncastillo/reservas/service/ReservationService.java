package com.simoncastillo.reservas.service;

import com.simoncastillo.reservas.dto.reservation.CreateReservationRequest;
import com.simoncastillo.reservas.dto.reservation.ReservationResponse;
import com.simoncastillo.reservas.entity.Activity;
import com.simoncastillo.reservas.entity.ActivityStatus;
import com.simoncastillo.reservas.entity.Attendance;
import com.simoncastillo.reservas.entity.Reservation;
import com.simoncastillo.reservas.entity.ReservationStatus;
import com.simoncastillo.reservas.entity.User;
import com.simoncastillo.reservas.exception.BusinessRuleViolationException;
import com.simoncastillo.reservas.exception.DuplicateResourceException;
import com.simoncastillo.reservas.exception.ResourceNotFoundException;
import com.simoncastillo.reservas.repository.ActivityRepository;
import com.simoncastillo.reservas.repository.AttendanceRepository;
import com.simoncastillo.reservas.repository.ReservationRepository;
import com.simoncastillo.reservas.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ReservationRepository reservationRepository;
    private final AttendanceRepository attendanceRepository;

    public ReservationService(UserRepository userRepository,
                              ActivityRepository activityRepository,
                              ReservationRepository reservationRepository,
                              AttendanceRepository attendanceRepository) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.reservationRepository = reservationRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + request.userId()));

        Activity activity = activityRepository.findByIdWithLock(request.activityId())
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con ID: " + request.activityId()));

        if (activity.getStatus() != ActivityStatus.PROGRAMADA || activity.getStartAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleViolationException("La actividad no está disponible para reservas (debe estar PROGRAMADA y no haber comenzado)");
        }

        if (reservationRepository.existsByUserIdAndActivityId(request.userId(), request.activityId())) {
            throw new DuplicateResourceException("El usuario ya posee una reserva para esta actividad");
        }

        long confirmedReservationsCount = reservationRepository.countByActivityIdAndStatus(request.activityId(), ReservationStatus.CONFIRMADA);
        if (confirmedReservationsCount >= activity.getCapacity()) {
            throw new BusinessRuleViolationException("No hay cupos disponibles para esta actividad");
        }

        Reservation reservation = new Reservation(user, activity);
        Reservation savedReservation = reservationRepository.save(reservation);

        Attendance attendance = new Attendance(savedReservation);
        attendanceRepository.save(attendance);

        return ReservationResponse.fromEntity(savedReservation);
    }

    public ReservationResponse getReservationById(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + reservationId));
        return ReservationResponse.fromEntity(reservation);
    }

    @Transactional
    public void cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + reservationId));

        if (reservation.getStatus() == ReservationStatus.CANCELADA) {
            throw new BusinessRuleViolationException("La reserva ya se encuentra cancelada");
        }

        if (reservation.getActivity().getStartAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleViolationException("No se puede cancelar una reserva de una actividad que ya ha comenzado");
        }

        reservation.cancel();
        reservationRepository.save(reservation);
    }

    public List<ReservationResponse> getReservationsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + userId);
        }
        return reservationRepository.findByUserId(userId)
                .stream()
                .map(ReservationResponse::fromEntity)
                .toList();
    }


    public List<ReservationResponse> getReservationsByActivityId(Long activityId) {
        if (!activityRepository.existsById(activityId)) {
            throw new ResourceNotFoundException("Actividad no encontrada con ID: " + activityId);
        }
        return reservationRepository.findByActivityId(activityId)
                .stream()
                .map(ReservationResponse::fromEntity)
                .toList();
    }
}
