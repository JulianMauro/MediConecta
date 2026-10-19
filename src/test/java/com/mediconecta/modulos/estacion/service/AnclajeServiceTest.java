package com.mediconecta.modulos.estacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.estacion.entity.Anclaje;
import com.mediconecta.modulos.estacion.entity.EstadoDock;
import com.mediconecta.modulos.estacion.repository.AnclajeRepository;
import com.mediconecta.testutil.Datos;

@ExtendWith(MockitoExtension.class)
class AnclajeServiceTest {

	@Mock
	private AnclajeRepository anclajeRepository;
	@Mock
	private EstacionService estacionService;

	private AnclajeService anclajeService;

	@BeforeEach
	void setUp() {
		anclajeService = new AnclajeService(anclajeRepository, estacionService);
	}

	@Test
	void marcarFueraDeServicioUnAnclajeLibre() {
		Anclaje anclaje = Datos.anclajeLibre(5L);
		when(anclajeRepository.findById(5L)).thenReturn(Optional.of(anclaje));

		anclajeService.marcarFueraServicio(5L);

		assertThat(anclaje.getEstado()).isEqualTo(EstadoDock.FUERA_SERVICIO);
	}

	/** Si no, la bici queda anclada en un anclaje que la estacion da por roto. */
	@Test
	void marcarFueraDeServicioUnAnclajeOcupadoDevuelve409() {
		Anclaje anclaje = Datos.anclajeLibre(5L);
		anclaje.ocupar();
		when(anclajeRepository.findById(5L)).thenReturn(Optional.of(anclaje));

		assertThatThrownBy(() -> anclajeService.marcarFueraServicio(5L))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("ocupado");
		assertThat(anclaje.getEstado()).isEqualTo(EstadoDock.OCUPADO);
	}
}
