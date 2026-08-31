package com.bienCriollas.stock.seguridad;

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

import com.bienCriollas.stock.pedido.entity.Pedido;
import com.bienCriollas.stock.pedido.enums.TipoEstado;
import com.bienCriollas.stock.pedido.enums.TipoPago;
import com.bienCriollas.stock.pedido.enums.TipoVenta;
import com.bienCriollas.stock.pedido.repository.PedidoRepository;
import com.bienCriollas.stock.pedido.repository.RegularizacionPedidoRepository;
import com.bienCriollas.stock.seguridad.dto.LoginRequestDTO;
import com.bienCriollas.stock.seguridad.dto.UsuarioRequestDTO;
import com.bienCriollas.stock.seguridad.enums.RolUsuario;
import com.bienCriollas.stock.seguridad.repository.UsuarioRepository;
import com.bienCriollas.stock.seguridad.service.AuthService;
import com.bienCriollas.stock.seguridad.service.UsuarioService;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    private static final String PASSWORD = "clave-segura-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private RegularizacionPedidoRepository regularizacionPedidoRepository;

    @BeforeEach
    void configurarUsuarios() {
        regularizacionPedidoRepository.deleteAll();
        pedidoRepository.deleteAll();
        usuarioRepository.deleteAll();
        usuarioService.crear(new UsuarioRequestDTO(
                "Administrador", "admin", PASSWORD, RolUsuario.ADMINISTRADOR));
        usuarioService.crear(new UsuarioRequestDTO(
                "Empleado", "empleado", PASSWORD, RolUsuario.EMPLEADO));
    }

    @Test
    void loginDevuelveTokenYRechazaCredencialesInvalidas() throws Exception {
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
    void protegeLaApiYRespetaElRolEmpleado() throws Exception {
        mockMvc.perform(get("/api/v2/catalogo"))
                .andExpect(status().isUnauthorized());

        String tokenEmpleado = token("empleado");
        mockMvc.perform(get("/api/v2/catalogo")
                        .header("Authorization", "Bearer " + tokenEmpleado))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v2/catalogo/1")
                        .header("Authorization", "Bearer " + tokenEmpleado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloAdministradorPuedeGestionarUsuarios() throws Exception {
        mockMvc.perform(get("/api/v2/usuarios")
                        .header("Authorization", "Bearer " + token("empleado")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v2/usuarios")
                        .header("Authorization", "Bearer " + token("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void soloAdministradorPuedeRegularizarPedidos() throws Exception {
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
    void administradorRegularizaPedidoYQuedaAuditado() throws Exception {
        Pedido pedido = pedidoRepository.save(Pedido.builder()
                .cliente("Cliente de prueba")
                .tipoVenta(TipoVenta.PARTICULAR)
                .tipoPago(TipoPago.EFECTIVO)
                .montoEfectivo(new BigDecimal("8500.00"))
                .montoTransferencia(BigDecimal.ZERO)
                .totalPedido(new BigDecimal("8500.00"))
                .estado(TipoEstado.PREPARADO)
                .fechaCreacion(LocalDate.of(2026, 8, 20))
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
                                """.formatted(pedido.getIdPedido())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadActualizada").value(1))
                .andExpect(jsonPath("$.ingresoIncorporado").value(8500.00))
                .andExpect(jsonPath("$.realizadoPor").value("admin"));

        Pedido actualizado = pedidoRepository.findById(pedido.getIdPedido()).orElseThrow();
        assertEquals(TipoEstado.ENTREGADO, actualizado.getEstado());
        assertEquals(1, regularizacionPedidoRepository.count());
    }

    @Test
    void logoutRevocaElTokenInmediatamente() throws Exception {
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
