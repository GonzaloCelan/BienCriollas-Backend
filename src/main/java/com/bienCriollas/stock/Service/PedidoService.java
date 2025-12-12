package com.bienCriollas.stock.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.Dto.PedidoDetalleResponseDTO;
import com.bienCriollas.stock.Dto.PedidoRequestDTO;
import com.bienCriollas.stock.Dto.PedidoResponseDTO;
import com.bienCriollas.stock.Model.CajaDiaria;
import com.bienCriollas.stock.Model.DetallePedido;
import com.bienCriollas.stock.Model.EstadoCaja;
import com.bienCriollas.stock.Model.Pedido;
import com.bienCriollas.stock.Model.Stock;
import com.bienCriollas.stock.Model.TipoEstado;
import com.bienCriollas.stock.Model.TipoPago;
import com.bienCriollas.stock.Model.TipoVenta;
import com.bienCriollas.stock.Model.VariedadEmpanada;
import com.bienCriollas.stock.Repository.CajaDiariaRepository;
import com.bienCriollas.stock.Repository.PedidoDetalleRepository;
import com.bienCriollas.stock.Repository.PedidoRepository;
import com.bienCriollas.stock.Repository.StockRepository;
import com.bienCriollas.stock.Repository.VariedadEmpanadaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {

	private final PedidoRepository pedidoRepository;
	
	private final VariedadEmpanadaRepository variedadEmpanadaRepository;
	private final StockService stockService;
	private final VariedadEmpanadaService variedadEmpanadaService;
	private final PedidoDetalleRepository detallePedidoRepository;
	private final CajaDiariaRepository cajaDiariaRepository;
	private final StockRepository stockRepository;

	
	
	//Metodo para crear un nuevo pedido
	
	@Transactional
	public PedidoResponseDTO crearPedido(PedidoRequestDTO pedidoDTO) {

	    // ✅ Fecha de hoy en Argentina (evita desfasajes en Railway)
	    LocalDate fechaCaja = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));

	    // ✅ NUEVO: asegurar caja diaria ABIERTA para ese día
	    asegurarCajaAbiertaDelDia(fechaCaja);

	    // 1) Parsear enums una sola vez
	    TipoVenta tipoVenta = pedidoDTO.tipoVenta() != null
	            ? Enum.valueOf(TipoVenta.class, pedidoDTO.tipoVenta())
	            : null;

	    TipoPago tipoPago = pedidoDTO.tipoPago() != null
	            ? Enum.valueOf(TipoPago.class, pedidoDTO.tipoPago())
	            : null;

	    // 2) Crear el pedido base (sin total ni montos todavía)
	    Pedido nuevoPedido = Pedido.builder()
	            .cliente(pedidoDTO.cliente())
	            .tipoVenta(tipoVenta)
	            .tipoPago(tipoPago)
	            .totalPedido(null)  // se setea después
	            .numeroPedidoPedidosYa(pedidoDTO.numeroPedidoPedidosYa())
	            .horarioEntrega(pedidoDTO.horaEntrega() != null ? pedidoDTO.horaEntrega() : null)
	            .estado(TipoEstado.PENDIENTE)
	            .fechaCreacion(fechaCaja) // ✅ usar la misma fecha AR
	            .build();

	    // 3) Mapear detalles y descontar stock
	    List<DetallePedido> detallesPedidos = pedidoDTO.detalles().stream()
	            .map(p -> {
	                VariedadEmpanada variedad = variedadEmpanadaRepository
	                        .findById(p.idVariedad())
	                        .orElseThrow(() -> new RuntimeException(
	                                "No se encontró la variedad con id " + p.idVariedad()));

	                BigDecimal subTotal = variedadEmpanadaService
	                        .calcularPrecioTotalPedido(p.idVariedad(), p.cantidad());

	                DetallePedido detallePedido = DetallePedido.builder()
	                        .pedido(nuevoPedido)
	                        .variedad(variedad)
	                        .cantidad(p.cantidad())
	                        .precioUnitario(variedad.getPrecio_unitario())
	                        .subtotal(subTotal)
	                        .build();

	                // Descontar el stock
	                stockService.descontarStockVariedad(p.idVariedad(), p.cantidad());

	                return detallePedido;
	            })
	            .toList();

	    // 4) Calcular el total del pedido (sumando subtotales)
	    BigDecimal totalCalculado = detallesPedidos.stream()
	            .map(DetallePedido::getSubtotal)
	            .reduce(BigDecimal.ZERO, BigDecimal::add);

	    BigDecimal totalPedido = pedidoDTO.totalPedido() != null
	            ? pedidoDTO.totalPedido()
	            : totalCalculado;

	    // 5) Calcular montos según tipo de pago
	    BigDecimal montoEfectivo = BigDecimal.ZERO;
	    BigDecimal montoTransferencia = BigDecimal.ZERO;

	    if (tipoPago != null) {
	        switch (tipoPago) {
	            case EFECTIVO:
	                montoEfectivo = totalPedido;
	                montoTransferencia = BigDecimal.ZERO;
	                break;

	            case TRANSFERENCIA:
	                montoEfectivo = BigDecimal.ZERO;
	                montoTransferencia = totalPedido;
	                break;

	            case COMBINADO:
	                BigDecimal dtoEfectivo = pedidoDTO.montoEfectivo() != null
	                        ? pedidoDTO.montoEfectivo()
	                        : BigDecimal.ZERO;

	                BigDecimal dtoTransfer = pedidoDTO.montoTransferencia() != null
	                        ? pedidoDTO.montoTransferencia()
	                        : BigDecimal.ZERO;

	                if (dtoEfectivo.compareTo(BigDecimal.ZERO) < 0 || dtoTransfer.compareTo(BigDecimal.ZERO) < 0) {
	                    throw new IllegalArgumentException("Los montos de pago no pueden ser negativos");
	                }

	                BigDecimal suma = dtoEfectivo.add(dtoTransfer);
	                if (suma.compareTo(totalPedido) != 0) {
	                    throw new IllegalArgumentException(
	                            "La suma de efectivo + transferencia (" + suma +
	                            ") debe ser igual al total del pedido (" + totalPedido + ")");
	                }

	                montoEfectivo = dtoEfectivo;
	                montoTransferencia = dtoTransfer;
	                break;

	            default:
	                break;
	        }
	    }

	    // 6) Setear total, montos y detalles en el pedido
	    nuevoPedido.setTotalPedido(totalPedido);
	    nuevoPedido.setMontoEfectivo(montoEfectivo);
	    nuevoPedido.setMontoTransferencia(montoTransferencia);
	    nuevoPedido.setDetalles(detallesPedidos);

	    // 7) Guardar
	    pedidoRepository.save(nuevoPedido);

	    // 8) DTO de respuesta
	    return new PedidoResponseDTO(
	            nuevoPedido.getIdPedido(),
	            nuevoPedido.getCliente(),
	            nuevoPedido.getTipoVenta() != null ? nuevoPedido.getTipoVenta().name() : null,
	            nuevoPedido.getTipoPago() != null ? nuevoPedido.getTipoPago().name() : null,
	            nuevoPedido.getNumeroPedidoPedidosYa(),
	            nuevoPedido.getHorarioEntrega(),
	            nuevoPedido.getTotalPedido(),
	            TipoEstado.PENDIENTE
	    );
	}

	
	private CajaDiaria asegurarCajaAbiertaDelDia(LocalDate fechaCaja) {

	    return cajaDiariaRepository.findByFecha(fechaCaja).orElseGet(() -> {
	        try {
	            CajaDiaria nueva = CajaDiaria.builder()
	                    .fecha(fechaCaja)
	                    .estadoCaja(EstadoCaja.ABIERTA) // o "ABIERTA" si lo guardás como String
	                    .ingresosEfectivo(BigDecimal.ZERO)
	                    .ingresosTransferencia(BigDecimal.ZERO)
	                    .ingresosPedidosYa(BigDecimal.ZERO)
	                    .ingresosTotales(BigDecimal.ZERO)
	                    .mermas(BigDecimal.ZERO)
	                    .totalEgresos(BigDecimal.ZERO)
	                    .balanceFinal(BigDecimal.ZERO)
	                    .build();

	            return cajaDiariaRepository.save(nueva);

	        } catch (DataIntegrityViolationException ex) {
	            // Otro pedido creó la caja justo antes (por el UNIQUE(fecha))
	            return cajaDiariaRepository.findByFecha(fechaCaja)
	                    .orElseThrow(() -> new RuntimeException("No se pudo asegurar la caja del día " + fechaCaja));
	        }
	    });
	}

	
	
	//Metodo para actualizar el estado de un pedido
	
	@Transactional
	public Boolean actualizarEstadoPedido(Long idPedido, TipoEstado nuevoEstado) {
	    Pedido pedido = pedidoRepository.findById(idPedido)
	            .orElseThrow(() -> new RuntimeException("No se encontró el pedido con id " + idPedido));

	    // 👉 estado ANTERIOR (como está guardado en la BD)
	    TipoEstado estadoAnterior = pedido.getEstado();

	    // 👉 acá ya podés detectar el caso PENDIENTE → CANCELADO
	    if (nuevoEstado == TipoEstado.CANCELADO && estadoAnterior == TipoEstado.PENDIENTE) {
	    	devolverStockPorCancelacion(pedido);
	    }

	    // ✅ actualizar el estado igual que antes
	    pedido.setEstado(nuevoEstado);
	    pedidoRepository.save(pedido);
	    return true;
	}

	
	
	//Metodo para obtener todos los pedidos, con filtro opcional por estado
	
	@Transactional(readOnly = true)
	public List<PedidoResponseDTO> obtenerTodosLosPedidos(TipoEstado estado) {
	    
		List<Pedido> pedidos;
	    
	    if (estado != null) {
	        pedidos = pedidoRepository.findByEstado(estado);
	    } else {
	        pedidos = pedidoRepository.findAll();
	    }
	    
	    return pedidos.stream()
	            .map(pedido -> new PedidoResponseDTO(
	            		pedido.getIdPedido(),
	                    pedido.getCliente(),
	                    pedido.getTipoVenta() != null ? pedido.getTipoVenta().name() : null,
	                    pedido.getTipoPago() != null ? pedido.getTipoPago().name() : null,
	                    pedido.getNumeroPedidoPedidosYa(),
	                    pedido.getHorarioEntrega(),
	                    pedido.getTotalPedido(),
	                    pedido.getEstado()
	            ))
	            .toList();
	
}
	
	//Metodo para obtener todos los pedidos por fecha de creacion
	
	@Transactional(readOnly = true)
	public List<PedidoResponseDTO> obtenerPedidosPorFecha(LocalDate fechaInicio) {
		
	    List<Pedido> pedidos = pedidoRepository.findByFechaCreacion(fechaInicio);
	    
	    return pedidos.stream()
	            .map(pedido -> new PedidoResponseDTO(
	            		pedido.getIdPedido(),
	                    pedido.getCliente(),
	                    pedido.getTipoVenta() != null ? pedido.getTipoVenta().name() : null,
	                    pedido.getTipoPago() != null ? pedido.getTipoPago().name() : null,
	                    pedido.getNumeroPedidoPedidosYa(),
	                    pedido.getHorarioEntrega(),
	                    pedido.getTotalPedido(),
	                    pedido.getEstado()
	            ))
	            .toList();
}
	
	//Metodo para obtener los detalles de un pedido por su id
	
	@Transactional(readOnly = true)
	public List<PedidoDetalleResponseDTO> obtenerDetallesPedido(Long idPedido) {
		
		Pedido pedidoExist = pedidoRepository.findById(idPedido)
				.orElseThrow(() -> new RuntimeException("No se encontró el pedido con id " + idPedido));
		
		BigDecimal totalPedido = pedidoExist.getTotalPedido();
		List<DetallePedido> pedido = detallePedidoRepository.findByPedidoIdPedido(idPedido);
		
		if(pedido.isEmpty()) {
			throw new RuntimeException("No se encontraron detalles para el pedido con id " + idPedido);
		}
		
		
		
		return pedido.stream()
	            .map(detalle -> new PedidoDetalleResponseDTO(
	            		pedidoExist.getCliente(),
	                    detalle.getVariedad().getId_variedad(),
	                     detalle.getVariedad().getNombre(),
	                    detalle.getCantidad(),
	                    detalle.getPrecioUnitario(),
	                    totalPedido
	            ))
	            .toList();
	}
	
	
	@Transactional(readOnly = true)
	public Page<PedidoResponseDTO> obtenerPedidosPaginados(TipoEstado estado, int page, int size) {

	    // ORDEN personalizado
	    Sort sort = Sort.by(
	            Sort.Order.by("tipoVenta").with(Sort.Direction.ASC),      // PARTICULAR primero
	            Sort.Order.by("horarioEntrega").with(Sort.Direction.ASC), // POR hora de entrega
	            Sort.Order.by("idPedido").with(Sort.Direction.DESC)       // PEDIDOS YA por id desc
	    );

	    Pageable pageable = PageRequest.of(page, size, sort);

	    // 🔹 solo pedidos del día (fechaCreacion = hoy)
	    LocalDate hoy = LocalDate.now(); 
	    // si querés zona explícita:
	    // LocalDate hoy = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));

	    Page<Pedido> pedidos =
	            pedidoRepository.findByEstadoAndFechaCreacion(estado, hoy, pageable);

	    return pedidos.map(p -> new PedidoResponseDTO(
	            p.getIdPedido(),
	            p.getCliente(),
	            p.getTipoVenta() != null ? p.getTipoVenta().name() : null,
	            p.getTipoPago() != null ? p.getTipoPago().name() : null,
	            p.getNumeroPedidoPedidosYa(),
	            p.getHorarioEntrega(),
	            p.getTotalPedido(),
	            p.getEstado()
	    ));
	}

	
	
	private void devolverStockPorCancelacion(Pedido pedido) {

	    // Recorremos cada detalle del pedido
	    for (DetallePedido det : pedido.getDetalles()) {

	        // ⚠️ Ajustá estos getters a como se llaman en tu entidad
	        Long idVariedad = det.getVariedad().getId_variedad();   // ej: getVariedadEmpanada()
	        Integer cantidad = det.getCantidad();

	        // buscamos el último stock ACTIVO de esa variedad
	        Stock ultimoStock = stockRepository
	                .findTopByIdVariedadAndActivoOrderByFechaElaboracionDesc(idVariedad, 1);

	        if (ultimoStock != null) {
	            Integer disponibleActual = ultimoStock.getStockDisponible();
	            ultimoStock.setStockDisponible(disponibleActual + cantidad);

	            stockRepository.save(ultimoStock);
	        }
	    }
	}


}