import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';

export interface AdminContexto {
	/** Se incrementa para forzar un refetch en las secciones que dependen de listas de otras. */
	refrescarSenal: number;
	notificarCambio: () => void;
}

const SECCIONES = [
	{ to: 'estaciones', label: 'Estaciones', desc: 'Altas, anclajes y bajas' },
	{ to: 'almacenes', label: 'Almacenes', desc: 'Depósitos de la flota' },
	{ to: 'flota', label: 'Flota', desc: 'Bicicletas y su estado' },
	{ to: 'movimientos', label: 'Movimientos', desc: 'Traslados en lote' },
	{ to: 'membresias', label: 'Membresías', desc: 'Planes que se ofrecen' },
];

/**
 * Layout del panel: una seccion por ruta, en vez de las cinco apiladas en una pagina.
 * Solo se monta y consulta la seccion visible.
 */
export function AdminPage() {
	const [refrescarSenal, setRefrescarSenal] = useState(0);
	const notificarCambio = () => setRefrescarSenal((n) => n + 1);
	const contexto: AdminContexto = { refrescarSenal, notificarCambio };

	return (
		<div className="contenido admin-layout">
			<header className="encabezado-pagina admin-titulo">
				<h1>Panel de administración</h1>
			</header>

			<nav className="admin-sidebar" aria-label="Secciones del panel">
				<ul>
					{SECCIONES.map((s) => (
						<li key={s.to}>
							<NavLink to={s.to}>
								<span className="admin-sidebar-label">{s.label}</span>
								<span className="admin-sidebar-desc">{s.desc}</span>
							</NavLink>
						</li>
					))}
				</ul>
			</nav>

			<div className="admin-panel">
				<Outlet context={contexto} />
			</div>
		</div>
	);
}
