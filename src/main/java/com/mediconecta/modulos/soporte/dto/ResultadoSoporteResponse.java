package com.mediconecta.modulos.soporte.dto;

/** Respuesta de toda accion de soporte: lo que quedo registrado y el estado nuevo del usuario. */
public record ResultadoSoporteResponse(
		AccionSoporteResponse accion,
		ResumenUsuarioResponse resumen
) {
}
