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
import com.simoncastillo.reservas.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ActivityService activityService;

    @Nested
    @DisplayName("Tests de creación de actividad (RF-ACT-001)")
    class CreateActivityTests {

        @Test
        @DisplayName("Debe crear la actividad exitosamente con fecha futura")
        void shouldCreateActivitySuccessfully() {
            LocalDateTime futureDate = LocalDateTime.now().plusDays(2);
            CreateActivityRequest request = new CreateActivityRequest("Yoga", "YOGA", futureDate, 60, 20);

            when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> {
                Activity a = invocation.getArgument(0);
                a.setId(1L);
                return a;
            });

            ActivityResponse response = activityService.createActivity(request);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("Yoga");
            assertThat(response.capacity()).isEqualTo(20);
            assertThat(response.reservedSpots()).isZero();
            assertThat(response.availableSpots()).isEqualTo(20);
            assertThat(response.status()).isEqualTo(ActivityStatus.PROGRAMADA);
        }

        @Test
        @DisplayName("Debe rechazar la creación si la fecha de inicio está en el pasado")
        void shouldThrowExceptionWhenStartDateInPast() {
            LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
            CreateActivityRequest request = new CreateActivityRequest("Yoga", "YOGA", pastDate, 60, 20);

            assertThatThrownBy(() -> activityService.createActivity(request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("futura");

            verify(activityRepository, never()).save(any(Activity.class));
        }
    }

    @Nested
    @DisplayName("Tests de consulta de actividades (RF-ACT-002 y RF-ACT-003)")
    class GetActivityTests {

        @Test
        @DisplayName("Debe retornar la actividad con cupos calculados cuando existe")
        void shouldReturnActivityWithCalculatedSpots() {
            Activity activity = new Activity("Yoga", "YOGA", LocalDateTime.now().plusDays(2), 60, 20);
            activity.setId(5L);

            when(activityRepository.findById(5L)).thenReturn(Optional.of(activity));
            when(reservationRepository.countByActivityIdAndStatus(5L, ReservationStatus.CONFIRMADA)).thenReturn(7L);

            ActivityResponse response = activityService.getActivityById(5L);

            assertThat(response.id()).isEqualTo(5L);
            assertThat(response.reservedSpots()).isEqualTo(7);
            assertThat(response.availableSpots()).isEqualTo(13);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si la actividad no existe")
        void shouldThrowNotFoundWhenActivityDoesNotExist() {
            when(activityRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.getActivityById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("Debe listar actividades filtradas con sus cupos calculados")
        void shouldReturnFilteredActivities() {
            Activity a1 = new Activity("Yoga", "YOGA", LocalDateTime.now().plusDays(1), 60, 10);
            a1.setId(1L);

            when(activityRepository.findAll(any(Specification.class))).thenReturn(List.of(a1));
            when(reservationRepository.countByActivityIdAndStatus(1L, ReservationStatus.CONFIRMADA)).thenReturn(3L);

            List<ActivityResponse> result = activityService.getActivities("YOGA", null, null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).reservedSpots()).isEqualTo(3);
            assertThat(result.get(0).availableSpots()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("Tests de actualización de actividad (RF-ACT-004)")
    class UpdateActivityTests {

        @Test
        @DisplayName("Debe actualizar la actividad si no ha comenzado y la capacidad es suficiente")
        void shouldUpdateActivitySuccessfully() {
            Activity activity = new Activity("Yoga", "YOGA", LocalDateTime.now().plusDays(2), 60, 20);
            activity.setId(1L);

            UpdateActivityRequest request = new UpdateActivityRequest("Yoga Avanzado", "YOGA", LocalDateTime.now().plusDays(3), 90, 25);

            when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
            when(reservationRepository.countByActivityIdAndStatus(1L, ReservationStatus.CONFIRMADA)).thenReturn(5L);
            when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ActivityResponse response = activityService.updateActivity(1L, request);

            assertThat(response.name()).isEqualTo("Yoga Avanzado");
            assertThat(response.capacity()).isEqualTo(25);
            assertThat(response.reservedSpots()).isEqualTo(5);
        }

        @Test
        @DisplayName("Debe rechazar modificación si la actividad ya comenzó")
        void shouldThrowExceptionWhenModifyingPastActivity() {
            Activity activity = new Activity("Yoga", "YOGA", LocalDateTime.now().minusHours(1), 60, 20);
            activity.setId(1L);

            UpdateActivityRequest request = new UpdateActivityRequest("Yoga", "YOGA", LocalDateTime.now().plusDays(1), 60, 20);

            when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

            assertThatThrownBy(() -> activityService.updateActivity(1L, request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("comenzado");

            verify(activityRepository, never()).save(any(Activity.class));
        }

        @Test
        @DisplayName("Debe rechazar modificación si la nueva capacidad es menor a las reservas confirmadas")
        void shouldThrowExceptionWhenCapacityLessThanReserved() {
            Activity activity = new Activity("Yoga", "YOGA", LocalDateTime.now().plusDays(1), 60, 20);
            activity.setId(1L);

            UpdateActivityRequest request = new UpdateActivityRequest("Yoga", "YOGA", LocalDateTime.now().plusDays(1), 60, 5);

            when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
            when(reservationRepository.countByActivityIdAndStatus(1L, ReservationStatus.CONFIRMADA)).thenReturn(10L);

            assertThatThrownBy(() -> activityService.updateActivity(1L, request))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("menor que las reservas");

            verify(activityRepository, never()).save(any(Activity.class));
        }
    }

    @Nested
    @DisplayName("Tests de cancelación de actividad (RF-ACT-005)")
    class CancelActivityTests {

        @Test
        @DisplayName("Debe cancelar lógicamente la actividad futura")
        void shouldCancelActivitySuccessfully() {
            Activity activity = new Activity("Yoga", "YOGA", LocalDateTime.now().plusDays(2), 60, 20);
            activity.setId(1L);

            when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

            activityService.cancelActivity(1L);

            assertThat(activity.getStatus()).isEqualTo(ActivityStatus.CANCELADA);
            verify(activityRepository).save(activity);
        }

        @Test
        @DisplayName("Debe rechazar cancelación si la actividad ya comenzó")
        void shouldThrowExceptionWhenCancellingStartedActivity() {
            Activity activity = new Activity("Yoga", "YOGA", LocalDateTime.now().minusHours(1), 60, 20);
            activity.setId(1L);

            when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

            assertThatThrownBy(() -> activityService.cancelActivity(1L))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("comenzado");
        }
    }
}
