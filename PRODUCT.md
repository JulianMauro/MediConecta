# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Dos roles con contextos de uso muy distintos:

- **Cliente** — persona registrada que contrata un plan y hace viajes. Opera de pie, en la calle, con una mano y a veces con sol directo sobre la pantalla: desbloquea una bici (tipeando `BICI-001` o escaneando el QR pegado en el caño), busca una estación con lugar libre para devolverla, y consulta su plan, sus pagos y sus penalizaciones desde el celular.
- **Administrador** — gestiona la infraestructura desde escritorio: estaciones y anclajes, flota, almacenes, traslados, planes y la generadora de QR para imprimir. Trabaja con tablas, formularios y listados largos.
- **Evaluador docente** — audiencia real de esta etapa (ver Operating Context): recorre la app para verificar que los 11 módulos y las reglas de negocio están implementados y son demostrables.

El **Sistema** es un tercer actor sin interfaz: genera cargos, penalizaciones, notificaciones y bloqueos sin intervención humana.

## Product Purpose

Sistema de transporte público en bicicletas compartidas: el usuario retira una bicicleta de una estación y la devuelve en cualquier otra, dentro de los límites del plan que contrató. El alcance funcional completo (58 funciones, F-01 a F-59, agrupadas en 11 módulos) está en [ALCANCE.md](ALCANCE.md) y es la fuente de verdad del dominio.

Éxito en esta etapa = un recorrido demostrable de punta a punta: registrarse → comparar y contratar un plan → pagar → desbloquear una bici → devolverla → y el camino de excepción (exceso de tiempo → penalización → pago → bloqueo a los 3 strikes).

## Positioning

**No hay una propuesta diferencial de producto y no debe inventarse una.** PeopleBikes es deliberadamente un clon didáctico del dominio de bici compartida (Ecobici / Citi Bike / Bicing), modelado con rigor. El valor está en la calidad del modelado y de la ejecución, no en una idea de negocio novedosa.

Consecuencia para el diseño: nada de copy que prometa ventajas competitivas, métricas de adopción, testimonios o partners. La app se defiende por lo que hace funcionar, no por lo que afirma.

## Operating Context

- **Entrega académica** de Desarrollo de Aplicaciones 2 (UADE), por Agustín Jorge Pulido y Julián Argentino Mauro. La audiencia inmediata es docente/evaluadora, no un ciclista real.
- Acompaña una presentación: `PeopleBikes-Presentacion.pptx`, con la estructura de láminas sugerida al final de [ALCANCE.md](ALCANCE.md).
- El backend arranca por `docker compose` y siembra un admin: `admin@mediconecta.com` / `admin1234`. El nombre heredado *MediConecta* sigue vivo en el package Java (`com.mediconecta.*`) y en el README raíz; **el producto se llama PeopleBikes** y ninguna superficie visible debe decir MediConecta.
- Cliente en la calle vs. admin en escritorio son dos escenas de uso reales y separadas; una misma tabla no sirve para las dos.

## Capabilities and Constraints

**Stack (ya resuelto por el código, no es una decisión abierta):** Spring Boot 4 + Spring Data JPA + Maven en el backend; React 19 + TypeScript + Vite 8 + React Router 7 en el frontend, con CSS propio y tokens en `frontend/src/styles/tokens.css` (sin framework de UI ni CSS-in-JS). Dev server: `npm --prefix frontend run dev` en el puerto 5173.

**Superficies actuales:** `/login`, `/register`, `/` (home con desbloqueo), `/estaciones`, `/mapa`, `/planes`, `/cuenta`, y `/admin` con seis secciones anidadas (estaciones, almacenes, flota, movimientos, membresías, qr).

**Reglas de negocio transversales** (ver ALCANCE.md para el detalle):

1. Sin plan vigente no hay viaje.
2. Un viaje a la vez por usuario.
3. Nada se borra: baja lógica en usuarios, estaciones, planes y bicicletas.
4. Toda penalización tiene un pago asociado.
5. Tres penalizaciones sin saldar bloquean la cuenta.
6. Una bicicleta está en un anclaje o en un almacén, nunca en los dos ni en ninguno.
7. Cada cambio de estado relevante deja historial, no un indicador.

**Restricciones técnicas vigentes:**

