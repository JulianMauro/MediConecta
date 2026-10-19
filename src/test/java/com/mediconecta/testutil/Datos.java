package com.mediconecta.testutil;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import org.springframework.test.util.ReflectionTestUtils;

import com.mediconecta.modulos.almacen.entity.Almacen;
import com.mediconecta.modulos.estacion.entity.Anclaje;
import com.mediconecta.modulos.estacion.entity.Estacion;
import com.mediconecta.modulos.flota.entity.Bicicleta;
import com.mediconecta.modulos.membresia.entity.Membresia;
import com.mediconecta.modulos.membresia.entity.SuscripcionUsuario;
import com.mediconecta.modulos.membresia.entity.TipoMembresia;
import com.mediconecta.modulos.usuario.entity.Rol;
import com.mediconecta.modulos.usuario.entity.Usuario;
import com.mediconecta.modulos.viaje.entity.Viaje;

/** Entidades armadas para unit tests: con id seteado a mano, como si vinieran de la base. */
public final class Datos {

	private Datos() {
	}

	public static <T> T conId(T entidad, long id) {
		ReflectionTestUtils.setField(entidad, "id", id);
		return entidad;
	}

	public static Usuario cliente(long id) {
		return conId(new Usuario("Juana", "Perez", "juana" + id + "@ejemplo.com", "hash", "1", "111", Rol.CLIENTE), id);
	}

	public static Usuario admin(long id) {
		return conId(new Usuario("Ana", "Admin", "ana" + id + "@ejemplo.com", "hash", "2", "222", Rol.ADMIN), id);
	}

	public static Almacen almacen(long id) {
		return conId(new Almacen("Deposito", "Calle 1", 50), id);
	}

	public static Anclaje anclajeLibre(long id) {
		Estacion estacion = conId(new Estacion("Plaza de Mayo", "Av. Rivadavia 100", -34.6, -58.37, 6), 1L);
		return conId(new Anclaje(estacion, (int) id), id);
	}

	/** Plan de 30 minutos con tarifa extra de 10 por minuto. */
	public static Membresia membresia() {
		return conId(new Membresia("Pase diario", TipoMembresia.INDIVIDUAL, 30, new BigDecimal("1000"),
				new BigDecimal("10"), 5, 0), 1L);
	}

	/** Viaje en curso que empezo hace minutosAtras, con la bici ya retirada de su anclaje. */
	public static Viaje viajeEnCurso(long id, Usuario usuario, long minutosAtras) {
		Membresia membresia = membresia();
		SuscripcionUsuario suscripcion = conId(new SuscripcionUsuario(usuario, membresia, Instant.now(),
				Instant.now().plus(Duration.ofDays(1))), 1L);
		Anclaje origen = anclajeLibre(100L);
		Bicicleta bici = conId(new Bicicleta("PB-001", almacen(1L)), 1L);
		bici.anclarEn(origen);
		Viaje viaje = conId(new Viaje(usuario, bici, origen, suscripcion), id);
		bici.retirarDeAnclaje();
		origen.liberar();
		ReflectionTestUtils.setField(viaje, "fechaInicio", Instant.now().minus(Duration.ofMinutes(minutosAtras)));
		return viaje;
	}
}
