package com.simoncastillo.reservas.controller;

import com.simoncastillo.reservas.dto.activity.ActivityResponse;
import com.simoncastillo.reservas.dto.activity.CreateActivityRequest;
import com.simoncastillo.reservas.dto.activity.UpdateActivityRequest;
import com.simoncastillo.reservas.entity.ActivityStatus;
import com.simoncastillo.reservas.exception.BusinessRuleViolationException;
import com.simoncastillo.reservas.exception.ResourceNotFoundException;
import com.simoncastillo.reservas.service.ActivityService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivityController.class)
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    @Test
    @DisplayName("POST /api/activities - Debe responder 201 Created y Location con datos válidos")
    void shouldCreateActivitySuccessfully() throws Exception {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(2);
        ActivityResponse response = new ActivityResponse(
                1L, "Yoga", "YOGA", futureDate, 60, 20, 0, 20, ActivityStatus.PROGRAMADA
        );

        when(activityService.createActivity(any(CreateActivityRequest.class))).thenReturn(response);

        String jsonBody = """
                {
                    "name": "Yoga",
                    "sport": "YOGA",
                    "startAt": "%s",
                    "durationMinutes": 60,
                    "capacity": 20
                }
                """.formatted(futureDate.toString());

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/activities/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Yoga"))
                .andExpect(jsonPath("$.availableSpots").value(20));
    }

    @Test
    @DisplayName("POST /api/activities - Debe responder 400 Bad Request cuando la fecha está en el pasado")
    void shouldReturn400WhenStartDateInPast() throws Exception {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        String invalidJson = """
                {
                    "name": "Yoga",
                    "sport": "YOGA",
                    "startAt": "%s",
                    "durationMinutes": 60,
                    "capacity": 20
                }
                """.formatted(pastDate.toString());

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/activities/{id} - Debe responder 200 OK con cupos calculados si existe")
    void shouldReturnActivityWhenExists() throws Exception {
        LocalDateTime date = LocalDateTime.now().plusDays(2);
        ActivityResponse response = new ActivityResponse(
                10L, "Spinning", "SPINNING", date, 45, 15, 5, 10, ActivityStatus.PROGRAMADA
        );

        when(activityService.getActivityById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/activities/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.reservedSpots").value(5))
                .andExpect(jsonPath("$.availableSpots").value(10));
    }

    @Test
    @DisplayName("GET /api/activities/{id} - Debe responder 404 Not Found si no existe")
    void shouldReturn404WhenNotFound() throws Exception {
        when(activityService.getActivityById(99L)).thenThrow(new ResourceNotFoundException("Actividad no encontrada con ID: 99"));

        mockMvc.perform(get("/api/activities/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/activities - Debe responder 200 OK con lista filtrada")
    void shouldReturnFilteredActivities() throws Exception {
        LocalDateTime date = LocalDateTime.now().plusDays(1);
        ActivityResponse a1 = new ActivityResponse(
                1L, "Yoga", "YOGA", date, 60, 20, 0, 20, ActivityStatus.PROGRAMADA
        );

        when(activityService.getActivities(eq("YOGA"), any(), any(), any())).thenReturn(List.of(a1));

        mockMvc.perform(get("/api/activities").param("sport", "YOGA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sport").value("YOGA"));
    }

    @Test
    @DisplayName("PUT /api/activities/{id} - Debe responder 200 OK al actualizar")
    void shouldUpdateActivitySuccessfully() throws Exception {
        LocalDateTime date = LocalDateTime.now().plusDays(3);
        ActivityResponse response = new ActivityResponse(
                1L, "Yoga Pro", "YOGA", date, 90, 25, 0, 25, ActivityStatus.PROGRAMADA
        );

        when(activityService.updateActivity(eq(1L), any(UpdateActivityRequest.class))).thenReturn(response);

        String updateJson = """
                {
                    "name": "Yoga Pro",
                    "sport": "YOGA",
                    "startAt": "%s",
                    "durationMinutes": 90,
                    "capacity": 25
                }
                """.formatted(date.toString());

        mockMvc.perform(put("/api/activities/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Yoga Pro"))
                .andExpect(jsonPath("$.capacity").value(25));
    }

    @Test
    @DisplayName("DELETE /api/activities/{id} - Debe responder 204 No Content al cancelar")
    void shouldCancelActivitySuccessfully() throws Exception {
        doNothing().when(activityService).cancelActivity(1L);

        mockMvc.perform(delete("/api/activities/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/activities/{id} - Debe responder 409 Conflict si ya comenzó")
    void shouldReturn409WhenCancellingStartedActivity() throws Exception {
        doThrow(new BusinessRuleViolationException("No se puede cancelar una actividad que ya ha comenzado"))
                .when(activityService).cancelActivity(1L);

        mockMvc.perform(delete("/api/activities/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));
    }
}
