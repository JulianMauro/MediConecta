import type { AuthResponse } from '../types/dto';

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

const ACCESS_TOKEN_KEY = 'peoplebikes.accessToken';
const REFRESH_TOKEN_KEY = 'peoplebikes.refreshToken';

export function getAccessToken(): string | null {
	return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
	return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function guardarTokens(auth: AuthResponse): void {
	localStorage.setItem(ACCESS_TOKEN_KEY, auth.accessToken);
	localStorage.setItem(REFRESH_TOKEN_KEY, auth.refreshToken);
}

export function limpiarTokens(): void {
	localStorage.removeItem(ACCESS_TOKEN_KEY);
	localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export class ApiError extends Error {
	status: number;

	constructor(status: number, mensaje: string) {
		super(mensaje);
		this.status = status;
	}
}

/** Evita disparar varios refresh en paralelo cuando varios requests pegan 401 al mismo tiempo. */
let refrescoEnCurso: Promise<boolean> | null = null;

async function intentarRefresh(): Promise<boolean> {
	const refreshToken = getRefreshToken();
	if (!refreshToken) {
		return false;
	}

	if (!refrescoEnCurso) {
		refrescoEnCurso = fetch(`${BASE_URL}/api/auth/refresh`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ refreshToken }),
		})
			.then(async (res) => {
				if (!res.ok) return false;
				const auth = (await res.json()) as AuthResponse;
				guardarTokens(auth);
				return true;
			})
			.catch(() => false)
			.finally(() => {
				refrescoEnCurso = null;
			});
	}

	return refrescoEnCurso;
}

interface Opciones {
	body?: unknown;
	autenticado?: boolean;
}

/** Wrapper unico de fetch: agrega el JSON, el Authorization header, y reintenta una vez si el access token vencio. */
async function request<T>(method: string, path: string, opciones: Opciones = {}, reintentar = true): Promise<T> {
	const { body, autenticado = true } = opciones;

	const headers: Record<string, string> = { 'Content-Type': 'application/json' };
	if (autenticado) {
		const token = getAccessToken();
		if (token) headers.Authorization = `Bearer ${token}`;
	}

	const res = await fetch(`${BASE_URL}${path}`, {
		method,
		headers,
		body: body !== undefined ? JSON.stringify(body) : undefined,
	});

	if (res.status === 401 && autenticado && reintentar) {
		const refrescado = await intentarRefresh();
		if (refrescado) {
			return request<T>(method, path, opciones, false);
		}
		limpiarTokens();
		throw new ApiError(401, 'sesion vencida, iniciá sesión de nuevo');
	}

	if (res.status === 204) {
		return undefined as T;
	}

	const texto = await res.text();
	const data = texto ? JSON.parse(texto) : undefined;

	if (!res.ok) {
		const mensaje = (data && (data.message ?? data.error)) || `error ${res.status}`;
		throw new ApiError(res.status, mensaje);
	}

	return data as T;
}

export const api = {
	get: <T>(path: string, opciones?: Opciones) => request<T>('GET', path, opciones),
	post: <T>(path: string, body?: unknown, opciones?: Opciones) => request<T>('POST', path, { ...opciones, body }),
	put: <T>(path: string, body?: unknown, opciones?: Opciones) => request<T>('PUT', path, { ...opciones, body }),
	patch: <T>(path: string, body?: unknown, opciones?: Opciones) => request<T>('PATCH', path, { ...opciones, body }),
	del: <T>(path: string, opciones?: Opciones) => request<T>('DELETE', path, opciones),
};
