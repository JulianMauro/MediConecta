package com.mediconecta.modulos.almacen.dto;

import com.mediconecta.modulos.almacen.entity.Almacen;

public record AlmacenResponse(
		Long id,
		String nombre,
		String direccion,
		int capacidad
) {

	public static AlmacenResponse desde(Almacen almacen) {
		return new AlmacenResponse(almacen.getId(), almacen.getNombre(), almacen.getDireccion(),
				almacen.getCapacidad());
	}
}
