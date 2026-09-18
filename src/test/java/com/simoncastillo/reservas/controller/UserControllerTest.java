package com.simoncastillo.reservas.controller;

import com.simoncastillo.reservas.dto.user.CreateUserRequest;
import com.simoncastillo.reservas.dto.user.UpdateUserRequest;
import com.simoncastillo.reservas.dto.user.UserResponse;
import com.simoncastillo.reservas.exception.BusinessRuleViolationException;
import com.simoncastillo.reservas.exception.ResourceNotFoundException;
import com.simoncastillo.reservas.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("POST /api/users - Debe responder 201 Created y header Location con datos válidos")
    void shouldCreateUserSuccessfully() throws Exception {
        UserResponse response = new UserResponse(1L, "Juan", "Pérez", "juan@example.com");
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

        String jsonBody = """
                {
                    "firstName": "Juan",
                    "lastName": "Pérez",
                    "email": "juan@example.com"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/users/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Juan"))
                .andExpect(jsonPath("$.email").value("juan@example.com"));
    }

    @Test
    @DisplayName("POST /api/users - Debe responder 400 Bad Request cuando faltan campos o el email es inválido")
    void shouldReturn400WhenValidationFails() throws Exception {
        String invalidJson = """
                {
                    "firstName": "",
                    "lastName": "",
                    "email": "email-invalido"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Debe responder 200 OK si el usuario existe")
    void shouldReturnUserWhenExists() throws Exception {
        UserResponse response = new UserResponse(10L, "Juan", "Pérez", "juan@example.com");
        when(userService.getUserById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/users/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.email").value("juan@example.com"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Debe responder 404 Not Found si el usuario no existe")
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("Usuario no encontrado con ID: 99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(containsString("99")));
    }

    @Test
    @DisplayName("GET /api/users - Debe responder 200 OK con la lista de usuarios")
    void shouldReturnAllUsers() throws Exception {
        UserResponse u1 = new UserResponse(1L, "Juan", "Pérez", "juan@example.com");
        UserResponse u2 = new UserResponse(2L, "Ana", "Gómez", "ana@example.com");
        when(userService.getAllUsers()).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("juan@example.com"))
                .andExpect(jsonPath("$[1].email").value("ana@example.com"));
    }

    @Test
    @DisplayName("PUT /api/users/{id} - Debe responder 200 OK al actualizar")
    void shouldUpdateUserSuccessfully() throws Exception {
        UserResponse response = new UserResponse(1L, "Juan Carlos", "Pérez", "nuevo@example.com");
        when(userService.updateUser(eq(1L), any(UpdateUserRequest.class))).thenReturn(response);

        String updateJson = """
                {
                    "firstName": "Juan Carlos",
                    "lastName": "Pérez",
                    "email": "nuevo@example.com"
                }
                """;

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Juan Carlos"))
                .andExpect(jsonPath("$.email").value("nuevo@example.com"));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Debe responder 204 No Content al eliminar sin reservas")
    void shouldDeleteUserSuccessfully() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Debe responder 409 Conflict si el usuario posee reservas históricas")
    void shouldReturn409WhenUserHasReservations() throws Exception {
        doThrow(new BusinessRuleViolationException("No se puede eliminar el usuario porque posee reservas históricas"))
                .when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));
    }
}
