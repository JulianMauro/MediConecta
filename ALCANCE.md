# PeopleBikes — Alcance funcional

Sistema de transporte público en bicicletas compartidas: el usuario retira una bicicleta de una estación y la devuelve en cualquier otra, dentro de los límites del plan que contrató.

## Actores

| Actor | Descripción |
|---|---|
| **Cliente** | Persona registrada que contrata un plan y realiza viajes. |
| **Administrador** | Gestiona la infraestructura: estaciones, flota, almacenes, traslados y planes. |
| **Sistema** | Actor automático: genera cargos, penalizaciones, notificaciones y bloqueos sin intervención humana. |

---

## 1. Usuarios y autenticación

| # | Función | Actor |
|---|---|---|
| F-01 | Registrarse con nombre, apellido, email, contraseña, DNI y teléfono | Cliente |
| F-02 | Iniciar sesión y obtener una sesión activa | Cliente / Admin |
| F-03 | Renovar la sesión automáticamente sin volver a ingresar credenciales | Sistema |
| F-04 | Consultar los datos del usuario de la sesión actual | Cliente / Admin |
| F-05 | Cerrar sesión | Cliente / Admin |
| F-06 | Restringir el acceso a funciones de gestión según el rol del usuario | Sistema |
| F-07 | Dar de baja lógica a un usuario, conservando su historial | Admin |

## 2. Planes de membresía

| # | Función | Actor |
|---|---|---|
| F-08 | Crear un plan definiendo nombre, tipo, precio, minutos incluidos por viaje, viajes por día, espera entre viajes y tarifa del minuto extra | Admin |
| F-09 | Listar los planes existentes | Admin / Cliente |
| F-10 | Consultar el detalle de un plan | Admin / Cliente |
| F-11 | Desactivar un plan para que deje de ofrecerse, sin afectar a quienes ya lo contrataron | Admin |
| F-12 | Comparar los planes disponibles antes de contratar | Cliente |

## 3. Suscripciones

| # | Función | Actor |
|---|---|---|
| F-13 | Contratar un plan, lo que genera un pago pendiente | Cliente |
| F-14 | Activar la suscripción automáticamente al confirmarse el pago, con vigencia según el tipo de plan | Sistema |
| F-15 | Consultar el plan vigente y su fecha de vencimiento | Cliente |
| F-16 | Consultar el historial de suscripciones y su estado | Cliente |
| F-17 | Cancelar la suscripción activa | Cliente |

## 4. Pagos

| # | Función | Actor |
|---|---|---|
| F-18 | Generar un pago pendiente al contratar un plan | Sistema |
| F-19 | Generar un pago pendiente al exceder el tiempo incluido en un viaje | Sistema |
| F-20 | Consultar los pagos propios, pendientes y confirmados | Cliente |
| F-21 | Confirmar un pago registrando la referencia de la pasarela | Cliente / Admin |
| F-22 | Rechazar un pago | Cliente / Admin |
| F-23 | Impedir que un usuario opere sobre pagos de otro | Sistema |

## 5. Estaciones y anclajes

| # | Función | Actor |
|---|---|---|
| F-24 | Crear una estación con nombre, dirección, coordenadas y capacidad | Admin |
| F-25 | Listar y consultar estaciones | Admin / Cliente |
| F-26 | Agregar anclajes a una estación, hasta su capacidad | Admin |
| F-27 | Marcar un anclaje como fuera de servicio | Admin |
| F-28 | Volver a habilitar un anclaje | Admin |
| F-29 | Desactivar una estación conservando su historial | Admin |
| F-30 | Consultar la disponibilidad de la red: bicicletas para retirar y lugares para devolver, por estación | Cliente |

## 6. Flota de bicicletas

| # | Función | Actor |
|---|---|---|
| F-31 | Dar de alta una bicicleta con su código, ubicada en un almacén | Admin |
| F-32 | Listar la flota con el estado y la ubicación de cada bicicleta | Admin |
| F-33 | Cambiar el estado de una bicicleta: disponible, en viaje, en reparación o desactivada | Admin |
| F-34 | Registrar la ubicación de cada bicicleta, que es siempre un anclaje o un almacén, nunca ambos | Sistema |

