package com.mediconecta.modulos.estacion.dto;

import com.mediconecta.modulos.estacion.entity.Anclaje;
import com.mediconecta.modulos.estacion.entity.EstadoDock;

public record AnclajeResponse(
		Long id,
		Long estacionId,
		int numero,
		EstadoDock estado
) {

	public static AnclajeResponse desde(Anclaje anclaje) {
		return new AnclajeResponse(
				anclaje.getId(),
				anclaje.getEstacion().getId(),
				anclaje.getNumero(),
				anclaje.getEstado());
	}
}
