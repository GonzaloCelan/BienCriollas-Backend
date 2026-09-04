package com.bienCriollas.stock.security.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.bienCriollas.stock.security.dto.UserRequestDTO;
import com.bienCriollas.stock.security.enums.UserRole;
import com.bienCriollas.stock.security.repository.UserRepository;
import com.bienCriollas.stock.security.service.UserService;

@Component
public class AdministratorBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdministratorBootstrap.class);

    private final SecurityProperties properties;
    private final UserRepository userRepository;
    private final UserService userService;

    public AdministratorBootstrap(
            SecurityProperties properties,
            UserRepository userRepository,
            UserService userService) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.countByRoleAndActiveTrue(UserRole.ADMINISTRADOR) > 0) {
            return;
        }

        SecurityProperties.BootstrapAdmin admin = properties.getBootstrapAdmin();
        boolean hasUsername = hasText(admin.getUsername());
        boolean hasPassword = hasText(admin.getPassword());
        if (!hasUsername && !hasPassword) {
            LOGGER.warn("No existe un administrador activo. Configurá ADMIN_USERNAME y ADMIN_PASSWORD para crear el primero.");
            return;
        }
        if (!hasUsername || !hasPassword) {
            throw new IllegalStateException(
                    "ADMIN_USERNAME y ADMIN_PASSWORD deben configurarse juntos");
        }

        userService.create(new UserRequestDTO(
                admin.getName(), admin.getUsername(), admin.getPassword(), UserRole.ADMINISTRADOR));
        LOGGER.info("Administrador inicial creado correctamente: {}", admin.getUsername());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
