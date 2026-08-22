package com.mediconecta.modulos.logistica.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.almacen.entity.Almacen;
import com.mediconecta.modulos.almacen.repository.AlmacenRepository;
import com.mediconecta.modulos.estacion.entity.Anclaje;
import com.mediconecta.modulos.estacion.entity.Estacion;
import com.mediconecta.modulos.estacion.entity.EstadoDock;
import com.mediconecta.modulos.estacion.repository.AnclajeRepository;
import com.mediconecta.modulos.estacion.repository.EstacionRepository;
import com.mediconecta.modulos.flota.entity.Bicicleta;
import com.mediconecta.modulos.flota.entity.EstadoBicicleta;
import com.mediconecta.modulos.flota.repository.BicicletaRepository;
import com.mediconecta.modulos.logistica.dto.MovimientoBicisRequest;
import com.mediconecta.modulos.logistica.dto.MovimientoBicisResponse;
import com.mediconecta.modulos.logistica.entity.MovimientoBiciItem;
import com.mediconecta.modulos.logistica.entity.MovimientoBicis;
import com.mediconecta.modulos.logistica.repository.MovimientoBiciItemRepository;
import com.mediconecta.modulos.logistica.repository.MovimientoBicisRepository;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;


/** Traslado en lote (simula el camion de redistribucion). Registra qué admin movió qué bicis, de dónde a dónde. */
@Service
public class MovimientoBicisService {

	private final MovimientoBicisRepository movimientoBicisRepository;
	private final MovimientoBiciItemRepository movimientoBiciItemRepository;
	private final BicicletaRepository bicicletaRepository;
	private final EstacionRepository estacionRepository;
	private final AlmacenRepository almacenRepository;
	private final AnclajeRepository anclajeRepository;
	private final UsuarioRepository usuarioRepository;

	public MovimientoBicisService(MovimientoBicisRepository movimientoBicisRepository,
			MovimientoBiciItemRepository movimientoBiciItemRepository, BicicletaRepository bicicletaRepository,
			EstacionRepository estacionRepository, AlmacenRepository almacenRepository,
			AnclajeRepository anclajeRepository, UsuarioRepository usuarioRepository) {
		this.movimientoBicisRepository = movimientoBicisRepository;
		this.movimientoBiciItemRepository = movimientoBiciItemRepository;
		this.bicicletaRepository = bicicletaRepository;
		this.estacionRepository = estacionRepository;
		this.almacenRepository = almacenRepository;
		this.anclajeRepository = anclajeRepository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional
	public MovimientoBicisResponse registrar(Long adminId, MovimientoBicisRequest request) {
		Usuario admin = usuarioRepository.findById(adminId)
				.orElseThrow(() -> new IllegalStateException("usuario autenticado no encontrado"));

		exactamenteUno(request.origenEstacionId(), request.origenAlmacenId(), "origen");
		exactamenteUno(request.destinoEstacionId(), request.destinoAlmacenId(), "destino");

		Estacion origenEstacion = request.origenEstacionId() != null ? buscarEstacion(request.origenEstacionId()) : null;
		Almacen origenAlmacen = request.origenAlmacenId() != null ? buscarAlmacen(request.origenAlmacenId()) : null;
		Estacion destinoEstacion = request.destinoEstacionId() != null ? buscarEstacion(request.destinoEstacionId()) : null;
		Almacen destinoAlmacen = request.destinoAlmacenId() != null ? buscarAlmacen(request.destinoAlmacenId()) : null;

		MovimientoBicis movimiento = movimientoBicisRepository.save(
				new MovimientoBicis(admin, origenEstacion, origenAlmacen, destinoEstacion, destinoAlmacen));

		List<Long> idsMovidos = new ArrayList<>();
		for (Long bicicletaId : request.bicicletaIds()) {
			Bicicleta bicicleta = bicicletaRepository.findById(bicicletaId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
							"bicicleta " + bicicletaId + " no encontrada"));

			validarEnOrigen(bicicleta, origenEstacion, origenAlmacen);
			moverADestino(bicicleta, destinoEstacion, destinoAlmacen);

			movimientoBiciItemRepository.save(new MovimientoBiciItem(movimiento, bicicleta));
			idsMovidos.add(bicicletaId);
		}

		return MovimientoBicisResponse.desde(movimiento, idsMovidos);
	}

	private void exactamenteUno(Long idA, Long idB, String etiqueta) {
		boolean aPresente = idA != null;
		boolean bPresente = idB != null;
		if (aPresente == bPresente) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"el " + etiqueta + " debe ser exactamente una estacion o un almacen, no ambos ni ninguno");
		}
	}

	private void validarEnOrigen(Bicicleta bicicleta, Estacion origenEstacion, Almacen origenAlmacen) {
		if (origenEstacion != null) {
			boolean enEsaEstacion = bicicleta.getAnclaje() != null
					&& bicicleta.getAnclaje().getEstacion().getId().equals(origenEstacion.getId());
			if (!enEsaEstacion) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"la bicicleta " + bicicleta.getCodigo() + " no esta en la estacion de origen indicada");
			}
			Anclaje anclaje = bicicleta.getAnclaje();
			bicicleta.retirarDeAnclaje();
			anclaje.liberar();
		} else {
			boolean enEseAlmacen = bicicleta.getAlmacen() != null
					&& bicicleta.getAlmacen().getId().equals(origenAlmacen.getId());
			if (!enEseAlmacen) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"la bicicleta " + bicicleta.getCodigo() + " no esta en el almacen de origen indicado");
			}
		}
	}

	private void moverADestino(Bicicleta bicicleta, Estacion destinoEstacion, Almacen destinoAlmacen) {
		if (destinoAlmacen != null) {
			// Toda bici que entra a un almacen queda desactivada hasta que un admin la repare y la vuelva a sacar.
			bicicleta.moverAAlmacen(destinoAlmacen, EstadoBicicleta.DESACTIVADA);
			return;
		}

		Anclaje anclajeLibre = anclajeRepository.findByEstacionIdAndEstado(destinoEstacion.getId(), EstadoDock.LIBRE)
				.stream().findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
						"no hay anclajes libres en la estacion destino " + destinoEstacion.getNombre()));

		bicicleta.anclarEn(anclajeLibre);
		anclajeLibre.ocupar();
	}

	private Estacion buscarEstacion(Long id) {
		return estacionRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "estacion no encontrada"));
	}

	private Almacen buscarAlmacen(Long id) {
		return almacenRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "almacen no encontrado"));
	}
}
