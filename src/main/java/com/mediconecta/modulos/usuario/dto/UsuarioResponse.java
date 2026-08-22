package com.mediconecta.modulos.usuario.dto;

import java.time.Instant;

import com.mediconecta.modulos.usuario.entity.Rol;
import com.mediconecta.modulos.usuario.entity.Usuario;


/** Lo que se devuelve al cliente. Nunca incluye la password. */
public record UsuarioResponse(
		Long id,
		String nombre,
		String apellido,
		String email,
		String dni,
		String telefono,
		Rol rol,
		boolean activo,
		Instant fechaCreacion
) {

	public static UsuarioResponse desde(Usuario usuario) {
		return new UsuarioResponse(
				usuario.getId(),
				usuario.getNombre(),
				usuario.getApellido(),
				usuario.getEmail(),
				usuario.getDni(),
				usuario.getTelefono(),
				usuario.getRol(),
				usuario.isActivo(),
				usuario.getFechaCreacion());
	}
}
