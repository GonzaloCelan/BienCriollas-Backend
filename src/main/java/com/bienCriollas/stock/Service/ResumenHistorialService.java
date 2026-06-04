package com.bienCriollas.stock.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bienCriollas.stock.Dto.BalanceMensualDTO;
import com.bienCriollas.stock.Dto.PedidoResponseDTO;
import com.bienCriollas.stock.Dto.ResumenAcumuladoDTO;
import com.bienCriollas.stock.Interface.CajaAcumuladoProjection;
import com.bienCriollas.stock.Interface.ICajaService;
import com.bienCriollas.stock.Interface.IEgresoService;
import com.bienCriollas.stock.Interface.IPedidosYaService;
import com.bienCriollas.stock.Interface.IPerdidasService;
import com.bienCriollas.stock.Interface.IResumenHistoricoService;
import com.bienCriollas.stock.Model.BalanceMensual;
import com.bienCriollas.stock.Model.CajaDiaria;
import com.bienCriollas.stock.Model.Egreso;
import com.bienCriollas.stock.Model.IngresoPedidosYa;
import com.bienCriollas.stock.Model.Pedido;
import com.bienCriollas.stock.Repository.BalanceMensualRepository;
import com.bienCriollas.stock.Repository.CajaDiariaRepository;
import com.bienCriollas.stock.Repository.IngresoPedidosYaRepository;
import com.bienCriollas.stock.Repository.PedidoRepository;
import com.bienCriollas.stock.enums.EstadoCaja;
import com.bienCriollas.stock.enums.TipoEstado;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class ResumenHistorialService implements IResumenHistoricoService {

	 private final BalanceMensualRepository balanceMensualRepository;
	 private final IngresoPedidosYaRepository ingresoPedidosYaRepository;
	 
	 private final IPedidosYaService pedidosYaService;
	 private final ICajaService cajaService;
	 private final IEgresoService egresoService;
	 private final IPerdidasService perdidasService;
	@Override
	public ResumenAcumuladoDTO obtenerAcumuladoHistorico(Integer anio, Integer mes) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List<BalanceMensualDTO> resumenMensualGrafico(Integer anio) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List<IngresoPedidosYa> obtenerPedidosYaLiquidaciones() {
		// TODO Auto-generated method stub
		return null;
	}
	
	

	
}
