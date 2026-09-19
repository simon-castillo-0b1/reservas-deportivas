package com.simoncastillo.reservas.service;

import com.simoncastillo.reservas.dto.attendance.AttendanceResponse;
import com.simoncastillo.reservas.dto.attendance.UpdateAttendanceRequest;
import com.simoncastillo.reservas.entity.Attendance;
import com.simoncastillo.reservas.entity.AttendanceStatus;
import com.simoncastillo.reservas.entity.Reservation;
import com.simoncastillo.reservas.entity.ReservationStatus;
import com.simoncastillo.reservas.exception.BusinessRuleViolationException;
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
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ReservationRepository reservationRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             ReservationRepository reservationRepository,
                             ActivityRepository activityRepository,
                             UserRepository userRepository) {
        this.attendanceRepository = attendanceRepository;
        this.reservationRepository = reservationRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AttendanceResponse registerAttendance(Long reservationId, UpdateAttendanceRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.CONFIRMADA) {
            throw new BusinessRuleViolationException("No se puede registrar asistencia en una reserva que no está confirmada");
        }

        if (reservation.getActivity().getStartAt().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleViolationException("No se puede registrar asistencia antes del inicio de la actividad");
        }

        Attendance attendance = attendanceRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de asistencia no encontrado para la reserva con ID: " + reservationId));

        if (attendance.getStatus() != AttendanceStatus.PENDIENTE) {
            throw new BusinessRuleViolationException("El estado de asistencia ya fue registrado (" + attendance.getStatus() + ") y no admite modificaciones posteriores");
        }

        if (request.status() == AttendanceStatus.PENDIENTE) {
            throw new BusinessRuleViolationException("El estado de asistencia no puede restablecerse a PENDIENTE");
        }

        if (request.status() == AttendanceStatus.PRESENTE) {
            attendance.markPresent();
        } else if (request.status() == AttendanceStatus.AUSENTE) {
            attendance.markAbsent();
        }

        Attendance savedAttendance = attendanceRepository.save(attendance);
        return AttendanceResponse.fromEntity(savedAttendance);
    }

    public List<AttendanceResponse> getAttendanceByActivityId(Long activityId) {
        if (!activityRepository.existsById(activityId)) {
            throw new ResourceNotFoundException("Actividad no encontrada con ID: " + activityId);
        }
        return attendanceRepository.findByReservationActivityId(activityId)
                .stream()
                .map(AttendanceResponse::fromEntity)
                .toList();
    }

    public List<AttendanceResponse> getAttendanceByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + userId);
        }
        return attendanceRepository.findByReservationUserId(userId)
                .stream()
                .map(AttendanceResponse::fromEntity)
                .toList();
    }
}
