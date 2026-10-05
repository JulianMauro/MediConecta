package com.mediconecta.modulos.chatP2P.messaging;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;


/**
 * Acceso a la queue chat.p2p. Todas las conversaciones comparten la misma queue: cada
 * mensaje lleva ticketId y destinatarioId como propiedades JMS, y el destinatario lee
 * solo lo suyo con un selector. Asi no hace falta una queue por conversacion.
 *
 * Usa un JmsTemplate propio (no el bean compartido) porque necesita dos ajustes que no
 * deben afectar a las otras colas: TTL en los mensajes, para que un chat abandonado no
 * deje mensajes en el broker para siempre, y un timeout de lectura corto, porque el
 * default del template bloquea hasta que llegue un mensaje.
 *
 * La sesion es transaccional y se sincroniza con la transaccion de la base: lo enviado
 * se confirma despues del commit, y lo leido se saca de la queue recien cuando el commit
 * (que marca fechaEntrega) salio bien; si falla, los mensajes vuelven a quedar pendientes.
 */
@Component
public class ChatCola {

	static final String PROPIEDAD_TICKET = "ticketId";
	static final String PROPIEDAD_DESTINATARIO = "destinatarioId";

	private static final long TIEMPO_DE_VIDA_MS = Duration.ofDays(1).toMillis();
	/** Con receiveNoWait ActiveMQ devuelve null si el broker todavia no despacho el mensaje al consumer. */
	private static final long ESPERA_LECTURA_MS = 300;
	private static final int MAXIMO_POR_LECTURA = 50;

	private final ConnectionFactory connectionFactory;
	private final JmsTemplate jmsTemplate;
	private final MessageConverter messageConverter;
	private final String destino;

	public ChatCola(ConnectionFactory connectionFactory, MessageConverter messageConverter,
			@Value("${app.mensajeria.chat-p2p}") String destino) {
		JmsTemplate template = new JmsTemplate(connectionFactory);
		template.setMessageConverter(messageConverter);
		template.setSessionTransacted(true);
		template.setExplicitQosEnabled(true);
		template.setTimeToLive(TIEMPO_DE_VIDA_MS);
		this.connectionFactory = connectionFactory;
		this.jmsTemplate = template;
		this.messageConverter = messageConverter;
		this.destino = destino;
	}

	public void enviar(ChatMensajeJms mensaje) {
		jmsTemplate.convertAndSend(destino, mensaje, jms -> {
			jms.setLongProperty(PROPIEDAD_TICKET, mensaje.ticketId());
			jms.setLongProperty(PROPIEDAD_DESTINATARIO, mensaje.destinatarioId());
			return jms;
		});
	}

	/**
	 * Consume (y por lo tanto saca de la queue) los mensajes pendientes para ese destinatario
	 * en ese ticket. Un solo consumer para toda la lectura: con un consumer por mensaje, en
	 * una sesion transaccional ActiveMQ retiene hasta el commit lo que cada uno precargo.
	 */
	public List<ChatMensajeJms> recibirPendientes(Long ticketId, Long destinatarioId) {
		return jmsTemplate.execute(session -> {
			Destination queue = session.createQueue(destino);
			try (MessageConsumer consumer = session.createConsumer(queue, selector(ticketId, destinatarioId))) {
				List<ChatMensajeJms> recibidos = new ArrayList<>();
				Message mensaje;
				while (recibidos.size() < MAXIMO_POR_LECTURA && (mensaje = consumer.receive(ESPERA_LECTURA_MS)) != null) {
					recibidos.add((ChatMensajeJms) messageConverter.fromMessage(mensaje));
				}
				// execute() no confirma la sesion por su cuenta (solo send/receive lo hacen). Dentro de
				// un @Transactional la confirma Spring despues del commit de la base; fuera, aca.
				if (session.getTransacted() && !ConnectionFactoryUtils.isSessionTransactional(session, connectionFactory)) {
					session.commit();
				}
				return recibidos;
			}
		}, true);
	}

	static String selector(Long ticketId, Long destinatarioId) {
		return PROPIEDAD_TICKET + " = " + ticketId + " AND " + PROPIEDAD_DESTINATARIO + " = " + destinatarioId;
	}
}
