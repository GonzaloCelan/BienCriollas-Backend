package com.bienCriollas.stock.pedido.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bienCriollas.stock.pedido.dto.PedidoDetalleRequestDTO;
import com.bienCriollas.stock.pedido.dto.PedidoRequestDTO;
import com.bienCriollas.stock.pedido.dto.PedidoResponseDTO;
import com.bienCriollas.stock.pedido.entity.DetallePedido;
import com.bienCriollas.stock.pedido.entity.Pedido;
import com.bienCriollas.stock.pedido.enums.TipoEstado;
import com.bienCriollas.stock.pedido.enums.TipoPago;
import com.bienCriollas.stock.pedido.enums.TipoVenta;
import com.bienCriollas.stock.pedido.exception.PedidoNoEncontradoException;
import com.bienCriollas.stock.pedido.repository.PedidoDetalleRepository;
import com.bienCriollas.stock.pedido.repository.PedidoRepository;
import com.bienCriollas.stock.stock.service.StockService;
import com.bienCriollas.stock.variedad.entity.VariedadEmpanada;
import com.bienCriollas.stock.variedad.repository.VariedadEmpanadaRepository;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private StockService stockService;

    @Mock
    private PedidoDetalleRepository detallePedidoRepository;

    @Mock
    private VariedadEmpanadaRepository variedadEmpanadaRepository;

    @InjectMocks
    private PedidoService pedidoService;

    @Test
    void crearPedidoGuardaYRetornaElHorarioDeEntrega() {
        LocalTime horarioEntrega = LocalTime.of(21, 30);
        VariedadEmpanada variedad = VariedadEmpanada.builder()
                .id_variedad(2L)
                .nombre("Pollo")
                .build();
        PedidoRequestDTO request = new PedidoRequestDTO(
                "Cliente",
                "PARTICULAR",
                "EFECTIVO",
                null,
                horarioEntrega,
                null,
                null,
                new BigDecimal("9000"),
                List.of(new PedidoDetalleRequestDTO(2L, 6)));

        when(variedadEmpanadaRepository.findById(2L)).thenReturn(Optional.of(variedad));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            pedido.setIdPedido(10L);
            return pedido;
        });

        PedidoResponseDTO response = pedidoService.crearPedido(request);

        assertEquals(horarioEntrega, response.horaEntrega());
        verify(pedidoRepository).save(any(Pedido.class));
        verify(stockService).ajustarDisponibilidad(eq(Map.of(2L, -6)));
    }

    @Test
    void actualizarPedidoReemplazaDatosDetallesYStock() {
        VariedadEmpanada variedadAnterior = VariedadEmpanada.builder()
                .id_variedad(1L)
                .nombre("Carne")
                .build();
        VariedadEmpanada variedadNueva = VariedadEmpanada.builder()
                .id_variedad(2L)
                .nombre("Pollo")
                .build();

        Pedido pedido = Pedido.builder()
                .idPedido(10L)
                .cliente("Cliente anterior")
                .tipoVenta(TipoVenta.PARTICULAR)
                .tipoPago(TipoPago.EFECTIVO)
                .montoEfectivo(new BigDecimal("5000"))
                .montoTransferencia(BigDecimal.ZERO)
                .totalPedido(new BigDecimal("5000"))
                .estado(TipoEstado.PENDIENTE)
                .detalles(new ArrayList<>())
                .build();
        pedido.getDetalles().add(DetallePedido.builder()
                .pedido(pedido)
                .variedad(variedadAnterior)
                .cantidad(4)
                .build());

        PedidoRequestDTO request = new PedidoRequestDTO(
                "Cliente actualizado",
                "particular",
                "transferencia",
                null,
                LocalTime.of(21, 30),
                null,
                null,
                new BigDecimal("9000"),
                List.of(new PedidoDetalleRequestDTO(2L, 6)));

        when(pedidoRepository.findByIdParaActualizar(10L)).thenReturn(Optional.of(pedido));
        when(variedadEmpanadaRepository.findById(2L)).thenReturn(Optional.of(variedadNueva));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PedidoResponseDTO response = pedidoService.actualizarPedido(10L, request);

        assertEquals("Cliente actualizado", pedido.getCliente());
        assertEquals(TipoVenta.PARTICULAR, pedido.getTipoVenta());
        assertEquals(TipoPago.TRANSFERENCIA, pedido.getTipoPago());
        assertEquals(BigDecimal.ZERO, pedido.getMontoEfectivo());
        assertEquals(new BigDecimal("9000"), pedido.getMontoTransferencia());
        assertEquals(new BigDecimal("9000"), pedido.getTotalPedido());
        assertEquals(LocalTime.of(21, 30), pedido.getHorarioEntrega());
        assertEquals(1, pedido.getDetalles().size());
        assertEquals(2L, pedido.getDetalles().get(0).getVariedad().getId_variedad());
        assertEquals(6, pedido.getDetalles().get(0).getCantidad());
        assertEquals(10L, response.idPedido());

        verify(stockService).ajustarDisponibilidad(eq(Map.of(1L, 4, 2L, -6)));
    }

    @Test
    void actualizarPedidoRechazaPedidosEntregados() {
        Pedido pedido = Pedido.builder()
                .idPedido(10L)
                .estado(TipoEstado.ENTREGADO)
                .detalles(new ArrayList<>())
                .build();
        PedidoRequestDTO request = new PedidoRequestDTO(
                "Cliente",
                "PARTICULAR",
                "EFECTIVO",
                null,
                null,
                null,
                null,
                new BigDecimal("1000"),
                List.of(new PedidoDetalleRequestDTO(1L, 1)));

        when(pedidoRepository.findByIdParaActualizar(10L)).thenReturn(Optional.of(pedido));

        assertThrows(IllegalStateException.class,
                () -> pedidoService.actualizarPedido(10L, request));

        verify(stockService, never()).ajustarDisponibilidad(any());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void actualizarEstadoRechazaEntregadoACancelado() {
        Pedido pedido = Pedido.builder()
                .idPedido(10L)
                .estado(TipoEstado.ENTREGADO)
                .detalles(new ArrayList<>())
                .build();
        when(pedidoRepository.findByIdParaActualizar(10L)).thenReturn(Optional.of(pedido));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> pedidoService.actualizarEstadoPedido(10L, TipoEstado.CANCELADO));

        assertEquals(
                "No se permite cambiar el pedido de ENTREGADO a CANCELADO",
                error.getMessage());
        verify(stockService, never()).ajustarDisponibilidad(any());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void actualizarPagoAceptaCombinadoConImportesValidos() {
        Pedido pedido = Pedido.builder()
                .idPedido(10L)
                .estado(TipoEstado.PENDIENTE)
                .tipoPago(TipoPago.EFECTIVO)
                .montoEfectivo(new BigDecimal("9000"))
                .montoTransferencia(BigDecimal.ZERO)
                .totalPedido(new BigDecimal("9000"))
                .detalles(new ArrayList<>())
                .build();
        when(pedidoRepository.findByIdParaActualizar(10L)).thenReturn(Optional.of(pedido));

        boolean actualizado = pedidoService.actualizarTipoPago(
                10L,
                TipoPago.COMBINADO,
                new BigDecimal("5000"),
                new BigDecimal("4000"));

        assertEquals(true, actualizado);
        assertEquals(TipoPago.COMBINADO, pedido.getTipoPago());
        assertEquals(new BigDecimal("5000"), pedido.getMontoEfectivo());
        assertEquals(new BigDecimal("4000"), pedido.getMontoTransferencia());
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void modificarPedidoInexistenteDevuelveExcepcionEspecifica() {
        when(pedidoRepository.findByIdParaActualizar(999L)).thenReturn(Optional.empty());

        assertThrows(
                PedidoNoEncontradoException.class,
                () -> pedidoService.actualizarEstadoPedido(999L, TipoEstado.PREPARADO));
    }
}
