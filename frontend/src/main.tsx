import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { aplicarEstiloGuardado } from './lib/estilo'

// Antes de montar React: si se hiciera dentro de un componente, el primer cuadro
// se pintaria con el estilo por defecto y recien despues saltaria al elegido.
aplicarEstiloGuardado()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
