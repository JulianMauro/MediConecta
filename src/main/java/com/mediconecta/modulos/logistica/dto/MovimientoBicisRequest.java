package com.mediconecta.modulos.logistica.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;


/**
 * Origen y destino son polimorficos (Estacion o Almacen): el service valida que
 * en cada par (origen/destino) venga exactamente uno de los dos ids.
 */
public record MovimientoBicisRequest(

		Long origenEstacionId,
		Long origenAlmacenId,
		Long destinoEstacionId,
		Long destinoAlmacenId,

		@NotEmpty(message = "el movimiento debe incluir al menos una bicicleta")
		List<Long> bicicletaIds
) {
}
