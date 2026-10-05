package com.mediconecta.mensajeria.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.jupiter.api.Test;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;

import com.mediconecta.modulos.tickets.entity.CategoriaTicket;
import com.mediconecta.modulos.tickets.messaging.TicketMensaje;

import jakarta.jms.Session;
import jakarta.jms.TextMessage;

/**
 * Prueba el conversor sin broker: la sesion JMS es un mock que devuelve un TextMessage
 * real de ActiveMQ. Sin "_tipo" el conversor no sabe a que clase volver al recibir.
 */
class JmsConfigTest {

	private final MessageConverter converter = new JmsConfig().jacksonJmsMessageConverter();

	@Test
	void idaYVueltaDevuelveElMismoMensajeYElBodyEsSoloJson() throws Exception {
		TicketMensaje original = new TicketMensaje(1L, 10L, null, CategoriaTicket.ANCLAJE, "no puedo anclar", Instant.parse("2026-10-05T12:00:00Z"));

		TextMessage jms = (TextMessage) converter.toMessage(original, sesionQueCreaTextMessages());

		assertThat(jms.getText()).contains("\"motivo\":\"no puedo anclar\"").doesNotContain("_tipo");
		assertThat(jms.getStringProperty("_tipo")).isEqualTo(TicketMensaje.class.getName());
		assertThat(converter.fromMessage(jms)).isEqualTo(original);
	}

	@Test
	void rechazaTiposFueraDeLosPaquetesDeMensajeria() throws Exception {
		ActiveMQTextMessage ajeno = new ActiveMQTextMessage();
		ajeno.setText("{}");
		ajeno.setStringProperty("_tipo", "java.util.ArrayList");

		assertThatThrownBy(() -> converter.fromMessage(ajeno))
				.isInstanceOf(MessageConversionException.class);
	}

	static Session sesionQueCreaTextMessages() throws Exception {
		Session session = mock(Session.class);
		when(session.createTextMessage(anyString())).thenAnswer(inv -> {
			ActiveMQTextMessage mensaje = new ActiveMQTextMessage();
			mensaje.setText(inv.getArgument(0));
			return mensaje;
		});
		return session;
	}
}
