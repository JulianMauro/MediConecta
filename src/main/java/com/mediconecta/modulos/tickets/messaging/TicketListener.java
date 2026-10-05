package com.mediconecta.modulos.tickets.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mediconecta.modulos.tickets.entity.Ticket;
import com.mediconecta.modulos.tickets.repository.TicketRepository;


/**
 * Consume tickets.nuevos. Representa al "backend de los admins": en un despliegue con
 * varias instancias, la queue le entrega el mensaje a una sola (competing consumers).
 * Marca fechaNotificacionAdmin para dejar evidencia de que el mensaje se proceso.
 */
@Component
public class TicketListener {

	private static final Logger log = LoggerFactory.getLogger(TicketListener.class);

	private final TicketRepository ticketRepository;

	public TicketListener(TicketRepository ticketRepository) {
		this.ticketRepository = ticketRepository;
	}

	@JmsListener(destination = "${app.mensajeria.tickets-nuevos}")
	@Transactional
	public void recibir(TicketMensaje mensaje) {
		log.info("ticket #{} notificado a los admins (usuario {}, motivo: {})",
				mensaje.ticketId(), mensaje.usuarioId(), mensaje.motivo());

		ticketRepository.findById(mensaje.ticketId()).ifPresent(Ticket::marcarNotificado);
	}
}
