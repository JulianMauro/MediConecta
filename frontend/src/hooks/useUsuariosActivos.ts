import { useEffect, useState } from 'react';
import { monitoreoApi } from '../api/monitoreo';

const INTERVALO_MS = 15_000;

/**
 * Cantidad de usuarios con un viaje en curso ahora mismo. Hace polling: no hay
 * push desde el backend, así que se vuelve a pedir cada INTERVALO_MS mientras
 * el componente que lo usa esté montado.
 */
export function useUsuariosActivos() {
	const [cantidad, setCantidad] = useState<number | null>(null);
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		let cancelado = false;

		function consultar() {
			monitoreoApi
				.obtenerUsuariosActivos()
				.then((r) => {
					if (!cancelado) {
						setCantidad(r.cantidad);
						setError(null);
					}
				})
				.catch(() => {
					if (!cancelado) setError('No se pudo actualizar el contador.');
				});
		}

		consultar();
		const intervalo = setInterval(consultar, INTERVALO_MS);
		return () => {
			cancelado = true;
			clearInterval(intervalo);
		};
	}, []);

	return { cantidad, error };
}
