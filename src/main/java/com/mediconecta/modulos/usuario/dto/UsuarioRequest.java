package com.mediconecta.modulos.usuario.dto;

import com.mediconecta.modulos.usuario.entity.Rol;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/** Datos que llegan para crear un usuario nuevo. */
public record UsuarioRequest(

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
		String telefono,

		@NotNull(message = "el rol es obligatorio")
		Rol rol
) {
}
