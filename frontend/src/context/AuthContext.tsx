import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { authApi } from '../api/auth';
import { getAccessToken, guardarTokens, limpiarTokens } from '../api/client';
import type { LoginRequest, RegisterRequest, UsuarioResponse } from '../types/dto';

interface AuthContextValue {
	usuario: UsuarioResponse | null;
	cargando: boolean;
	login: (req: LoginRequest) => Promise<UsuarioResponse>;
	register: (req: RegisterRequest) => Promise<UsuarioResponse>;
	logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
	const [usuario, setUsuario] = useState<UsuarioResponse | null>(null);
	const [cargando, setCargando] = useState(true);

	// Al montar, si hay un access token guardado, valida la sesion contra /api/auth/me.
	useEffect(() => {
		if (!getAccessToken()) {
			setCargando(false);
			return;
		}
		authApi
			.me()
			.then(setUsuario)
			.catch(() => limpiarTokens())
			.finally(() => setCargando(false));
	}, []);

	async function login(req: LoginRequest) {
		const auth = await authApi.login(req);
		guardarTokens(auth);
		setUsuario(auth.usuario);
		return auth.usuario;
	}

	async function register(req: RegisterRequest) {
		const auth = await authApi.register(req);
		guardarTokens(auth);
		setUsuario(auth.usuario);
		return auth.usuario;
	}

	function logout() {
		limpiarTokens();
		setUsuario(null);
	}

	return (
		<AuthContext.Provider value={{ usuario, cargando, login, register, logout }}>
			{children}
		</AuthContext.Provider>
	);
}

export function useAuth(): AuthContextValue {
	const ctx = useContext(AuthContext);
	if (!ctx) throw new Error('useAuth debe usarse dentro de un AuthProvider');
	return ctx;
}
