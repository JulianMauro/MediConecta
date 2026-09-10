package com.mediconecta.modulos.estacion.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.estacion.dto.EstacionRequest;
import com.mediconecta.modulos.estacion.dto.EstacionResponse;
import com.mediconecta.modulos.estacion.dto.EstacionUpdateRequest;
import com.mediconecta.modulos.estacion.entity.Estacion;
import com.mediconecta.modulos.estacion.repository.AnclajeRepository;
import com.mediconecta.modulos.estacion.repository.EstacionRepository;


@Service
public class EstacionService {

	private final EstacionRepository estacionRepository;
	private final AnclajeRepository anclajeRepository;
	private final GeocodingService geocodingService;

	public EstacionService(EstacionRepository estacionRepository, AnclajeRepository anclajeRepository,
			GeocodingService geocodingService) {
		this.estacionRepository = estacionRepository;
		this.anclajeRepository = anclajeRepository;
		this.geocodingService = geocodingService;
	}

	@Transactional
	public EstacionResponse crear(EstacionRequest request) {
		Estacion estacion = new Estacion(request.nombre(), request.direccion(), request.latitud(),
				request.longitud(), request.capacidad());

		/*
		 * Si el alta no trajo coordenadas, se resuelven desde la direccion. Se
		 * geocodifica UNA sola vez, aca, y queda guardado: hacerlo en cada lectura
		 * seria lento y se pagaria por request.
		 *
		 * Que falle no aborta el alta. Sin API key configurada (o con Google caido)
		 * la estacion se guarda igual, solo que sin coordenadas: es una estacion
		 * valida y operativa, lo unico que no puede hacer todavia es aparecer en el
		 * mapa. Para completarla despues esta geocodificar(id).
		 */
		if (request.latitud() == null || request.longitud() == null) {
			geocodingService.geocodificar(request.direccion())
					.ifPresent(c -> estacion.asignarCoordenadas(c.latitud(), c.longitud()));
		}

		return EstacionResponse.desde(estacionRepository.save(estacion));
	}

	/**
	 * Edita los datos de una estacion existente.
	 *
	 * Dos reglas propias del update, que no existen en el alta:
	 *
	 * 1. La capacidad no puede quedar por debajo de los anclajes ya creados. Bajarla
	 *    no borra anclajes: dejaria a la estacion en un estado que ella misma declara
	 *    imposible, y el alta de anclajes usa esa capacidad como tope.
	 *
	 * 2. Si cambio la direccion, las coordenadas viejas dejan de ser validas. Se
	 *    reintenta el geocoding, y si no se puede resolver se BORRAN en vez de
	 *    conservarlas: una estacion sin pin es un dato faltante, pero una estacion
	 *    con el pin de su direccion anterior es un dato erroneo, y manda gente a un
	 *    lugar donde no hay bicis.
	 */
	@Transactional
	public EstacionResponse actualizar(Long id, EstacionUpdateRequest request) {
		Estacion estacion = buscar(id);

		long anclajesCreados = anclajeRepository.countByEstacionId(id);
		if (request.capacidad() < anclajesCreados) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"la estacion ya tiene " + anclajesCreados + " anclajes: la capacidad no puede ser menor");
		}

		String direccionNueva = request.direccion().trim();
		boolean cambioLaDireccion = !direccionNueva.equalsIgnoreCase(estacion.getDireccion());

		estacion.actualizarDatos(request.nombre().trim(), direccionNueva, request.capacidad());

		if (cambioLaDireccion) {
			geocodingService.geocodificar(direccionNueva)
					.ifPresentOrElse(
							c -> estacion.asignarCoordenadas(c.latitud(), c.longitud()),
							estacion::limpiarCoordenadas);
		}

		// Dentro de la transaccion, JPA detecta el cambio y persiste solo.
		return EstacionResponse.desde(estacion);
	}

	/**
	 * Resuelve las coordenadas de una estacion ya creada.
	 *
	 * Sirve para las estaciones dadas de alta antes de tener la API key, y para
	 * reintentar cuando la direccion estaba mal escrita y se corrigio.
	 *
	 * A diferencia del alta, aca el fallo SI es un error: el admin pidio
	 * explicitamente geocodificar, y quedarse callado le haria creer que funciono.
	 */
	@Transactional
	public EstacionResponse geocodificar(Long id) {
		Estacion estacion = buscar(id);
		Coordenadas coordenadas = geocodingService.geocodificar(estacion.getDireccion())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
						"no se pudieron obtener las coordenadas de \"" + estacion.getDireccion()
								+ "\": revisa la direccion o la configuracion de la API key"));
		estacion.asignarCoordenadas(coordenadas.latitud(), coordenadas.longitud());
		return EstacionResponse.desde(estacion);
	}

	@Transactional(readOnly = true)
	public List<EstacionResponse> listar() {
		return estacionRepository.findAll().stream()
				.map(EstacionResponse::desde)
				.toList();
	}

	@Transactional(readOnly = true)
	public EstacionResponse obtenerPorId(Long id) {
		return EstacionResponse.desde(buscar(id));
	}

	@Transactional
	public void desactivar(Long id) {
		buscar(id).desactivar();
	}

	Estacion buscar(Long id) {
		return estacionRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "estacion no encontrada"));
	}
}
