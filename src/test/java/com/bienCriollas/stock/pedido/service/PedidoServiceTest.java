package com.bienCriollas.stock.pedido.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
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
import com.bienCriollas.stock.pedido.repository.PedidoDetalleRepository;
import com.bienCriollas.stock.pedido.repository.PedidoRepository;
import com.bienCriollas.stock.stock.entity.Stock;
import com.bienCriollas.stock.stock.repository.StockRepository;
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
    private StockRepository stockRepository;

    @Mock
    private VariedadEmpanadaRepository variedadEmpanadaRepository;

    @InjectMocks
    private PedidoService pedidoService;

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

        Stock stockAnterior = Stock.builder()
                .idVariedad(1L)
                .stockDisponible(10)
                .activo(1)
                .build();

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

        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(stockRepository.findTopByIdVariedadAndActivoOrderByFechaElaboracionDesc(1L, 1))
                .thenReturn(stockAnterior);
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
        assertEquals(14, stockAnterior.getStockDisponible());
        assertEquals(10L, response.idPedido());

        verify(stockRepository).save(stockAnterior);
        verify(stockService).descontarStockVariedad(2L, 6);
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

        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));

        assertThrows(IllegalStateException.class,
                () -> pedidoService.actualizarPedido(10L, request));

        verify(stockRepository, never())
                .findTopByIdVariedadAndActivoOrderByFechaElaboracionDesc(anyLong(), anyInt());
        verify(stockService, never()).descontarStockVariedad(anyLong(), anyInt());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }
}
