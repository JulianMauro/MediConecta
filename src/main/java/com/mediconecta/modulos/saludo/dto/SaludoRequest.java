package com.mediconecta.modulos.saludo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record SaludoRequest(

		@NotBlank(message = "el nombre es obligatorio")
		@Size(max = 100, message = "el nombre no puede superar los 100 caracteres")
		String nombre
) {
}