## 7. Almacenes

| # | Función | Actor |
|---|---|---|
| F-35 | Crear un almacén con nombre, dirección y capacidad | Admin |
| F-36 | Listar y consultar almacenes | Admin |
| F-37 | Eliminar un almacén | Admin |

## 8. Logística

| # | Función | Actor |
|---|---|---|
| F-38 | Registrar el traslado de varias bicicletas en un solo movimiento, entre estaciones y almacenes | Admin |
| F-39 | Dejar traza de quién realizó cada traslado, cuándo, desde dónde y hacia dónde | Sistema |
| F-40 | Consultar el historial de movimientos | Admin |

## 9. Viajes

| # | Función | Actor |
|---|---|---|
| F-41 | Desbloquear una bicicleta disponible e iniciar un viaje | Cliente |
| F-42 | Validar antes de iniciar que el usuario tiene plan vigente, no supera los viajes diarios, respetó la espera entre viajes y no tiene la cuenta bloqueada | Sistema |
| F-43 | Consultar el viaje en curso con su tiempo transcurrido | Cliente |
| F-44 | Finalizar el viaje devolviendo la bicicleta en un anclaje libre | Cliente |
| F-45 | Calcular la duración del viaje y determinar si excedió el tiempo incluido | Sistema |
| F-46 | Liberar el anclaje de origen y ocupar el de destino al iniciar y finalizar | Sistema |
| F-47 | Consultar el historial de viajes propios | Cliente |
| F-48 | Consultar los viajes de cualquier usuario | Admin |

## 10. Penalizaciones y bloqueo de cuenta

| # | Función | Actor |
|---|---|---|
| F-49 | Generar una penalización cuando un viaje excede el tiempo incluido | Sistema |
| F-50 | Calcular el cargo por minuto extra según la tarifa del plan y emitir el pago correspondiente | Sistema |
| F-51 | Saldar la penalización automáticamente al confirmarse su pago | Sistema |
| F-52 | Bloquear la cuenta al acumular 3 penalizaciones sin saldar | Sistema |
| F-53 | Consultar las penalizaciones propias y su estado | Cliente |
| F-54 | Consultar las penalizaciones de cualquier usuario | Admin |
| F-55 | Registrar cada bloqueo con su motivo y fecha, como historial auditable | Sistema |

## 11. Notificaciones

| # | Función | Actor |
|---|---|---|
| F-56 | Notificar automáticamente el exceso de tiempo y el bloqueo de cuenta | Sistema |
| F-57 | Consultar las notificaciones propias | Cliente |
| F-58 | Consultar solo las no leídas | Cliente |
| F-59 | Marcar una notificación como leída | Cliente |

---

## Reglas de negocio transversales

1. **Sin plan vigente no hay viaje.** El plan define minutos incluidos, viajes por día y espera mínima entre viajes.
2. **Un viaje a la vez por usuario.**
3. **Nada se borra.** Usuarios, estaciones, planes y bicicletas se dan de baja lógica para preservar la trazabilidad con viajes y pagos históricos.
4. **Toda penalización tiene un pago asociado**, y saldarla requiere confirmarlo.
5. **Tres penalizaciones sin saldar bloquean la cuenta**, y el bloqueo impide iniciar viajes.
6. **Una bicicleta está en un anclaje o en un almacén**, nunca en los dos ni en ninguno.
7. **Cada cambio de estado relevante deja historial**, no un simple indicador.

---

## Estructura sugerida de la presentación

| Lámina | Contenido |
|---|---|
| 1 | Portada — PeopleBikes |
| 2 | Problema y propuesta |
| 3 | Actores del sistema |
| 4 | Mapa de módulos (los 11) |
| 5-7 | Flujo del cliente: contratar plan → retirar bici → devolver |
| 8 | Flujo de excepción: exceso de tiempo, penalización y bloqueo |
| 9-10 | Funciones de administración: infraestructura, flota y logística |
| 11 | Reglas de negocio transversales |
| 12 | Alcance de esta etapa y próximos pasos |
