package com.simoncastillo.reservas.service;

import com.simoncastillo.reservas.dto.user.CreateUserRequest;
import com.simoncastillo.reservas.dto.user.UpdateUserRequest;
import com.simoncastillo.reservas.dto.user.UserResponse;
import com.simoncastillo.reservas.entity.User;
import com.simoncastillo.reservas.exception.BusinessRuleViolationException;
import com.simoncastillo.reservas.exception.DuplicateResourceException;
import com.simoncastillo.reservas.exception.ResourceNotFoundException;
import com.simoncastillo.reservas.repository.ReservationRepository;
import com.simoncastillo.reservas.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {


    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    public UserService(UserRepository userRepository, ReservationRepository reservationRepository) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest
                                           request) {
        if (userRepository.existsByEmail(request.email()))
        {
            throw new DuplicateResourceException("Ya existe un usuario con el email: " + request.email());
        }

        User user = new User(request.lastName(), request.
                firstName(), request.email());
        User savedUser = userRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }
    public UserResponse getUserById(Long id) {
        User user = findUserById(id);
        return UserResponse.fromEntity(user);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new
                        ResourceNotFoundException("Usuario no encontrado con ID:" + id));
    }
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::fromEntity).toList();
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUserById(id);

        if (!user.getEmail().equalsIgnoreCase(request.
                email()) && userRepository.existsByEmail(request.
                email())) {
            throw new DuplicateResourceException("Ya existe otro usuario con el email: " + request.email());
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());

        User updatedUser = userRepository.save(user);
        return UserResponse.fromEntity(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUserById(id);

        if (!reservationRepository.findByUserId(id).
                isEmpty()) {
            throw new BusinessRuleViolationException("No se puede eliminar el usuario con ID " + id + " porque posee reservas históricas registradas");
        }

        userRepository.delete(user);
    }

}
