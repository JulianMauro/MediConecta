package com.mediconecta.modulos.viaje.dto;

import java.time.Instant;

import com.mediconecta.modulos.viaje.entity.Viaje;

public record ViajeResponse(
		Long id,
		Long usuarioId,
		String codigoBicicleta,
		Long anclajeOrigenId,
		Long anclajeDestinoId,
		Long membresiaId,
		Instant fechaInicio,
		Instant fechaFin,
		boolean excedioTiempo,
		long duracionMinutos
) {

	public static ViajeResponse desde(Viaje viaje) {
		return new ViajeResponse(
				viaje.getId(),
				viaje.getUsuario().getId(),
				viaje.getBicicleta().getCodigo(),
				viaje.getAnclajeOrigen().getId(),
				viaje.getAnclajeDestino() != null ? viaje.getAnclajeDestino().getId() : null,
				viaje.getMembresia().getId(),
				viaje.getFechaInicio(),
				viaje.getFechaFin(),
				viaje.isExcedioTiempo(),
				viaje.duracionMinutos());
	}
}
