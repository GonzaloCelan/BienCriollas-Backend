package com.bienCriollas.stock.egreso.interfaces;

import java.math.BigDecimal;

public interface EgresoMesTotalesProjection {

	 	String getTipoEgreso();
	    BigDecimal getTotalMesActual();
	    BigDecimal getTotalMesAnterior();
}
