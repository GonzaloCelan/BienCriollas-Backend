package com.bienCriollas.stock.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.Dto.EmpanadaMermaDTO;
import com.bienCriollas.stock.Dto.EmpanadaVendidaDTO;
import com.bienCriollas.stock.Dto.EstadisticaDTO;
import com.bienCriollas.stock.Dto.PedidoResponseDTO;
import com.bienCriollas.stock.Dto.StockResponseDTO;
import com.bienCriollas.stock.Model.Pedido;
import com.bienCriollas.stock.Model.Stock;
import com.bienCriollas.stock.Model.TipoEstado;
import com.bienCriollas.stock.Model.TipoPago;
import com.bienCriollas.stock.Model.TipoVenta;
import com.bienCriollas.stock.Repository.MermaRepository;
import com.bienCriollas.stock.Repository.PedidoDetalleRepository;
import com.bienCriollas.stock.Repository.PedidoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstadisticaService {
	
	private final PedidoService pedidoService;
	private final PedidoDetalleRepository pedidoDetalleRepository;
	private final StockService stockService;
	private final MermaRepository mermaRepository;
	private final PedidoRepository pedidoRepository;

	
	// Metodo para obtener datos por dia
	@Transactional(readOnly = true)
	public EstadisticaDTO obtenerEstadisticasPorFecha(LocalDate fecha) {

	    Integer totalEmpanadasVendidas = calcularTotalEmpanadasVendidas(fecha);
	    Integer totalMermas            = calcularTotalEmpanadasPerdidas(fecha);
	    Integer totalPedidos           = calcularTotalPedidosEmpanadas(fecha);

	    BigDecimal totalIngresos       = calcularTotalVenta(fecha);
	    BigDecimal totalEfectivo       = calcularTotalEfectivo(fecha);
	    BigDecimal totalTransferencia  = calcularTotalTransferencia(fecha);
	    BigDecimal totalImporteMerma   = calcularTotalImporteMermas(fecha);
	    BigDecimal totalPedidosYa      = calcularTotalPedidosYa(fecha);

	    Integer variedadBajoStock      = calcularVariedadesStockBajo();

	    List<EmpanadaVendidaDTO> empanadasMasVendidas =
	            calcularEmpanadasVendidasPorVariedad(fecha);

	    List<EmpanadaMermaDTO> empanadasPerdidas =
	    		calcularEmpanadasPerdidasPorVariedadImporte(fecha);

	    return new EstadisticaDTO(
	            totalEmpanadasVendidas,
	            totalMermas,
	            totalImporteMerma,
	            totalPedidos,
	            totalIngresos,
	            totalEfectivo,
	            totalTransferencia,
	            totalPedidosYa,
	            variedadBajoStock,
	            empanadasMasVendidas,
	            empanadasPerdidas
	    );
	}
	
	// Metodo para obtener datos por mes
	
	@Transactional(readOnly = true)
	public EstadisticaDTO obtenerEstadisticasPorMes(int año, int mes) {

	    YearMonth ym = YearMonth.of(año, mes);
	    LocalDate desde = ym.atDay(1);
	    LocalDate hasta = ym.atEndOfMonth();

	    int totalEmpanadasVendidas = 0;
	    int totalMermas            = 0;
	    int totalPedidos           = 0;

	    BigDecimal totalPedidosYa = pedidoRepository.totalEntregadoPedidosYaEntre(desde, hasta );
	    if (totalPedidosYa == null) totalPedidosYa = BigDecimal.ZERO;
	    BigDecimal totalIngresos       = BigDecimal.ZERO;
	    BigDecimal totalEfectivo       = BigDecimal.ZERO;
	    BigDecimal totalTransferencia  = BigDecimal.ZERO;
	    BigDecimal totalMermasImporte  = BigDecimal.ZERO;   // 💰 total plata perdida en el mes

	    // acumuladores por variedad
	    Map<String, Integer>    vendidasPorVariedad        = new HashMap<>();
	    Map<String, Integer>    mermasPorVariedadCantidad  = new HashMap<>();
	    Map<String, BigDecimal> mermasPorVariedadImporte   = new HashMap<>();

	    for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {

	        EstadisticaDTO estDia = obtenerEstadisticasPorFecha(dia); // 👉 REUSAMOS lo diario

	        if (estDia == null) continue;

	        // Totales simples
	        totalEmpanadasVendidas += estDia.totalEmpanadasVendidas() != null
	                ? estDia.totalEmpanadasVendidas() : 0;

	        totalMermas += estDia.totalMermas() != null
	                ? estDia.totalMermas() : 0;

	        totalPedidos += estDia.totalPedidos() != null
	                ? estDia.totalPedidos() : 0;

	        if (estDia.totalIngresos() != null) {
	            totalIngresos = totalIngresos.add(estDia.totalIngresos());
	        }
	        if (estDia.totalEfectivo() != null) {
	            totalEfectivo = totalEfectivo.add(estDia.totalEfectivo());
	        }
	        if (estDia.totalTransferencia() != null) {
	            totalTransferencia = totalTransferencia.add(estDia.totalTransferencia());
	        }

	        // Ventas por variedad
	        if (estDia.empanadasMasVendidas() != null) {
	            for (EmpanadaVendidaDTO v : estDia.empanadasMasVendidas()) {
	                vendidasPorVariedad.merge(
	                        v.nombre(),
	                        v.cantidad(),
	                        Integer::sum
	                );
	            }
	        }

	        // Mermas por variedad (cantidad + plata)
	        if (estDia.empanadasPerdidas() != null) {
	            for (EmpanadaMermaDTO m : estDia.empanadasPerdidas()) {

	                // cantidad
	                mermasPorVariedadCantidad.merge(
	                        m.nombre(),
	                        m.cantidad(),
	                        Integer::sum
	                );

	                // importe perdido por variedad
	                BigDecimal importeMerma = m.montoPerdido() != null
	                        ? m.montoPerdido()
	                        : BigDecimal.ZERO;

	                mermasPorVariedadImporte.merge(
	                        m.nombre(),
	                        importeMerma,
	                        BigDecimal::add
	                );

	                // total general de plata perdida en el mes
	                totalMermasImporte = totalMermasImporte.add(importeMerma);
	            }
	        }
	    }

	    // Pasar los mapas a listas de DTO ordenadas de mayor a menor
	    List<EmpanadaVendidaDTO> listaVendidasMes = vendidasPorVariedad.entrySet()
	            .stream()
	            .map(e -> new EmpanadaVendidaDTO(e.getKey(), e.getValue()))
	            .sorted(Comparator.comparing(EmpanadaVendidaDTO::cantidad).reversed())
	            .collect(Collectors.toList());

	    List<EmpanadaMermaDTO> listaMermasMes = mermasPorVariedadCantidad.entrySet()
	            .stream()
	            .map(e -> new EmpanadaMermaDTO(
	                    e.getKey(),
	                    e.getValue(),
	                    mermasPorVariedadImporte.getOrDefault(e.getKey(), BigDecimal.ZERO)
	            ))
	            .sorted(Comparator.comparing(EmpanadaMermaDTO::cantidad).reversed())
	            .collect(Collectors.toList());

	    // Para el modo "mes" seguimos poniendo bajo stock en 0 (o lo calculás distinto si querés)
	    Integer variedadBajoStockMes = 0;

	    return new EstadisticaDTO(
	            totalEmpanadasVendidas,
	            totalMermas,
	            totalMermasImporte,   // 💰 total de plata perdida en mermas
	            totalPedidos,
	            totalIngresos,
	            totalEfectivo,
	            totalTransferencia,
	            totalPedidosYa,
	            variedadBajoStockMes,
	            listaVendidasMes,
	            listaMermasMes
	    );
	}

	//METODOS PRIVADOS

	private List<EmpanadaVendidaDTO> calcularEmpanadasVendidasPorVariedad(LocalDate fecha) {

	    List<Object[]> resultados =
	            pedidoDetalleRepository.obtenerTotalEmpanadasPorVariedadEnFechaYEstado(
	                    fecha,
	                    TipoEstado.ENTREGADO.name()   // 👈 solo pedidos entregados
	            );

	    return resultados.stream()
	            .map(row -> {
	                // 0 -> nombre variedad (String)
	                String nombre = row[0] != null ? row[0].toString() : null;

	                // 1 -> cantidad (puede venir como BigDecimal, Long, Integer o String)
	                Integer cantidad = 0;
	                if (row[1] != null) {
	                    cantidad = new BigDecimal(row[1].toString()).intValue();
	                }

	                return new EmpanadaVendidaDTO(nombre, cantidad);
	            })
	            .toList();
	}
	
	private List<EmpanadaMermaDTO> calcularEmpanadasPerdidasPorVariedadImporte(LocalDate fecha) {

	    List<Object[]> resultados = mermaRepository.obtenerMermaPorVariedadConImporte(fecha);

	    return resultados.stream()
	            .map(row -> {
	                String nombre        = (String) row[0];
	                Integer cantidad     = row[1] != null ? ((Number) row[1]).intValue() : 0;
	                BigDecimal importe   = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;

	                return new EmpanadaMermaDTO(nombre, cantidad, importe);
	            })
	            .toList();
	}
	
	private BigDecimal calcularTotalImporteMermas(LocalDate fecha) {
	    List<EmpanadaMermaDTO> mermas = calcularEmpanadasPerdidasPorVariedadImporte(fecha);

	    return mermas.stream()
	            .map(EmpanadaMermaDTO::montoPerdido)
	            .reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	
 	private Integer calcularVariedadesStockBajo() {

	    
	    List<StockResponseDTO> stocks = stockService.obtenerTodosLosRegistrosDeStock();

	    if (stocks == null || stocks.isEmpty()) {
	        return 0;
	    }

	    long cantidadVariedadesStockBajo = stocks.stream()
	        .filter(Objects::nonNull)
	        .filter(s -> s.stock_disponible() != null)
	    
	        .filter(s -> s.stock_disponible() <= 100)
	        .count();

	    return (int) cantidadVariedadesStockBajo;
	}
	
	private Integer calcularTotalEmpanadasVendidas(LocalDate fecha) {

	    // Ya filtra por fecha + estado ENTREGADO
	    List<EmpanadaVendidaDTO> porVariedad = calcularEmpanadasVendidasPorVariedad(fecha);

	    if (porVariedad == null || porVariedad.isEmpty()) {
	        return 0;
	    }

	    int total = 0;

	    for (EmpanadaVendidaDTO dto : porVariedad) {
	        // 👇 ajustá el getter según cómo esté definido tu DTO
	        Integer cantidad = dto.cantidad(); // si es record
	        // Integer cantidad = dto.getCantidad(); // si es clase normal

	        if (cantidad != null) {
	            total += cantidad;
	        }
	    }

	    return total;
	}
	
	private Integer calcularTotalPedidosEmpanadas(LocalDate fecha) {

	    List<PedidoResponseDTO> pedidos = pedidoService.obtenerPedidosPorFecha(fecha);

	    if (pedidos.isEmpty()) {
	        return 0;
	    }

	    int totalCantidadVentas = 0;

	    for (PedidoResponseDTO pedido : pedidos) {
	        // 👇 solo contamos pedidos ENTREGADOS
	        if (pedido.estadoPedido() == TipoEstado.ENTREGADO) {
	            totalCantidadVentas++;
	        }
	    }

	    return totalCantidadVentas;
	}
	
	private BigDecimal calcularTotalPedidosYa(LocalDate fecha) {
		
	    if (fecha == null) {
	        fecha = LocalDate.now();
	    }

	  
	    BigDecimal total = pedidoRepository.totalEntregadoPedidosYaEnFecha(fecha);

	 
	    return total != null ? total : BigDecimal.ZERO;
	}
	
	
	private BigDecimal calcularTotalVenta(LocalDate fecha) {

	    BigDecimal totalVentas = BigDecimal.ZERO;
	    List<PedidoResponseDTO> pedidos = pedidoService.obtenerPedidosPorFecha(fecha);

	    for (PedidoResponseDTO pedido : pedidos) {
	        // 👇 solo sumamos si el pedido está ENTREGADO
	        if (pedido.estadoPedido() == TipoEstado.ENTREGADO && pedido.tipoVenta() == "PARTICULAR") {
	            totalVentas = totalVentas.add(pedido.totalPedido());
	        }
	    }

	    return totalVentas;
	}
	
	private BigDecimal calcularTotalEfectivo (LocalDate fecha) {
		
		BigDecimal totalVentasEfectivo = BigDecimal.ZERO;
	    List<PedidoResponseDTO> pedidos = pedidoService.obtenerPedidosPorFecha(fecha);

	    for (PedidoResponseDTO pedido : pedidos) {
	        // 👇 solo sumamos si el pedido está ENTREGADO
	        if (pedido.estadoPedido() == TipoEstado.ENTREGADO && pedido.tipoPago() == "EFECTIVO" && pedido.tipoVenta() == "PARTICULAR") {
	        	totalVentasEfectivo = totalVentasEfectivo.add(pedido.totalPedido());
	        }
	    }

	    return totalVentasEfectivo;
	}
	
	private BigDecimal calcularTotalTransferencia (LocalDate fecha) {

		
		BigDecimal totalVentasTransferencia = BigDecimal.ZERO;
	    List<PedidoResponseDTO> pedidos = pedidoService.obtenerPedidosPorFecha(fecha);

	    for (PedidoResponseDTO pedido : pedidos) {
	        
	        if (pedido.estadoPedido() == TipoEstado.ENTREGADO && pedido.tipoPago() == "TRANSFERENCIA" && pedido.tipoVenta() == "PARTICULAR") {
	        	totalVentasTransferencia = totalVentasTransferencia.add(pedido.totalPedido());
	        }
	    }

	    return totalVentasTransferencia;
	}


	
	private Integer calcularTotalEmpanadasPerdidas(LocalDate fecha) {
	    Integer totalMerma = mermaRepository.obtenerTotalEmpanadasPerdidasEnFecha(fecha);
	    return (totalMerma != null) ? totalMerma : 0;
	}
}



