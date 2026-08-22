package com.mediconecta.modulos.flota.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.flota.entity.Bicicleta;

public interface BicicletaRepository extends JpaRepository<Bicicleta, Long> {

	Optional<Bicicleta> findByCodigo(String codigo);

	boolean existsByCodigo(String codigo);
}
