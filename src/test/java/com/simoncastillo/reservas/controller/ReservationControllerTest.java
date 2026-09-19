package com.simoncastillo.reservas.controller;

import com.simoncastillo.reservas.dto.reservation.CreateReservationRequest;
import com.simoncastillo.reservas.dto.reservation.ReservationResponse;
import com.simoncastillo.reservas.entity.ReservationStatus;
import com.simoncastillo.reservas.exception.BusinessRuleViolationException;
import com.simoncastillo.reservas.exception.DuplicateResourceException;
import com.simoncastillo.reservas.exception.ResourceNotFoundException;
import com.simoncastillo.reservas.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @Test
    @DisplayName("POST /api/reservations - Debe responder 201 Created y Location con datos válidos")
    void shouldCreateReservationSuccessfully() throws Exception {
        ReservationResponse response = new ReservationResponse(
                100L, 1L, 10L, ReservationStatus.CONFIRMADA, LocalDateTime.now(), null
        );

        when(reservationService.createReservation(any(CreateReservationRequest.class))).thenReturn(response);

        String jsonBody = """
                {
                    "userId": 1,
                    "activityId": 10
                }
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/reservations/100")))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.activityId").value(10))
                .andExpect(jsonPath("$.status").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("POST /api/reservations - Debe responder 400 Bad Request si los IDs son nulos")
    void shouldReturnBadRequestWhenFieldsAreNull() throws Exception {
        String jsonBody = """
                {
                    "userId": null,
                    "activityId": null
                }
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/reservations - Debe responder 404 Not Found si el usuario o la actividad no existen")
    void shouldReturnNotFoundWhenUserOrActivityNotFound() throws Exception {
        when(reservationService.createReservation(any(CreateReservationRequest.class)))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado con ID: 99"));

        String jsonBody = """
                {
                    "userId": 99,
                    "activityId": 10
                }
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Usuario no encontrado con ID: 99"));
    }

    @Test
    @DisplayName("POST /api/reservations - Debe responder 409 Conflict si ya existe reserva para el usuario")
    void shouldReturnConflictWhenReservationAlreadyExists() throws Exception {
        when(reservationService.createReservation(any(CreateReservationRequest.class)))
                .thenThrow(new DuplicateResourceException("El usuario ya posee una reserva para esta actividad"));

        String jsonBody = """
                {
                    "userId": 1,
                    "activityId": 10
                }
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").value("El usuario ya posee una reserva para esta actividad"));
    }

    @Test
    @DisplayName("POST /api/reservations - Debe responder 409 Conflict si no hay cupos disponibles")
    void shouldReturnConflictWhenNoSpotsAvailable() throws Exception {
        when(reservationService.createReservation(any(CreateReservationRequest.class)))
                .thenThrow(new BusinessRuleViolationException("No hay cupos disponibles para esta actividad"));

        String jsonBody = """
                {
                    "userId": 1,
                    "activityId": 10
                }
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("No hay cupos disponibles para esta actividad"));
    }

    @Test
    @DisplayName("GET /api/reservations/{id} - Debe responder 200 OK con los datos de la reserva")
    void shouldGetReservationByIdSuccessfully() throws Exception {
        ReservationResponse response = new ReservationResponse(
                50L, 1L, 10L, ReservationStatus.CONFIRMADA, LocalDateTime.now(), null
        );

        when(reservationService.getReservationById(50L)).thenReturn(response);

        mockMvc.perform(get("/api/reservations/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.activityId").value(10))
                .andExpect(jsonPath("$.status").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("GET /api/reservations/{id} - Debe responder 404 Not Found cuando no existe la reserva")
    void shouldReturnNotFoundWhenReservationDoesNotExist() throws Exception {
        when(reservationService.getReservationById(999L))
                .thenThrow(new ResourceNotFoundException("Reserva no encontrada con ID: 999"));

        mockMvc.perform(get("/api/reservations/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/reservations/{id} - Debe responder 204 No Content al cancelar exitosamente")
    void shouldCancelReservationSuccessfully() throws Exception {
        doNothing().when(reservationService).cancelReservation(50L);

        mockMvc.perform(delete("/api/reservations/50"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/reservations/{id} - Debe responder 409 Conflict si ya está cancelada o ya comenzó")
    void shouldReturnConflictWhenCancellingAlreadyCancelled() throws Exception {
        doThrow(new BusinessRuleViolationException("La reserva ya se encuentra cancelada"))
                .when(reservationService).cancelReservation(50L);

        mockMvc.perform(delete("/api/reservations/50"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @DisplayName("GET /api/users/{userId}/reservations - Debe responder 200 OK con la lista de reservas")
    void shouldGetReservationsByUserId() throws Exception {
        ReservationResponse r1 = new ReservationResponse(
                100L, 1L, 10L, ReservationStatus.CONFIRMADA, LocalDateTime.now(), null
        );
        when(reservationService.getReservationsByUserId(1L)).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/users/1/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(100));
    }

    @Test
    @DisplayName("GET /api/activities/{activityId}/reservations - Debe responder 200 OK con la lista de reservas")
    void shouldGetReservationsByActivityId() throws Exception {
        ReservationResponse r1 = new ReservationResponse(
                200L, 1L, 10L, ReservationStatus.CONFIRMADA, LocalDateTime.now(), null
        );
        when(reservationService.getReservationsByActivityId(10L)).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/activities/10/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(200));
    }
}
