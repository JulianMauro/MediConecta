import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { RealtimeUsers } from './home/RealtimeUsers';

export function Nav() {
	const { usuario, logout } = useAuth();
	if (!usuario) return null;

	return (
		<nav className="nav" aria-label="Navegación principal">
			<span className="marca">
				<span className="marca-punto" aria-hidden="true" />
				PeopleBikes
			</span>
			<NavLink to="/" end>
				Inicio
			</NavLink>
			<NavLink to="/estaciones">Estaciones</NavLink>
			<NavLink to="/planes">Planes</NavLink>
			<NavLink to="/cuenta">Mi cuenta</NavLink>
			{usuario.rol === 'ADMIN' && <NavLink to="/admin">Admin</NavLink>}
			<span className="separador" />
			{/* Visible para cualquier usuario logueado, en todas las páginas. */}
			<RealtimeUsers />
			<span className="nav-usuario">
				<strong>{usuario.nombre}</strong>
				{usuario.rol === 'ADMIN' ? 'Administrador' : 'Usuario'}
			</span>
			<button type="button" className="boton-secundario boton-chico" onClick={logout}>
				Salir
			</button>
		</nav>
	);
}
