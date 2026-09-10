import { useEffect, useRef } from 'react';
import QRCode from 'qrcode';

interface Props {
	/** El string que codifica el QR: el código pelado de la bici (ej: "BICI-001"). */
	valor: string;
	/** Lado del QR en píxeles. */
	tamanio?: number;
}

/**
 * Envoltorio de React alrededor de la escritura imperativa de `qrcode` sobre un
 * <canvas>. El canvas es un objeto mutable ajeno a React: vive en un ref (no se
 * dibuja en el JSX) y se redibuja en un efecto keyeado solo por lo que cambia el
 * dibujo — el valor y el tamaño —, no en cada render.
 */
export function QrCanvas({ valor, tamanio = 200 }: Props) {
	const canvas = useRef<HTMLCanvasElement>(null);

	useEffect(() => {
		if (!canvas.current) return;
		// margin 2 es el "quiet zone" mínimo que pide el estándar para que un lector
		// enganche el código. Un string ASCII corto como un código de bici nunca
		// desborda la capacidad del QR, así que un fallo acá no es un caso real.
		QRCode.toCanvas(canvas.current, valor, { width: tamanio, margin: 2 }).catch(() => {});
	}, [valor, tamanio]);

	return (
		<canvas
			ref={canvas}
			className="qr-canvas"
			width={tamanio}
			height={tamanio}
			role="img"
			aria-label={`Código QR de ${valor}`}
		/>
	);
}
