package com.mediconecta.modulos.almacen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;


public record AlmacenRequest(

		@NotBlank(message = "el nombre es obligatorio")
		@Size(max = 100, message = "el nombre no puede superar los 100 caracteres")
		String nombre,

		@NotBlank(message = "la direccion es obligatoria")
		@Size(max = 255, message = "la direccion no puede superar los 255 caracteres")
		String direccion,

		@Positive(message = "la capacidad debe ser mayor a 0")
		int capacidad
) {
}
