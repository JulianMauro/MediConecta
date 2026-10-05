package com.mediconecta.mensajeria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;


/**
 * Hace que JmsTemplate mande TextMessage con JSON en vez de ObjectMessage con la clase
 * Java serializada: el body queda legible en la consola de ActiveMQ.
 *
 * El nombre de la clase viaja aparte, en la propiedad JMS "_tipo" (no en el body): sin
 * ella el conversor no sabe a que clase deserializar al recibir. Solo se aceptan clases
 * de los paquetes listados (comparacion exacta, no por prefijo), para que un mensaje
 * ajeno no pueda pedir instanciar cualquier clase. Un modulo nuevo con mensajeria
 * tiene que agregar su paquete aca.
 */
@Configuration
public class JmsConfig {

	@Bean
	public MessageConverter jacksonJmsMessageConverter() {
		JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
		converter.setTargetType(MessageType.TEXT);
		converter.setTypeIdPropertyName("_tipo");
		converter.setTrustedPackages(
				"com.mediconecta.modulos.tickets.messaging",
				"com.mediconecta.modulos.chatP2P.messaging");
		return converter;
	}
}
