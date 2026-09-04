package com.bienCriollas.stock.security.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.security.dto.UserRequestDTO;
import com.bienCriollas.stock.security.dto.UserResponseDTO;
import com.bienCriollas.stock.security.entity.UserAccount;
import com.bienCriollas.stock.security.enums.UserRole;
import com.bienCriollas.stock.security.exception.InvalidCredentialsException;
import com.bienCriollas.stock.security.exception.UserOperationNotAllowedException;
import com.bienCriollas.stock.security.exception.DuplicateUserException;
import com.bienCriollas.stock.security.exception.InvalidUserException;
import com.bienCriollas.stock.security.exception.UserNotFoundException;
import com.bienCriollas.stock.security.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userRepository.findByUsernameIgnoreCase(normalizeUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(!user.isActive())
                .build();
    }

    @Transactional(readOnly = true)
    public UserAccount findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(normalizeUsername(username))
                .orElseThrow(() -> new UserNotFoundException("No se encontró el usuario"));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponseDTO::from)
                .toList();
    }

    @Transactional
    public UserResponseDTO create(UserRequestDTO request) {
        String username = normalizeUsername(request.username());
        if (request.name() == null || request.name().isBlank()) {
            throw new InvalidUserException("El nombre es obligatorio");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateUserException("Ya existe un usuario con ese nombre de acceso");
        }
        validatePassword(request.password());

        UserAccount user = UserAccount.builder()
                .name(request.name().trim())
                .username(username)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .tokenVersion(0)
                .build();
        return UserResponseDTO.from(userRepository.save(user));
    }

    @Transactional
    public UserResponseDTO changeStatus(Long id, boolean active, String actorUsername) {
        UserAccount user = findById(id);
        if (!active && user.getUsername().equalsIgnoreCase(actorUsername)) {
            throw new UserOperationNotAllowedException("No podés desactivar tu propio usuario");
        }
        if (!active && user.getRole() == UserRole.ADMINISTRADOR) {
            ensureNotLastAdministrator(user);
        }

        if (user.isActive() != active) {
            user.setActive(active);
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        return UserResponseDTO.from(user);
    }

    @Transactional
    public UserResponseDTO changeRole(Long id, UserRole newRole, String actorUsername) {
        UserAccount user = findById(id);
        if (user.getRole() == newRole) {
            return UserResponseDTO.from(user);
        }
        if (user.getUsername().equalsIgnoreCase(actorUsername)) {
            throw new UserOperationNotAllowedException("No podés cambiar tu propio rol");
        }
        if (user.getRole() == UserRole.ADMINISTRADOR) {
            ensureNotLastAdministrator(user);
        }

        user.setRole(newRole);
        user.setTokenVersion(user.getTokenVersion() + 1);
        return UserResponseDTO.from(user);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        validatePassword(newPassword);
        UserAccount user = findById(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
    }

    @Transactional
    public void changeOwnPassword(String username, String currentPassword, String newPassword) {
        validatePassword(newPassword);
        UserAccount user = findByUsername(username);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new InvalidUserException("La contraseña nueva debe ser diferente de la actual");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
    }

    @Transactional
    public void revokeTokens(String username) {
        UserAccount user = findByUsername(username);
        user.setTokenVersion(user.getTokenVersion() + 1);
    }

    private UserAccount findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("No se encontró el usuario con id " + id));
    }

    private void ensureNotLastAdministrator(UserAccount user) {
        if (user.isActive()
                && userRepository.countByRoleAndActiveTrue(UserRole.ADMINISTRADOR) <= 1) {
            throw new UserOperationNotAllowedException("Debe existir al menos un administrador activo");
        }
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 10) {
            throw new InvalidUserException("La contraseña debe tener al menos 10 caracteres");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidUserException("La contraseña no puede superar 72 bytes");
        }
    }
}
