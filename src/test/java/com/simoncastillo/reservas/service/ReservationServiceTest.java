package com.simoncastillo.reservas.service;

import com.simoncastillo.reservas.dto.reservation.CreateReservationRequest;
import com.simoncastillo.reservas.dto.reservation.ReservationResponse;
import com.simoncastillo.reservas.entity.*;
import com.simoncastillo.reservas.exception.BusinessRuleViolationException;
import com.simoncastillo.reservas.exception.DuplicateResourceException;
import com.simoncastillo.reservas.exception.ResourceNotFoundException;
import com.simoncastillo.reservas.repository.ActivityRepository;
import com.simoncastillo.reservas.repository.AttendanceRepository;
import com.simoncastillo.reservas.repository.ReservationRepository;
import com.simoncastillo.reservas.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ReservationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Nested
    @DisplayName("Tests de creación de reserva (RF-RES-001)")
    class CreateReservationTests {

        @Test
        @DisplayName("Debe crear la reserva y la asistencia pendiente exitosamente")
        void shouldCreateReservationAndAttendanceSuccessfully() {
            Long userId = 1L;
            Long activityId = 10L;
            CreateReservationRequest request = new CreateReservationRequest(userId, activityId);

            User user = new User("Pérez", "Juan", "juan@test.com");
            user.setId(userId);

            Activity activity = new Activity("Fútbol", "FUTBOL", LocalDateTime.now().plusDays(1), 90, 10);
            activity.setId(activityId);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(activityRepository.findByIdWithLock(activityId)).thenReturn(Optional.of(activity));
            when(reservationRepository.existsByUserIdAndActivityId(userId, activityId)).thenReturn(false);
            when(reservationRepository.countByActivityIdAndStatus(activityId, ReservationStatus.CONFIRMADA)).thenReturn(3L);

            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
                Reservation r = invocation.getArgument(0);
                r.setId(100L);
                return r;
            });

            ReservationResponse response = reservationService.createReservation(request);

            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.activityId()).isEqualTo(activityId);
            assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMADA);

            ArgumentCaptor<Attendance> attendanceCaptor = ArgumentCaptor.forClass(Attendance.class);
            verify(attendanceRepository).save(attendanceCaptor.capture());
            Attendance createdAttendance = attendanceCaptor.getValue();
            assertThat(createdAttendance.getReservation().getId()).isEqualTo(100L);
            assertThat(createdAttendance.getStatus()).isEqualTo(AttendanceStatus.PENDIENTE);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si el usuario no existe")
        void shouldThrowExceptionWhenUserNotFound() {
            CreateReservationRequest request = new CreateReservationRequest(999L, 10L);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuario no encontrado");

            verify(activityRepository, never()).findByIdWithLock(any());
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si la actividad no existe")
        void shouldThrowExceptionWhenActivityNotFound() {
            User user = new User("Pérez", "Juan", "juan@test.com");
            user.setId(1L);

            CreateReservationRequest request = new CreateReservationRequest(1L, 999L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(activityRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Actividad no encontrada");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar BusinessRuleViolationException si la actividad no está PROGRAMADA")
        void shouldThrowExceptionWhenActivityNotProgramada() {
            User user = new User("Pérez", "Juan", "juan@test.com");
            user.setId(1L);

            Activity activity = new Activity("Fútbol", "FUTBOL", LocalDateTime.now().plusDays(1), 90, 10);
            activity.setId(10L);
            activity.setStatus(ActivityStatus.CANCELADA);

            CreateReservationRequest request = new CreateReservationRequest(1L, 10L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(activityRepository.findByIdWithLock(10L)).thenReturn(Optional.of(activity));

            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("no está disponible");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar BusinessRuleViolationException si la actividad ya ha comenzado")
        void shouldThrowExceptionWhenActivityAlreadyStarted() {
            User user = new User("Pérez", "Juan", "juan@test.com");
            user.setId(1L);

            Activity activity = new Activity("Fútbol", "FUTBOL", LocalDateTime.now().minusHours(1), 90, 10);
            activity.setId(10L);

            CreateReservationRequest request = new CreateReservationRequest(1L, 10L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(activityRepository.findByIdWithLock(10L)).thenReturn(Optional.of(activity));

            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("no está disponible");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar DuplicateResourceException si el usuario ya tiene reserva para la actividad")
        void shouldThrowExceptionWhenUserAlreadyHasReservation() {
            User user = new User("Pérez", "Juan", "juan@test.com");
            user.setId(1L);

            Activity activity = new Activity("Fútbol", "FUTBOL", LocalDateTime.now().plusDays(1), 90, 10);
            activity.setId(10L);

            CreateReservationRequest request = new CreateReservationRequest(1L, 10L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(activityRepository.findByIdWithLock(10L)).thenReturn(Optional.of(activity));
            when(reservationRepository.existsByUserIdAndActivityId(1L, 10L)).thenReturn(true);

            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("ya posee una reserva");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar BusinessRuleViolationException si la actividad no tiene cupos disponibles")
        void shouldThrowExceptionWhenActivityIsFull() {
            User user = new User("Pérez", "Juan", "juan@test.com");
            user.setId(1L);

            Activity activity = new Activity("Fútbol", "FUTBOL", LocalDateTime.now().plusDays(1), 90, 5);
            activity.setId(10L);

            CreateReservationRequest request = new CreateReservationRequest(1L, 10L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(activityRepository.findByIdWithLock(10L)).thenReturn(Optional.of(activity));
            when(reservationRepository.existsByUserIdAndActivityId(1L, 10L)).thenReturn(false);
            when(reservationRepository.countByActivityIdAndStatus(10L, ReservationStatus.CONFIRMADA)).thenReturn(5L);

            assertThatThrownBy(() -> reservationService.createReservation(request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("No hay cupos disponibles");

            verify(reservationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests de consulta de reserva (RF-RES-002)")
    class GetReservationTests {

        @Test
        @DisplayName("Debe retornar la reserva cuando existe el ID")
        void shouldReturnReservationWhenExists() {
            User user = new User("Pérez", "Juan", "juan@test.com");
            user.setId(1L);

            Activity activity = new Activity("Fútbol", "FUTBOL", LocalDateTime.now().plusDays(1), 90, 10);
            activity.setId(10L);

            Reservation reservation = new Reservation(user, activity);
            reservation.setId(50L);

            when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));

            ReservationResponse response = reservationService.getReservationById(50L);

            assertThat(response.id()).isEqualTo(50L);
            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.activityId()).isEqualTo(10L);
            assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMADA);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si la reserva no existe")
        void shouldThrowExceptionWhenReservationNotFound() {
            when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.getReservationById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Reserva no encontrada");
        }
    }

    @Nested
    @DisplayName("Tests de cancelación de reserva (RF-RES-003)")
    class CancelReservationTests {

        @Test
        @DisplayName("Debe cancelar la reserva exitosamente si la actividad es futura y está confirmada")
        void shouldCancelReservationSuccessfully() {
            User user = new User("Pérez", "Juan", "juan@test.com");
            user.setId(1L);

            Activity activity = new Activity("Fútbol", "FUTBOL", LocalDateTime.now().plusDays(1), 90, 10);
            activity.setId(10L);

            Reservation reservation = new Reservation(user, activity);
            reservation.setId(50L);

            when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));

            reservationService.cancelReservation(50L);

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELADA);
            assertThat(reservation.getCancelledAt()).isNotNull();
            verify(reservationRepository).save(reservation);
        }

        @Test
        @DisplayName("Debe lanzar BusinessRuleViolationException si la reserva ya está cancelada")
        void shouldThrowExceptionWhenReservationAlreadyCancelled() {
            User user = new User("Pérez", "Juan", "juan@test.com");
            user.setId(1L);

            Activity activity = new Activity("Fútbol", "FUTBOL", LocalDateTime.now().plusDays(1), 90, 10);
            activity.setId(10L);

            Reservation reservation = new Reservation(user, activity);
            reservation.setId(50L);
            reservation.setStatus(ReservationStatus.CANCELADA);

            when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.cancelReservation(50L))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("ya se encuentra cancelada");

            verify(reservationRepository, never()).save(reservation);
        }

        @Test
        @DisplayName("Debe lanzar BusinessRuleViolationException si la actividad ya comenzó")
        void shouldThrowExceptionWhenActivityAlreadyStarted() {
            User user = new User("Pérez", "Juan", "juan@test.com");
            user.setId(1L);

            Activity activity = new Activity("Fútbol", "FUTBOL", LocalDateTime.now().minusHours(1), 90, 10);
            activity.setId(10L);

            Reservation reservation = new Reservation(user, activity);
            reservation.setId(50L);

            when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.cancelReservation(50L))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("ya ha comenzado");

            verify(reservationRepository, never()).save(reservation);
        }
    }

    @Nested
    @DisplayName("Tests de listado de reservas por usuario y actividad (RF-RES-004 y RF-RES-005)")
    class ListReservationsTests {

        @Test
        @DisplayName("Debe listar las reservas de un usuario existente")
        void shouldListReservationsByUserId() {
            User user = new User("Pérez", "Juan", "juan@test.com");
            user.setId(1L);

            Activity activity = new Activity("Fútbol", "FUTBOL", LocalDateTime.now().plusDays(1), 90, 10);
            activity.setId(10L);

            Reservation reservation = new Reservation(user, activity);
            reservation.setId(100L);

            when(userRepository.existsById(1L)).thenReturn(true);
            when(reservationRepository.findByUserId(1L)).thenReturn(List.of(reservation));

            List<ReservationResponse> results = reservationService.getReservationsByUserId(1L);

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().id()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si el usuario no existe al listar sus reservas")
        void shouldThrowExceptionWhenUserNotFoundForReservations() {
            when(userRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> reservationService.getReservationsByUserId(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }

        @Test
        @DisplayName("Debe listar las reservas de una actividad existente")
        void shouldListReservationsByActivityId() {
            User user = new User("Pérez", "Juan", "juan@test.com");
            user.setId(1L);

            Activity activity = new Activity("Fútbol", "FUTBOL", LocalDateTime.now().plusDays(1), 90, 10);
            activity.setId(10L);

            Reservation reservation = new Reservation(user, activity);
            reservation.setId(200L);

            when(activityRepository.existsById(10L)).thenReturn(true);
            when(reservationRepository.findByActivityId(10L)).thenReturn(List.of(reservation));

            List<ReservationResponse> results = reservationService.getReservationsByActivityId(10L);

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().id()).isEqualTo(200L);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si la actividad no existe al listar sus reservas")
        void shouldThrowExceptionWhenActivityNotFoundForReservations() {
            when(activityRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> reservationService.getReservationsByActivityId(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Actividad no encontrada");
        }
    }
}
