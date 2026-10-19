package com.mediconecta.modulos.soporte.dto;

import java.time.Instant;

import com.mediconecta.modulos.estacion.entity.Anclaje;
import com.mediconecta.modulos.viaje.entity.Viaje;

/** Un viaje con lo que el admin necesita leer de un vistazo: estaciones por nombre, no por id. */
public record ViajeSoporteResponse(
		Long id,
		String codigoBicicleta,
		String estacionOrigen,
		int anclajeOrigenNumero,
		/** Null si el viaje sigue en curso o si la bici termino en un almacen. */
		String estacionDestino,
		Integer anclajeDestinoNumero,
		Instant fechaInicio,
		Instant fechaFin,
		long duracionMinutos,
		int minutosPermitidos,
		boolean excedioTiempo,
		boolean excesoCondonado
) {

	public static ViajeSoporteResponse desde(Viaje viaje) {
		Anclaje destino = viaje.getAnclajeDestino();
		return new ViajeSoporteResponse(
				viaje.getId(),
				viaje.getBicicleta().getCodigo(),
				viaje.getAnclajeOrigen().getEstacion().getNombre(),
				viaje.getAnclajeOrigen().getNumero(),
				destino != null ? destino.getEstacion().getNombre() : null,
				destino != null ? destino.getNumero() : null,
				viaje.getFechaInicio(),
				viaje.getFechaFin(),
				viaje.duracionMinutos(),
				viaje.getMembresia().getTiempoPermitidoMinutos(),
				viaje.isExcedioTiempo(),
				viaje.isExcesoCondonado());
	}
}
