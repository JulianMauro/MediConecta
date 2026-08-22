package com.mediconecta.modulos.strike.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediconecta.modulos.notificacion.entity.TipoNotificacion;
import com.mediconecta.modulos.notificacion.service.NotificacionService;
import com.mediconecta.modulos.pago.entity.ConceptoPago;
import com.mediconecta.modulos.pago.entity.Pago;
import com.mediconecta.modulos.pago.service.PagoService;
import com.mediconecta.modulos.strike.dto.StrikeResponse;
import com.mediconecta.modulos.strike.entity.EstadoStrike;
import com.mediconecta.modulos.strike.entity.Strike;
import com.mediconecta.modulos.strike.repository.StrikeRepository;
import com.mediconecta.modulos.usuario.entity.BloqueoCuenta;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.BloqueoCuentaRepository;
import com.mediconecta.modulos.viaje.entity.Viaje;


/**
 * Genera el strike y todo lo que dispara: el cobro de tiempo extra, el aviso al usuario,
 * y si corresponde el bloqueo de la cuenta. No prescribe con el tiempo: queda ACTIVO hasta
 * que PagoService.confirmar salda el pago de tiempo extra asociado.
 */
@Service
public class StrikeService {

	private static final int STRIKES_PARA_BLOQUEAR = 3;

	private final StrikeRepository strikeRepository;
	private final PagoService pagoService;
	private final BloqueoCuentaRepository bloqueoCuentaRepository;
	private final NotificacionService notificacionService;

	public StrikeService(StrikeRepository strikeRepository, PagoService pagoService,
			BloqueoCuentaRepository bloqueoCuentaRepository, NotificacionService notificacionService) {
		this.strikeRepository = strikeRepository;
		this.pagoService = pagoService;
		this.bloqueoCuentaRepository = bloqueoCuentaRepository;
		this.notificacionService = notificacionService;
	}

	@Transactional
	public Strike generarPorExceso(Usuario usuario, Viaje viaje) {
		Strike strike = strikeRepository.save(new Strike(usuario, viaje));

		long minutosExcedidos = viaje.duracionMinutos() - viaje.getMembresia().getTiempoPermitidoMinutos();
		BigDecimal monto = viaje.getMembresia().getTarifaMinutoExtra().multiply(BigDecimal.valueOf(minutosExcedidos));

		Pago pago = pagoService.crear(usuario, viaje, null, ConceptoPago.TIEMPO_EXTRA, monto);
		strike.asociarPago(pago);

		notificacionService.crear(usuario, viaje, TipoNotificacion.TIEMPO_EXTRA,
				"Tu viaje excedio el tiempo permitido. Pagá el tiempo extra para evitar que el strike quede activo.");

		evaluarBloqueo(usuario);
		return strike;
	}

	private void evaluarBloqueo(Usuario usuario) {
		long activos = strikeRepository.findByUsuarioIdAndEstado(usuario.getId(), EstadoStrike.ACTIVO).size();
		boolean yaBloqueado = bloqueoCuentaRepository.findByUsuarioIdAndActivoTrue(usuario.getId()).isPresent();

		if (activos >= STRIKES_PARA_BLOQUEAR && !yaBloqueado) {
			bloqueoCuentaRepository.save(new BloqueoCuenta(usuario, "acumulo " + STRIKES_PARA_BLOQUEAR + " strikes activos"));
			notificacionService.crear(usuario, null, TipoNotificacion.BLOQUEO,
					"Tu cuenta fue bloqueada por acumular " + STRIKES_PARA_BLOQUEAR + " strikes sin saldar.");
		}
	}

	@Transactional(readOnly = true)
	public List<StrikeResponse> listarPropios(Long usuarioId) {
		return strikeRepository.findByUsuarioId(usuarioId).stream()
				.map(StrikeResponse::desde)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<StrikeResponse> listarDeUsuario(Long usuarioId) {
		return listarPropios(usuarioId);
	}
}
