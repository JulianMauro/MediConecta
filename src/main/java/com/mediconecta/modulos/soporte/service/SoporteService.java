package com.mediconecta.modulos.soporte.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.almacen.entity.Almacen;
import com.mediconecta.modulos.almacen.repository.AlmacenRepository;
import com.mediconecta.modulos.chatP2P.dto.ChatMensajeRequest;
import com.mediconecta.modulos.chatP2P.service.ChatService;
import com.mediconecta.modulos.estacion.entity.Anclaje;
import com.mediconecta.modulos.estacion.repository.AnclajeRepository;
import com.mediconecta.modulos.estacion.service.AnclajeService;
import com.mediconecta.modulos.membresia.entity.EstadoSuscripcion;
import com.mediconecta.modulos.membresia.repository.SuscripcionUsuarioRepository;
import com.mediconecta.modulos.notificacion.entity.TipoNotificacion;
import com.mediconecta.modulos.notificacion.service.NotificacionService;
import com.mediconecta.modulos.pago.dto.PagoResponse;
import com.mediconecta.modulos.pago.entity.ConceptoPago;
import com.mediconecta.modulos.pago.entity.EstadoPago;
import com.mediconecta.modulos.pago.entity.Pago;
import com.mediconecta.modulos.pago.repository.PagoRepository;
import com.mediconecta.modulos.pago.service.PagoService;
import com.mediconecta.modulos.soporte.dto.AccionSoporteResponse;
import com.mediconecta.modulos.soporte.dto.CerrarViajeRequest;
import com.mediconecta.modulos.soporte.dto.ConfirmarPagoSoporteRequest;
import com.mediconecta.modulos.soporte.dto.CorregirViajeRequest;
import com.mediconecta.modulos.soporte.dto.ResultadoSoporteResponse;
import com.mediconecta.modulos.soporte.dto.ResumenUsuarioResponse;
import com.mediconecta.modulos.soporte.dto.StrikeSoporteResponse;
import com.mediconecta.modulos.soporte.dto.ViajeSoporteResponse;
import com.mediconecta.modulos.soporte.entity.AccionSoporte;
import com.mediconecta.modulos.soporte.entity.TipoAccionSoporte;
import com.mediconecta.modulos.soporte.repository.AccionSoporteRepository;
import com.mediconecta.modulos.strike.entity.EstadoStrike;
import com.mediconecta.modulos.strike.entity.Strike;
import com.mediconecta.modulos.strike.repository.StrikeRepository;
import com.mediconecta.modulos.strike.service.StrikeService;
import com.mediconecta.modulos.tickets.entity.EstadoTicket;
import com.mediconecta.modulos.tickets.entity.Ticket;
import com.mediconecta.modulos.tickets.repository.TicketRepository;
import com.mediconecta.modulos.usuario.dto.BloqueoCuentaResponse;
import com.mediconecta.modulos.usuario.dto.UsuarioResponse;
import com.mediconecta.modulos.usuario.entity.BloqueoCuenta;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.BloqueoCuentaRepository;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;
import com.mediconecta.modulos.viaje.entity.Viaje;
import com.mediconecta.modulos.viaje.repository.ViajeRepository;
import com.mediconecta.modulos.viaje.service.ViajeService;

/**
 * Lo que un admin puede resolverle a un usuario desde el chat de un ticket. Es un
 * orquestador, como ViajeService: no tiene reglas propias sobre viajes, strikes o pagos,
 * reusa las de cada modulo y agrega tres cosas que valen para toda accion:
 *
 * 1. Solo el admin que tomo el ticket, y solo mientras esta EN_CHAT, opera sobre el dueño
 *    del ticket. No hay pagina de usuarios: el ticket es la unica puerta de entrada.
 * 2. Cada accion queda registrada en AccionSoporte, con el motivo que da el admin.
 * 3. El usuario se entera: mensaje en el chat del ticket y notificacion.
 */
@Service
public class SoporteService {

	private final TicketRepository ticketRepository;
	private final UsuarioRepository usuarioRepository;
	private final ViajeRepository viajeRepository;
	private final ViajeService viajeService;
	private final AlmacenRepository almacenRepository;
	private final AnclajeRepository anclajeRepository;
	private final AnclajeService anclajeService;
	private final StrikeRepository strikeRepository;
	private final PagoRepository pagoRepository;
	private final PagoService pagoService;
	private final SuscripcionUsuarioRepository suscripcionUsuarioRepository;
	private final BloqueoCuentaRepository bloqueoCuentaRepository;
	private final ChatService chatService;
	private final NotificacionService notificacionService;
	private final AccionSoporteRepository accionSoporteRepository;

