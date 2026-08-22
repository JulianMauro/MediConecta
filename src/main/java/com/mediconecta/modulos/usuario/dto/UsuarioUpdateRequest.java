package com.mediconecta.modulos.usuario.dto;

import com.mediconecta.modulos.usuario.entity.Rol;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


/** Datos editables de un usuario. El email y la password no se tocan aca. */
public record UsuarioUpdateRequest(

		@NotBlank(message = "el nombre es obligatorio")
		@Size(max = 100, message = "el nombre no puede superar los 100 caracteres")
		String nombre,

		@NotBlank(message = "el apellido es obligatorio")
		@Size(max = 100, message = "el apellido no puede superar los 100 caracteres")
		String apellido,

		@Size(max = 20, message = "el dni no puede superar los 20 caracteres")
		String dni,

		@Size(max = 30, message = "el telefono no puede superar los 30 caracteres")
		String telefono,

		@NotNull(message = "el rol es obligatorio")
		Rol rol
) {
}
