import { useEffect, useState } from 'react';
import { viajesApi } from '../api/viajes';
import { UnlockForm } from '../components/home/UnlockForm';
import { BikeModeCard } from '../components/home/BikeModeCard';
import type { ViajeResponse } from '../types/dto';

export function HomePage() {
	const [viaje, setViaje] = useState<ViajeResponse | null>(null);
	const [cargando, setCargando] = useState(true);
	const [ultimoResultado, setUltimoResultado] = useState<ViajeResponse | null>(null);

	useEffect(() => {
		viajesApi
			.obtenerActual()
			.then(setViaje)
			.finally(() => setCargando(false));
	}, []);

	function onDevuelta(viajeFinalizado: ViajeResponse) {
		setViaje(null);
		setUltimoResultado(viajeFinalizado);
	}

	if (cargando) return <p className="centrado">Cargando...</p>;

	return (
		<div className="pantalla-centrada">
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
					<UnlockForm onDesbloqueada={setViaje} />
				</div>
			)}
		</div>
	);
}
