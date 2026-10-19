package com.mediconecta.modulos.viaje.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.almacen.entity.Almacen;
import com.mediconecta.modulos.estacion.entity.Anclaje;
import com.mediconecta.modulos.estacion.entity.EstadoDock;
import com.mediconecta.modulos.estacion.repository.AnclajeRepository;
import com.mediconecta.modulos.flota.entity.Bicicleta;
import com.mediconecta.modulos.flota.entity.EstadoBicicleta;
import com.mediconecta.modulos.flota.repository.BicicletaRepository;
import com.mediconecta.modulos.membresia.entity.EstadoSuscripcion;
import com.mediconecta.modulos.membresia.entity.Membresia;
import com.mediconecta.modulos.membresia.entity.SuscripcionUsuario;
import com.mediconecta.modulos.membresia.repository.SuscripcionUsuarioRepository;
import com.mediconecta.modulos.monitoreo.service.UsuariosActivosService;
import com.mediconecta.modulos.strike.service.StrikeService;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.BloqueoCuentaRepository;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;
import com.mediconecta.modulos.viaje.dto.ViajeFinalizarRequest;
import com.mediconecta.modulos.viaje.dto.ViajeIniciarRequest;
import com.mediconecta.modulos.viaje.dto.ViajeResponse;
import com.mediconecta.modulos.viaje.entity.Viaje;
import com.mediconecta.modulos.viaje.repository.ViajeRepository;


/**
 * Orquesta el ciclo de vida del viaje: es el unico punto que mueve el estado de
 * Bicicleta y Anclaje, y el que dispara strike/pago/notificacion al finalizar.
 *
 * Regla central: no hay tarifa automatica. Sin un plan vigente (contratado y pagado
 * de antemano) el usuario no puede sacar una bici — tiene que contratar uno primero.
 */
@Service
public class ViajeService {

	private final ViajeRepository viajeRepository;
	private final UsuarioRepository usuarioRepository;
	private final BicicletaRepository bicicletaRepository;
	private final AnclajeRepository anclajeRepository;
	private final SuscripcionUsuarioRepository suscripcionUsuarioRepository;
	private final BloqueoCuentaRepository bloqueoCuentaRepository;
	private final StrikeService strikeService;
	private final UsuariosActivosService usuariosActivosService;

	public ViajeService(ViajeRepository viajeRepository, UsuarioRepository usuarioRepository,
			BicicletaRepository bicicletaRepository, AnclajeRepository anclajeRepository,
			SuscripcionUsuarioRepository suscripcionUsuarioRepository,
			BloqueoCuentaRepository bloqueoCuentaRepository, StrikeService strikeService,
			UsuariosActivosService usuariosActivosService) {
		this.viajeRepository = viajeRepository;
		this.usuarioRepository = usuarioRepository;
		this.bicicletaRepository = bicicletaRepository;
		this.anclajeRepository = anclajeRepository;
		this.suscripcionUsuarioRepository = suscripcionUsuarioRepository;
		this.bloqueoCuentaRepository = bloqueoCuentaRepository;
		this.strikeService = strikeService;
		this.usuariosActivosService = usuariosActivosService;
	}

	@Transactional
	public ViajeResponse iniciar(Long usuarioId, ViajeIniciarRequest request) {
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new IllegalStateException("usuario autenticado no encontrado"));

