package com.mediconecta.modulos.chatP2P.service;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.chatP2P.dto.ChatMensajeRequest;
import com.mediconecta.modulos.chatP2P.dto.ChatMensajeResponse;
import com.mediconecta.modulos.chatP2P.entity.ChatMensaje;
import com.mediconecta.modulos.chatP2P.messaging.ChatCola;
import com.mediconecta.modulos.chatP2P.messaging.ChatMensajeJms;
import com.mediconecta.modulos.chatP2P.repository.ChatMensajeRepository;
import com.mediconecta.modulos.tickets.entity.EstadoTicket;
import com.mediconecta.modulos.tickets.entity.Ticket;
import com.mediconecta.modulos.tickets.repository.TicketRepository;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;


/**
 * Chat 1 a 1 entre el cliente dueño del ticket y el admin que lo tomo. Solo se puede
 * escribir mientras el ticket esta EN_CHAT: cuando el admin lo resuelve, el chat queda
 * cerrado. Leer pendientes e historial sigue permitido despues, para no perder lo ultimo.
 */
@Service
public class ChatService {

	private final ChatMensajeRepository chatMensajeRepository;
	private final TicketRepository ticketRepository;
	private final UsuarioRepository usuarioRepository;
	private final ChatCola chatCola;

	public ChatService(ChatMensajeRepository chatMensajeRepository, TicketRepository ticketRepository,
			UsuarioRepository usuarioRepository, ChatCola chatCola) {
		this.chatMensajeRepository = chatMensajeRepository;
		this.ticketRepository = ticketRepository;
		this.usuarioRepository = usuarioRepository;
		this.chatCola = chatCola;
	}

	@Transactional
	public ChatMensajeResponse enviar(Long remitenteId, Long ticketId, ChatMensajeRequest request) {
		Ticket ticket = obtenerTicket(ticketId);
		validarParticipante(ticket, remitenteId);
		if (ticket.getEstado() != EstadoTicket.EN_CHAT) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "el chat de este ticket no esta abierto");
		}

		Usuario remitente = usuarioRepository.findById(remitenteId)
				.orElseThrow(() -> new IllegalStateException("usuario autenticado no encontrado"));
		Long destinatarioId = esCliente(ticket, remitenteId) ? ticket.getAdmin().getId() : ticket.getUsuario().getId();

		ChatMensaje mensaje = chatMensajeRepository.save(new ChatMensaje(ticket, remitente, request.contenido()));
		chatCola.enviar(new ChatMensajeJms(
				mensaje.getId(),
				ticketId,
				remitenteId,
				remitente.getRol(),
				destinatarioId,
				mensaje.getContenido(),
				mensaje.getFechaEnvio()));

		return ChatMensajeResponse.desde(mensaje);
	}

	@Transactional
	public List<ChatMensajeResponse> recibirNuevos(Long usuarioId, Long ticketId) {
		validarParticipante(obtenerTicket(ticketId), usuarioId);

		List<ChatMensajeJms> pendientes = chatCola.recibirPendientes(ticketId, usuarioId);
		Instant ahora = Instant.now();
		for (ChatMensajeJms pendiente : pendientes) {
			chatMensajeRepository.findById(pendiente.mensajeId()).ifPresent(m -> m.marcarEntregado(ahora));
		}

		return pendientes.stream()
				.map(pendiente -> ChatMensajeResponse.recibido(pendiente, ahora))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ChatMensajeResponse> historial(Long usuarioId, Long ticketId) {
		validarParticipante(obtenerTicket(ticketId), usuarioId);

		return chatMensajeRepository.findByTicketIdOrderByFechaEnvioAsc(ticketId).stream()
				.map(ChatMensajeResponse::desde)
				.toList();
	}

	private Ticket obtenerTicket(Long ticketId) {
		return ticketRepository.findById(ticketId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ticket no encontrado"));
	}

	/** El rol no alcanza: un admin que no tomo este ticket tampoco puede leer ni escribir. */
	private void validarParticipante(Ticket ticket, Long usuarioId) {
		boolean esAdminAsignado = ticket.getAdmin() != null && ticket.getAdmin().getId().equals(usuarioId);
		if (!esCliente(ticket, usuarioId) && !esAdminAsignado) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "no participas del chat de este ticket");
		}
	}

	private boolean esCliente(Ticket ticket, Long usuarioId) {
		return ticket.getUsuario().getId().equals(usuarioId);
	}
}
