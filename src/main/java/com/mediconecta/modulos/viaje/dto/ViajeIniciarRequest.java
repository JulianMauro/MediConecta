package com.mediconecta.modulos.viaje.dto;

import jakarta.validation.constraints.NotBlank;


public record ViajeIniciarRequest(

		@NotBlank(message = "el codigo de la bicicleta es obligatorio")
		String codigoBicicleta
) {
}
