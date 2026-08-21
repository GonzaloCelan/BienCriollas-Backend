package com.bienCriollas.stock.pedido.service;

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

import com.bienCriollas.stock.ingreso.dto.IngresosDiariosDTO;
import com.bienCriollas.stock.pedido.dto.PedidoDetalleRequestDTO;
import com.bienCriollas.stock.pedido.dto.PedidoDetalleResponseDTO;
import com.bienCriollas.stock.pedido.dto.PedidoRequestDTO;
import com.bienCriollas.stock.pedido.dto.PedidoResponseDTO;
import com.bienCriollas.stock.pedido.interfaces.IPedidoService;

import com.bienCriollas.stock.pedido.entity.DetallePedido;
import com.bienCriollas.stock.pedido.entity.Pedido;
import com.bienCriollas.stock.stock.entity.Stock;
import com.bienCriollas.stock.variedad.entity.VariedadEmpanada;

import com.bienCriollas.stock.pedido.repository.PedidoDetalleRepository;
import com.bienCriollas.stock.pedido.repository.PedidoRepository;
import com.bienCriollas.stock.stock.repository.StockRepository;
import com.bienCriollas.stock.variedad.repository.VariedadEmpanadaRepository;
import com.bienCriollas.stock.stock.service.StockService;

import com.bienCriollas.stock.pedido.enums.TipoEstado;
import com.bienCriollas.stock.pedido.enums.TipoPago;
import com.bienCriollas.stock.pedido.enums.TipoVenta;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService implements IPedidoService {

	private final PedidoRepository pedidoRepository;
	
	
	private final StockService stockService;

	private final PedidoDetalleRepository detallePedidoRepository;
	
	private final StockRepository stockRepository;
	
	private final VariedadEmpanadaRepository variedadEmpanadaRepository;

	
	
	//Metodo para crear un nuevo pedido
	
	@Override
	@Transactional
	public PedidoResponseDTO crearPedido(PedidoRequestDTO pedidoDTO) {

	   
	    
	    if(pedidoDTO == null) {
	    	throw new RuntimeException("El pedido no puede ser null");
	    }
	    
	    // ✅ Fecha de hoy en Argentina (evita desfasajes en Railway)
	    LocalDate fechaActual = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));
	    
	    
	 // 1) Parsear enums una sola vez
	    TipoVenta tipoVenta = pedidoDTO.tipoVenta() != null
	            ? Enum.valueOf(TipoVenta.class, pedidoDTO.tipoVenta())
	            : null;

	    TipoPago tipoPago = pedidoDTO.tipoPago() != null
	            ? Enum.valueOf(TipoPago.class, pedidoDTO.tipoPago())
	            : null;

	    
	    //Calcular montos según tipo de pago
	    BigDecimal montoEfectivo = BigDecimal.ZERO;
	    BigDecimal montoTransferencia = BigDecimal.ZERO;

	    if (tipoPago != null) {
	        switch (tipoPago) {
	            case EFECTIVO:
	                montoEfectivo = pedidoDTO.totalPedido();
	                montoTransferencia = BigDecimal.ZERO;
	                break;

	            case TRANSFERENCIA:
	                montoEfectivo = BigDecimal.ZERO;
	                montoTransferencia = pedidoDTO.totalPedido();
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
	                if (suma.compareTo(pedidoDTO.totalPedido()) != 0) {
	                    throw new IllegalArgumentException(
	                            "La suma de efectivo + transferencia (" + suma +
	                            ") debe ser igual al total del pedido (" + pedidoDTO.totalPedido() + ")");
	                }

	                montoEfectivo = dtoEfectivo;
	                montoTransferencia = dtoTransfer;
	                break;

	            default:
	                break;
	        }
	    }
	    
	    // Creamos el pedidos con estado PENDIENTE (siempre se guarda en pendiente)

	    Pedido nuevoPedido = Pedido.builder()
	    		.cliente(pedidoDTO.cliente())
	    		.tipoVenta(tipoVenta)
	    		.tipoPago(tipoPago)
	    		.numeroPedidoPedidosYa(pedidoDTO.numeroPedidoPedidosYa() != null ? pedidoDTO.numeroPedidoPedidosYa() : null)
	    		.fechaCreacion(fechaActual)
	    		.montoEfectivo(montoEfectivo)
	    		.montoTransferencia(montoTransferencia)
	    		.totalPedido(pedidoDTO.totalPedido() != null ? pedidoDTO.totalPedido() : BigDecimal.ZERO)
	    		.estado(TipoEstado.PENDIENTE)
	    		.build();
	    
	    // Guardamos el pedido para obtener su ID y poder asociar los detalles
	    
	  
	    Pedido pedidoGuardado = pedidoRepository.save(nuevoPedido);
	    
	    // Recorremos los detalles del pedido y los guardamos
	    
	    List<PedidoDetalleRequestDTO> detallesPedido = pedidoDTO.detalles();
	    
	    detallesPedido.forEach(det -> {
	    	
	    	VariedadEmpanada variedad = variedadEmpanadaRepository.findById(det.idVariedad()).orElse(null);
	    	
	    	if(variedad == null) {
	    		throw new RuntimeException("No se encontró la variedad con id " + det.idVariedad());
	    	}
	    	
	    	DetallePedido detalle = DetallePedido.builder()
	    			.pedido(nuevoPedido)
	    			.variedad(variedad)
	    			.cantidad(det.cantidad())
	    			.build();
	    	
	    	
	    	stockService.descontarStockVariedad(variedad.getId_variedad(), det.cantidad());
	    	detallePedidoRepository.save(detalle);
	    	
	    	nuevoPedido.getDetalles().add(detalle);
	    });
	    
	    //Retornamos el pedido guardado como DTO
	    return new PedidoResponseDTO(
	    		pedidoGuardado.getIdPedido(),
	    		pedidoGuardado.getCliente(),
	    		pedidoGuardado.getTipoVenta() != null ? pedidoGuardado.getTipoVenta().name() : null,
	    		pedidoGuardado.getTipoPago() != null ? pedidoGuardado.getTipoPago().name() : null,
	    		pedidoGuardado.getNumeroPedidoPedidosYa(),
	    		pedidoGuardado.getHorarioEntrega(),
	    		pedidoGuardado.getTotalPedido(),
	    		pedidoGuardado.getEstado()
	    );

	    
	}

	
	
	
	//Metodo para actualizar el estado de un pedido
	
	@Override
	@Transactional
	public boolean actualizarEstadoPedido(Long idPedido, TipoEstado nuevoEstado) {

	    Pedido pedido = pedidoRepository.findById(idPedido)
	            .orElseThrow(() -> new RuntimeException("No se encontró el pedido con id " + idPedido));

	    TipoEstado estadoAnterior = pedido.getEstado();

	    // PENDIENTE -> CANCELADO
	    if (nuevoEstado == TipoEstado.CANCELADO
	            && (estadoAnterior == TipoEstado.PENDIENTE || estadoAnterior == TipoEstado.PREPARADO)) {

	        devolverStockPorCancelacion(pedido);

	        // ✅ Si es PEDIDOS_YA, liberamos el número para poder reutilizarlo 
	        if (pedido.getTipoVenta() == TipoVenta.PEDIDOS_YA) {
	            pedido.setNumeroPedidoPedidosYa(null);
	        }

	        // ✅ Al cancelar, resetear montos
	        pedido.setMontoEfectivo(BigDecimal.ZERO);
	        pedido.setMontoTransferencia(BigDecimal.ZERO);
	        pedido.setTotalPedido(BigDecimal.ZERO);
	    }

	    pedido.setEstado(nuevoEstado);
	    pedidoRepository.save(pedido);
	    return true;
	}
	
	
	@Override
	@Transactional
	public boolean actualizarTipoPago(Long idPedido, TipoPago nuevoTipoPago) {

	    Pedido pedido = pedidoRepository.findById(idPedido)
	            .orElseThrow(() -> new RuntimeException("No se encontró el pedido con id " + idPedido));

	    if (pedido.getEstado() != TipoEstado.PENDIENTE && pedido.getEstado() != TipoEstado.PREPARADO) {
	        throw new RuntimeException("Solo se puede cambiar el tipo de pago si el pedido está PENDIENTE o PREPARADO");
	    }

	    if (nuevoTipoPago == null) {
	        throw new RuntimeException("El nuevo tipo de pago no puede ser null");
	    }

	    // total actual (si tenés pedido.getTotal() / getImporteTotal(), usalo mejor)
	    BigDecimal total = pedido.getMontoEfectivo().add(pedido.getMontoTransferencia());

	    switch (nuevoTipoPago) {
	        case EFECTIVO -> {
	            pedido.setTipoPago(TipoPago.EFECTIVO);
	            pedido.setMontoEfectivo(total);
	            pedido.setMontoTransferencia(BigDecimal.ZERO);
	        }
	        case TRANSFERENCIA -> {
	            pedido.setTipoPago(TipoPago.TRANSFERENCIA);
	            pedido.setMontoEfectivo(BigDecimal.ZERO);
	            pedido.setMontoTransferencia(total);
	        }
	        case COMBINADO -> {
	            // con esta firma no podés saber los montos
	            throw new RuntimeException("Para COMBINADO tenés que enviar montoEfectivo y montoTransferencia");
	        }
	    }

	    pedidoRepository.save(pedido);
	    return true;
	}

	//Metodo para obtener todos los pedidos, con filtro opcional por estado
	@Override
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
	
	@Override
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
	
	@Override
	@Transactional(readOnly = true)
	public List<PedidoDetalleResponseDTO> obtenerDetallesPedido(Long idPedido) {

	    Pedido pedidoExist = pedidoRepository.findById(idPedido)
	            .orElseThrow(() -> new RuntimeException("No se encontró el pedido con id " + idPedido));

	    List<DetallePedido> detalles = pedidoExist.getDetalles(); 

	    if (detalles.isEmpty()) {
	        throw new RuntimeException("No se encontraron detalles para el pedido con id " + idPedido);
	    }

	    return detalles.stream()
	            .map(detalle -> new PedidoDetalleResponseDTO(
	                    pedidoExist.getCliente(),
	                    detalle.getVariedad().getId_variedad(),
	                    detalle.getVariedad().getNombre(),
	                    detalle.getCantidad(),
	                    pedidoExist.getTotalPedido(),
	                    pedidoExist.getTipoVenta(),
	                    pedidoExist.getTipoPago()
	            ))
	            .toList();
	}
	
	@Override
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

	
	
	
	
	@Transactional(readOnly = true)
    public Page<PedidoResponseDTO> obtenerPedidosPaginadosPorEstadoYFecha(
            TipoEstado estado,
            LocalDate fecha,   // la fecha seleccionada en caja
            int page,
            int size
    ) {
        if (fecha == null) {
            fecha = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "idPedido") // ajustá al nombre real del campo ID en tu entity
        );

        return pedidoRepository.findByEstadoAndFechaCreacion(estado, fecha, pageable)
                .map(this::toDto);
    }

    private PedidoResponseDTO toDto(Pedido p) {
        return new PedidoResponseDTO(
                p.getIdPedido(),
                p.getCliente(), // o p.getCliente().getNombre() si tenés entidad Cliente
                p.getTipoVenta() == null ? null : p.getTipoVenta().name(), // si es enum
                p.getTipoPago()  == null ? null : p.getTipoPago().name(),  // si es enum
                p.getNumeroPedidoPedidosYa(), // tu campo de PedidosYa
                p.getHorarioEntrega(),
                p.getTotalPedido(),
                p.getEstado()
        );
    }


	@Override
	public IngresosDiariosDTO calcularIngresosDiarios(LocalDate fecha, TipoEstado estado) {
		
		List<Pedido> pedidos = pedidoRepository.findByFechaCreacionAndEstado(fecha, estado);
		if(pedidos.isEmpty()) {
			throw new RuntimeException("No se encontraron pedidos para la fecha " + fecha + " y estado " + estado);
		}
		
		BigDecimal ingresosEfectivo = pedidos.stream()
		        .filter(p -> p.getTipoVenta() != TipoVenta.PEDIDOS_YA)  
		        .filter(p -> p.getTipoPago() == TipoPago.EFECTIVO || p.getTipoPago() == TipoPago.COMBINADO)
		        .map(Pedido::getMontoEfectivo)
		        .reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal ingresosTransferencia = pedidos.stream()
		        .filter(p -> p.getTipoVenta() != TipoVenta.PEDIDOS_YA)  
		        .filter(p -> p.getTipoPago() == TipoPago.TRANSFERENCIA || p.getTipoPago() == TipoPago.COMBINADO)
		        .map(Pedido::getMontoTransferencia)
		        .reduce(BigDecimal.ZERO, BigDecimal::add);
		
		BigDecimal ingresoTotal = ingresosEfectivo.add(ingresosTransferencia);
		
		return new IngresosDiariosDTO(ingresosEfectivo, ingresosTransferencia,ingresoTotal);
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
