package com.mediconecta.modulos.tickets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.tickets.dto.TicketRequest;
import com.mediconecta.modulos.tickets.dto.TicketResponse;
import com.mediconecta.modulos.tickets.entity.CategoriaTicket;
import com.mediconecta.modulos.tickets.entity.EstadoTicket;
import com.mediconecta.modulos.tickets.entity.Ticket;
import com.mediconecta.modulos.tickets.messaging.TicketProducer;
import com.mediconecta.modulos.tickets.repository.TicketRepository;
import com.mediconecta.modulos.usuario.entity.Rol;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;
import com.mediconecta.modulos.viaje.entity.Viaje;
import com.mediconecta.modulos.viaje.repository.ViajeRepository;
import com.mediconecta.testutil.Datos;

/**
 * Unit test puro (sin Spring, sin base, sin broker): TicketService solo depende de
 * interfaces mockeadas. Prueba que el modulo de tickets se puede validar aislado del
 * resto de la app, tal como lo estan los otros modulos.
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

	@Mock
	private TicketRepository ticketRepository;
	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private ViajeRepository viajeRepository;
	@Mock
	private TicketProducer ticketProducer;

	private TicketService ticketService;

	private Usuario cliente;
	private Usuario admin;

	@BeforeEach
	void setUp() {
		ticketService = new TicketService(ticketRepository, usuarioRepository, viajeRepository, ticketProducer);

		cliente = new Usuario("Juana", "Perez", "juana@ejemplo.com", "hash", "1", "111", Rol.CLIENTE);
		ReflectionTestUtils.setField(cliente, "id", 10L);

		admin = new Usuario("Ana", "Admin", "ana@ejemplo.com", "hash", "2", "222", Rol.ADMIN);
		ReflectionTestUtils.setField(admin, "id", 99L);
	}

	@Test
	void crearPersisteElTicketYLoPublicaEnLaQueue() {
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(cliente));
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

		TicketResponse response = ticketService.crear(10L, new TicketRequest(null, CategoriaTicket.ANCLAJE, "no puedo anclar la bici"));

		assertThat(response.usuarioId()).isEqualTo(10L);
		assertThat(response.estado()).isEqualTo(EstadoTicket.ABIERTO);
		assertThat(response.motivo()).isEqualTo("no puedo anclar la bici");
		assertThat(response.categoria()).isEqualTo(CategoriaTicket.ANCLAJE);
		verify(ticketProducer).publicarNuevo(any(Ticket.class));
	}

	@Test
	void crearConViajeInexistenteDevuelve404YNoPublicaNada() {
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(cliente));
		when(viajeRepository.findById(555L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> ticketService.crear(10L, new TicketRequest(555L, CategoriaTicket.OTRO, "motivo")))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("viaje no encontrado");

		verify(ticketProducer, never()).publicarNuevo(any());
	}

	@Test
	void crearConElViajeDeOtroUsuarioDevuelve403YNoPublicaNada() {
		Viaje viajeAjeno = Datos.viajeEnCurso(555L, Datos.cliente(77L), 5);
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(cliente));
		when(viajeRepository.findById(555L)).thenReturn(Optional.of(viajeAjeno));

		assertThatThrownBy(() -> ticketService.crear(10L, new TicketRequest(555L, CategoriaTicket.ANCLAJE, "motivo")))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("otro usuario");

		verify(ticketRepository, never()).save(any());
		verify(ticketProducer, never()).publicarNuevo(any());
	}

	@Test
	void crearConUnViajePropioLoAsocia() {
		Viaje propio = Datos.viajeEnCurso(555L, cliente, 5);
		when(usuarioRepository.findById(10L)).thenReturn(Optional.of(cliente));
		when(viajeRepository.findById(555L)).thenReturn(Optional.of(propio));
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

		TicketResponse response = ticketService.crear(10L, new TicketRequest(555L, CategoriaTicket.ANCLAJE, "no traba"));

		assertThat(response.viajeId()).isEqualTo(555L);
	}

	@Test
	void tomarAsignaElAdminYPasaAEnChat() {
		Ticket ticket = new Ticket(cliente, null, CategoriaTicket.ANCLAJE, "motivo");
		ReflectionTestUtils.setField(ticket, "id", 1L);

		when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
		when(usuarioRepository.findById(99L)).thenReturn(Optional.of(admin));

		TicketResponse response = ticketService.tomar(99L, 1L);

		assertThat(response.adminId()).isEqualTo(99L);
		assertThat(response.estado()).isEqualTo(EstadoTicket.EN_CHAT);
	}

	@Test
	void tomarUnTicketYaTomadoDevuelve409() {
		Ticket ticket = new Ticket(cliente, null, CategoriaTicket.ANCLAJE, "motivo");
		ticket.tomar(admin);
		when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

		assertThatThrownBy(() -> ticketService.tomar(99L, 1L))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("ya fue tomado");
	}

	@Test
	void resolverPorOtroAdminDistintoAlQueLoTomoDevuelve403() {
		Ticket ticket = new Ticket(cliente, null, CategoriaTicket.ANCLAJE, "motivo");
		ticket.tomar(admin);
		when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

		assertThatThrownBy(() -> ticketService.resolver(123L, 1L))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("otro admin");
	}

	@Test
	void resolverSinHaberTomadoElTicketDevuelve409() {
		Ticket ticket = new Ticket(cliente, null, CategoriaTicket.ANCLAJE, "motivo");
		when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

		assertThatThrownBy(() -> ticketService.resolver(99L, 1L))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("primero hay que tomar");
	}

	@Test
	void resolverPorElAdminAsignadoLoCierra() {
		Ticket ticket = new Ticket(cliente, null, CategoriaTicket.ANCLAJE, "motivo");
		ticket.tomar(admin);
		when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

		TicketResponse response = ticketService.resolver(99L, 1L);

		assertThat(response.estado()).isEqualTo(EstadoTicket.RESUELTO);
		assertThat(response.fechaCierre()).isNotNull();
	}

	@Test
	void listarAsignadosDevuelveLosTicketsQueTomoEseAdmin() {
		Ticket ticket = new Ticket(cliente, null, CategoriaTicket.ANCLAJE, "motivo");
		ticket.tomar(admin);
		when(ticketRepository.findByAdminIdOrderByFechaCreacionDesc(99L)).thenReturn(List.of(ticket));

		assertThat(ticketService.listarAsignados(99L, null)).singleElement()
				.satisfies(t -> assertThat(t.adminId()).isEqualTo(99L));
	}

	@Test
	void listarAbiertosFiltraPorCategoria() {
		Ticket anclaje = new Ticket(cliente, null, CategoriaTicket.ANCLAJE, "no traba");
		Ticket pago = new Ticket(cliente, null, CategoriaTicket.PAGO, "me cobraron dos veces");
		when(ticketRepository.findByEstadoOrderByFechaCreacionAsc(EstadoTicket.ABIERTO)).thenReturn(List.of(anclaje, pago));

		assertThat(ticketService.listarAbiertos(CategoriaTicket.PAGO))
				.extracting(TicketResponse::motivo).containsExactly("me cobraron dos veces");
		assertThat(ticketService.listarAbiertos(null)).hasSize(2);
	}

	@Test
	void listarPropiosMapeaLosTicketsDelUsuario() {
		Ticket ticket = new Ticket(cliente, null, CategoriaTicket.ANCLAJE, "motivo");
		ReflectionTestUtils.setField(ticket, "id", 1L);
		when(ticketRepository.findByUsuarioIdOrderByFechaCreacionDesc(10L)).thenReturn(List.of(ticket));

		List<TicketResponse> response = ticketService.listarPropios(10L, null);

		assertThat(response).hasSize(1);
		assertThat(response.get(0).id()).isEqualTo(1L);
	}
}
