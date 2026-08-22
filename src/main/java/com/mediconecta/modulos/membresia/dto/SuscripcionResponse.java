package com.mediconecta.modulos.membresia.dto;

import java.time.Instant;

import com.mediconecta.modulos.membresia.entity.EstadoSuscripcion;
import com.mediconecta.modulos.membresia.entity.SuscripcionUsuario;

public record SuscripcionResponse(
		Long id,
		Long usuarioId,
		Long membresiaId,
		Instant fechaInicio,
		Instant fechaFin,
		EstadoSuscripcion estado
) {

	public static SuscripcionResponse desde(SuscripcionUsuario suscripcion) {
		return new SuscripcionResponse(
				suscripcion.getId(),
				suscripcion.getUsuario().getId(),
				suscripcion.getMembresia().getId(),
				suscripcion.getFechaInicio(),
				suscripcion.getFechaFin(),
				suscripcion.getEstado());
	}
}
