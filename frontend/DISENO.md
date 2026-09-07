# Sistema de diseño — PeopleBikes

## Paleta

Cuatro colores de marca. Todo lo demás se deriva de ellos.

| Token | Hex | Rol |
|---|---|---|
| `--marca-100` | `#F3E8FF` | Superficie de acento: fondos de sección, avisos, chips seleccionados, hover de filas |
| `--marca-500` | `#A855F7` | **Decorativo únicamente**: bordes, anillo de foco, barras, íconos grandes, gráficos |
| `--marca-800` | `#6B21A8` | Interactivo: fondo de botón primario, links, texto enfatizado de marca |
| `--tinta-900` | `#1F2937` | Texto principal y superficie oscura en modo oscuro |

## Contrastes medidos (WCAG 2.1)

| Combinación | Ratio | Veredicto |
|---|---|---|
| `#1F2937` sobre blanco | 14.68:1 | AAA — texto base |
| `#1F2937` sobre `#F3E8FF` | 12.44:1 | AAA — texto sobre superficie de acento |
| `#6B21A8` sobre blanco | 8.72:1 | AAA — links y texto de marca |
| blanco sobre `#6B21A8` | 8.72:1 | AAA — botón primario |
| `#6B21A8` sobre `#F3E8FF` | 7.39:1 | AAA — link dentro de aviso |
| **`#A855F7` sobre blanco** | **3.96:1** | **Falla AA para texto.** Solo bordes, íconos y foco (mínimo 3:1 para componentes, lo cumple) |
| **blanco sobre `#A855F7`** | **3.96:1** | **Falla AA.** Nunca botón primario con texto blanco |
| `#A855F7` sobre `#1F2937` | 3.71:1 | Solo UI, no texto |
| `#F3E8FF` sobre `#1F2937` | 12.44:1 | AAA — texto en modo oscuro |
| `#D8B4FE` sobre `#1F2937` | 8.30:1 | AAA — acento interactivo en modo oscuro |

**Regla dura: `#A855F7` nunca lleva ni recibe texto.** Cuando hace falta púrpura con texto encima, es `#6B21A8`.

## Derivados permitidos

Tintes de la misma familia, no colores nuevos:

- `--marca-50 #FAF5FF`, `--marca-200 #E9D5FF`, `--marca-300 #D8B4FE` (acento en modo oscuro), `--marca-600 #9333EA` (hover de botón primario)
- Neutros derivados de la tinta: `--tinta-700 #374151`, `--tinta-500 #4B5563`, `--tinta-400 #6B7280` (texto secundario, 4.83:1 sobre blanco — el mínimo aceptable)

## Colores funcionales

Fuera de la paleta de marca porque comunican estado, no identidad. Se usan **solo** para éxito y error, nunca decorativos.

| Rol | Texto | Fondo | Contraste |
|---|---|---|---|
| Éxito | `#166534` | `#DCFCE7` | 7.4:1 |
| Error | `#B91C1C` | `#FEE2E2` | 6.4:1 |
| Advertencia | `#854D0E` | `#FEF9C3` | 7.9:1 |

## Reglas de uso

1. **Nunca color solo.** Todo estado (anclaje libre/ocupado/fuera de servicio, viaje excedido, notificación no leída) lleva además texto o forma. WCAG 1.4.1.
2. **Nunca `opacity` para bajar contraste.** Se usa el token `--texto-suave`, que tiene ratio garantizado.
3. **Foco siempre visible**: anillo de 2px `--marca-500` con 2px de separación. Nunca `outline: none` sin reemplazo.
4. **Jerarquía por tamaño y peso, no por color.** El texto secundario baja de tamaño, no se aclara hasta desaparecer.
5. **Targets de 44×44px mínimo** en el modo bici y en la navegación. En la calle, con una mano.

## Tipografía

Base **17px** (punto medio entre los 16px de Citi Bike y los 18px de Bicing, que es sector público con obligación de accesibilidad). Escala:

| Nivel | Tamaño / peso | Uso |
|---|---|---|
| Display | 48px / 700 | Hero de la landing |
| H1 | 32px / 700 | Título de página |
| H2 | 22px / 600 | Título de tarjeta o sección |
| H3 | 17px / 600 | Subtítulo |
| Cuerpo | 17px / 400 | Texto general |
| Chico | 15px / 400 | Ayudas, metadatos de tabla |
| Micro | 13px / 500 | Chips y pills |
| Contador | 64px / 700 tabular | Cronómetro del viaje activo |

Todo en `rem` para que respete el tamaño de fuente del navegador.

## Referencias

Estructura y decisiones tomadas de Citi Bike (Lyft Core UI), Bicing Barcelona y Ecobici Buenos Aires — ver el análisis de benchmark. De Lyft se copia el esquema de tokens con par claro/oscuro y la escala de elevación; de Bicing, la tipografía grande de base; de los tres, el skip link.

## Estructura de la aplicación

| Ruta | Qué es |
|---|---|
| `/` | Inicio: desbloqueo de bici, o modo bici si hay viaje en curso |
| `/estaciones` | Disponibilidad de la red: bicis para sacar y lugares para devolver, por estación |
| `/planes` | Comparativa de membresías en tabla, con contratación |
| `/cuenta` | Suscripción, pagos, strikes, notificaciones e historial de viajes |
| `/admin/:seccion` | Panel con sidebar: estaciones, almacenes, flota, movimientos, membresías |

El panel de administración es una ruta por sección en vez de las cinco apiladas: solo se monta y consulta la que está visible.

### Capas

- `styles/tokens.css` — primitivos y semánticos. Ningún componente usa un color literal.
- `hooks/` — estado y acciones de un dominio (`useSuscripcion`, `useEstaciones`). La lógica no se duplica entre pantallas que muestran lo mismo de distinta forma.
- `api/` — un archivo por módulo del backend, sobre el `client.ts` que maneja token y refresh.
- `components/` — presentación, agrupada por dominio.

## Decisiones de accesibilidad implementadas

- **Skip link** como primer elemento tabulable, más landmark `<main>`.
- **Foco visible** único para todo lo enfocable, con anillo de `--foco` y separación de 2px.
- **Modal**: foco atrapado mientras está abierto, respeta el `autoFocus` del contenido si lo hay, y devuelve el foco al elemento que lo abrió. El disparador se captura en el render, no en el efecto: para cuando corren los efectos, un campo con `autoFocus` ya se llevó el foco.
- **Contador de viaje**: el número visual está oculto para lectores de pantalla y hay una región viva aparte que solo cambia cuando cambia el minuto. Anunciar un valor por segundo haría la página inusable.
- **Tablas** con `<caption>` y `scope` en cada cabecera.
- **Estados nunca solo por color**: los chips de anclaje suman símbolo, las notificaciones no leídas suman punto, y las cifras en cero de una estación llevan además un aviso en texto.
- **Targets de 44px** en botones, campos y navegación; 56px en el modo bici.
