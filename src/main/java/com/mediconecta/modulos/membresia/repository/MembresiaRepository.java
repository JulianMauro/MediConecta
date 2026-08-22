package com.mediconecta.modulos.membresia.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.membresia.entity.Membresia;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {
}
