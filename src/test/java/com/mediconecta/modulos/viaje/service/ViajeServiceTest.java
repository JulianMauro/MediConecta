package com.mediconecta.modulos.viaje.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.mediconecta.modulos.almacen.entity.Almacen;
import com.mediconecta.modulos.estacion.entity.Anclaje;
import com.mediconecta.modulos.estacion.entity.EstadoDock;
import com.mediconecta.modulos.estacion.repository.AnclajeRepository;
import com.mediconecta.modulos.flota.entity.EstadoBicicleta;
import com.mediconecta.modulos.flota.repository.BicicletaRepository;
import com.mediconecta.modulos.membresia.repository.SuscripcionUsuarioRepository;
import com.mediconecta.modulos.monitoreo.service.UsuariosActivosService;
import com.mediconecta.modulos.strike.service.StrikeService;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.usuario.repository.BloqueoCuentaRepository;
import com.mediconecta.modulos.usuario.repository.UsuarioRepository;
import com.mediconecta.modulos.viaje.dto.ViajeFinalizarRequest;
import com.mediconecta.modulos.viaje.entity.Viaje;
import com.mediconecta.modulos.viaje.repository.ViajeRepository;
import com.mediconecta.testutil.Datos;

/** El cierre de viaje es comun al cliente y a soporte: se prueba una sola vez, aca. */
@ExtendWith(MockitoExtension.class)
class ViajeServiceTest {

	@Mock
	private ViajeRepository viajeRepository;
	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private BicicletaRepository bicicletaRepository;
	@Mock
	private AnclajeRepository anclajeRepository;
	@Mock
	private SuscripcionUsuarioRepository suscripcionUsuarioRepository;
	@Mock
	private BloqueoCuentaRepository bloqueoCuentaRepository;
	@Mock
	private StrikeService strikeService;
	@Mock
	private UsuariosActivosService usuariosActivosService;

	private ViajeService viajeService;
	private Usuario cliente;

	@BeforeEach
	void setUp() {
		viajeService = new ViajeService(viajeRepository, usuarioRepository, bicicletaRepository, anclajeRepository,
				suscripcionUsuarioRepository, bloqueoCuentaRepository, strikeService, usuariosActivosService);
		cliente = Datos.cliente(10L);
	}

	@Test
	void finalizarDentroDelTiempoAnclaLaBiciSinStrike() {
		Viaje viaje = Datos.viajeEnCurso(1L, cliente, 10);
		Anclaje destino = Datos.anclajeLibre(5L);
		when(viajeRepository.findEnCursoParaCerrar(10L)).thenReturn(Optional.of(viaje));
		when(anclajeRepository.findParaOcupar(5L)).thenReturn(Optional.of(destino));

		viajeService.finalizar(10L, new ViajeFinalizarRequest(5L));

		assertThat(viaje.getFechaFin()).isNotNull();
		assertThat(viaje.isExcedioTiempo()).isFalse();
		assertThat(viaje.getBicicleta().getAnclaje()).isSameAs(destino);
		assertThat(viaje.getBicicleta().getEstado()).isEqualTo(EstadoBicicleta.DISPONIBLE);
		assertThat(destino.getEstado()).isEqualTo(EstadoDock.OCUPADO);
		verify(usuariosActivosService).decrementar();
		verify(strikeService, never()).generarPorExceso(any(), any());
	}

	@Test
	void finalizarFueraDeTiempoGeneraStrike() {
		Viaje viaje = Datos.viajeEnCurso(1L, cliente, 45);
		when(viajeRepository.findEnCursoParaCerrar(10L)).thenReturn(Optional.of(viaje));
		when(anclajeRepository.findParaOcupar(5L)).thenReturn(Optional.of(Datos.anclajeLibre(5L)));

		viajeService.finalizar(10L, new ViajeFinalizarRequest(5L));

		assertThat(viaje.isExcedioTiempo()).isTrue();
		verify(strikeService).generarPorExceso(cliente, viaje);
	}