	public SoporteService(TicketRepository ticketRepository, UsuarioRepository usuarioRepository,
			ViajeRepository viajeRepository, ViajeService viajeService, AlmacenRepository almacenRepository,
			AnclajeRepository anclajeRepository, AnclajeService anclajeService, StrikeRepository strikeRepository,
			PagoRepository pagoRepository, PagoService pagoService,
			SuscripcionUsuarioRepository suscripcionUsuarioRepository,
			BloqueoCuentaRepository bloqueoCuentaRepository, ChatService chatService,
			NotificacionService notificacionService, AccionSoporteRepository accionSoporteRepository) {
		this.ticketRepository = ticketRepository;
		this.usuarioRepository = usuarioRepository;
		this.viajeRepository = viajeRepository;
		this.viajeService = viajeService;
		this.almacenRepository = almacenRepository;
		this.anclajeRepository = anclajeRepository;
		this.anclajeService = anclajeService;
		this.strikeRepository = strikeRepository;
		this.pagoRepository = pagoRepository;
		this.pagoService = pagoService;
		this.suscripcionUsuarioRepository = suscripcionUsuarioRepository;
		this.bloqueoCuentaRepository = bloqueoCuentaRepository;
		this.chatService = chatService;
		this.notificacionService = notificacionService;
		this.accionSoporteRepository = accionSoporteRepository;
	}

	/** Se puede consultar tambien con el ticket ya resuelto, para ver que se hizo. */
	@Transactional(readOnly = true)
	public ResumenUsuarioResponse resumen(Long adminId, Long ticketId) {
		Ticket ticket = obtenerTicket(ticketId);
		validarAdminAsignado(ticket, adminId);
		return armarResumen(ticket);
	}

