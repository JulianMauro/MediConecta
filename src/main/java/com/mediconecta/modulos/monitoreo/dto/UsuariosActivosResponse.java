package com.mediconecta.modulos.monitoreo.dto;

import java.time.Instant;

import com.mediconecta.modulos.monitoreo.entity.RegistroUsuariosActivos;

/** cantidad = usuarios con un viaje en curso; timestamp = momento de la lectura. */
public record UsuariosActivosResponse(
		int cantidad,
		Instant timestamp
) {

	public static UsuariosActivosResponse desde(RegistroUsuariosActivos registro) {
		return new UsuariosActivosResponse(registro.getCantidad(), registro.getTimestamp());
	}

	/** Para el valor en vivo, que no viene de un registro guardado sino del contador en memoria. */
	public static UsuariosActivosResponse actual(int cantidad) {
		return new UsuariosActivosResponse(cantidad, Instant.now());
	}
}
