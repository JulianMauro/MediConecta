package com.mediconecta.modulos.tickets.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.tickets.dto.TicketRequest;
import com.mediconecta.modulos.tickets.dto.TicketResponse;
import com.mediconecta.modulos.tickets.entity.CategoriaTicket;
import com.mediconecta.modulos.tickets.entity.EstadoTicket;
import com.mediconecta.modulos.tickets.entity.Ticket;
import com.mediconecta.modulos.tickets.messaging.TicketProducer;
import com.mediconecta.modulos.tickets.repository.TicketRepository;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;
import com.mediconecta.modulos.viaje.entity.Viaje;
import com.mediconecta.modulos.viaje.repository.ViajeRepository;


/**
 * Alta y ciclo de vida del ticket. Al crear uno, publica a la queue tickets.nuevos
 * (ver TicketProducer): el resto del flujo (tomar/resolver) es REST comun, la
 * mensajeria solo interviene para avisar que hay un ticket nuevo.
 */
@Service
public class TicketService {

	private final TicketRepository ticketRepository;
	private final UsuarioRepository usuarioRepository;
	private final ViajeRepository viajeRepository;
	private final TicketProducer ticketProducer;

	public TicketService(TicketRepository ticketRepository, UsuarioRepository usuarioRepository,
			ViajeRepository viajeRepository, TicketProducer ticketProducer) {
		this.ticketRepository = ticketRepository;
		this.usuarioRepository = usuarioRepository;
		this.viajeRepository = viajeRepository;
		this.ticketProducer = ticketProducer;
	}

	@Transactional
	public TicketResponse crear(Long usuarioId, TicketRequest request) {
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new IllegalStateException("usuario autenticado no encontrado"));

		Viaje viaje = null;
		if (request.viajeId() != null) {
			viaje = viajeRepository.findById(request.viajeId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "viaje no encontrado"));
		}

		Ticket ticket = ticketRepository.save(new Ticket(usuario, viaje, request.categoria(), request.motivo()));
		ticketProducer.publicarNuevo(ticket);

		return TicketResponse.desde(ticket);
	}

	@Transactional(readOnly = true)
	public List<TicketResponse> listarPropios(Long usuarioId, CategoriaTicket categoria) {
		return filtrar(ticketRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId), categoria);
	}

	@Transactional(readOnly = true)
	public List<TicketResponse> listarAbiertos(CategoriaTicket categoria) {
		return filtrar(ticketRepository.findByEstadoOrderByFechaCreacionAsc(EstadoTicket.ABIERTO), categoria);
	}

	@Transactional(readOnly = true)
	public List<TicketResponse> listarAsignados(Long adminId, CategoriaTicket categoria) {
		return filtrar(ticketRepository.findByAdminIdOrderByFechaCreacionDesc(adminId), categoria);
	}

	/** categoria null = sin filtro. */
	private static List<TicketResponse> filtrar(List<Ticket> tickets, CategoriaTicket categoria) {
		return tickets.stream()
				.filter(t -> categoria == null || t.getCategoria() == categoria)
				.map(TicketResponse::desde)
				.toList();
	}

	@Transactional
	public TicketResponse tomar(Long adminId, Long ticketId) {
		Ticket ticket = obtener(ticketId);
		if (ticket.getEstado() != EstadoTicket.ABIERTO) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "el ticket ya fue tomado");
		}

		Usuario admin = usuarioRepository.findById(adminId)
				.orElseThrow(() -> new IllegalStateException("usuario autenticado no encontrado"));

		ticket.tomar(admin);
		return TicketResponse.desde(ticket);
	}

	@Transactional
	public TicketResponse resolver(Long adminId, Long ticketId) {
		Ticket ticket = obtener(ticketId);
		if (ticket.getEstado() == EstadoTicket.ABIERTO) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "primero hay que tomar el ticket");
		}
		if (ticket.getEstado() == EstadoTicket.RESUELTO) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "el ticket ya esta resuelto");
		}
		if (!ticket.getAdmin().getId().equals(adminId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "el ticket lo tomo otro admin");
		}

		ticket.resolver();
		return TicketResponse.desde(ticket);
	}

	private Ticket obtener(Long ticketId) {
		return ticketRepository.findById(ticketId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ticket no encontrado"));
	}
}
