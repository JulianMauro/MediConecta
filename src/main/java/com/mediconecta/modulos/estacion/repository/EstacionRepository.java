package com.mediconecta.modulos.estacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.estacion.entity.Estacion;

public interface EstacionRepository extends JpaRepository<Estacion, Long> {
}
