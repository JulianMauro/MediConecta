import { useEffect, type ReactNode } from 'react';
import { createPortal } from 'react-dom';

interface Props {
	title: string;
	description?: string;
	onClose: () => void;
	children: ReactNode;
	/** Botones de acción (confirmar/cancelar). Si no se pasa, el modal solo tiene la X para cerrar. */
	footer?: ReactNode;
}

/**
 * Modal genérico: título + descripción de qué es esta acción, contenido libre, footer de acciones.
 * Cierra con Escape, con click en el fondo, o con la X — siempre alguna forma obvia de salir sin guardar.
 */
export function Modal({ title, description, onClose, children, footer }: Props) {
	useEffect(() => {
		function onKeyDown(e: KeyboardEvent) {
			if (e.key === 'Escape') onClose();
		}
		document.addEventListener('keydown', onKeyDown);
		return () => document.removeEventListener('keydown', onKeyDown);
	}, [onClose]);

	return createPortal(
		<div className="modal-fondo" onClick={onClose}>
			<div className="modal-caja" role="dialog" aria-modal="true" aria-labelledby="modal-titulo" onClick={(e) => e.stopPropagation()}>
				<div className="modal-encabezado">
					<div>
						<h2 id="modal-titulo">{title}</h2>
						{description && <p className="modal-descripcion">{description}</p>}
					</div>
					<button type="button" className="modal-cerrar" onClick={onClose} aria-label="Cerrar">
						✕
					</button>
				</div>
				<div className="modal-contenido">{children}</div>
				{footer && <div className="modal-footer">{footer}</div>}
			</div>
		</div>,
		document.body,
	);
}
