import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function Nav() {
	const { usuario, logout } = useAuth();
	if (!usuario) return null;

	return (
		<nav className="nav">
			<span className="marca">PeopleBikes</span>
			<NavLink to="/" end>
				Inicio
			</NavLink>
			<NavLink to="/cuenta">Mi cuenta</NavLink>
			{usuario.rol === 'ADMIN' && <NavLink to="/admin">Admin</NavLink>}
			<span className="separador" />
			<span>
				{usuario.nombre} ({usuario.rol})
			</span>
			<button className="boton-chico" onClick={logout}>
				Salir
			</button>
		</nav>
	);
}