	/** Si el cliente y soporte cierran a la vez, el segundo ya no encuentra el viaje en curso. */
	@Test
	void finalizarSinViajeEnCursoDevuelve409YNoTocaNada() {
		when(viajeRepository.findEnCursoParaCerrar(10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> viajeService.finalizar(10L, new ViajeFinalizarRequest(5L)))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("no tenes un viaje en curso");
		verify(usuariosActivosService, never()).decrementar();
	}

	@Test
	void cerrarEnAnclajeOcupadoDevuelve409() {
		Viaje viaje = Datos.viajeEnCurso(1L, cliente, 10);
		Anclaje ocupado = Datos.anclajeLibre(5L);
		ocupado.ocupar();
		when(anclajeRepository.findParaOcupar(5L)).thenReturn(Optional.of(ocupado));

		assertThatThrownBy(() -> viajeService.cerrarEnAnclaje(viaje, 5L, Instant.now(), false))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("no esta libre");
		assertThat(viaje.getFechaFin()).isNull();
	}

	@Test
	void cerrarConHoraAnteriorEvitaElExceso() {
		Viaje viaje = Datos.viajeEnCurso(1L, cliente, 50);
		when(anclajeRepository.findParaOcupar(5L)).thenReturn(Optional.of(Datos.anclajeLibre(5L)));

		// Llego a la estacion a los 20 minutos y no pudo anclar hasta ahora.
		viajeService.cerrarEnAnclaje(viaje, 5L, viaje.getFechaInicio().plus(Duration.ofMinutes(20)), false);

		assertThat(viaje.duracionMinutos()).isEqualTo(20);
		assertThat(viaje.isExcedioTiempo()).isFalse();
		verify(strikeService, never()).generarPorExceso(any(), any());
	}

	@Test
	void condonarElExcesoNoGeneraStrikePeroDejaRegistradoQueSePaso() {
		Viaje viaje = Datos.viajeEnCurso(1L, cliente, 45);
		when(anclajeRepository.findParaOcupar(5L)).thenReturn(Optional.of(Datos.anclajeLibre(5L)));

		viajeService.cerrarEnAnclaje(viaje, 5L, Instant.now(), true);

		assertThat(viaje.isExcedioTiempo()).isTrue();
		assertThat(viaje.isExcesoCondonado()).isTrue();
		verify(strikeService, never()).generarPorExceso(any(), any());
	}

	@Test
	void cerrarEnAlmacenDejaLaBiciEnReparacionYSinAnclaje() {
		Viaje viaje = Datos.viajeEnCurso(1L, cliente, 10);
		Almacen almacen = Datos.almacen(3L);

		viajeService.cerrarEnAlmacen(viaje, almacen, EstadoBicicleta.EN_REPARACION, Instant.now(), true);

		assertThat(viaje.getFechaFin()).isNotNull();
		assertThat(viaje.getAnclajeDestino()).isNull();
		assertThat(viaje.getBicicleta().getAlmacen()).isSameAs(almacen);
		assertThat(viaje.getBicicleta().getAnclaje()).isNull();
		assertThat(viaje.getBicicleta().getEstado()).isEqualTo(EstadoBicicleta.EN_REPARACION);
		verify(usuariosActivosService).decrementar();
	}

	@Test
	void cerrarEnAlmacenComoDisponibleSeRechaza() {
		Viaje viaje = Datos.viajeEnCurso(1L, cliente, 10);

		assertThatThrownBy(() -> viajeService.cerrarEnAlmacen(viaje, Datos.almacen(3L), EstadoBicicleta.DISPONIBLE,
				Instant.now(), false))
				.isInstanceOf(ResponseStatusException.class);
		assertThat(viaje.getFechaFin()).isNull();
	}

	@Test
	void horaDeFinFuturaOAnteriorAlInicioSeRechaza() {
		Viaje viaje = Datos.viajeEnCurso(1L, cliente, 10);
		when(anclajeRepository.findParaOcupar(5L)).thenReturn(Optional.of(Datos.anclajeLibre(5L)));

		assertThatThrownBy(() -> viajeService.cerrarEnAnclaje(viaje, 5L, Instant.now().plus(Duration.ofHours(1)), false))
				.hasMessageContaining("futura");
		assertThatThrownBy(() -> viajeService.cerrarEnAnclaje(viaje, 5L,
				viaje.getFechaInicio().minus(Duration.ofMinutes(1)), false))
				.hasMessageContaining("anterior al inicio");
		assertThat(viaje.getFechaFin()).isNull();
		verify(usuariosActivosService, never()).decrementar();
	}

	@Test
	void unViajeCerradoNoSePuedeVolverACerrar() {
		Viaje viaje = Datos.viajeEnCurso(1L, cliente, 10);
		viaje.cerrar(null, Instant.now(), false);

		assertThatThrownBy(() -> viaje.cerrar(null, Instant.now(), false)).isInstanceOf(IllegalStateException.class);
	}
}
