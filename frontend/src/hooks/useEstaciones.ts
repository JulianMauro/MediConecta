import { useCallback, useEffect, useState } from 'react';
import { estacionesApi } from '../api/estaciones';
import type { AnclajeResponse, EstacionResponse } from '../types/dto';

export interface EstacionConDisponibilidad extends EstacionResponse {
	anclajes: AnclajeResponse[];
	/** Anclaje OCUPADO = hay una bici trabada ahi, lista para sacar. */
	bicisDisponibles: number;
	/** Anclaje LIBRE = hay lugar para devolver una bici. */
	anclajesLibres: number;
	fueraServicio: number;
}

function resumir(estacion: EstacionResponse, anclajes: AnclajeResponse[]): EstacionConDisponibilidad {
	return {
		...estacion,
		anclajes: [...anclajes].sort((a, b) => a.numero - b.numero),
		bicisDisponibles: anclajes.filter((a) => a.estado === 'OCUPADO').length,
		anclajesLibres: anclajes.filter((a) => a.estado === 'LIBRE').length,
		fueraServicio: anclajes.filter((a) => a.estado === 'FUERA_SERVICIO').length,
	};
}

/** Estaciones activas con el detalle de sus anclajes, para la pantalla publica de disponibilidad. */
export function useEstaciones() {
	const [estaciones, setEstaciones] = useState<EstacionConDisponibilidad[]>([]);
	const [cargando, setCargando] = useState(true);
	const [error, setError] = useState<string | null>(null);

	const recargar = useCallback(async () => {
		setError(null);
		try {
			const lista = (await estacionesApi.listar()).filter((e) => e.activa);
			const conAnclajes = await Promise.all(
				lista.map(async (e) => resumir(e, await estacionesApi.listarAnclajes(e.id))),
			);
			setEstaciones(conAnclajes);
		} catch {
			setError('No se pudo cargar el estado de las estaciones.');
		} finally {
			setCargando(false);
		}
	}, []);

	useEffect(() => {
		recargar();
	}, [recargar]);

	return { estaciones, cargando, error, recargar };
}
