import type { ReactNode } from 'react';

interface Props {
	label: string;
	helpText?: string;
	required?: boolean;
	htmlFor?: string;
	children: ReactNode;
}

/** Campo de formulario con label de verdad (no placeholder) + texto de ayuda explicando qué es. */
export function Field({ label, helpText, required, htmlFor, children }: Props) {
	return (
		<div className="campo">
			<label htmlFor={htmlFor}>
				{label}
				{required && <span className="campo-requerido"> *</span>}
			</label>
			{children}
			{helpText && <p className="campo-ayuda">{helpText}</p>}
		</div>
	);
}
