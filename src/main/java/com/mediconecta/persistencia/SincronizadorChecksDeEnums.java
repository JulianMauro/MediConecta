package com.mediconecta.persistencia;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.Column;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;

/**
 * Hibernate crea cada columna @Enumerated(STRING) con un CHECK que lista los valores del enum
 * (ej. strike_estado_check: estado IN ('ACTIVO','SALDADO')). Con ddl-auto=update ese CHECK no
 * se vuelve a generar: al sumar un valor al enum, la base ya creada rechaza el INSERT.
 *
 * Al arrancar, y antes de que la app atienda requests, compara cada CHECK con los valores
 * actuales del enum en Java y lo regenera si no coinciden. Solo en PostgreSQL: el perfil h2
 * crea la base de cero en cada arranque, asi que ahi el CHECK ya nace al dia.
 */
@Component
public class SincronizadorChecksDeEnums implements SmartInitializingSingleton {

	private static final Logger log = LoggerFactory.getLogger(SincronizadorChecksDeEnums.class);
	private static final Pattern VALOR = Pattern.compile("'([^']+)'");

	private final EntityManagerFactory entityManagerFactory;
	private final DataSource dataSource;
	private final JdbcTemplate jdbcTemplate;
	private final TransactionTemplate transactionTemplate;

	public SincronizadorChecksDeEnums(EntityManagerFactory entityManagerFactory, DataSource dataSource,
			JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
		this.entityManagerFactory = entityManagerFactory;
		this.dataSource = dataSource;
		this.jdbcTemplate = jdbcTemplate;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public void afterSingletonsInstantiated() {
		if (!esPostgres()) {
			return;
		}
		for (EntityType<?> entidad : entityManagerFactory.getMetamodel().getEntities()) {
			Class<?> clase = entidad.getJavaType();
			for (Field campo : clase.getDeclaredFields()) {
				Enumerated enumerated = campo.getAnnotation(Enumerated.class);
				if (enumerated != null && enumerated.value() == EnumType.STRING && campo.getType().isEnum()) {
					sincronizar(tabla(clase), columna(campo), campo.getType());
				}
			}
		}
	}

	private void sincronizar(String tabla, String columna, Class<?> tipoEnum) {
		String nombre = tabla + "_" + columna + "_check";
		Set<String> esperados = Arrays.stream(tipoEnum.getEnumConstants())
				.map(c -> ((Enum<?>) c).name())
				.collect(Collectors.toCollection(TreeSet::new));

		String definicion = jdbcTemplate.query(
				"select pg_get_constraintdef(oid) from pg_constraint where conname = ? and conrelid = to_regclass(?)",
				rs -> rs.next() ? rs.getString(1) : null, nombre, tabla);
		if (definicion != null && valoresDe(definicion).equals(esperados)) {
			return;
		}

		String lista = esperados.stream().map(v -> "'" + v + "'").collect(Collectors.joining(", "));
		transactionTemplate.executeWithoutResult(tx -> {
			jdbcTemplate.execute("alter table " + tabla + " drop constraint if exists " + nombre);
			jdbcTemplate.execute("alter table " + tabla + " add constraint " + nombre
					+ " check (" + columna + " in (" + lista + "))");
		});
		log.info("check {} actualizado a los valores de {}: {}", nombre, tipoEnum.getSimpleName(), esperados);
	}

	private static Set<String> valoresDe(String definicion) {
		Set<String> valores = new TreeSet<>();
		Matcher m = VALOR.matcher(definicion);
		while (m.find()) {
			valores.add(m.group(1));
		}
		return valores;
	}

	private boolean esPostgres() {
		try (Connection conexion = dataSource.getConnection()) {
			return "PostgreSQL".equals(conexion.getMetaData().getDatabaseProductName());
		} catch (SQLException e) {
			throw new IllegalStateException("no se pudo leer el tipo de base de datos", e);
		}
	}

	private static String tabla(Class<?> clase) {
		Table table = clase.getAnnotation(Table.class);
		return table != null && !table.name().isEmpty() ? table.name() : snakeCase(clase.getSimpleName());
	}

	private static String columna(Field campo) {
		Column column = campo.getAnnotation(Column.class);
		return column != null && !column.name().isEmpty() ? column.name() : snakeCase(campo.getName());
	}

	/** Igual que la estrategia de nombres por defecto de Spring Boot: fechaFin -> fecha_fin. */
	private static String snakeCase(String nombre) {
		return nombre.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
	}
}
