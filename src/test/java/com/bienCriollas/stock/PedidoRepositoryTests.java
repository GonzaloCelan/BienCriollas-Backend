package com.bienCriollas.stock;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.bienCriollas.stock.Model.DetallePedido;
import com.bienCriollas.stock.Model.Pedido;
import com.bienCriollas.stock.Model.VariedadEmpanada;
import com.bienCriollas.stock.Repository.PedidoRepository;
import com.bienCriollas.stock.enums.TipoPago;
import com.bienCriollas.stock.enums.TipoVenta;

import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;



@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class PedidoRepositoryTests {

	
	@Autowired
	private PedidoRepository pedidoRepository;
	
	
	@Test
	
	public void PedidoRepository_SavePedido_ReturnsSavedPedido() {
	

		  Pedido pedido = Pedido.builder()
		      .cliente("gonzalo")
		      .tipoVenta(TipoVenta.PARTICULAR)
		      .tipoPago(TipoPago.TRANSFERENCIA)
		      .horarioEntrega(LocalTime.parse("22:00:00"))
		      .totalPedido(new BigDecimal("67000.00"))
		      .build();

		  

		  Pedido saved = pedidoRepository.save(pedido);
		  

		  Assertions.assertThat(saved.getIdPedido()).isNotNull();
		  Assertions.assertThat(saved.getDetalles()).hasSize(2);
}

}