import { useLayoutEffect, useRef, useState, type CSSProperties } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { RealtimeUsers } from './home/RealtimeUsers';

/**
 * Pestañas del cliente, en el orden del recorrido real y no en el que fueron
 * apareciendo: primero desbloquear una bici (Inicio), después encontrar dónde
 * hay una libre o dónde dejarla (Mapa como respuesta espacial, Estaciones como
 * el detalle en lista de lo mismo), y al final lo administrativo de la cuenta.
 *
 * Admin no está en esta lista a proposito: no es una pestaña mas del recorrido
 * del cliente sino otro rol, con otra escena de uso. Va del lado de la sesion.
 */
const PESTANAS = [
	{ to: '/', label: 'Inicio', end: true },
	{ to: '/mapa', label: 'Mapa', end: false },
	{ to: '/estaciones', label: 'Estaciones', end: false },
	{ to: '/planes', label: 'Planes', end: false },
	{ to: '/cuenta', label: 'Mi cuenta', end: false },
];

/**
 * Mide la pestaña activa para el indicador que se desliza detras de ella.
 *
 * Se mide en el DOM en lugar de calcularse: los labels tienen anchos distintos
 * (dependen de la fuente ya cargada) y en mobile el rail puede estar scrolleado.
 * useLayoutEffect y no useEffect para que la primera medicion ocurra antes del
 * pintado — si no, el indicador aparece en 0 y salta a su lugar.
 */
function useIndicador(pathname: string) {
	const railRef = useRef<HTMLDivElement>(null);
	const [caja, setCaja] = useState<{ x: number; ancho: number } | null>(null);

	useLayoutEffect(() => {
		const rail = railRef.current;
		if (!rail) return;

		function medir() {
			const activa = rail!.querySelector<HTMLAnchorElement>('a.active');
			if (!activa) {
				setCaja(null);
				return;
			}
			const izq = activa.offsetLeft;
			setCaja({ x: izq, ancho: activa.offsetWidth });

			// En mobile el rail scrollea en horizontal: si la activa quedo fuera de
			// la vista, se la trae. Sin esto se puede navegar a una pestaña que no
			// se ve, y la barra parece no tener seleccion.
			const der = izq + activa.offsetWidth;
			if (izq < rail!.scrollLeft || der > rail!.scrollLeft + rail!.clientWidth) {
				const suave = !window.matchMedia('(prefers-reduced-motion: reduce)').matches;
				rail!.scrollTo({ left: izq - 16, behavior: suave ? 'smooth' : 'auto' });
			}
		}

		medir();
		// El ancho de las pestañas cambia al rotar el telefono y al cargar la fuente
		// display; sin observer el indicador queda desfasado del texto.
		const observador = new ResizeObserver(medir);
		observador.observe(rail);
		return () => observador.disconnect();
	}, [pathname]);

	return { railRef, caja };
}

export function Nav() {
	const { usuario, logout } = useAuth();
	const { pathname } = useLocation();
	const { railRef, caja } = useIndicador(pathname);

	if (!usuario) return null;

	const esAdmin = usuario.rol === 'ADMIN';

	return (
		<nav className="nav" aria-label="Navegación principal">
			<span className="marca">
				<span className="marca-punto" aria-hidden="true" />
				PeopleBikes
			</span>

			{/*
			 * Las pestañas viven en su propio rail y no sueltas en la nav: asi el
			 * indicador tiene un contenedor contra el cual posicionarse, y en mobile
			 * el grupo scrollea entero en vez de desarmarse en filas impredecibles.
			 */}
			<div className="nav-rail" ref={railRef}>
				<span
					className="nav-rail-indicador"
					aria-hidden="true"
					hidden={!caja}
					style={{ '--x': `${caja?.x ?? 0}px`, '--w': `${caja?.ancho ?? 0}px` } as CSSProperties}
				/>
				{PESTANAS.map((p) => (
					<NavLink key={p.to} to={p.to} end={p.end}>
						{p.label}
					</NavLink>
				))}
			</div>

			<span className="separador" />

			{/* Visible para cualquier usuario logueado, en todas las páginas. */}
			<RealtimeUsers />

			{/*
			 * Cluster de sesion: quien sos, y las dos acciones que dependen de eso.
			 * Admin esta aca y no entre las pestañas porque cambiar de rol no es
			 * navegar dentro del mismo recorrido.
			 */}
			{esAdmin && (
				<NavLink to="/admin" className="nav-admin">
					Admin
				</NavLink>
			)}
			<span className="nav-usuario">
				<strong>{usuario.nombre}</strong>
				{esAdmin ? 'Administrador' : 'Usuario'}
			</span>
			<button type="button" className="boton-secundario boton-chico" onClick={logout}>
				Salir
			</button>
		</nav>
	);
}
