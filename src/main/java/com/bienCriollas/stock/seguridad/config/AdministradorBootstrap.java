package com.bienCriollas.stock.seguridad.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.bienCriollas.stock.seguridad.dto.UsuarioRequestDTO;
import com.bienCriollas.stock.seguridad.enums.RolUsuario;
import com.bienCriollas.stock.seguridad.repository.UsuarioRepository;
import com.bienCriollas.stock.seguridad.service.UsuarioService;

@Component
public class AdministradorBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdministradorBootstrap.class);

    private final SecurityProperties properties;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    public AdministradorBootstrap(
            SecurityProperties properties,
            UsuarioRepository usuarioRepository,
            UsuarioService usuarioService) {
        this.properties = properties;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.countByRolAndActivoTrue(RolUsuario.ADMINISTRADOR) > 0) {
            return;
        }

        SecurityProperties.BootstrapAdmin admin = properties.getBootstrapAdmin();
        boolean tieneUsername = tieneTexto(admin.getUsername());
        boolean tienePassword = tieneTexto(admin.getPassword());
        if (!tieneUsername && !tienePassword) {
            LOGGER.warn("No existe un administrador activo. Configurá ADMIN_USERNAME y ADMIN_PASSWORD para crear el primero.");
            return;
        }
        if (!tieneUsername || !tienePassword) {
            throw new IllegalStateException(
                    "ADMIN_USERNAME y ADMIN_PASSWORD deben configurarse juntos");
        }

        usuarioService.crear(new UsuarioRequestDTO(
                admin.getName(), admin.getUsername(), admin.getPassword(), RolUsuario.ADMINISTRADOR));
        LOGGER.info("Administrador inicial creado correctamente: {}", admin.getUsername());
    }

    private boolean tieneTexto(String value) {
        return value != null && !value.isBlank();
    }
}
