import { useCallback, useEffect, useState } from 'react';
import { membresiasApi, suscripcionesApi } from '../api/membresias';
import { ApiError } from '../api/client';
import type { MembresiaResponse, SuscripcionResponse } from '../types/dto';

/**
 * Estado de planes y suscripciones del usuario, con las acciones de contratar y cancelar.
 * Vive en un hook porque lo consumen dos pantallas distintas (Planes y Mi cuenta):
 * la logica de negocio no se duplica, solo cambia como se presenta.
 */
export function useSuscripcion() {
	const [membresias, setMembresias] = useState<MembresiaResponse[]>([]);
	const [suscripciones, setSuscripciones] = useState<SuscripcionResponse[]>([]);
	const [cargando, setCargando] = useState(true);
	const [mensaje, setMensaje] = useState<string | null>(null);
	const [error, setError] = useState<string | null>(null);
	const [contratando, setContratando] = useState<number | null>(null);

	const recargar = useCallback(() => {
		return Promise.all([membresiasApi.listar(), suscripcionesApi.listarPropias()])
			.then(([lista, propias]) => {
				setMembresias(lista.filter((m) => m.activa));
				setSuscripciones(propias);
			})
			.finally(() => setCargando(false));
	}, []);

	useEffect(() => {
		recargar();
	}, [recargar]);

	const ahora = Date.now();
	const planVigente =
		suscripciones.find((s) => s.estado === 'ACTIVA' && (!s.fechaFin || new Date(s.fechaFin).getTime() > ahora)) ?? null;

	const contratar = useCallback(
		async (membresiaId: number) => {
			setError(null);
			setMensaje(null);
			setContratando(membresiaId);
			try {
				const pago = await suscripcionesApi.contratar({ membresiaId });
				setMensaje(
					`Se generó el pago #${pago.id} por $${pago.monto}. Confirmalo en "Pagos" para poder sacar una bici.`,
				);
				await recargar();
			} catch (err) {
				setError(err instanceof ApiError ? err.message : 'no se pudo contratar');
			} finally {
				setContratando(null);
			}
		},
		[recargar],
	);

	const cancelar = useCallback(
		async (id: number) => {
			setError(null);
			setMensaje(null);
			try {
				await suscripcionesApi.cancelar(id);
				await recargar();
			} catch (err) {
				setError(err instanceof ApiError ? err.message : 'no se pudo cancelar');
			}
		},
		[recargar],
	);

	return { membresias, suscripciones, planVigente, cargando, mensaje, error, contratando, contratar, cancelar, recargar };
}