- **El QR no toca el backend.** Es un método de captura del frontend: el escáner lee un string que *ya es* el código de la bici y llena el mismo campo que hoy se tipea, contra el mismo `POST /api/viajes/iniciar`. Sin columnas, endpoints ni DTOs nuevos. Ver [PLAN-qr-desbloqueo.md](PLAN-qr-desbloqueo.md).
- **Google Maps** requiere dos API keys distintas (backend restringida a Geocoding, frontend restringida por referrer a Maps JavaScript). La del frontend vive en `frontend/.env` — hay `.env.example`. Ver [README-google-maps.md](README-google-maps.md). El mapa debe degradar con dignidad cuando la key falta.
- Los patrones de código del backend son explícitos y estables: entidades sin setters, DTOs `record` con factory `desde(entidad)`, services transaccionales con `ResponseStatusException`. Ver [patrones.md](patrones.md).

## Brand Commitments

- **Nombre: PeopleBikes.** Vinculante en toda superficie visible.
- **Idioma: español rioplatense** en toda la interfaz, el código y los comentarios. Los identificadores del dominio son en español (`Estacion`, `Anclaje`, `Viaje`, `Strike`, `Membresia`, `Almacen`). Esta es la terminología del producto y no se traduce.
- **La identidad visual es el choque, y es deliberado:** rigor de accesibilidad y exceso psicodélico conviven a propósito. La capa decorativa (mesh gradients, fondo de rayas de neón, tarjetas holográficas tipo ticket, gradiente de títulos, el toggle de estilo) es parte de la identidad, no ruido a limpiar.
- **La capa decorativa está siempre subordinada a la legibilidad.** El sistema ya tiene el mecanismo para hacerlo cumplir: la variable maestra `--psico` multiplica todas las opacidades decorativas (0 = app sobria sin tocar una regla más), y `--ticket-velo` regula cuánto tapa el holograma bajo el texto. Ningún efecto nuevo puede saltearse ese control.
- El sistema de diseño vigente está documentado en [frontend/DISENO.md](frontend/DISENO.md) con contrastes medidos. Se preserva salvo pedido explícito de reemplazo.

## Evidence on Hand

- `ALCANCE.md` — alcance funcional completo y verificado contra el código.
- `patrones.md` — patrones de arquitectura del backend.
- `frontend/DISENO.md` — sistema de diseño con contrastes WCAG medidos.
- `PLAN-qr-desbloqueo.md`, `README-google-maps.md` — decisiones de implementación ya tomadas.
- `PeopleBikes-Presentacion.pptx` — la presentación de la entrega.
- Datos de demo sembrados por `docker compose` (usuario admin, bicis `BICI-00N`).

**No existe y no debe fabricarse:** usuarios reales, ciudades operando, métricas de uso, testimonios, prensa, precios de mercado, ni acuerdos con municipios. Los planes y precios que se ven son datos de demo cargados por un admin.

## Product Principles

1. **El dominio manda sobre la interfaz.** Las reglas de ALCANCE.md son la verdad; ninguna pantalla puede sugerir un comportamiento que el backend no hace. Si algo parece necesitar tocar el backend, es señal de que el diseño se desvió.
2. **Dos escenas, dos diseños.** Cliente en la calle (una mano, pantalla chica, targets de 44px, decisiones rápidas) y admin en escritorio (densidad, tablas, formularios largos) no comparten patrones de layout aunque compartan tokens.
3. **Lo psicodélico decora, nunca informa.** Todo estado se comunica con texto o forma además del color; los efectos viven en pseudo-elementos y capas de fondo, y ninguno lleva texto encima.
4. **Accesibilidad como piso, no como opción.** Contraste AAA donde el texto lo permite, foco siempre visible, `prefers-reduced-motion` respetado, targets de 44×44 en los flujos de calle. El exceso visual se gana el derecho a existir solo si no cuesta nada de esto.
5. **Nada se borra, todo deja rastro.** Vale para el dominio y para la interfaz: los estados históricos (viajes, pagos, strikes, movimientos) se muestran, no se ocultan.

## Accessibility & Inclusion

Piso vinculante, ya implementado en parte y verificable en `frontend/DISENO.md`:

- Texto a 4.5:1 mínimo; el sistema apunta a AAA donde el color lo permite. `#A855F7` (`--marca-500`) **nunca lleva ni recibe texto** — es solo bordes, íconos y foco.
- Nunca color solo: todo estado lleva texto o forma (WCAG 1.4.1).
- Nunca `opacity` para atenuar texto; se usa `--texto-suave`, que tiene ratio garantizado.
- Foco siempre visible: anillo de 2px con 2px de separación. Nunca `outline: none` sin reemplazo.
- Targets de 44×44px mínimo en navegación y en los flujos de uso en la calle.
- `prefers-reduced-motion` desactiva las transiciones (`--transicion: 0ms`); las animaciones decorativas de fondo deben respetarlo igual.
- Skip link como primer elemento tabulable, ya presente en `App.tsx`.
- Modo oscuro por `prefers-color-scheme` con override manual (`data-tema='claro'`).
