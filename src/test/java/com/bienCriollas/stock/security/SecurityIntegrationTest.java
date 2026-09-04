package com.bienCriollas.stock.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bienCriollas.stock.order.entity.Order;
import com.bienCriollas.stock.order.enums.OrderStatus;
import com.bienCriollas.stock.order.enums.PaymentType;
import com.bienCriollas.stock.order.enums.SaleType;
import com.bienCriollas.stock.order.repository.OrderRepository;
import com.bienCriollas.stock.order.repository.OrderReconciliationRepository;
import com.bienCriollas.stock.security.dto.LoginRequestDTO;
import com.bienCriollas.stock.security.dto.UserRequestDTO;
import com.bienCriollas.stock.security.enums.UserRole;
import com.bienCriollas.stock.security.repository.UserRepository;
import com.bienCriollas.stock.security.service.AuthService;
import com.bienCriollas.stock.security.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    private static final String PASSWORD = "clave-segura-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderReconciliationRepository orderReconciliationRepository;

    @BeforeEach
    void setUpUsers() {
        orderReconciliationRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        userService.create(new UserRequestDTO(
                "Administrador", "admin", PASSWORD, UserRole.ADMINISTRADOR));
        userService.create(new UserRequestDTO(
                "Empleado", "empleado", PASSWORD, UserRole.EMPLEADO));
    }

    @Test
    void loginReturnsTokenAndRejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v2/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"clave-segura-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol").value("ADMINISTRADOR"));

        mockMvc.perform(post("/api/v2/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"incorrecta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void protectsApiAndRespectsEmployeeRole() throws Exception {
        mockMvc.perform(get("/api/v2/catalogo"))
                .andExpect(status().isUnauthorized());

        String employeeToken = token("empleado");
        mockMvc.perform(get("/api/v2/catalogo")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v2/catalogo/1")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdministratorCanManageUsers() throws Exception {
        mockMvc.perform(get("/api/v2/usuarios")
                        .header("Authorization", "Bearer " + token("empleado")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v2/usuarios")
                        .header("Authorization", "Bearer " + token("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void onlyAdministratorCanReconcileOrders() throws Exception {
        mockMvc.perform(get("/api/v2/pedido/regularizacion")
                        .param("anio", "2026")
                        .param("mes", "8")
                        .header("Authorization", "Bearer " + token("empleado")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v2/pedido/regularizacion/entregar")
                        .header("Authorization", "Bearer " + token("empleado"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "anio": 2026,
                                  "mes": 8,
                                  "idsPedidos": [1],
                                  "motivo": "Regularización mensual",
                                  "confirmar": true
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v2/pedido/regularizacion")
                        .param("anio", "2026")
                        .param("mes", "8")
                        .header("Authorization", "Bearer " + token("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anio").value(2026))
                .andExpect(jsonPath("$.mes").value(8));
    }

    @Test
    void administratorReconcilesOrderAndCreatesAuditRecord() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .customer("Cliente de prueba")
                .saleType(SaleType.PARTICULAR)
                .paymentType(PaymentType.EFECTIVO)
                .cashAmount(new BigDecimal("8500.00"))
                .transferAmount(BigDecimal.ZERO)
                .orderTotal(new BigDecimal("8500.00"))
                .status(OrderStatus.PREPARADO)
                .creationDate(LocalDate.of(2026, 8, 20))
                .build());

        mockMvc.perform(patch("/api/v2/pedido/regularizacion/entregar")
                        .header("Authorization", "Bearer " + token("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "anio": 2026,
                                  "mes": 8,
                                  "idsPedidos": [%d],
                                  "motivo": "Regularización integral",
                                  "confirmar": true
                                }
                                """.formatted(order.getOrderId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadActualizada").value(1))
                .andExpect(jsonPath("$.ingresoIncorporado").value(8500.00))
                .andExpect(jsonPath("$.realizadoPor").value("admin"));

        Order updated = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.ENTREGADO, updated.getStatus());
        assertEquals(1, orderReconciliationRepository.count());
    }

    @Test
    void logoutRevokesTokenImmediately() throws Exception {
        String token = token("empleado");

        mockMvc.perform(post("/api/v2/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v2/catalogo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String token(String username) {
        return authService.login(new LoginRequestDTO(username, PASSWORD)).accessToken();
    }
}
