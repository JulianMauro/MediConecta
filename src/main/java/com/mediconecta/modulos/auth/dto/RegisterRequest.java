package com.mediconecta.modulos.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


/** Alta publica: siempre crea un usuario rol CLIENTE (no se puede elegir el rol desde aca). */
public record RegisterRequest(

		@NotBlank(message = "el nombre es obligatorio")
		@Size(max = 100, message = "el nombre no puede superar los 100 caracteres")
		String nombre,

		@NotBlank(message = "el apellido es obligatorio")
		@Size(max = 100, message = "el apellido no puede superar los 100 caracteres")
		String apellido,

		@NotBlank(message = "el email es obligatorio")
		@Email(message = "el email no tiene un formato valido")
		@Size(max = 150, message = "el email no puede superar los 150 caracteres")
		String email,

		@NotBlank(message = "la password es obligatoria")
		@Size(min = 8, max = 72, message = "la password debe tener entre 8 y 72 caracteres")
		String password,

		@Size(max = 20, message = "el dni no puede superar los 20 caracteres")
		String dni,

		@Size(max = 30, message = "el telefono no puede superar los 30 caracteres")
		String telefono
) {
}
