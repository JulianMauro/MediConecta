package com.mediconecta.modulos.estacion.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.estacion.dto.AnclajeRequest;
import com.mediconecta.modulos.estacion.dto.AnclajeResponse;
import com.mediconecta.modulos.estacion.entity.Anclaje;
import com.mediconecta.modulos.estacion.entity.Estacion;
import com.mediconecta.modulos.estacion.entity.EstadoDock;
import com.mediconecta.modulos.estacion.repository.AnclajeRepository;


@Service
public class AnclajeService {

	private final AnclajeRepository anclajeRepository;
	private final EstacionService estacionService;

	public AnclajeService(AnclajeRepository anclajeRepository, EstacionService estacionService) {
		this.anclajeRepository = anclajeRepository;
		this.estacionService = estacionService;
	}

	@Transactional
	public AnclajeResponse agregar(Long estacionId, AnclajeRequest request) {
		Estacion estacion = estacionService.buscar(estacionId);

		if (anclajeRepository.existsByEstacionIdAndNumero(estacionId, request.numero())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "ya existe un anclaje con ese numero en la estacion");
		}
		if (anclajeRepository.countByEstacionId(estacionId) >= estacion.getCapacidad()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"la estacion ya alcanzo su capacidad maxima de anclajes (" + estacion.getCapacidad() + ")");
		}

		Anclaje anclaje = new Anclaje(estacion, request.numero());
		return AnclajeResponse.desde(anclajeRepository.save(anclaje));
	}

	@Transactional(readOnly = true)
	public List<AnclajeResponse> listarPorEstacion(Long estacionId) {
		estacionService.buscar(estacionId);
		return anclajeRepository.findAll().stream()
				.filter(a -> a.getEstacion().getId().equals(estacionId))
				.map(AnclajeResponse::desde)
				.toList();
	}

	@Transactional
	public AnclajeResponse marcarFueraServicio(Long anclajeId) {
		Anclaje anclaje = buscar(anclajeId);
		anclaje.marcarFueraServicio();
		return AnclajeResponse.desde(anclaje);
	}

	@Transactional
	public AnclajeResponse habilitar(Long anclajeId) {
		Anclaje anclaje = buscar(anclajeId);
		if (anclaje.getEstado() == EstadoDock.OCUPADO) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "el anclaje esta ocupado por una bicicleta");
		}
		anclaje.liberar();
		return AnclajeResponse.desde(anclaje);
	}

	Anclaje buscar(Long id) {
		return anclajeRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "anclaje no encontrado"));
	}
}
