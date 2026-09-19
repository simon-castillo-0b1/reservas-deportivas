package com.simoncastillo.reservas.service;

import com.simoncastillo.reservas.dto.attendance.AttendanceResponse;
import com.simoncastillo.reservas.dto.attendance.UpdateAttendanceRequest;
import com.simoncastillo.reservas.entity.*;
import com.simoncastillo.reservas.exception.BusinessRuleViolationException;
import com.simoncastillo.reservas.exception.ResourceNotFoundException;
import com.simoncastillo.reservas.repository.ActivityRepository;
import com.simoncastillo.reservas.repository.AttendanceRepository;
import com.simoncastillo.reservas.repository.ReservationRepository;
import com.simoncastillo.reservas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private User testUser;
    private Activity pastActivity;
    private Activity futureActivity;
    private Reservation confirmedReservation;
    private Attendance attendance;

    @BeforeEach
    void setUp() {
        testUser = new User("Pérez", "Juan", "juan@test.com");
        testUser.setId(1L);

        pastActivity = new Activity("Crossfit", "CROSSFIT", LocalDateTime.now().minusHours(1), 60, 15);
        pastActivity.setId(10L);

        futureActivity = new Activity("Crossfit Mañana", "CROSSFIT", LocalDateTime.now().plusDays(1), 60, 15);
        futureActivity.setId(20L);

        confirmedReservation = new Reservation(testUser, pastActivity);
        confirmedReservation.setId(50L);

        attendance = new Attendance(confirmedReservation);
        attendance.setId(100L);
    }

    @Nested
    @DisplayName("Tests de registro de asistencia (RF-ATT-001)")
    class RegisterAttendanceTests {

        @Test
        @DisplayName("Debe registrar asistencia como PRESENTE correctamente cuando la actividad ya inició")
        void shouldMarkAttendancePresentSuccessfully() {
            UpdateAttendanceRequest request = new UpdateAttendanceRequest(AttendanceStatus.PRESENTE);

            when(reservationRepository.findById(50L)).thenReturn(Optional.of(confirmedReservation));
            when(attendanceRepository.findByReservationId(50L)).thenReturn(Optional.of(attendance));
            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

            AttendanceResponse response = attendanceService.registerAttendance(50L, request);

            assertThat(response.status()).isEqualTo(AttendanceStatus.PRESENTE);
            assertThat(response.registeredAt()).isNotNull();
            assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.PRESENTE);
            verify(attendanceRepository).save(attendance);
        }

        @Test
        @DisplayName("Debe registrar asistencia como AUSENTE correctamente cuando la actividad ya inició")
        void shouldMarkAttendanceAbsentSuccessfully() {
            UpdateAttendanceRequest request = new UpdateAttendanceRequest(AttendanceStatus.AUSENTE);

            when(reservationRepository.findById(50L)).thenReturn(Optional.of(confirmedReservation));
            when(attendanceRepository.findByReservationId(50L)).thenReturn(Optional.of(attendance));
            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

            AttendanceResponse response = attendanceService.registerAttendance(50L, request);

            assertThat(response.status()).isEqualTo(AttendanceStatus.AUSENTE);
            assertThat(response.registeredAt()).isNotNull();
            assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.AUSENTE);
            verify(attendanceRepository).save(attendance);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si la reserva no existe")
        void shouldThrowExceptionWhenReservationNotFound() {
            UpdateAttendanceRequest request = new UpdateAttendanceRequest(AttendanceStatus.PRESENTE);
            when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> attendanceService.registerAttendance(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Reserva no encontrada");

            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar BusinessRuleViolationException si la reserva está CANCELADA")
        void shouldThrowExceptionWhenReservationIsCancelled() {
            confirmedReservation.setStatus(ReservationStatus.CANCELADA);
            UpdateAttendanceRequest request = new UpdateAttendanceRequest(AttendanceStatus.PRESENTE);
            when(reservationRepository.findById(50L)).thenReturn(Optional.of(confirmedReservation));

            assertThatThrownBy(() -> attendanceService.registerAttendance(50L, request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("no está confirmada");

            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar BusinessRuleViolationException si la actividad todavía no comenzó")
        void shouldThrowExceptionWhenActivityHasNotStarted() {
            Reservation futureReservation = new Reservation(testUser, futureActivity);
            futureReservation.setId(60L);
            UpdateAttendanceRequest request = new UpdateAttendanceRequest(AttendanceStatus.PRESENTE);

            when(reservationRepository.findById(60L)).thenReturn(Optional.of(futureReservation));

            assertThatThrownBy(() -> attendanceService.registerAttendance(60L, request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("antes del inicio de la actividad");

            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si no existe registro de asistencia para la reserva")
        void shouldThrowExceptionWhenAttendanceNotFound() {
            UpdateAttendanceRequest request = new UpdateAttendanceRequest(AttendanceStatus.PRESENTE);
            when(reservationRepository.findById(50L)).thenReturn(Optional.of(confirmedReservation));
            when(attendanceRepository.findByReservationId(50L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> attendanceService.registerAttendance(50L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Registro de asistencia no encontrado");

            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe rechazar transición posterior si ya está marcado como PRESENTE (irreversible)")
        void shouldThrowExceptionWhenAlreadyMarkedPresent() {
            attendance.markPresent();
            UpdateAttendanceRequest request = new UpdateAttendanceRequest(AttendanceStatus.AUSENTE);

            when(reservationRepository.findById(50L)).thenReturn(Optional.of(confirmedReservation));
            when(attendanceRepository.findByReservationId(50L)).thenReturn(Optional.of(attendance));

            assertThatThrownBy(() -> attendanceService.registerAttendance(50L, request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("no admite modificaciones posteriores");

            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe rechazar transición posterior si ya está marcado como AUSENTE (irreversible)")
        void shouldThrowExceptionWhenAlreadyMarkedAbsent() {
            attendance.markAbsent();
            UpdateAttendanceRequest request = new UpdateAttendanceRequest(AttendanceStatus.PRESENTE);

            when(reservationRepository.findById(50L)).thenReturn(Optional.of(confirmedReservation));
            when(attendanceRepository.findByReservationId(50L)).thenReturn(Optional.of(attendance));

            assertThatThrownBy(() -> attendanceService.registerAttendance(50L, request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("no admite modificaciones posteriores");

            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe rechazar transición si el nuevo estado solicitado es PENDIENTE")
        void shouldThrowExceptionWhenTargetStatusIsPendiente() {
            UpdateAttendanceRequest request = new UpdateAttendanceRequest(AttendanceStatus.PENDIENTE);

            when(reservationRepository.findById(50L)).thenReturn(Optional.of(confirmedReservation));
            when(attendanceRepository.findByReservationId(50L)).thenReturn(Optional.of(attendance));

            assertThatThrownBy(() -> attendanceService.registerAttendance(50L, request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("no puede restablecerse a PENDIENTE");

            verify(attendanceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests de consulta de asistencia (RF-ATT-002 y RF-ATT-003)")
    class QueryAttendanceTests {

        @Test
        @DisplayName("Debe retornar asistencias por actividad cuando la actividad existe")
        void shouldGetAttendanceByActivityId() {
            when(activityRepository.existsById(10L)).thenReturn(true);
            when(attendanceRepository.findByReservationActivityId(10L)).thenReturn(List.of(attendance));

            List<AttendanceResponse> results = attendanceService.getAttendanceByActivityId(10L);

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().id()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si la actividad no existe al consultar asistencia")
        void shouldThrowExceptionWhenActivityNotFoundForAttendance() {
            when(activityRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> attendanceService.getAttendanceByActivityId(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Actividad no encontrada");
        }

        @Test
        @DisplayName("Debe retornar asistencias por usuario cuando el usuario existe")
        void shouldGetAttendanceByUserId() {
            when(userRepository.existsById(1L)).thenReturn(true);
            when(attendanceRepository.findByReservationUserId(1L)).thenReturn(List.of(attendance));

            List<AttendanceResponse> results = attendanceService.getAttendanceByUserId(1L);

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().id()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si el usuario no existe al consultar asistencia")
        void shouldThrowExceptionWhenUserNotFoundForAttendance() {
            when(userRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> attendanceService.getAttendanceByUserId(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }
}
