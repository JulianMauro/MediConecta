import { useEffect, useRef, useState } from 'react';
import { BrowserQRCodeReader } from '@zxing/browser';
import { DecodeHintType } from '@zxing/library';

interface Props {
	/** Se llama una sola vez, con el texto del primer QR que engancha. */
	onLeido: (texto: string) => void;
	onCerrar: () => void;
}

// TRY_HARDER: el caso real es un webcam mirando la pantalla de un teléfono —
// reflejo, foco, moiré. Sin esto el decodificador falla en cuadros que un
// lector de celu engancharía.
const HINTS = new Map([[DecodeHintType.TRY_HARDER, true]]);

/**
 * Abre la cámara, lee un QR y devuelve su texto por callback.
 *
 * El loop de lectura es propio (un frame cada 250 ms al decodificador de zxing)
 * en vez de su escáner continuo: ese, ante cualquier error que no sea "no
 * encontré un QR", corta la cámara sin invocar el callback — se ve como que el
 * escáner "no hace nada".
 *
 * Se usa `@zxing/*` y no `BarcodeDetector` nativo porque este último no existe
 * en Safari iOS ni en Firefox, y el uso real es un teléfono en la calle.
 */
export function QrScanner({ onLeido, onCerrar }: Props) {
	const video = useRef<HTMLVideoElement>(null);
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		let stream: MediaStream | null = null;
		let timer: number | undefined;
		let cancelado = false;
		let leido = false;

		// Apaga el stream: sin esto la cámara queda prendida después de cerrar.
		function apagar() {
			cancelado = true;
			window.clearTimeout(timer);
			stream?.getTracks().forEach((t) => t.stop());
			stream = null;
		}

		async function arrancar() {
			// getUserMedia solo existe en contextos seguros: https:// o localhost. Por
			// HTTP contra la IP de la PC ni aparece.
			if (!navigator.mediaDevices?.getUserMedia) {
				setError(
					'El navegador no da acceso a la cámara en esta página. Suele pasar cuando se abre por HTTP y no por https:// o localhost. Tipeá el código a mano.',
				);
				return;
			}

			try {
				stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
			} catch (err) {
				const denegado =
					err instanceof DOMException && (err.name === 'NotAllowedError' || err.name === 'SecurityError');
				setError(
					denegado
						? 'No diste permiso para usar la cámara. Habilitalo en el navegador o tipeá el código a mano.'
						: 'No se pudo abrir la cámara. Revisá que no la esté usando otra app, o tipeá el código a mano.',
				);
				return;
			}

			if (cancelado || !video.current) {
				apagar();
				return;
			}

			video.current.srcObject = stream;
			try {
				await video.current.play();
			} catch {
				// Autoplay bloqueado: el <video> igual se ve y seguimos leyendo frames.
			}

			const lector = new BrowserQRCodeReader(HINTS);
			const lienzo = document.createElement('canvas');
			const ctx = lienzo.getContext('2d', { willReadFrequently: true });

			function tick() {
				const v = video.current;
				if (!cancelado && !leido && ctx && v && v.readyState >= 2 && v.videoWidth > 0) {
					lienzo.width = v.videoWidth;
					lienzo.height = v.videoHeight;
					ctx.drawImage(v, 0, 0);
					try {
						const texto = lector.decodeFromCanvas(lienzo).getText();
						if (texto) {
							leido = true;
							apagar();
							onLeido(texto);
							return;
						}
					} catch {
						// Ningún QR en este frame: probamos con el siguiente.
					}
				}
				if (!cancelado && !leido) timer = window.setTimeout(tick, 250);
			}

			tick();
		}

		void arrancar();
		return apagar;
	}, [onLeido]);

	return (
		<div className="qr-scanner">
			{error ? (
				<p className="aviso aviso-alerta">{error}</p>
			) : (
				<video
					ref={video}
					className="qr-scanner-video"
					autoPlay
					muted
					playsInline
					aria-label="Vista de la cámara para escanear el QR"
				/>
			)}
			<button type="button" className="boton-secundario" onClick={onCerrar}>
				Cerrar
			</button>
		</div>
	);
}
