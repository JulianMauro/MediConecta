package com.mediconecta.modulos.viaje.dto;

import jakarta.validation.constraints.NotNull;


public record ViajeFinalizarRequest(

		@NotNull(message = "el anclaje de destino es obligatorio")
		Long anclajeDestinoId
) {
}
