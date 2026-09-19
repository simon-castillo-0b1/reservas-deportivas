package com.simoncastillo.reservas.service;

import com.simoncastillo.reservas.dto.reservation.CreateReservationRequest;
import com.simoncastillo.reservas.dto.reservation.ReservationResponse;
import com.simoncastillo.reservas.entity.Activity;
import com.simoncastillo.reservas.entity.ReservationStatus;
import com.simoncastillo.reservas.entity.User;
import com.simoncastillo.reservas.exception.BusinessRuleViolationException;
import com.simoncastillo.reservas.repository.ActivityRepository;
import com.simoncastillo.reservas.repository.AttendanceRepository;
import com.simoncastillo.reservas.repository.ReservationRepository;
import com.simoncastillo.reservas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReservationConcurrencyIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();
        reservationRepository.deleteAll();
        activityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Concurrencia: Ante 2 peticiones simultáneas por el último cupo, solo 1 tiene éxito y la otra es rechazada (evita overbooking)")
    void shouldPreventOverbookingUnderConcurrentRequests() throws InterruptedException {
        // Given: Una actividad con capacidad = 1 y 2 usuarios distintos
        Activity activity = new Activity("Spinning Concurrente", "SPINNING", LocalDateTime.now().plusDays(3), 45, 1);
        activity = activityRepository.save(activity);

        User user1 = userRepository.save(new User("Gómez", "Carlos", "carlos@test.com"));
        User user2 = userRepository.save(new User("López", "Ana", "ana@test.com"));

        Long activityId = activity.getId();
        Long user1Id = user1.getId();
        Long user2Id = user2.getId();

        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<ReservationResponse> successfulReservations = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        List<Long> userIds = List.of(user1Id, user2Id);

        for (Long uid : userIds) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Espera la señal de disparo simultáneo
                    ReservationResponse res = reservationService.createReservation(new CreateReservationRequest(uid, activityId));
                    successfulReservations.add(res);
                } catch (Throwable t) {
                    exceptions.add(t);
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // ¡Disparo concurrente!

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        // Then: Exactamente 1 reserva exitosa y 1 rechazada por falta de cupo
        assertThat(successfulReservations).hasSize(1);
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions.getFirst()).isInstanceOf(BusinessRuleViolationException.class);

        // Verificación en base de datos: Nunca se debe sobrepasar la capacidad de 1
        long totalConfirmed = reservationRepository.countByActivityIdAndStatus(activityId, ReservationStatus.CONFIRMADA);
        assertThat(totalConfirmed).isEqualTo(1);
    }
}
