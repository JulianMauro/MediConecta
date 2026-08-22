import { api } from './client';
import type { MovimientoBicisRequest, MovimientoBicisResponse } from '../types/dto';

export const movimientosApi = {
	registrar: (req: MovimientoBicisRequest) => api.post<MovimientoBicisResponse>('/api/movimientos-bicis', req),
};
