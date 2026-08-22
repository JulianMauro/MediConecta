package com.mediconecta.modulos.usuario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.usuario.entity.BloqueoCuenta;

public interface BloqueoCuentaRepository extends JpaRepository<BloqueoCuenta, Long> {

	/** Lo usa el login/uso de la app para saber si la cuenta esta bloqueada ahora mismo. */
	Optional<BloqueoCuenta> findByUsuarioIdAndActivoTrue(Long usuarioId);
}
