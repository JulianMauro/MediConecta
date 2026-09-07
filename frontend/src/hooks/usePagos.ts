import { useCallback, useEffect, useState } from 'react';
import { pagosApi } from '../api/pagos';
import { ApiError } from '../api/client';
import type { PagoResponse } from '../types/dto';

/**
 * Pagos del usuario con las acciones de confirmar y rechazar.
 * Vive en un hook para que "Mi cuenta" pueda recargarlo cuando otra sección
 * (contratar una suscripción) genera un pago nuevo.
 */
export function usePagos() {
	const [pagos, setPagos] = useState<PagoResponse[]>([]);
	const [error, setError] = useState<string | null>(null);

	const recargar = useCallback(() => {
		return pagosApi.listarPropios().then(setPagos);
	}, []);

	useEffect(() => {
		recargar();
	}, [recargar]);

	const confirmar = useCallback(
		async (id: number) => {
			setError(null);
			try {
				// Simula la respuesta de la pasarela: en un sistema real esto lo dispara un webhook, no el usuario.
				await pagosApi.confirmar(id, { referenciaPasarela: `SIM-${Date.now()}` });
				await recargar();
			} catch (err) {
				setError(err instanceof ApiError ? err.message : 'no se pudo confirmar el pago');
			}
		},
		[recargar],
	);

	const rechazar = useCallback(
		async (id: number) => {
			setError(null);
			try {
				await pagosApi.rechazar(id);
				await recargar();
			} catch (err) {
				setError(err instanceof ApiError ? err.message : 'no se pudo rechazar el pago');
			}
		},
		[recargar],
	);

	return { pagos, error, recargar, confirmar, rechazar };
}
