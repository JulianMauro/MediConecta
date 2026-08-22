package com.mediconecta.modulos.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediconecta.modulos.almacen.entity.Almacen;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
}