	@Transactional
	public ResultadoSoporteResponse cerrarViaje(Long adminId, Long ticketId, CerrarViajeRequest request) {
		Ticket ticket = ticketEnAtencion(adminId, ticketId);
		boolean enAnclaje = request.anclajeDestinoId() != null;
		if (enAnclaje == (request.almacenId() != null)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"elegi donde queda la bici: un anclaje o un almacen, no ambos ni ninguno");
		}
		if (!enAnclaje && request.estadoBicicleta() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"si la bici va a un almacen, indica si queda EN_REPARACION o DESACTIVADA");
		}
		Instant fin = request.fechaFin() != null ? request.fechaFin() : Instant.now();

		Viaje viaje = viajeService.enCursoParaCerrar(ticket.getUsuario().getId());
		String dondeQuedo;
		if (enAnclaje) {
			viajeService.cerrarEnAnclaje(viaje, request.anclajeDestinoId(), fin, request.condonarExceso());
			dondeQuedo = "la bici " + viaje.getBicicleta().getCodigo() + " quedó en "
					+ describir(viaje.getAnclajeDestino()) + ".";
		} else {
			Almacen almacen = almacenRepository.findById(request.almacenId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "almacen no encontrado"));
			viajeService.cerrarEnAlmacen(viaje, almacen, request.estadoBicicleta(), fin, request.condonarExceso());
			dondeQuedo = "la bici " + viaje.getBicicleta().getCodigo() + " la retira logística, no hace falta que la ancles.";
		}

		String detalle = "Cerré tu viaje #" + viaje.getId() + ": " + dondeQuedo + " " + describirTiempo(viaje);
		return registrar(ticket, adminId, TipoAccionSoporte.CERRAR_VIAJE, detalle, request.motivo(), viaje);
	}

	@Transactional
	public ResultadoSoporteResponse corregirViaje(Long adminId, Long ticketId, Long viajeId, CorregirViajeRequest request) {
		Ticket ticket = ticketEnAtencion(adminId, ticketId);
		Viaje viaje = viajeRepository.findParaModificar(viajeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "viaje no encontrado"));
		validarDelUsuario(ticket, viaje.getUsuario(), "el viaje");
		if (viaje.getFechaFin() == null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "el viaje sigue en curso: usa \"cerrar viaje\"");
		}
		if (request.fechaFin().isBefore(viaje.getFechaInicio()) || !request.fechaFin().isBefore(viaje.getFechaFin())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"la nueva hora de fin tiene que estar entre el inicio del viaje y su hora de fin actual");
		}

		viaje.corregirFin(request.fechaFin());
		StringBuilder detalle = new StringBuilder("Corregí tu viaje #" + viaje.getId() + ": ahora dura "
				+ viaje.duracionMinutos() + " min (tu plan incluye " + viaje.getMembresia().getTiempoPermitidoMinutos() + ").");

		Strike strike = strikeRepository.findByViajeId(viaje.getId()).orElse(null);
		if (strike != null && strike.getEstado() == EstadoStrike.ACTIVO) {
			if (!viaje.isExcedioTiempo()) {
				detalle.append(" Ya no te pasaste del tiempo: anulé el strike y su cobro.");
				if (anularConCobro(strike, adminId)) {
					detalle.append(" Tu cuenta quedó desbloqueada.");
				}
			} else if (strike.getPago() != null && strike.getPago().getEstado() == EstadoPago.PENDIENTE) {
				BigDecimal monto = StrikeService.montoPorExceso(viaje);
				strike.getPago().ajustarMonto(monto);
				detalle.append(" El cobro de tiempo extra bajó a ").append(pesos(monto)).append('.');
			}
		} else if (strike != null && strike.getEstado() == EstadoStrike.SALDADO) {
			detalle.append(" El tiempo extra ya estaba pagado: no hay reintegro.");
		}

		return registrar(ticket, adminId, TipoAccionSoporte.CORREGIR_VIAJE, detalle.toString(), request.motivo(), viaje);
	}

	@Transactional
	public ResultadoSoporteResponse anularStrike(Long adminId, Long ticketId, Long strikeId, String motivo) {
		Ticket ticket = ticketEnAtencion(adminId, ticketId);
		Strike strike = strikeRepository.findParaModificar(strikeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "strike no encontrado"));
		validarDelUsuario(ticket, strike.getUsuario(), "el strike");
		if (strike.getEstado() == EstadoStrike.SALDADO) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"el strike ya esta saldado: el usuario lo pago y no hay reintegros");
		}
		if (strike.getEstado() == EstadoStrike.ANULADO) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "el strike ya estaba anulado");
		}

		StringBuilder detalle = new StringBuilder("Anulé el strike del viaje #" + strike.getViaje().getId());
		if (strike.getPago() != null) {
			detalle.append(" y su cobro de ").append(pesos(strike.getPago().getMonto()));
		}
		detalle.append('.');
		if (anularConCobro(strike, adminId)) {
			detalle.append(" Tu cuenta quedó desbloqueada.");
		}

		return registrar(ticket, adminId, TipoAccionSoporte.ANULAR_STRIKE, detalle.toString(), motivo, strike.getViaje());
	}

	/** Pagar no desbloquea la cuenta: aunque salde un strike, el bloqueo lo levanta un admin a mano. */
	@Transactional
	public ResultadoSoporteResponse confirmarPago(Long adminId, Long ticketId, Long pagoId,
			ConfirmarPagoSoporteRequest request) {
		Ticket ticket = ticketEnAtencion(adminId, ticketId);
		Pago pago = pagoRepository.findById(pagoId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "pago no encontrado"));
		validarDelUsuario(ticket, pago.getUsuario(), "el pago");
		if (pago.getEstado() != EstadoPago.PENDIENTE) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"solo se confirma un pago PENDIENTE, este esta " + pago.getEstado());
		}

		// PagoService.confirmar crea la suscripcion sin mirar si ya hay una: con dos planes ACTIVA
		// a la vez, ViajeService.planVigente ya no puede elegir cual usar y el usuario no viaja.
		if (pago.getConcepto() == ConceptoPago.MEMBRESIA && suscripcionUsuarioRepository
				.findByUsuarioIdAndEstadoAndFechaFinAfter(pago.getUsuario().getId(), EstadoSuscripcion.ACTIVA, Instant.now())
				.isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"el usuario ya tiene un plan vigente: confirmar este pago le dejaria dos planes activos");
		}

		pagoService.confirmar(admin(adminId), pagoId, request.referenciaPasarela());
		String que = switch (pago.getConcepto()) {
			case MEMBRESIA -> " del plan " + pago.getMembresia().getNombre() + ": ya lo tenés activo.";
			case TIEMPO_EXTRA -> " de tiempo extra: el strike quedó saldado.";
		};
		String detalle = "Confirmé tu pago de " + pesos(pago.getMonto()) + que;
		return registrar(ticket, adminId, TipoAccionSoporte.CONFIRMAR_PAGO, detalle, request.motivo(), pago.getViaje());
	}

	@Transactional
	public ResultadoSoporteResponse desbloquear(Long adminId, Long ticketId, String motivo) {
		Ticket ticket = ticketEnAtencion(adminId, ticketId);
		BloqueoCuenta bloqueo = bloqueoCuentaRepository.findByUsuarioIdAndActivoTrue(ticket.getUsuario().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "la cuenta no esta bloqueada"));

		bloqueo.desbloquear(admin(adminId));
		String detalle = "Desbloqueé tu cuenta: ya podés volver a sacar bicis.";
		if (strikesActivos(ticket.getUsuario().getId()) >= StrikeService.STRIKES_PARA_BLOQUEAR) {
			detalle += " Ojo: seguís con " + StrikeService.STRIKES_PARA_BLOQUEAR
					+ " strikes o más sin pagar, el próximo exceso la vuelve a bloquear.";
		}
		return registrar(ticket, adminId, TipoAccionSoporte.DESBLOQUEAR_CUENTA, detalle, motivo, null);
	}

	@Transactional
	public ResultadoSoporteResponse anclajeFueraDeServicio(Long adminId, Long ticketId, Long anclajeId, String motivo) {
		Ticket ticket = ticketEnAtencion(adminId, ticketId);
		anclajeService.marcarFueraServicio(anclajeId);
		Anclaje anclaje = anclajeRepository.findById(anclajeId).orElseThrow();

		String detalle = "Marqué fuera de servicio " + describir(anclaje) + ": nadie más va a poder anclar ahí hasta que lo reparen.";
		return registrar(ticket, adminId, TipoAccionSoporte.ANCLAJE_FUERA_SERVICIO, detalle, motivo, null);
	}

	/**
	 * Anula el strike y su cobro sin pagar; si con eso el usuario baja del umbral de bloqueo,
	 * levanta el bloqueo. Hoy todo bloqueo sale de los strikes (StrikeService.evaluarBloqueo):
	 * si algun dia se bloquea a mano por otro motivo, hay que distinguirlo aca.
	 * Devuelve true si desbloqueo la cuenta.
	 */
	private boolean anularConCobro(Strike strike, Long adminId) {
		strike.anular();
		if (strike.getPago() != null && strike.getPago().getEstado() != EstadoPago.PAGADO) {
			strike.getPago().anular();
		}
		Long usuarioId = strike.getUsuario().getId();
		if (strikesActivos(usuarioId) >= StrikeService.STRIKES_PARA_BLOQUEAR) {
			return false;
		}
		return bloqueoCuentaRepository.findByUsuarioIdAndActivoTrue(usuarioId)
				.map(bloqueo -> {
					bloqueo.desbloquear(admin(adminId));
					return true;
				})
				.orElse(false);
	}

	private ResultadoSoporteResponse registrar(Ticket ticket, Long adminId, TipoAccionSoporte tipo, String detalle,
			String motivo, Viaje viaje) {
		AccionSoporte accion = accionSoporteRepository.save(new AccionSoporte(ticket, admin(adminId), tipo, detalle, motivo));
		// El chat valida de nuevo que el ticket siga EN_CHAT, y el mensaje sale recien con el commit.
		chatService.enviar(adminId, ticket.getId(), new ChatMensajeRequest(detalle + " Motivo: " + motivo));
		notificacionService.crear(ticket.getUsuario(), viaje, TipoNotificacion.SOPORTE, detalle);
		return new ResultadoSoporteResponse(AccionSoporteResponse.desde(accion), armarResumen(ticket));
	}

	private ResumenUsuarioResponse armarResumen(Ticket ticket) {
		Usuario usuario = ticket.getUsuario();
		Long usuarioId = usuario.getId();

		ViajeSoporteResponse enCurso = viajeRepository.findByUsuarioIdAndFechaFinIsNull(usuarioId)
				.map(ViajeSoporteResponse::desde)
				.orElse(null);
		List<ViajeSoporteResponse> ultimos = viajeRepository
				.findTop10ByUsuarioIdAndFechaFinIsNotNullOrderByFechaInicioDesc(usuarioId).stream()
				.map(ViajeSoporteResponse::desde)
				.toList();
		ResumenUsuarioResponse.PlanVigente plan = suscripcionUsuarioRepository
				.findByUsuarioIdAndEstadoAndFechaFinAfter(usuarioId, EstadoSuscripcion.ACTIVA, Instant.now())
				.map(s -> new ResumenUsuarioResponse.PlanVigente(s.getId(), s.getMembresia().getNombre(),
						s.getMembresia().getTiempoPermitidoMinutos(), s.getMembresia().getViajesPorDia(), s.getFechaFin()))
				.orElse(null);
		List<StrikeSoporteResponse> strikes = strikeRepository.findByUsuarioIdOrderByFechaGeneracionDesc(usuarioId).stream()
				.map(StrikeSoporteResponse::desde)
				.toList();
		long activos = strikes.stream().filter(s -> s.estado() == EstadoStrike.ACTIVO).count();
		List<PagoResponse> pendientes = pagoRepository.findByUsuarioIdAndEstado(usuarioId, EstadoPago.PENDIENTE).stream()
				.map(PagoResponse::desde)
				.toList();
		BloqueoCuentaResponse bloqueo = bloqueoCuentaRepository.findByUsuarioIdAndActivoTrue(usuarioId)
				.map(BloqueoCuentaResponse::desde)
				.orElse(null);
		List<AccionSoporteResponse> acciones = accionSoporteRepository.findByTicketIdOrderByFechaAsc(ticket.getId()).stream()
				.map(AccionSoporteResponse::desde)
				.toList();

		return new ResumenUsuarioResponse(
				ticket.getId(),
				ticket.getViaje() != null ? ticket.getViaje().getId() : null,
				UsuarioResponse.desde(usuario),
				enCurso,
				ultimos,
				plan,
				activos,
				StrikeService.STRIKES_PARA_BLOQUEAR,
				strikes,
				pendientes,
				bloqueo,
				acciones);
	}

	private Ticket ticketEnAtencion(Long adminId, Long ticketId) {
		Ticket ticket = obtenerTicket(ticketId);
		validarAdminAsignado(ticket, adminId);
		if (ticket.getEstado() != EstadoTicket.EN_CHAT) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"el ticket ya esta resuelto: para operar sobre el usuario tiene que estar en atencion");
		}
		return ticket;
	}

	private Ticket obtenerTicket(Long ticketId) {
		return ticketRepository.findById(ticketId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ticket no encontrado"));
	}

	private static void validarAdminAsignado(Ticket ticket, Long adminId) {
		if (ticket.getAdmin() == null || !ticket.getAdmin().getId().equals(adminId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"solo el admin que tomo el ticket puede gestionar al usuario");
		}
	}

	/** Lo que se toca tiene que ser del dueño del ticket, no de cualquier usuario. */
	private static void validarDelUsuario(Ticket ticket, Usuario dueño, String que) {
		if (!dueño.getId().equals(ticket.getUsuario().getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, que + " no es del usuario de este ticket");
		}
	}

	private long strikesActivos(Long usuarioId) {
		return strikeRepository.findByUsuarioIdAndEstado(usuarioId, EstadoStrike.ACTIVO).size();
	}

	private Usuario admin(Long adminId) {
		return usuarioRepository.findById(adminId)
				.orElseThrow(() -> new IllegalStateException("usuario autenticado no encontrado"));
	}

	private static String describir(Anclaje anclaje) {
		return anclaje.getEstacion().getNombre() + ", anclaje " + anclaje.getNumero();
	}

	private static String describirTiempo(Viaje viaje) {
		long permitidos = viaje.getMembresia().getTiempoPermitidoMinutos();
		String duracion = "Duró " + viaje.duracionMinutos() + " min (tu plan incluye " + permitidos + ").";
		if (!viaje.isExcedioTiempo()) {
			return duracion;
		}
		long pasado = viaje.duracionMinutos() - permitidos;
		if (viaje.isExcesoCondonado()) {
			return duracion + " Te pasaste " + pasado + " min, pero no se te cobra ni suma strike.";
		}
		return duracion + " Te pasaste " + pasado + " min: se generó un strike y un cobro de "
				+ pesos(StrikeService.montoPorExceso(viaje)) + ".";
	}

	private static String pesos(BigDecimal monto) {
		return "$" + monto.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}
}
