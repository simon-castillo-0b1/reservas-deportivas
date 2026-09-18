package com.simoncastillo.reservas.repository;

import com.simoncastillo.reservas.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class PersistenceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Test
    @DisplayName("Debe rechazar la inserción de dos usuarios con el mismo email (restricción UNIQUE)")
    void shouldEnforceUniqueEmailConstraint() {
        User user1 = new User("Pérez", "Juan", "juan@example.com");
        userRepository.saveAndFlush(user1);

        User user2 = new User("Gómez", "Carlos", "juan@example.com");

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }

    @Test
    @DisplayName("Debe impedir dos reservas para el mismo usuario y actividad (restricción uk_reservation_user_activity)")
    void shouldEnforceUniqueUserActivityConstraintOnReservation() {
        User user = userRepository.saveAndFlush(new User("Pérez", "Juan", "juan@example.com"));
        Activity activity = activityRepository.saveAndFlush(
                new Activity("Clase de Yoga", "YOGA", LocalDateTime.now().plusDays(2), 60, 20)
        );

        Reservation res1 = new Reservation(user, activity);
        reservationRepository.saveAndFlush(res1);

        Reservation res2 = new Reservation(user, activity);

        assertThrows(DataIntegrityViolationException.class, () -> {
            reservationRepository.saveAndFlush(res2);
        });
    }

    @Test
    @DisplayName("Debe contar únicamente las reservas confirmadas de una actividad")
    void shouldCountOnlyConfirmedReservationsForActivity() {
        User user1 = userRepository.saveAndFlush(new User("Pérez", "Juan", "juan@example.com"));
        User user2 = userRepository.saveAndFlush(new User("Gómez", "Ana", "ana@example.com"));
        User user3 = userRepository.saveAndFlush(new User("López", "Pedro", "pedro@example.com"));

        Activity activity = activityRepository.saveAndFlush(
                new Activity("Spinning", "SPINNING", LocalDateTime.now().plusDays(1), 45, 15)
        );

        Reservation res1 = new Reservation(user1, activity);
        Reservation res2 = new Reservation(user2, activity);
        Reservation res3 = new Reservation(user3, activity);
        res3.cancel(); // Esta queda cancelada

        reservationRepository.saveAndFlush(res1);
        reservationRepository.saveAndFlush(res2);
        reservationRepository.saveAndFlush(res3);

        long confirmedCount = reservationRepository.countByActivityIdAndStatus(activity.getId(), ReservationStatus.CONFIRMADA);
        long cancelledCount = reservationRepository.countByActivityIdAndStatus(activity.getId(), ReservationStatus.CANCELADA);

        assertThat(confirmedCount).isEqualTo(2);
        assertThat(cancelledCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Debe persistir la asistencia asociada a una reserva con estado inicial PENDIENTE")
    void shouldPersistAttendanceForReservation() {
        User user = userRepository.saveAndFlush(new User("Pérez", "Juan", "juan@example.com"));
        Activity activity = activityRepository.saveAndFlush(
                new Activity("Crossfit", "CROSSFIT", LocalDateTime.now().plusDays(3), 50, 10)
        );
        Reservation reservation = reservationRepository.saveAndFlush(new Reservation(user, activity));

        Attendance attendance = new Attendance(reservation);
        attendanceRepository.saveAndFlush(attendance);

        Optional<Attendance> found = attendanceRepository.findByReservationId(reservation.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(AttendanceStatus.PENDIENTE);
        assertThat(found.get().getRegisteredAt()).isNull();
    }
}
