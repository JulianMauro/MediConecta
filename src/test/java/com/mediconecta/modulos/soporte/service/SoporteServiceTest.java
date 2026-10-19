package com.mediconecta.modulos.soporte.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.almacen.repository.AlmacenRepository;
import com.mediconecta.modulos.chatP2P.dto.ChatMensajeRequest;
import com.mediconecta.modulos.chatP2P.service.ChatService;
import com.mediconecta.modulos.estacion.entity.Anclaje;
import com.mediconecta.modulos.estacion.repository.AnclajeRepository;
import com.mediconecta.modulos.estacion.service.AnclajeService;
import com.mediconecta.modulos.membresia.entity.EstadoSuscripcion;
import com.mediconecta.modulos.membresia.entity.SuscripcionUsuario;
import com.mediconecta.modulos.membresia.repository.SuscripcionUsuarioRepository;
import com.mediconecta.modulos.notificacion.entity.TipoNotificacion;
import com.mediconecta.modulos.notificacion.service.NotificacionService;
import com.mediconecta.modulos.pago.entity.ConceptoPago;
import com.mediconecta.modulos.pago.entity.EstadoPago;
import com.mediconecta.modulos.pago.entity.Pago;
import com.mediconecta.modulos.pago.repository.PagoRepository;
import com.mediconecta.modulos.pago.service.PagoService;
import com.mediconecta.modulos.soporte.dto.CerrarViajeRequest;
import com.mediconecta.modulos.soporte.dto.ConfirmarPagoSoporteRequest;
import com.mediconecta.modulos.soporte.dto.CorregirViajeRequest;
import com.mediconecta.modulos.soporte.dto.ResultadoSoporteResponse;
import com.mediconecta.modulos.soporte.entity.AccionSoporte;
import com.mediconecta.modulos.soporte.entity.TipoAccionSoporte;
import com.mediconecta.modulos.soporte.repository.AccionSoporteRepository;
import com.mediconecta.modulos.strike.entity.EstadoStrike;
import com.mediconecta.modulos.strike.entity.Strike;
import com.mediconecta.modulos.strike.repository.StrikeRepository;
import com.mediconecta.modulos.tickets.entity.CategoriaTicket;
import com.mediconecta.modulos.tickets.entity.Ticket;
import com.mediconecta.modulos.tickets.repository.TicketRepository;
import com.mediconecta.modulos.usuario.entity.BloqueoCuenta;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.BloqueoCuentaRepository;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;
import com.mediconecta.modulos.viaje.entity.Viaje;
import com.mediconecta.modulos.viaje.repository.ViajeRepository;
import com.mediconecta.modulos.viaje.service.ViajeService;
import com.mediconecta.testutil.Datos;

@ExtendWith(MockitoExtension.class)
class SoporteServiceTest {

	@Mock
	private TicketRepository ticketRepository;
	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private ViajeRepository viajeRepository;
	@Mock
	private ViajeService viajeService;
	@Mock
	private AlmacenRepository almacenRepository;
	@Mock
	private AnclajeRepository anclajeRepository;
	@Mock
	private AnclajeService anclajeService;
	@Mock
	private StrikeRepository strikeRepository;
	@Mock
	private PagoRepository pagoRepository;
	@Mock
	private PagoService pagoService;
	@Mock
	private SuscripcionUsuarioRepository suscripcionUsuarioRepository;
	@Mock
	private BloqueoCuentaRepository bloqueoCuentaRepository;
	@Mock
	private ChatService chatService;
	@Mock
	private NotificacionService notificacionService;
	@Mock
	private AccionSoporteRepository accionSoporteRepository;

	private SoporteService soporteService;
	private Usuario cliente;
	private Usuario admin;
	private Ticket ticket;

