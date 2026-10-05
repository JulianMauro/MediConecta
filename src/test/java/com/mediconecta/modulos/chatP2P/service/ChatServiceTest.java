package com.mediconecta.modulos.chatP2P.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.chatP2P.dto.ChatMensajeRequest;
import com.mediconecta.modulos.chatP2P.dto.ChatMensajeResponse;
import com.mediconecta.modulos.chatP2P.entity.ChatMensaje;
import com.mediconecta.modulos.chatP2P.messaging.ChatCola;
import com.mediconecta.modulos.chatP2P.messaging.ChatMensajeJms;
import com.mediconecta.modulos.chatP2P.repository.ChatMensajeRepository;
import com.mediconecta.modulos.tickets.entity.CategoriaTicket;
import com.mediconecta.modulos.tickets.entity.Ticket;
import com.mediconecta.modulos.tickets.repository.TicketRepository;
import com.mediconecta.modulos.usuario.entity.Rol;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;

/** Unit test puro: sin Spring, sin base y sin broker (ChatCola mockeada). */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

	private static final Long CLIENTE = 10L;
	private static final Long ADMIN = 99L;
	private static final Long TICKET = 1L;

	@Mock
	private ChatMensajeRepository chatMensajeRepository;
	@Mock
	private TicketRepository ticketRepository;
	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private ChatCola chatCola;

	private ChatService chatService;
	private Usuario cliente;
	private Usuario admin;
	private Ticket ticket;

	@BeforeEach
	void setUp() {
		chatService = new ChatService(chatMensajeRepository, ticketRepository, usuarioRepository, chatCola);

		cliente = new Usuario("Juana", "Perez", "juana@ejemplo.com", "hash", "1", "111", Rol.CLIENTE);
		ReflectionTestUtils.setField(cliente, "id", CLIENTE);
		admin = new Usuario("Ana", "Admin", "ana@ejemplo.com", "hash", "2", "222", Rol.ADMIN);
		ReflectionTestUtils.setField(admin, "id", ADMIN);

		ticket = new Ticket(cliente, null, CategoriaTicket.ANCLAJE, "no puedo anclar la bici");
		ReflectionTestUtils.setField(ticket, "id", TICKET);
	}

	@Test
	void elClienteLeEscribeAlAdminQueTomoElTicket() {
		ticket.tomar(admin);
		dadoQueExisteElTicket();
		when(usuarioRepository.findById(CLIENTE)).thenReturn(Optional.of(cliente));
		when(chatMensajeRepository.save(any(ChatMensaje.class))).thenAnswer(inv -> conId(inv.getArgument(0), 7L));

		chatService.enviar(CLIENTE, TICKET, new ChatMensajeRequest("hola"));

		ChatMensajeJms encolado = mensajeEncolado();
		assertThat(encolado.destinatarioId()).isEqualTo(ADMIN);
		assertThat(encolado.remitenteRol()).isEqualTo(Rol.CLIENTE);
		assertThat(encolado.mensajeId()).isEqualTo(7L);
	}

	@Test
	void elAdminLeEscribeAlClienteDuenoDelTicket() {
		ticket.tomar(admin);
		dadoQueExisteElTicket();
		when(usuarioRepository.findById(ADMIN)).thenReturn(Optional.of(admin));
		when(chatMensajeRepository.save(any(ChatMensaje.class))).thenAnswer(inv -> conId(inv.getArgument(0), 8L));

		chatService.enviar(ADMIN, TICKET, new ChatMensajeRequest("ya lo reviso"));

		assertThat(mensajeEncolado().destinatarioId()).isEqualTo(CLIENTE);
	}

	@Test
	void noSePuedeEscribirSiElTicketTodaviaNoFueTomado() {
		dadoQueExisteElTicket();

		assertThatThrownBy(() -> chatService.enviar(CLIENTE, TICKET, new ChatMensajeRequest("hola")))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("no esta abierto");
		verify(chatCola, never()).enviar(any());
	}

	@Test
	void resolverElTicketCierraElChat() {
		ticket.tomar(admin);
		ticket.resolver();
		dadoQueExisteElTicket();

		assertThatThrownBy(() -> chatService.enviar(ADMIN, TICKET, new ChatMensajeRequest("chau")))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("no esta abierto");
		verify(chatCola, never()).enviar(any());
	}

	@Test
	void otroAdminQueNoTomoElTicketNoParticipa() {
		ticket.tomar(admin);
		dadoQueExisteElTicket();

		assertThatThrownBy(() -> chatService.enviar(123L, TICKET, new ChatMensajeRequest("hola")))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("no participas");
		assertThatThrownBy(() -> chatService.historial(123L, TICKET))
				.isInstanceOf(ResponseStatusException.class);
		assertThatThrownBy(() -> chatService.recibirNuevos(123L, TICKET))
				.isInstanceOf(ResponseStatusException.class);
		verify(chatCola, never()).recibirPendientes(any(), any());
	}

	@Test
	void recibirNuevosDevuelveLoDeLaQueueYLoMarcaEntregado() {
		ticket.tomar(admin);
		dadoQueExisteElTicket();
		ChatMensaje guardado = conId(new ChatMensaje(ticket, cliente, "hola"), 7L);
		ChatMensajeJms pendiente = new ChatMensajeJms(7L, TICKET, CLIENTE, Rol.CLIENTE, ADMIN, "hola", Instant.now());
		when(chatCola.recibirPendientes(TICKET, ADMIN)).thenReturn(List.of(pendiente));
		when(chatMensajeRepository.findById(7L)).thenReturn(Optional.of(guardado));

		List<ChatMensajeResponse> nuevos = chatService.recibirNuevos(ADMIN, TICKET);

		assertThat(nuevos).singleElement().satisfies(m -> {
			assertThat(m.contenido()).isEqualTo("hola");
			assertThat(m.remitenteId()).isEqualTo(CLIENTE);
			assertThat(m.fechaEntrega()).isNotNull();
		});
		assertThat(guardado.getFechaEntrega()).isNotNull();
	}

	@Test
	void historialDevuelveLaConversacionEnOrden() {
		ticket.tomar(admin);
		dadoQueExisteElTicket();
		when(chatMensajeRepository.findByTicketIdOrderByFechaEnvioAsc(TICKET)).thenReturn(List.of(
				conId(new ChatMensaje(ticket, cliente, "hola"), 7L),
				conId(new ChatMensaje(ticket, admin, "ya lo reviso"), 8L)));

		List<ChatMensajeResponse> historial = chatService.historial(CLIENTE, TICKET);

		assertThat(historial).extracting(ChatMensajeResponse::contenido).containsExactly("hola", "ya lo reviso");
	}

	private void dadoQueExisteElTicket() {
		when(ticketRepository.findById(TICKET)).thenReturn(Optional.of(ticket));
	}

	private ChatMensajeJms mensajeEncolado() {
		ArgumentCaptor<ChatMensajeJms> captor = ArgumentCaptor.forClass(ChatMensajeJms.class);
		verify(chatCola).enviar(captor.capture());
		return captor.getValue();
	}

	private static ChatMensaje conId(ChatMensaje mensaje, Long id) {
		ReflectionTestUtils.setField(mensaje, "id", id);
		return mensaje;
	}
}
