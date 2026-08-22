import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { Rol } from '../types/dto';

interface Props {
	children: ReactNode;
	rolRequerido?: Rol;
}

/** Bloquea el acceso si no hay sesion, o si la ruta exige un rol que el usuario no tiene. */
export function ProtectedRoute({ children, rolRequerido }: Props) {
	const { usuario, cargando } = useAuth();

	if (cargando) return <p className="centrado">Cargando...</p>;
	if (!usuario) return <Navigate to="/login" replace />;
	if (rolRequerido && usuario.rol !== rolRequerido) return <Navigate to="/" replace />;

	return <>{children}</>;
}
