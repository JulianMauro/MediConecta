import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { viajesApi } from '../../api/viajes';
import { ApiError } from '../../api/client';
import type { ViajeResponse } from '../../types/dto';

interface Props {
	onDesbloqueada: (viaje: ViajeResponse) => void;
}

export function UnlockForm({ onDesbloqueada }: Props) {
	const [codigo, setCodigo] = useState('');
	const [error, setError] = useState<string | null>(null);
	const [sinPlan, setSinPlan] = useState(false);
	const [enviando, setEnviando] = useState(false);

	async function onSubmit(e: FormEvent) {
		e.preventDefault();
		setError(null);
		setSinPlan(false);
		setEnviando(true);
		try {
			const viaje = await viajesApi.iniciar({ codigoBicicleta: codigo.trim() });
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
	}

	return (
		<form className="tarjeta formulario" onSubmit={onSubmit}>
			<h2>Desbloquear bici</h2>
			<p>Ingresá el código que tiene la bici para empezar tu viaje.</p>
			<label>
				Código de la bici
				<input value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="BICI-001" required />
			</label>
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
