package com.mediconecta.modulos.saludo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.saludo.entity.Saludo;


public interface SaludoRepository extends JpaRepository<Saludo, Long> {
}
