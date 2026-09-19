package com.simoncastillo.reservas.controller;

import com.simoncastillo.reservas.dto.attendance.AttendanceResponse;
import com.simoncastillo.reservas.dto.attendance.UpdateAttendanceRequest;
import com.simoncastillo.reservas.entity.AttendanceStatus;
import com.simoncastillo.reservas.exception.BusinessRuleViolationException;
import com.simoncastillo.reservas.exception.ResourceNotFoundException;
import com.simoncastillo.reservas.service.AttendanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttendanceController.class)
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceService attendanceService;

    @Test
    @DisplayName("PUT /api/reservations/{id}/attendance - Debe responder 200 OK al registrar asistencia exitosamente")
    void shouldRegisterAttendanceSuccessfully() throws Exception {
        AttendanceResponse response = new AttendanceResponse(
                100L, 50L, AttendanceStatus.PRESENTE, LocalDateTime.now()
        );

        when(attendanceService.registerAttendance(eq(50L), any(UpdateAttendanceRequest.class)))
                .thenReturn(response);

        String jsonBody = """
                {
                    "status": "PRESENTE"
                }
                """;

        mockMvc.perform(put("/api/reservations/50/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.reservationId").value(50))
                .andExpect(jsonPath("$.status").value("PRESENTE"));
    }

    @Test
    @DisplayName("PUT /api/reservations/{id}/attendance - Debe responder 400 Bad Request si el status es nulo")
    void shouldReturnBadRequestWhenStatusIsNull() throws Exception {
        String jsonBody = """
                {
                    "status": null
                }
                """;

        mockMvc.perform(put("/api/reservations/50/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PUT /api/reservations/{id}/attendance - Debe responder 404 Not Found si la reserva no existe")
    void shouldReturnNotFoundWhenReservationDoesNotExist() throws Exception {
        when(attendanceService.registerAttendance(eq(999L), any(UpdateAttendanceRequest.class)))
                .thenThrow(new ResourceNotFoundException("Reserva no encontrada con ID: 999"));

        String jsonBody = """
                {
                    "status": "PRESENTE"
                }
                """;

        mockMvc.perform(put("/api/reservations/999/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Reserva no encontrada con ID: 999"));
    }

    @Test
    @DisplayName("PUT /api/reservations/{id}/attendance - Debe responder 409 Conflict si la transición de estado es inválida")
    void shouldReturnConflictWhenInvalidStateTransition() throws Exception {
        when(attendanceService.registerAttendance(eq(50L), any(UpdateAttendanceRequest.class)))
                .thenThrow(new BusinessRuleViolationException("El estado de asistencia ya fue registrado (PRESENTE) y no admite modificaciones posteriores"));

        String jsonBody = """
                {
                    "status": "AUSENTE"
                }
                """;

        mockMvc.perform(put("/api/reservations/50/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @DisplayName("GET /api/activities/{activityId}/attendance - Debe responder 200 OK con la lista de asistencias")
    void shouldGetAttendanceByActivityId() throws Exception {
        AttendanceResponse response = new AttendanceResponse(
                100L, 50L, AttendanceStatus.PRESENTE, LocalDateTime.now()
        );

        when(attendanceService.getAttendanceByActivityId(10L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/activities/10/attendance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(100));
    }

    @Test
    @DisplayName("GET /api/activities/{activityId}/attendance - Debe responder 404 Not Found si la actividad no existe")
    void shouldReturnNotFoundWhenActivityNotFound() throws Exception {
        when(attendanceService.getAttendanceByActivityId(999L))
                .thenThrow(new ResourceNotFoundException("Actividad no encontrada con ID: 999"));

        mockMvc.perform(get("/api/activities/999/attendance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/users/{userId}/attendance - Debe responder 200 OK con la lista de asistencias")
    void shouldGetAttendanceByUserId() throws Exception {
        AttendanceResponse response = new AttendanceResponse(
                100L, 50L, AttendanceStatus.PRESENTE, LocalDateTime.now()
        );

        when(attendanceService.getAttendanceByUserId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/users/1/attendance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(100));
    }

    @Test
    @DisplayName("GET /api/users/{userId}/attendance - Debe responder 404 Not Found si el usuario no existe")
    void shouldReturnNotFoundWhenUserNotFound() throws Exception {
        when(attendanceService.getAttendanceByUserId(999L))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado con ID: 999"));

        mockMvc.perform(get("/api/users/999/attendance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }
}
