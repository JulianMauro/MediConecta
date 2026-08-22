import { api } from './client';
import type { AuthResponse, LoginRequest, RegisterRequest, UsuarioResponse } from '../types/dto';

export const authApi = {
	login: (req: LoginRequest) => api.post<AuthResponse>('/api/auth/login', req, { autenticado: false }),
	register: (req: RegisterRequest) => api.post<AuthResponse>('/api/auth/register', req, { autenticado: false }),
	me: () => api.get<UsuarioResponse>('/api/auth/me'),
};
