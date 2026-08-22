package com.mediconecta.modulos.membresia.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.membresia.dto.MembresiaRequest;
import com.mediconecta.modulos.membresia.dto.MembresiaResponse;
import com.mediconecta.modulos.membresia.entity.Membresia;
import com.mediconecta.modulos.membresia.repository.MembresiaRepository;


@Service
public class MembresiaService {

	private final MembresiaRepository membresiaRepository;

	public MembresiaService(MembresiaRepository membresiaRepository) {
		this.membresiaRepository = membresiaRepository;
	}

	@Transactional
	public MembresiaResponse crear(MembresiaRequest request) {
		Membresia membresia = new Membresia(request.nombre(), request.tipo(), request.tiempoPermitidoMinutos(),
				request.precio(), request.tarifaMinutoExtra(), request.viajesPorDia(), request.tiempoEsperaMinutos());
		return MembresiaResponse.desde(membresiaRepository.save(membresia));
	}

	@Transactional(readOnly = true)
	public List<MembresiaResponse> listar() {
		return membresiaRepository.findAll().stream()
				.map(MembresiaResponse::desde)
				.toList();
	}

	@Transactional(readOnly = true)
	public MembresiaResponse obtenerPorId(Long id) {
		return MembresiaResponse.desde(buscar(id));
	}

	@Transactional
	public void desactivar(Long id) {
		buscar(id).desactivar();
	}

	Membresia buscar(Long id) {
		return membresiaRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "membresia no encontrada"));
	}
}
