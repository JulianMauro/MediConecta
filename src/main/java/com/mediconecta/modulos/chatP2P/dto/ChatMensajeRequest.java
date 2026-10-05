package com.mediconecta.modulos.chatP2P.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record ChatMensajeRequest(

		@NotBlank(message = "el mensaje no puede estar vacio")
		@Size(max = 1000, message = "el mensaje no puede superar los 1000 caracteres")
		String contenido
) {
}
