import { api, ApiError } from './client';
import type { ViajeFinalizarRequest, ViajeIniciarRequest, ViajeResponse } from '../types/dto';

export const viajesApi = {
	iniciar: (req: ViajeIniciarRequest) => api.post<ViajeResponse>('/api/viajes/iniciar', req),
	finalizar: (req: ViajeFinalizarRequest) => api.post<ViajeResponse>('/api/viajes/finalizar', req),
	listarPropios: () => api.get<ViajeResponse[]>('/api/viajes'),

	/** null si no hay viaje en curso (el backend devuelve 404 en ese caso, no es un error de verdad). */
	obtenerActual: async (): Promise<ViajeResponse | null> => {
		try {
			return await api.get<ViajeResponse>('/api/viajes/actual');
		} catch (err) {
			if (err instanceof ApiError && err.status === 404) return null;
			throw err;
		}
	},
};
