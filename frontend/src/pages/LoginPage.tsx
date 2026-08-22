import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../api/client';

export function LoginPage() {
	const { login } = useAuth();
	const navigate = useNavigate();
	const [email, setEmail] = useState('');
	const [password, setPassword] = useState('');
	const [error, setError] = useState<string | null>(null);
	const [enviando, setEnviando] = useState(false);

	async function onSubmit(e: FormEvent) {
		e.preventDefault();
		setError(null);
		setEnviando(true);
		try {
			const usuario = await login({ email, password });
			navigate(usuario.rol === 'ADMIN' ? '/admin' : '/');
		} catch (err) {
			setError(err instanceof ApiError ? err.message : 'no se pudo iniciar sesion');
		} finally {
			setEnviando(false);
		}
	}

	return (
		<div className="pantalla-centrada">
			<form className="tarjeta formulario" onSubmit={onSubmit}>
				<h1>PeopleBikes</h1>
				<label>
					Email
					<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
				</label>
				<label>
					Password
					<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
				</label>
				{error && <p className="error">{error}</p>}
				<button type="submit" disabled={enviando}>
					{enviando ? 'Ingresando...' : 'Ingresar'}
				</button>
				<p>
					¿No tenés cuenta? <Link to="/register">Registrate</Link>
				</p>
			</form>
		</div>
	);
}
