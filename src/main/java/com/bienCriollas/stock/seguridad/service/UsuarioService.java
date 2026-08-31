package com.bienCriollas.stock.seguridad.service;

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

import com.bienCriollas.stock.seguridad.dto.UsuarioRequestDTO;
import com.bienCriollas.stock.seguridad.dto.UsuarioResponseDTO;
import com.bienCriollas.stock.seguridad.entity.Usuario;
import com.bienCriollas.stock.seguridad.enums.RolUsuario;
import com.bienCriollas.stock.seguridad.exception.CredencialesInvalidasException;
import com.bienCriollas.stock.seguridad.exception.UsuarioDuplicadoException;
import com.bienCriollas.stock.seguridad.exception.UsuarioNoEncontradoException;
import com.bienCriollas.stock.seguridad.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(normalizarUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .roles(usuario.getRol().name())
                .disabled(!usuario.isActivo())
                .build();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorUsername(String username) {
        return usuarioRepository.findByUsernameIgnoreCase(normalizarUsername(username))
                .orElseThrow(() -> new UsuarioNoEncontradoException("No se encontró el usuario"));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponseDTO::desde)
                .toList();
    }

    @Transactional
    public UsuarioResponseDTO crear(UsuarioRequestDTO request) {
        String username = normalizarUsername(request.username());
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsuarioDuplicadoException("Ya existe un usuario con ese nombre de acceso");
        }
        validarPassword(request.password());

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre().trim())
                .username(username)
                .passwordHash(passwordEncoder.encode(request.password()))
                .rol(request.rol())
                .activo(true)
                .tokenVersion(0)
                .build();
        return UsuarioResponseDTO.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDTO cambiarEstado(Long id, boolean activo, String actorUsername) {
        Usuario usuario = buscarPorId(id);
        if (!activo && usuario.getUsername().equalsIgnoreCase(actorUsername)) {
            throw new IllegalStateException("No podés desactivar tu propio usuario");
        }
        if (!activo && usuario.getRol() == RolUsuario.ADMINISTRADOR) {
            asegurarQueNoSeaUltimoAdministrador(usuario);
        }

        if (usuario.isActivo() != activo) {
            usuario.setActivo(activo);
            usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        }
        return UsuarioResponseDTO.desde(usuario);
    }

    @Transactional
    public UsuarioResponseDTO cambiarRol(Long id, RolUsuario nuevoRol, String actorUsername) {
        Usuario usuario = buscarPorId(id);
        if (usuario.getRol() == nuevoRol) {
            return UsuarioResponseDTO.desde(usuario);
        }
        if (usuario.getUsername().equalsIgnoreCase(actorUsername)) {
            throw new IllegalStateException("No podés cambiar tu propio rol");
        }
        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) {
            asegurarQueNoSeaUltimoAdministrador(usuario);
        }

        usuario.setRol(nuevoRol);
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        return UsuarioResponseDTO.desde(usuario);
    }

    @Transactional
    public void restablecerPassword(Long id, String nuevaPassword) {
        validarPassword(nuevaPassword);
        Usuario usuario = buscarPorId(id);
        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
    }

    @Transactional
    public void cambiarMiPassword(String username, String passwordActual, String passwordNueva) {
        validarPassword(passwordNueva);
        Usuario usuario = buscarPorUsername(username);
        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        if (passwordEncoder.matches(passwordNueva, usuario.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña nueva debe ser diferente de la actual");
        }

        usuario.setPasswordHash(passwordEncoder.encode(passwordNueva));
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
    }

    @Transactional
    public void revocarTokens(String username) {
        Usuario usuario = buscarPorUsername(username);
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
    }

    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException("No se encontró el usuario con id " + id));
    }

    private void asegurarQueNoSeaUltimoAdministrador(Usuario usuario) {
        if (usuario.isActivo()
                && usuarioRepository.countByRolAndActivoTrue(RolUsuario.ADMINISTRADOR) <= 1) {
            throw new IllegalStateException("Debe existir al menos un administrador activo");
        }
    }

    private String normalizarUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void validarPassword(String password) {
        if (password == null || password.length() < 10) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 10 caracteres");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña no puede superar 72 bytes");
        }
    }
}