	@BeforeEach
	void setUp() {
		soporteService = new SoporteService(ticketRepository, usuarioRepository, viajeRepository, viajeService,
				almacenRepository, anclajeRepository, anclajeService, strikeRepository, pagoRepository, pagoService,
				suscripcionUsuarioRepository, bloqueoCuentaRepository, chatService, notificacionService,
				accionSoporteRepository);
		cliente = Datos.cliente(10L);
		admin = Datos.admin(99L);
		ticket = Datos.conId(new Ticket(cliente, null, CategoriaTicket.ANCLAJE, "no traba"), 1L);
		ticket.tomar(admin);

		lenient().when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
		lenient().when(usuarioRepository.findById(99L)).thenReturn(Optional.of(admin));
		lenient().when(accionSoporteRepository.save(any(AccionSoporte.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	// ---- quien puede operar ----

	@Test
	void unAdminQueNoTomoElTicketRecibe403() {
		assertThatThrownBy(() -> soporteService.desbloquear(123L, 1L, "motivo"))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("solo el admin que tomo el ticket");
		verify(accionSoporteRepository, never()).save(any());
	}

	@Test
	void conElTicketResueltoNoSePuedeOperar() {
		ticket.resolver();

		assertThatThrownBy(() -> soporteService.desbloquear(99L, 1L, "motivo"))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("ya esta resuelto");
	}

	@Test
	void elResumenSePuedeVerConElTicketResuelto() {
		ticket.resolver();

		assertThat(soporteService.resumen(99L, 1L).usuario().id()).isEqualTo(10L);
	}

	@Test
	void noSePuedeTocarElStrikeDeOtroUsuario() {
		Strike ajeno = strikeActivo(Datos.cliente(77L), 5L);
		when(strikeRepository.findParaModificar(5L)).thenReturn(Optional.of(ajeno));

		assertThatThrownBy(() -> soporteService.anularStrike(99L, 1L, 5L, "motivo"))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("no es del usuario de este ticket");
		assertThat(ajeno.getEstado()).isEqualTo(EstadoStrike.ACTIVO);
	}

	// ---- cerrar viaje ----

	@Test
	void cerrarViajeSinElegirDondeQuedaLaBiciDevuelve400() {
		assertThatThrownBy(() -> soporteService.cerrarViaje(99L, 1L,
				new CerrarViajeRequest(null, null, null, null, false, "motivo")))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("un anclaje o un almacen");
		assertThatThrownBy(() -> soporteService.cerrarViaje(99L, 1L,
				new CerrarViajeRequest(5L, 3L, null, null, false, "motivo")))
				.isInstanceOf(ResponseStatusException.class);
		verify(viajeService, never()).enCursoParaCerrar(anyLong());
	}

	@Test
	void cerrarViajeEnAnclajeRegistraLaAccionYAvisaAlUsuario() {
		Viaje viaje = Datos.viajeEnCurso(7L, cliente, 45);
		Anclaje anclaje = Datos.anclajeLibre(5L);
		when(viajeService.enCursoParaCerrar(10L)).thenReturn(viaje);
		doAnswer(inv -> {
			viaje.cerrar(anclaje, inv.getArgument(2), inv.getArgument(3));
			return null;
		}).when(viajeService).cerrarEnAnclaje(eq(viaje), eq(5L), any(Instant.class), eq(true));

		ResultadoSoporteResponse resultado = soporteService.cerrarViaje(99L, 1L,
				new CerrarViajeRequest(5L, null, null, null, true, "el anclaje no trababa"));

		assertThat(resultado.accion().tipo()).isEqualTo(TipoAccionSoporte.CERRAR_VIAJE);
		assertThat(resultado.accion().detalle())
				.contains("Cerré tu viaje #7", "Plaza de Mayo, anclaje 5", "no se te cobra");
		verify(chatService).enviar(eq(99L), eq(1L),
				argThat((ChatMensajeRequest m) -> m.contenido().contains("Motivo: el anclaje no trababa")));
		verify(notificacionService).crear(eq(cliente), eq(viaje), eq(TipoNotificacion.SOPORTE), any());
	}

	// ---- anular strike y desbloqueo automatico ----

	@Test
	void anularUnStrikeAnulaSuCobroYDesbloqueaSiBajaDeTres() {
		Strike strike = strikeActivo(cliente, 5L);
		BloqueoCuenta bloqueo = new BloqueoCuenta(cliente, "acumulo 3 strikes activos");
		when(strikeRepository.findParaModificar(5L)).thenReturn(Optional.of(strike));
		when(strikeRepository.findByUsuarioIdAndEstado(10L, EstadoStrike.ACTIVO))
				.thenReturn(List.of(strikeActivo(cliente, 6L), strikeActivo(cliente, 8L)));
		when(bloqueoCuentaRepository.findByUsuarioIdAndActivoTrue(10L)).thenReturn(Optional.of(bloqueo));

		ResultadoSoporteResponse resultado = soporteService.anularStrike(99L, 1L, 5L, "fallo del anclaje");

		assertThat(strike.getEstado()).isEqualTo(EstadoStrike.ANULADO);
		assertThat(strike.getPago().getEstado()).isEqualTo(EstadoPago.ANULADO);
		assertThat(bloqueo.isActivo()).isFalse();
		assertThat(bloqueo.getAdminDesbloqueo()).isSameAs(admin);
		assertThat(resultado.accion().detalle()).contains("$150", "desbloqueada");
	}

	@Test
	void siSigueConTresStrikesActivosLaCuentaQuedaBloqueada() {
		Strike strike = strikeActivo(cliente, 5L);
		BloqueoCuenta bloqueo = new BloqueoCuenta(cliente, "acumulo 3 strikes activos");
		when(strikeRepository.findParaModificar(5L)).thenReturn(Optional.of(strike));
		when(strikeRepository.findByUsuarioIdAndEstado(10L, EstadoStrike.ACTIVO)).thenReturn(
				List.of(strikeActivo(cliente, 6L), strikeActivo(cliente, 8L), strikeActivo(cliente, 9L)));

		ResultadoSoporteResponse resultado = soporteService.anularStrike(99L, 1L, 5L, "motivo");

		assertThat(strike.getEstado()).isEqualTo(EstadoStrike.ANULADO);
		assertThat(bloqueo.isActivo()).isTrue();
		assertThat(resultado.accion().detalle()).doesNotContain("desbloqueada");
	}

	@Test
	void unStrikeSaldadoNoSeAnulaPorqueNoHayReintegros() {
		Strike strike = strikeActivo(cliente, 5L);
		strike.saldar();
		when(strikeRepository.findParaModificar(5L)).thenReturn(Optional.of(strike));

		assertThatThrownBy(() -> soporteService.anularStrike(99L, 1L, 5L, "motivo"))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("no hay reintegros");
		verify(chatService, never()).enviar(anyLong(), anyLong(), any());
	}

	// ---- corregir viaje ----

	@Test
	void acortarUnViajeHastaNoPasarseAnulaElStrike() {
		Viaje viaje = viajeCerrado(45);
		Strike strike = strikeDe(viaje);
		when(viajeRepository.findParaModificar(7L)).thenReturn(Optional.of(viaje));
		when(strikeRepository.findByViajeId(7L)).thenReturn(Optional.of(strike));

		ResultadoSoporteResponse resultado = soporteService.corregirViaje(99L, 1L, 7L,
				new CorregirViajeRequest(viaje.getFechaInicio().plus(Duration.ofMinutes(20)), "llego antes"));

		assertThat(viaje.duracionMinutos()).isEqualTo(20);
		assertThat(viaje.isExcedioTiempo()).isFalse();
		assertThat(strike.getEstado()).isEqualTo(EstadoStrike.ANULADO);
		assertThat(strike.getPago().getEstado()).isEqualTo(EstadoPago.ANULADO);
		assertThat(resultado.accion().detalle()).contains("ahora dura 20 min", "anulé el strike");
	}

	@Test
	void acortarUnViajeQueSigueExcedidoBajaElCobroPendiente() {
		Viaje viaje = viajeCerrado(45);
		Strike strike = strikeDe(viaje);
		when(viajeRepository.findParaModificar(7L)).thenReturn(Optional.of(viaje));
		when(strikeRepository.findByViajeId(7L)).thenReturn(Optional.of(strike));

		soporteService.corregirViaje(99L, 1L, 7L,
				new CorregirViajeRequest(viaje.getFechaInicio().plus(Duration.ofMinutes(40)), "llego antes"));

		// 10 minutos de exceso a 10 por minuto.
		assertThat(strike.getEstado()).isEqualTo(EstadoStrike.ACTIVO);
		assertThat(strike.getPago().getMonto()).isEqualByComparingTo("100");
	}

	@Test
	void corregirNoPuedeAlargarElViaje() {
		Viaje viaje = viajeCerrado(20);
		when(viajeRepository.findParaModificar(7L)).thenReturn(Optional.of(viaje));

		assertThatThrownBy(() -> soporteService.corregirViaje(99L, 1L, 7L,
				new CorregirViajeRequest(viaje.getFechaFin().plus(Duration.ofMinutes(5)), "motivo")))
				.isInstanceOf(ResponseStatusException.class);
		assertThat(viaje.duracionMinutos()).isEqualTo(20);
	}

	// ---- pagos y bloqueo ----

	@Test
	void noSeConfirmaUnPagoAnulado() {
		Pago pago = Datos.conId(new Pago(cliente, null, null, ConceptoPago.TIEMPO_EXTRA, BigDecimal.TEN), 3L);
		pago.anular();
		when(pagoRepository.findById(3L)).thenReturn(Optional.of(pago));

		assertThatThrownBy(() -> soporteService.confirmarPago(99L, 1L, 3L, new ConfirmarPagoSoporteRequest("REF", "motivo")))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("PENDIENTE");
		verify(pagoService, never()).confirmar(any(), anyLong(), any());
	}

	@Test
	void noSeConfirmaUnPlanSiElUsuarioYaTieneUnoVigente() {
		Pago pago = Datos.conId(new Pago(cliente, null, Datos.membresia(), ConceptoPago.MEMBRESIA, BigDecimal.TEN), 3L);
		when(pagoRepository.findById(3L)).thenReturn(Optional.of(pago));
		when(suscripcionUsuarioRepository.findByUsuarioIdAndEstadoAndFechaFinAfter(eq(10L), eq(EstadoSuscripcion.ACTIVA),
				any())).thenReturn(Optional.of(new SuscripcionUsuario(cliente, Datos.membresia(), Instant.now(),
						Instant.now().plus(Duration.ofDays(1)))));

		assertThatThrownBy(() -> soporteService.confirmarPago(99L, 1L, 3L, new ConfirmarPagoSoporteRequest("REF", "motivo")))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("dos planes");
		verify(pagoService, never()).confirmar(any(), anyLong(), any());
	}

	@Test
	void confirmarUnPagoPendienteDelegaEnPagoService() {
		Pago pago = Datos.conId(new Pago(cliente, null, null, ConceptoPago.TIEMPO_EXTRA, new BigDecimal("150")), 3L);
		when(pagoRepository.findById(3L)).thenReturn(Optional.of(pago));

		ResultadoSoporteResponse resultado = soporteService.confirmarPago(99L, 1L, 3L,
				new ConfirmarPagoSoporteRequest("MP-123", "mostro el comprobante"));

		verify(pagoService).confirmar(admin, 3L, "MP-123");
		assertThat(resultado.accion().detalle()).contains("$150", "saldado");
	}

	@Test
	void desbloquearSinBloqueoActivoDevuelve409() {
		assertThatThrownBy(() -> soporteService.desbloquear(99L, 1L, "motivo"))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("no esta bloqueada");
	}

	@Test
	void desbloquearConTresStrikesActivosAvisaQueSeVuelveABloquear() {
		BloqueoCuenta bloqueo = new BloqueoCuenta(cliente, "acumulo 3 strikes activos");
		when(bloqueoCuentaRepository.findByUsuarioIdAndActivoTrue(10L)).thenReturn(Optional.of(bloqueo));
		when(strikeRepository.findByUsuarioIdAndEstado(10L, EstadoStrike.ACTIVO)).thenReturn(
				Collections.nCopies(3, strikeActivo(cliente, 6L)));

		ResultadoSoporteResponse resultado = soporteService.desbloquear(99L, 1L, "pago todo en efectivo");

		assertThat(bloqueo.isActivo()).isFalse();
		assertThat(resultado.accion().detalle()).contains("la vuelve a bloquear");
	}

	// ---- helpers ----

	/** Strike ACTIVO con su cobro PENDIENTE de 150, sobre un viaje propio del usuario. */
	private static Strike strikeActivo(Usuario usuario, long id) {
		Viaje viaje = Datos.viajeEnCurso(id, usuario, 45);
		Strike strike = Datos.conId(new Strike(usuario, viaje), id);
		strike.asociarPago(Datos.conId(
				new Pago(usuario, viaje, null, ConceptoPago.TIEMPO_EXTRA, new BigDecimal("150")), id));
		return strike;
	}

	private Viaje viajeCerrado(long minutos) {
		Viaje viaje = Datos.viajeEnCurso(7L, cliente, minutos);
		viaje.cerrar(Datos.anclajeLibre(5L), viaje.getFechaInicio().plus(Duration.ofMinutes(minutos)), false);
		return viaje;
	}

	private Strike strikeDe(Viaje viaje) {
		Strike strike = Datos.conId(new Strike(cliente, viaje), 5L);
		strike.asociarPago(Datos.conId(new Pago(cliente, viaje, null, ConceptoPago.TIEMPO_EXTRA, new BigDecimal("150")), 5L));
		return strike;
	}
}
