package com.mediconecta.modulos.pago.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mediconecta.modulos.pago.entity.ConceptoPago;
import com.mediconecta.modulos.pago.entity.EstadoPago;
import com.mediconecta.modulos.pago.entity.Pago;

public record PagoResponse(
		Long id,
		Long usuarioId,
		Long viajeId,
		Long membresiaId,
		ConceptoPago concepto,
		BigDecimal monto,
		EstadoPago estado,
		Instant fechaCreacion,
		Instant fechaResolucion,
		String referenciaPasarela
) {

	public static PagoResponse desde(Pago pago) {
		return new PagoResponse(
				pago.getId(),
				pago.getUsuario().getId(),
				pago.getViaje() != null ? pago.getViaje().getId() : null,
				pago.getMembresia() != null ? pago.getMembresia().getId() : null,
				pago.getConcepto(),
				pago.getMonto(),
				pago.getEstado(),
				pago.getFechaCreacion(),
				pago.getFechaResolucion(),
				pago.getReferenciaPasarela());
	}
}
