package com.bienCriollas.stock.pedido.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.bienCriollas.stock.pedido.dto.RegularizacionPedidoConsultaDTO;
import com.bienCriollas.stock.pedido.dto.RegularizacionPedidoResultadoDTO;
import com.bienCriollas.stock.pedido.dto.RegularizarPedidosRequestDTO;
import com.bienCriollas.stock.pedido.entity.Pedido;
import com.bienCriollas.stock.pedido.entity.RegularizacionPedido;
import com.bienCriollas.stock.pedido.enums.TipoEstado;
import com.bienCriollas.stock.pedido.enums.TipoPago;
import com.bienCriollas.stock.pedido.enums.TipoVenta;
import com.bienCriollas.stock.pedido.repository.PedidoRepository;
import com.bienCriollas.stock.pedido.repository.RegularizacionPedidoRepository;

@ExtendWith(MockitoExtension.class)
class RegularizacionPedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private RegularizacionPedidoRepository regularizacionPedidoRepository;

    @InjectMocks
    private RegularizacionPedidoService service;

    @Test
    void consultarMesDevuelvePendientesPreparadosYTotalPotencial() {
        Pedido pendiente = pedido(1L, TipoEstado.PENDIENTE, "3500.00");
        Pedido preparado = pedido(2L, TipoEstado.PREPARADO, "6500.00");
        when(pedidoRepository
                .findByEstadoInAndFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(
                        any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pendiente, preparado)));
        when(pedidoRepository.sumarTotalPorEstadosYPeriodo(any(), any(), any()))
                .thenReturn(new BigDecimal("10000.00"));

        RegularizacionPedidoConsultaDTO resultado = service.consultarMes(2026, 8, 0, 50);

        assertEquals(2, resultado.cantidadPedidos());
        assertEquals(new BigDecimal("10000.00"), resultado.ingresoPotencial());
        assertEquals(List.of(1L, 2L), resultado.pedidos().stream()
                .map(item -> item.idPedido())
                .toList());
    }

    @Test
    void regularizarActualizaTodoElLoteYRegistraAuditoria() {
        Pedido pendiente = pedido(1L, TipoEstado.PENDIENTE, "3500.00");
        Pedido preparado = pedido(2L, TipoEstado.PREPARADO, "6500.00");
        when(pedidoRepository.findAllByIdParaRegularizar(any()))
                .thenReturn(List.of(pendiente, preparado));
        RegularizarPedidosRequestDTO request = new RegularizarPedidosRequestDTO(
                2026,
                8,
                List.of(1L, 2L),
                "Cierre de agosto",
                true);

        RegularizacionPedidoResultadoDTO resultado = service.regularizar(request, "admin");

        assertEquals(TipoEstado.ENTREGADO, pendiente.getEstado());
        assertEquals(TipoEstado.ENTREGADO, preparado.getEstado());
        assertEquals(2, resultado.cantidadActualizada());
        assertEquals(new BigDecimal("10000.00"), resultado.ingresoIncorporado());
        assertEquals("admin", resultado.realizadoPor());
        assertEquals(List.of(1L, 2L), resultado.idsPedidos());
        verify(pedidoRepository).saveAll(List.of(pendiente, preparado));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RegularizacionPedido>> captor = ArgumentCaptor.forClass(List.class);
        verify(regularizacionPedidoRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals(TipoEstado.PENDIENTE, captor.getValue().get(0).getEstadoAnterior());
        assertEquals(TipoEstado.PREPARADO, captor.getValue().get(1).getEstadoAnterior());
        assertEquals(resultado.idLote(), captor.getValue().get(0).getIdLote());
        assertEquals(resultado.idLote(), captor.getValue().get(1).getIdLote());
    }

    @Test
    void regularizarRechazaTodoElLoteSiUnPedidoYaNoEsRegularizable() {
        Pedido pendiente = pedido(1L, TipoEstado.PENDIENTE, "3500.00");
        Pedido cancelado = pedido(2L, TipoEstado.CANCELADO, "6500.00");
        when(pedidoRepository.findAllByIdParaRegularizar(any()))
                .thenReturn(List.of(pendiente, cancelado));
        RegularizarPedidosRequestDTO request = new RegularizarPedidosRequestDTO(
                2026,
                8,
                List.of(1L, 2L),
                "Cierre de agosto",
                true);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.regularizar(request, "admin"));

        assertEquals("El pedido 2 ya no está PENDIENTE o PREPARADO", error.getMessage());
        assertEquals(TipoEstado.PENDIENTE, pendiente.getEstado());
        verify(pedidoRepository, never()).saveAll(any());
        verify(regularizacionPedidoRepository, never()).saveAll(any());
    }

    private Pedido pedido(Long id, TipoEstado estado, String total) {
        return Pedido.builder()
                .idPedido(id)
                .fechaCreacion(LocalDate.of(2026, 8, 15))
                .cliente("Cliente " + id)
                .tipoVenta(TipoVenta.PARTICULAR)
                .tipoPago(TipoPago.EFECTIVO)
                .montoEfectivo(new BigDecimal(total))
                .montoTransferencia(BigDecimal.ZERO)
                .totalPedido(new BigDecimal(total))
                .estado(estado)
                .build();
    }
}
