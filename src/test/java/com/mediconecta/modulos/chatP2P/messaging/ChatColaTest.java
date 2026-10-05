package com.mediconecta.modulos.chatP2P.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.activemq.filter.MessageEvaluationContext;
import org.apache.activemq.selector.SelectorParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jms.support.converter.MessageConverter;

import com.mediconecta.mensajeria.config.JmsConfig;
import com.mediconecta.modulos.usuario.entity.Rol;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;

/**
 * Sin broker: la cadena ConnectionFactory -> Session -> Producer es de mocks y se captura
 * el mensaje que se iba a mandar. El selector se evalua con el parser real de ActiveMQ,
 * el mismo que usa el broker para decidir a quien le entrega cada mensaje.
 */
class ChatColaTest {

	private final MessageConverter converter = new JmsConfig().jacksonJmsMessageConverter();
	private Session session;
	private MessageProducer producer;
	private ChatCola chatCola;

	@BeforeEach
	void setUp() throws Exception {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		session = mock(Session.class);
		producer = mock(MessageProducer.class);

		when(connectionFactory.createConnection()).thenReturn(connection);
		when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
		when(connection.createSession(anyInt())).thenReturn(session);
		when(session.createQueue(anyString())).thenAnswer(inv -> new ActiveMQQueue(inv.getArgument(0)));
		when(session.createProducer(any())).thenReturn(producer);
		when(session.createTextMessage(anyString())).thenAnswer(inv -> {
			ActiveMQTextMessage mensaje = new ActiveMQTextMessage();
			mensaje.setText(inv.getArgument(0));
			return mensaje;
		});

		chatCola = new ChatCola(connectionFactory, converter, "chat.p2p");
	}

	@Test
	void enviarPoneLasPropiedadesDelSelectorYUnTiempoDeVida() throws Exception {
		ChatMensajeJms enviado = mensaje(5L, 99L);

		chatCola.enviar(enviado);

		ArgumentCaptor<Message> capturado = ArgumentCaptor.forClass(Message.class);
		ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);
		verify(producer).send(capturado.capture(), anyInt(), anyInt(), ttl.capture());

		Message jms = capturado.getValue();
		assertThat(jms.getLongProperty("ticketId")).isEqualTo(5L);
		assertThat(jms.getLongProperty("destinatarioId")).isEqualTo(99L);
		assertThat(ttl.getValue()).isEqualTo(Duration.ofDays(1).toMillis());
		assertThat(converter.fromMessage(jms)).isEqualTo(enviado);
	}

	@Test
	void elSelectorSoloMatcheaAlDestinatarioDeEseTicket() throws Exception {
		chatCola.enviar(mensaje(5L, 99L));
		ArgumentCaptor<Message> capturado = ArgumentCaptor.forClass(Message.class);
		verify(producer).send(capturado.capture(), anyInt(), anyInt(), anyLong());
		ActiveMQTextMessage jms = (ActiveMQTextMessage) capturado.getValue();

		assertThat(matchea(ChatCola.selector(5L, 99L), jms)).isTrue();
		assertThat(matchea(ChatCola.selector(5L, 10L), jms)).as("el remitente no recibe su propio mensaje").isFalse();
		assertThat(matchea(ChatCola.selector(6L, 99L), jms)).as("otro ticket del mismo admin").isFalse();
	}

	@Test
	void elDestinoEsLaQueueDelChat() throws Exception {
		chatCola.enviar(mensaje(5L, 99L));

		verify(session).createProducer(eq(new ActiveMQQueue("chat.p2p")));
	}

	@Test
	void recibirPendientesDrenaConUnSoloConsumerFiltradoPorSelector() throws Exception {
		ChatMensajeJms primero = mensaje(5L, 99L);
		ChatMensajeJms segundo = new ChatMensajeJms(2L, 5L, 10L, Rol.CLIENTE, 99L, "chau", Instant.parse("2026-10-05T12:01:00Z"));
		Message jmsPrimero = converter.toMessage(primero, session);
		Message jmsSegundo = converter.toMessage(segundo, session);
		MessageConsumer consumer = mock(MessageConsumer.class);
		when(session.getTransacted()).thenReturn(true);
		when(session.createConsumer(any(), anyString())).thenReturn(consumer);
		when(consumer.receive(anyLong())).thenReturn(jmsPrimero, jmsSegundo, null);

		List<ChatMensajeJms> recibidos = chatCola.recibirPendientes(5L, 99L);

		assertThat(recibidos).containsExactly(primero, segundo);
		verify(session).createConsumer(eq(new ActiveMQQueue("chat.p2p")), eq(ChatCola.selector(5L, 99L)));
		verify(session).commit();
	}

	private static boolean matchea(String selector, ActiveMQTextMessage mensaje) throws Exception {
		MessageEvaluationContext contexto = new MessageEvaluationContext();
		contexto.setMessageReference(mensaje);
		return SelectorParser.parse(selector).matches(contexto);
	}

	private static ChatMensajeJms mensaje(Long ticketId, Long destinatarioId) {
		return new ChatMensajeJms(1L, ticketId, 10L, Rol.CLIENTE, destinatarioId, "hola", Instant.parse("2026-10-05T12:00:00Z"));
	}
}
