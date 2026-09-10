package com.mediconecta.modulos.estacion.service;

/** Par lat/lng ya resuelto. No es un DTO de la API: no sale nunca del backend. */
public record Coordenadas(double latitud, double longitud) {
}
