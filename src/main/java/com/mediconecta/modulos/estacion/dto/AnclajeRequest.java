package com.mediconecta.modulos.estacion.dto;

import jakarta.validation.constraints.Positive;


public record AnclajeRequest(

		@Positive(message = "el numero debe ser mayor a 0")
		int numero
) {
}
