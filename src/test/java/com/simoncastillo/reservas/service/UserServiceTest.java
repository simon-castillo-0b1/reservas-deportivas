package com.simoncastillo.reservas.service;

import com.simoncastillo.reservas.dto.user.CreateUserRequest;
import com.simoncastillo.reservas.dto.user.UpdateUserRequest;
import com.simoncastillo.reservas.dto.user.UserResponse;
import com.simoncastillo.reservas.entity.Activity;
import com.simoncastillo.reservas.entity.Reservation;
import com.simoncastillo.reservas.entity.User;
import com.simoncastillo.reservas.exception.BusinessRuleViolationException;
import com.simoncastillo.reservas.exception.DuplicateResourceException;
import com.simoncastillo.reservas.exception.ResourceNotFoundException;
import com.simoncastillo.reservas.repository.ReservationRepository;
import com.simoncastillo.reservas.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("Tests de creación de usuario (RF-USR-001)")
    class CreateUserTests {

        @Test
        @DisplayName("Debe crear un usuario exitosamente cuando el email no existe")
        void shouldCreateUserSuccessfully() {
            CreateUserRequest request = new CreateUserRequest("Juan", "Pérez", "juan@example.com");

            when(userRepository.existsByEmail("juan@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(1L);
                return u;
            });

            UserResponse response = userService.createUser(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.firstName()).isEqualTo("Juan");
            assertThat(response.lastName()).isEqualTo("Pérez");
            assertThat(response.email()).isEqualTo("juan@example.com");

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Debe lanzar DuplicateResourceException si el email ya existe")
        void shouldThrowDuplicateResourceExceptionWhenEmailExists() {
            CreateUserRequest request = new CreateUserRequest("Juan", "Pérez", "juan@example.com");

            when(userRepository.existsByEmail("juan@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("juan@example.com");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Tests de consulta de usuario (RF-USR-002 y RF-USR-003)")
    class GetUserTests {

        @Test
        @DisplayName("Debe retornar el usuario cuando el ID existe")
        void shouldReturnUserWhenIdExists() {
            User user = new User("Pérez", "Juan", "juan@example.com");
            user.setId(10L);

            when(userRepository.findById(10L)).thenReturn(Optional.of(user));

            UserResponse response = userService.getUserById(10L);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.email()).isEqualTo("juan@example.com");
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException cuando el ID no existe")
        void shouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("Debe listar todos los usuarios")
        void shouldReturnAllUsers() {
            User u1 = new User("Pérez", "Juan", "juan@example.com");
            u1.setId(1L);
            User u2 = new User("Gómez", "Ana", "ana@example.com");
            u2.setId(2L);

            when(userRepository.findAll()).thenReturn(List.of(u1, u2));

            List<UserResponse> list = userService.getAllUsers();

            assertThat(list).hasSize(2);
            assertThat(list.get(0).email()).isEqualTo("juan@example.com");
            assertThat(list.get(1).email()).isEqualTo("ana@example.com");
        }
    }

    @Nested
    @DisplayName("Tests de actualización de usuario (RF-USR-004)")
    class UpdateUserTests {

        @Test
        @DisplayName("Debe actualizar los datos correctamente")
        void shouldUpdateUserSuccessfully() {
            User user = new User("Pérez", "Juan", "juan@example.com");
            user.setId(1L);

            UpdateUserRequest request = new UpdateUserRequest("Juan Carlos", "Pérez Gómez", "nuevo@example.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail("nuevo@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userService.updateUser(1L, request);

            assertThat(response.firstName()).isEqualTo("Juan Carlos");
            assertThat(response.lastName()).isEqualTo("Pérez Gómez");
            assertThat(response.email()).isEqualTo("nuevo@example.com");
        }

        @Test
        @DisplayName("Debe rechazar actualización si el nuevo email ya está en uso por otro usuario")
        void shouldThrowDuplicateWhenNewEmailAlreadyInUse() {
            User user = new User("Pérez", "Juan", "juan@example.com");
            user.setId(1L);

            UpdateUserRequest request = new UpdateUserRequest("Juan", "Pérez", "otro@example.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail("otro@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(1L, request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("otro@example.com");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Tests de eliminación de usuario (RF-USR-005)")
    class DeleteUserTests {

        @Test
        @DisplayName("Debe eliminar el usuario si no tiene reservas históricas")
        void shouldDeleteUserWhenNoReservations() {
            User user = new User("Pérez", "Juan", "juan@example.com");
            user.setId(1L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(reservationRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

            userService.deleteUser(1L);

            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("Debe lanzar BusinessRuleViolationException si el usuario tiene reservas asociadas")
        void shouldThrowBusinessRuleViolationWhenUserHasReservations() {
            User user = new User("Pérez", "Juan", "juan@example.com");
            user.setId(1L);

            Activity activity = new Activity("Yoga", "YOGA", LocalDateTime.now().plusDays(1), 60, 20);
            Reservation reservation = new Reservation(user, activity);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(reservationRepository.findByUserId(1L)).thenReturn(List.of(reservation));

            assertThatThrownBy(() -> userService.deleteUser(1L))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("reservas históricas");

            verify(userRepository, never()).delete(any(User.class));
        }
    }
}
