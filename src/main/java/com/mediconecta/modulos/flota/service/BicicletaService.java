package com.mediconecta.modulos.flota.service;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.almacen.entity.Almacen;
import com.mediconecta.modulos.almacen.repository.AlmacenRepository;
import com.mediconecta.modulos.flota.dto.BicicletaRequest;
import com.mediconecta.modulos.flota.dto.BicicletaResponse;
import com.mediconecta.modulos.flota.entity.Bicicleta;
import com.mediconecta.modulos.flota.entity.EstadoBicicleta;
import com.mediconecta.modulos.flota.repository.BicicletaRepository;


@Service
public class BicicletaService {

	/** Estados que un admin puede asignar a mano mientras la bici esta en un almacen. */
	private static final Set<EstadoBicicleta> ESTADOS_EDITABLES_EN_ALMACEN =
			Set.of(EstadoBicicleta.EN_REPARACION, EstadoBicicleta.DESACTIVADA);

	private final BicicletaRepository bicicletaRepository;
	private final AlmacenRepository almacenRepository;

	public BicicletaService(BicicletaRepository bicicletaRepository, AlmacenRepository almacenRepository) {
		this.bicicletaRepository = bicicletaRepository;
		this.almacenRepository = almacenRepository;
	}

	@Transactional
	public BicicletaResponse crear(BicicletaRequest request) {
		if (bicicletaRepository.existsByCodigo(request.codigo())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "ya existe una bicicleta con ese codigo");
		}

		Almacen almacen = almacenRepository.findById(request.almacenId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "almacen no encontrado"));

		Bicicleta bicicleta = new Bicicleta(request.codigo(), almacen);
		return BicicletaResponse.desde(bicicletaRepository.save(bicicleta));
	}

	@Transactional(readOnly = true)
	public List<BicicletaResponse> listar() {
		return bicicletaRepository.findAll().stream()
				.map(BicicletaResponse::desde)
				.toList();
	}

	@Transactional(readOnly = true)
	public BicicletaResponse obtenerPorId(Long id) {
		return BicicletaResponse.desde(buscar(id));
	}

	/** Cambia el estado de una bici que ya esta en un almacen (ciclo de reparacion). */
	@Transactional
	public BicicletaResponse cambiarEstadoEnAlmacen(Long id, EstadoBicicleta nuevoEstado) {
		Bicicleta bicicleta = buscar(id);

		if (bicicleta.getAlmacen() == null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"la bicicleta debe estar en un almacen para cambiar su estado");
		}
		if (!ESTADOS_EDITABLES_EN_ALMACEN.contains(nuevoEstado)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"estado invalido: solo se puede pasar a EN_REPARACION o DESACTIVADA");
		}

		bicicleta.moverAAlmacen(bicicleta.getAlmacen(), nuevoEstado);
		return BicicletaResponse.desde(bicicleta);
	}

	Bicicleta buscar(Long id) {
		return bicicletaRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "bicicleta no encontrada"));
	}
}
