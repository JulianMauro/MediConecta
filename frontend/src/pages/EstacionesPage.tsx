import { useId, useMemo, useState } from 'react';
import { useEstaciones, type EstacionConDisponibilidad } from '../hooks/useEstaciones';
import { plural } from '../lib/plural';

/** Barra de disponibilidad: la proporcion se ve, pero el dato real siempre esta en texto al lado. */
function BarraDisponibilidad({ estacion }: { estacion: EstacionConDisponibilidad }) {
	const total = estacion.anclajes.length || 1;
	const pct = (n: number) => `${(n / total) * 100}%`;

	return (
		<div className="barra" aria-hidden="true">
			<span className="barra-bicis" style={{ width: pct(estacion.bicisDisponibles) }} />
			<span className="barra-libres" style={{ width: pct(estacion.anclajesLibres) }} />
			<span className="barra-fuera" style={{ width: pct(estacion.fueraServicio) }} />
		</div>
	);
}

function TarjetaEstacion({ estacion }: { estacion: EstacionConDisponibilidad }) {
	const [abierta, setAbierta] = useState(false);
	const panelId = useId();
	const sinBicis = estacion.bicisDisponibles === 0;
	const sinLugar = estacion.anclajesLibres === 0;

	return (
		<li className="tarjeta estacion-card">
			<h2 className="estacion-nombre">{estacion.nombre}</h2>
			<p className="estacion-direccion">{estacion.direccion}</p>

			<BarraDisponibilidad estacion={estacion} />

			{/* El numero es el dato; el color solo lo acompaña (WCAG 1.4.1). */}
			<dl className="estacion-cifras">
				<div>
					<dt>Bicis para sacar</dt>
					<dd className={sinBicis ? 'cifra cifra-cero' : 'cifra'}>{estacion.bicisDisponibles}</dd>
				</div>
				<div>
					<dt>Lugares para devolver</dt>
					<dd className={sinLugar ? 'cifra cifra-cero' : 'cifra'}>{estacion.anclajesLibres}</dd>
				</div>
				<div>
					<dt>Fuera de servicio</dt>
					<dd className="cifra cifra-suave">{estacion.fueraServicio}</dd>
				</div>
			</dl>

			{(sinBicis || sinLugar) && (
				<p className="aviso aviso-alerta">
					{sinBicis && sinLugar
						? 'Esta estación está fuera de servicio: no hay bicis ni lugares libres.'
						: sinBicis
							? 'No hay bicis disponibles en esta estación.'
							: 'No hay lugares libres: no vas a poder devolver acá.'}
				</p>
			)}

			<button
				type="button"
				className="boton-secundario boton-chico"
				aria-expanded={abierta}
				aria-controls={panelId}
				onClick={() => setAbierta((v) => !v)}
			>
				{abierta ? 'Ocultar anclajes' : `Ver ${plural(estacion.anclajes.length, 'anclaje', 'anclajes')}`}
			</button>

			{abierta && (
				<ul className="chips" id={panelId}>
					{estacion.anclajes.map((a) => (
						<li key={a.id} className={`chip chip-${a.estado.toLowerCase()}`}>
							<span className="solo-lectores">Anclaje </span>
							{a.numero}
							<span className="solo-lectores">
								: {a.estado === 'OCUPADO' ? 'con bici' : a.estado === 'LIBRE' ? 'libre' : 'fuera de servicio'}
							</span>
						</li>
					))}
				</ul>
			)}
		</li>
	);
}

export function EstacionesPage() {
	const { estaciones, cargando, error, recargar } = useEstaciones();
	const [busqueda, setBusqueda] = useState('');
	const [soloConBicis, setSoloConBicis] = useState(false);
	const buscadorId = useId();

	const visibles = useMemo(() => {
		const q = busqueda.trim().toLowerCase();
		return estaciones.filter((e) => {
			const coincide = !q || e.nombre.toLowerCase().includes(q) || e.direccion.toLowerCase().includes(q);
			return coincide && (!soloConBicis || e.bicisDisponibles > 0);
		});
	}, [estaciones, busqueda, soloConBicis]);

	const totalBicis = estaciones.reduce((n, e) => n + e.bicisDisponibles, 0);

	return (
		<div className="contenido">
			<header className="encabezado-pagina">
				<h1>Estaciones y disponibilidad</h1>
				<p className="bajada">
					Podés sacar una bici de cualquier estación y devolverla en cualquier otra que tenga un anclaje libre.
				</p>
			</header>

			{!cargando && !error && (
				<p className="resumen-red">
					<strong>{estaciones.length}</strong> {estaciones.length === 1 ? 'estación activa' : 'estaciones activas'} ·{' '}
					<strong>{totalBicis}</strong> {totalBicis === 1 ? 'bici disponible' : 'bicis disponibles'} ahora
				</p>
			)}

			<div className="fila filtros">
				<div className="campo campo-inline">
					<label htmlFor={buscadorId}>Buscar estación</label>
					<input
						id={buscadorId}
						type="search"
						value={busqueda}
						onChange={(e) => setBusqueda(e.target.value)}
						placeholder="Nombre o dirección"
					/>
				</div>
				<label className="check">
					<input type="checkbox" checked={soloConBicis} onChange={(e) => setSoloConBicis(e.target.checked)} />
					Solo estaciones con bicis
				</label>
				<button type="button" className="boton-secundario" onClick={recargar}>
					Actualizar
				</button>
			</div>

			<p className="ayuda" aria-live="polite">
				{cargando ? 'Cargando estaciones…' : `${plural(visibles.length, 'estación', 'estaciones')} en pantalla`}
			</p>

			{error && <p className="aviso aviso-alerta">{error}</p>}

			{!cargando && visibles.length === 0 && !error && (
				<p className="centrado">No hay estaciones que coincidan con la búsqueda.</p>
			)}

			<ul className="grilla lista-estaciones">
				{visibles.map((e) => (
					<TarjetaEstacion key={e.id} estacion={e} />
				))}
			</ul>
		</div>
	);
}
