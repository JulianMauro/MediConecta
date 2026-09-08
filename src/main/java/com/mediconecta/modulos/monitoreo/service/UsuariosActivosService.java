package com.mediconecta.modulos.monitoreo.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediconecta.modulos.monitoreo.dto.UsuariosActivosResponse;
import com.mediconecta.modulos.monitoreo.entity.RegistroUsuariosActivos;
import com.mediconecta.modulos.monitoreo.repository.RegistroUsuariosActivosRepository;
import com.mediconecta.modulos.viaje.repository.ViajeRepository;

import jakarta.annotation.PostConstruct;


/**
 * Cuenta en vivo cuantos usuarios estan en viaje ahora mismo: ViajeService suma uno al
 * iniciar un viaje y resta uno al finalizarlo. El contador vive en memoria (no en la base)
 * porque es un valor instantaneo, no un dato de negocio que haya que auditar viaje por viaje
 * — para eso ya esta la entidad Viaje.
 *
 * Cada una hora, guardarSnapshot() (@Scheduled) persiste una foto de ese contador en
 * RegistroUsuariosActivos, que es el historial que se puede graficar despues.
 */
@Service
public class UsuariosActivosService {

	private final AtomicInteger contador = new AtomicInteger(0);

	private final RegistroUsuariosActivosRepository registroRepository;
	private final ViajeRepository viajeRepository;

	public UsuariosActivosService(RegistroUsuariosActivosRepository registroRepository,
			ViajeRepository viajeRepository) {
		this.registroRepository = registroRepository;
		this.viajeRepository = viajeRepository;
	}

	/**
	 * El contador arranca en 0 en memoria, pero si la app se reinicia con viajes ya en
	 * curso (cargados desde la base) tiene que arrancar en ese numero, no en 0: si no,
	 * un restart subestimaria la cantidad real hasta que esos viajes se devuelvan.
	 */
	@PostConstruct
	public void inicializar() {
		contador.set((int) viajeRepository.countByFechaFinIsNull());
	}

	/** Llamado por ViajeService.iniciar() al desbloquear una bicicleta. */
	public void incrementar() {
		contador.incrementAndGet();
	}

	/** Llamado por ViajeService.finalizar() al devolver una bicicleta. Nunca baja de 0. */
	public void decrementar() {
		contador.updateAndGet(actual -> Math.max(0, actual - 1));
	}

	public UsuariosActivosResponse obtenerActual() {
		return UsuariosActivosResponse.actual(contador.get());
	}

	@Transactional(readOnly = true)
	public List<UsuariosActivosResponse> listarHistorial() {
		return registroRepository.findAllByOrderByTimestampDesc().stream()
				.map(UsuariosActivosResponse::desde)
				.toList();
	}

	/** Corre en punto de cada hora (minuto y segundo en 0), zona horaria del servidor. */
	@Scheduled(cron = "0 0 * * * *")
	@Transactional
	public void guardarSnapshot() {
		registroRepository.save(new RegistroUsuariosActivos(contador.get(), Instant.now()));
	}
}
