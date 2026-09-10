import { useEffect, useState } from 'react';
import { viajesApi } from '../api/viajes';
import { UnlockForm } from '../components/home/UnlockForm';
import { BikeModeCard } from '../components/home/BikeModeCard';
import type { ViajeResponse } from '../types/dto';

/** Tiene que coincidir con desbloqueo-dispersar en index.css. */
const MS_DISPERSION = 620;

/**
 * Con movimiento reducido no se espera nada: el cambio es inmediato.
 * Se consulta al momento de desbloquear y no en un estado, porque la preferencia
 * puede cambiar mientras la pestaña esta abierta.
 */
function prefiereQuieto(): boolean {
	return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

export function HomePage() {
	const [viaje, setViaje] = useState<ViajeResponse | null>(null);
	const [cargando, setCargando] = useState(true);
	const [ultimoResultado, setUltimoResultado] = useState<ViajeResponse | null>(null);
	/*
	 * El viaje recien creado espera aca mientras el formulario se dispersa. Si se
	 * asignara directo a "viaje", React desmontaria el formulario en el acto y la
	 * animacion de salida no llegaria a verse: no se puede animar lo que ya no esta
	 * en el DOM.
	 */
	const [enTransito, setEnTransito] = useState<ViajeResponse | null>(null);

	useEffect(() => {
		viajesApi
			.obtenerActual()
			.then(setViaje)
			.finally(() => setCargando(false));
	}, []);

	useEffect(() => {
		if (!enTransito) return;
		const t = setTimeout(() => {
			setViaje(enTransito);
			setEnTransito(null);
		}, MS_DISPERSION);
		return () => clearTimeout(t);
	}, [enTransito]);

	function onDesbloqueada(nuevo: ViajeResponse) {
		if (prefiereQuieto()) {
			setViaje(nuevo);
			return;
		}
		setEnTransito(nuevo);
	}

	function onDevuelta(viajeFinalizado: ViajeResponse) {
		setViaje(null);
		setUltimoResultado(viajeFinalizado);
	}

	if (cargando) return <p className="centrado">Cargando...</p>;

	return (
		<div className="pantalla-centrada">
			{/*
			 * El resultado se anuncia por texto ademas de por la animacion: quien usa
			 * lector de pantalla, o tiene el movimiento desactivado, se entera igual.
			 */}
			<p className="solo-lectores" role="status" aria-live="polite">
				{enTransito || viaje ? 'Bici desbloqueada. Empezó tu viaje.' : ''}
			</p>

			{/* La onda vive fuera de la tarjeta: sale del centro de la pantalla, no de una caja. */}
			{enTransito && <div className="desbloqueo-onda" aria-hidden="true" />}

			{viaje ? (
				<BikeModeCard viaje={viaje} onDevuelta={onDevuelta} />
			) : (
				<div aria-live="polite">
					{ultimoResultado && (
						<p className={`aviso ${ultimoResultado.excedioTiempo ? 'aviso-alerta' : 'aviso-ok'}`}>
							Bici devuelta. Viaje de {ultimoResultado.duracionMinutos} min.
							{ultimoResultado.excedioTiempo && ' Excediste el tiempo permitido: revisá tus pagos en "Mi cuenta".'}
						</p>
					)}
					<div className={enTransito ? 'desbloqueo-saliendo' : undefined}>
						<UnlockForm onDesbloqueada={onDesbloqueada} />
					</div>
				</div>
			)}
		</div>
	);
}
