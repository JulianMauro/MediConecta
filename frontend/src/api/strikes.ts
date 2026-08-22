import { api } from './client';
import type { StrikeResponse } from '../types/dto';

export const strikesApi = {
	listarPropios: () => api.get<StrikeResponse[]>('/api/strikes'),
};
