package com.mediconecta.modulos.estacion.dto;

import com.mediconecta.modulos.estacion.entity.Estacion;

public record EstacionResponse(
		Long id,
		String nombre,
		String direccion,
		Double latitud,
		Double longitud,
		int capacidad,
		boolean activa
) {

	public static EstacionResponse desde(Estacion estacion) {
		return new EstacionResponse(
				estacion.getId(),
				estacion.getNombre(),
				estacion.getDireccion(),
				estacion.getLatitud(),
				estacion.getLongitud(),
				estacion.getCapacidad(),
				estacion.isActiva());
	}
}