		if (bloqueoCuentaRepository.findByUsuarioIdAndActivoTrue(usuarioId).isPresent()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "la cuenta esta bloqueada");
		}
		if (viajeRepository.findByUsuarioIdAndFechaFinIsNull(usuarioId).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "ya tenes un viaje en curso");
		}

		SuscripcionUsuario suscripcion = planVigente(usuarioId);
		validarLimiteDiario(suscripcion);
		validarTiempoEspera(suscripcion);

		Bicicleta bicicleta = bicicletaRepository.findByCodigo(request.codigoBicicleta())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "bicicleta no encontrada"));
		if (bicicleta.getEstado() != EstadoBicicleta.DISPONIBLE || bicicleta.getAnclaje() == null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "la bicicleta no esta disponible");
		}

		Anclaje anclajeOrigen = bicicleta.getAnclaje();

		Viaje viaje = viajeRepository.save(new Viaje(usuario, bicicleta, anclajeOrigen, suscripcion));
		bicicleta.retirarDeAnclaje();
		anclajeOrigen.liberar();
		usuariosActivosService.incrementar();

		return ViajeResponse.desde(viaje);
	}

	@Transactional
	public ViajeResponse finalizar(Long usuarioId, ViajeFinalizarRequest request) {
		Viaje viaje = viajeRepository.findEnCursoParaCerrar(usuarioId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "no tenes un viaje en curso"));
		cerrarEnAnclaje(viaje, request.anclajeDestinoId(), Instant.now(), false);
		return ViajeResponse.desde(viaje);
	}

	/** Para soporte: el viaje en curso del usuario, con la fila bloqueada hasta el fin de la transaccion. */
	@Transactional
	public Viaje enCursoParaCerrar(Long usuarioId) {
		return viajeRepository.findEnCursoParaCerrar(usuarioId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "el usuario no tiene un viaje en curso"));
	}

	/** Cierre con la bici en un anclaje libre. Lo usan el cliente (fin = ahora) y soporte. */
	@Transactional
	public void cerrarEnAnclaje(Viaje viaje, Long anclajeDestinoId, Instant fin, boolean condonarExceso) {
		Anclaje anclajeDestino = anclajeRepository.findParaOcupar(anclajeDestinoId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "anclaje de destino no encontrado"));
		if (anclajeDestino.getEstado() != EstadoDock.LIBRE) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "el anclaje de destino no esta libre");
		}

		cerrar(viaje, anclajeDestino, fin, condonarExceso);
		viaje.getBicicleta().anclarEn(anclajeDestino);
		anclajeDestino.ocupar();
	}

	/**
	 * Cierre sin anclaje: la bici se rompio o se perdio en el camino. Solo lo usa soporte.
	 * Va a un almacen para respetar que una bici fuera de viaje siempre esta en un anclaje o
	 * en un almacen (ALCANCE, regla 6): logistica la retira de la calle.
	 */
	@Transactional
	public void cerrarEnAlmacen(Viaje viaje, Almacen almacen, EstadoBicicleta estadoBici, Instant fin,
			boolean condonarExceso) {
		if (estadoBici != EstadoBicicleta.EN_REPARACION && estadoBici != EstadoBicicleta.DESACTIVADA) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"una bici que no vuelve a un anclaje solo puede quedar EN_REPARACION o DESACTIVADA");
		}

		cerrar(viaje, null, fin, condonarExceso);
		viaje.getBicicleta().moverAAlmacen(almacen, estadoBici);
	}

	/** Lo comun a todo cierre: el viaje, el contador en vivo y el strike si se paso del tiempo. */
	private void cerrar(Viaje viaje, Anclaje anclajeDestino, Instant fin, boolean condonarExceso) {
		if (fin.isAfter(Instant.now())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "la hora de fin no puede ser futura");
		}
		if (fin.isBefore(viaje.getFechaInicio())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "la hora de fin no puede ser anterior al inicio del viaje");
		}

		viaje.cerrar(anclajeDestino, fin, condonarExceso);
		usuariosActivosService.decrementar();

		// El plan ya se pago al contratarlo: lo unico que se cobra aparte es el tiempo excedido.
		if (viaje.isExcedioTiempo() && !viaje.isExcesoCondonado()) {
			strikeService.generarPorExceso(viaje.getUsuario(), viaje);
		}
	}

	@Transactional(readOnly = true)
	public ViajeResponse obtenerActual(Long usuarioId) {
		Viaje viaje = viajeRepository.findByUsuarioIdAndFechaFinIsNull(usuarioId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no tenes un viaje en curso"));
		return ViajeResponse.desde(viaje);
	}

	@Transactional(readOnly = true)
	public List<ViajeResponse> listarPropios(Long usuarioId) {
		return viajeRepository.findByUsuarioId(usuarioId).stream()
				.map(ViajeResponse::desde)
				.toList();
	}

	/** Sin plan contratado y vigente, no se puede sacar una bici: no hay tarifa automatica de respaldo. */
	private SuscripcionUsuario planVigente(Long usuarioId) {
		return suscripcionUsuarioRepository
				.findByUsuarioIdAndEstadoAndFechaFinAfter(usuarioId, EstadoSuscripcion.ACTIVA, Instant.now())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
						"no tenes un plan activo: contrata uno desde \"Mi cuenta\" antes de sacar una bici"));
	}

	/** Escanea por suscripcion, no por usuario: un plan nuevo no hereda los viajes de uno anterior. */
	private void validarLimiteDiario(SuscripcionUsuario suscripcion) {
		Membresia membresia = suscripcion.getMembresia();
		Instant inicioDeHoy = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant();
		long usadosHoy = viajeRepository.countBySuscripcionIdAndFechaInicioAfter(suscripcion.getId(), inicioDeHoy);
		if (usadosHoy >= membresia.getViajesPorDia()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"ya usaste los " + membresia.getViajesPorDia() + " viajes de hoy que incluye tu plan");
		}
	}

	/** Idem: la espera se mide contra el ultimo viaje de esta misma suscripcion, no contra cualquier viaje del usuario. */
	private void validarTiempoEspera(SuscripcionUsuario suscripcion) {
		Membresia membresia = suscripcion.getMembresia();
		if (membresia.getTiempoEsperaMinutos() <= 0) {
			return;
		}
		viajeRepository.findFirstBySuscripcionIdAndFechaFinIsNotNullOrderByFechaFinDesc(suscripcion.getId())
				.ifPresent(ultimo -> {
					long minutosDesdeElUltimo = Duration.between(ultimo.getFechaFin(), Instant.now()).toMinutes();
					long faltan = membresia.getTiempoEsperaMinutos() - minutosDesdeElUltimo;
					if (faltan > 0) {
						throw new ResponseStatusException(HttpStatus.CONFLICT,
								"tenes que esperar " + faltan + " minuto(s) mas desde tu ultimo viaje para sacar otra bici");
					}
				});
	}
}
