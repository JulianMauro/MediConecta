package com.mediconecta.modulos.tickets.messaging;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.mediconecta.modulos.tickets.entity.Ticket;


/**
 * Publica a la queue tickets.nuevos. Point-to-point: si en el futuro hay mas de una
 * instancia del backend corriendo, cada mensaje lo procesa una sola de ellas (compiten
 * por el mensaje), nunca las dos.
 */
@Component
public class TicketProducer {

	private final JmsTemplate jmsTemplate;
	private final String destino;

	public TicketProducer(JmsTemplate jmsTemplate,
			@Value("${app.mensajeria.tickets-nuevos}") String destino) {
		this.jmsTemplate = jmsTemplate;
		this.destino = destino;
	}

	public void publicarNuevo(Ticket ticket) {
		TicketMensaje mensaje = new TicketMensaje(
				ticket.getId(),
				ticket.getUsuario().getId(),
				ticket.getViaje() != null ? ticket.getViaje().getId() : null,
				ticket.getCategoria(),
				ticket.getMotivo(),
				Instant.now());

		jmsTemplate.convertAndSend(destino, mensaje);
	}
}
