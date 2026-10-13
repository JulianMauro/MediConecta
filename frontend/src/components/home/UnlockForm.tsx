import { lazy, Suspense, useCallback, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { viajesApi } from '../../api/viajes';
import { ApiError } from '../../api/client';
import type { ViajeResponse } from '../../types/dto';
import { IconoEscanear } from '../ui/Iconos';

// El lector de QR arrastra ~500 kB (zxing). El uso real es un teléfono en la
// calle: no se descarga hasta que el usuario toca "Escanear".
const QrScanner = lazy(() => import('./QrScanner').then((m) => ({ default: m.QrScanner })));

interface Props {
	onDesbloqueada: (viaje: ViajeResponse) => void;
}

export function UnlockForm({ onDesbloqueada }: Props) {
	const [codigo, setCodigo] = useState('');
	const [error, setError] = useState<string | null>(null);
	const [sinPlan, setSinPlan] = useState(false);
	const [enviando, setEnviando] = useState(false);
	const [escaneando, setEscaneando] = useState(false);

	const desbloquear = useCallback(
		async (valor: string) => {
			const limpio = valor.trim();
			if (!limpio) return;
			setError(null);
			setSinPlan(false);
			setEnviando(true);
			try {
				const viaje = await viajesApi.iniciar({ codigoBicicleta: limpio });
				onDesbloqueada(viaje);
			} catch (err) {
				if (err instanceof ApiError) {
					setError(err.message);
					setSinPlan(err.status === 403 && err.message.includes('plan'));
				} else {
					setError('no se pudo desbloquear la bici');
				}
			} finally {
				setEnviando(false);
			}
		},
		[onDesbloqueada],
	);

	// El QR codifica el mismo string que se tipea: llena el input y dispara el
	// mismo envío. El backend nunca se entera de que hubo un QR.
	const onLeido = useCallback(
		(texto: string) => {
			setEscaneando(false);
			setCodigo(texto);
			void desbloquear(texto);
		},
		[desbloquear],
	);

	function onSubmit(e: FormEvent) {
		e.preventDefault();
		void desbloquear(codigo);
	}

	return (
		<form className="tarjeta formulario formulario-desbloqueo" onSubmit={onSubmit}>
			<h2>Desbloquear bici</h2>
			<p>Ingresá el código que tiene la bici, o escaneá su QR, para empezar tu viaje.</p>
			{/*
			 * En el celular escanear es LA accion, como en Citi Bike, Lime o BA Ecobici:
			 * la camara ya esta en la mano y el QR pegado en el caño. Tipear el codigo
			 * queda como alternativa debajo. En escritorio no se ve: ahi sigue el boton
			 * chico al lado del campo, que es el que tiene sentido con una webcam.
			 */}
			<button
				type="button"
				className="boton-escanear solo-movil"
				onClick={() => setEscaneando((v) => !v)}
				aria-expanded={escaneando}
			>
				<IconoEscanear />
				{escaneando ? 'Cerrar la cámara' : 'Escanear el QR de la bici'}
			</button>
			<p className="separador-o solo-movil" aria-hidden="true">
				o ingresá el código
			</p>
			<label>
				Código de la bici
				<div className="fila-codigo">
					{/* Atributos para el teclado del telefono: mayusculas, sin autocorrector
					    (que convierte BICI en "Bicicleta") y la tecla de enviar dice "Ir". */}
					<input
						value={codigo}
						onChange={(e) => setCodigo(e.target.value)}
						placeholder="BICI-001"
						required
						autoComplete="off"
						autoCapitalize="characters"
						autoCorrect="off"
						spellCheck={false}
						enterKeyHint="go"
					/>
					<button
						type="button"
						className="boton-secundario solo-escritorio"
						onClick={() => setEscaneando((v) => !v)}
						aria-expanded={escaneando}
					>
						{escaneando ? 'Cancelar' : 'Escanear'}
					</button>
				</div>
			</label>
			{escaneando && (
				<Suspense fallback={<p className="ayuda">Abriendo la cámara…</p>}>
					<QrScanner onLeido={onLeido} onCerrar={() => setEscaneando(false)} />
				</Suspense>
			)}
			{error && (
				<p className="error">
					{error}
					{sinPlan && (
						<>
							{' '}
							<Link to="/cuenta">Contratá un plan acá.</Link>
						</>
					)}
				</p>
			)}
			<button type="submit" disabled={enviando}>
				{enviando ? 'Desbloqueando...' : 'Desbloquear'}
			</button>
		</form>
	);
}
