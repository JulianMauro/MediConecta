package com.mediconecta.modulos.soporte.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mediconecta.modulos.pago.entity.EstadoPago;
import com.mediconecta.modulos.strike.entity.EstadoStrike;
import com.mediconecta.modulos.strike.entity.Strike;

/** Strike junto con su cobro: para decidir si anularlo hace falta ver cuanto y si ya se pago. */
public record StrikeSoporteResponse(
		Long id,
		Long viajeId,
		Instant fechaGeneracion,
		EstadoStrike estado,
		Long pagoId,
		BigDecimal monto,
		EstadoPago estadoPago
) {

	public static StrikeSoporteResponse desde(Strike strike) {
		boolean conPago = strike.getPago() != null;
		return new StrikeSoporteResponse(
				strike.getId(),
				strike.getViaje().getId(),
				strike.getFechaGeneracion(),
				strike.getEstado(),
				conPago ? strike.getPago().getId() : null,
				conPago ? strike.getPago().getMonto() : null,
				conPago ? strike.getPago().getEstado() : null);
	}
}
