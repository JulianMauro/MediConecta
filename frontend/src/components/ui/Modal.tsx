import { useEffect, useId, useRef, useState, type ReactNode } from 'react';
import { createPortal } from 'react-dom';

interface Props {
	title: string;
	description?: string;
	onClose: () => void;
	children: ReactNode;
	/** Botones de acción (confirmar/cancelar). Si no se pasa, el modal solo tiene la X para cerrar. */
	footer?: ReactNode;
}

const FOCUSABLES =
	'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * Modal generico: titulo + descripcion de que es esta accion, contenido libre, footer de acciones.
 * Cierra con Escape, con click en el fondo, o con la X — siempre alguna forma obvia de salir sin guardar.
 *
 * Accesibilidad: el foco entra al abrir, queda atrapado adentro mientras esta abierto
 * (Tab y Shift+Tab circulan), y vuelve al elemento que lo abrio al cerrar.
 */
export function Modal({ title, description, onClose, children, footer }: Props) {
	const cajaRef = useRef<HTMLDivElement>(null);
	const tituloId = useId();
	const descripcionId = useId();

	/*
	 * Quien abrio el modal, capturado en el primer render y no en el efecto:
	 * si el contenido trae un campo con autoFocus, ese campo ya se llevo el foco
	 * para cuando corren los efectos, y guardariamos un elemento que esta por desaparecer.
	 */
	const [disparador] = useState<HTMLElement | null>(() => document.activeElement as HTMLElement | null);

	// onClose suele llegar como arrow inline, o sea con identidad nueva en cada render.
	// Si el efecto dependiera de el, se desmontaria y remontaria todo el tiempo: se perderia
	// la referencia al disparador y el foco saltaria fuera del modal mientras se escribe.
	const onCloseRef = useRef(onClose);
	onCloseRef.current = onClose;

	useEffect(() => {
		const caja = cajaRef.current;

		// Si el contenido ya se llevo el foco (autoFocus), se respeta.
		// Si no, el foco va al contenedor: asi se lee el titulo antes que la primera accion.
		if (caja && !caja.contains(document.activeElement)) caja.focus();

		function onKeyDown(e: KeyboardEvent) {
			if (e.key === 'Escape') {
				onCloseRef.current();
				return;
			}
			if (e.key !== 'Tab') return;
			if (!caja) return;
			const focusables = Array.from(caja.querySelectorAll<HTMLElement>(FOCUSABLES)).filter(
				(el) => el.offsetParent !== null,
			);
			if (focusables.length === 0) {
				e.preventDefault();
				caja.focus();
				return;
			}

			const primero = focusables[0];
			const ultimo = focusables[focusables.length - 1];
			const activo = document.activeElement;

			if (e.shiftKey && (activo === primero || activo === caja)) {
				e.preventDefault();
				ultimo.focus();
			} else if (!e.shiftKey && activo === ultimo) {
				e.preventDefault();
				primero.focus();
			}
		}

		document.addEventListener('keydown', onKeyDown);
		return () => {
			document.removeEventListener('keydown', onKeyDown);
			// Solo devolvemos el foco si el disparador sigue en el documento.
			if (disparador?.isConnected) disparador.focus();
		};
	}, [disparador]);

	return createPortal(
		<div className="modal-fondo" onClick={onClose}>
			<div
				ref={cajaRef}
				className="modal-caja"
				role="dialog"
				aria-modal="true"
				aria-labelledby={tituloId}
				aria-describedby={description ? descripcionId : undefined}
				tabIndex={-1}
				onClick={(e) => e.stopPropagation()}
			>
				<div className="modal-encabezado">
					<div>
						<h2 id={tituloId}>{title}</h2>
						{description && (
							<p id={descripcionId} className="modal-descripcion">
								{description}
							</p>
						)}
					</div>
					<button type="button" className="modal-cerrar" onClick={onClose} aria-label="Cerrar">
						{/* SVG dibujado y no el caracter "✕": un glifo de texto cambia de forma,
						    peso y alineacion segun la fuente que resuelva el navegador. */}
						<svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
							<path
								d="M4 4l8 8M12 4l-8 8"
								stroke="currentColor"
								strokeWidth="1.75"
								strokeLinecap="round"
							/>
						</svg>
					</button>
				</div>
				<div className="modal-contenido">{children}</div>
				{footer && <div className="modal-footer">{footer}</div>}
			</div>
		</div>,
		document.body,
	);
}
