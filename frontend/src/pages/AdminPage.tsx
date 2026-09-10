import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';

export interface AdminContexto {
	/** Se incrementa para forzar un refetch en las secciones que dependen de listas de otras. */
	refrescarSenal: number;
	notificarCambio: () => void;
}

/**
 * Las seis secciones agrupadas por lo que administran, y dentro de cada grupo
 * en el orden en que se usan de verdad.
 *
 * Antes eran una lista plana en el orden en que se fueron programando, y el
 * salto de "Movimientos" a "Membresías" a "Generar QR" no lo explicaba nada.
 * Los QR son stickers que se pegan en las bicis, asi que pertenecen a Flota y
 * no a un cajon de herramientas sueltas; Membresías queda sola porque es el
 * unico rubro comercial, y eso es informacion, no un hueco a rellenar.
 */
const GRUPOS = [
	{
		titulo: 'Infraestructura',
		secciones: [
			{ to: 'estaciones', label: 'Estaciones', desc: 'Altas, anclajes y bajas' },
			{ to: 'almacenes', label: 'Almacenes', desc: 'Depósitos de la flota' },
		],
	},
	{
		titulo: 'Flota',
		secciones: [
			{ to: 'flota', label: 'Bicicletas', desc: 'Estado y ubicación' },
			{ to: 'movimientos', label: 'Movimientos', desc: 'Traslados en lote' },
			{ to: 'qr', label: 'Generar QR', desc: 'Stickers para las bicis' },
		],
	},
	{
		titulo: 'Comercial',
		secciones: [{ to: 'membresias', label: 'Membresías', desc: 'Planes que se ofrecen' }],
	},
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
				{GRUPOS.map((g) => (
					<section key={g.titulo} className="admin-sidebar-grupo">
						{/* aria-labelledby y no aria-label: el titulo del grupo ya esta
						    escrito, y asi el lector de pantalla anuncia el mismo texto
						    que se ve en pantalla. */}
						<h2 id={`grupo-${g.titulo}`} className="admin-sidebar-titulo">
							{g.titulo}
						</h2>
						<ul aria-labelledby={`grupo-${g.titulo}`}>
							{g.secciones.map((s) => (
								<li key={s.to}>
									<NavLink to={s.to}>
										<span className="admin-sidebar-label">{s.label}</span>
										<span className="admin-sidebar-desc">{s.desc}</span>
									</NavLink>
								</li>
							))}
						</ul>
					</section>
				))}
			</nav>

			<div className="admin-panel">
				<Outlet context={contexto} />
			</div>
		</div>
	);
}
