# SPEC: Task Assignment App

Prueba técnica (Desarrollador Junior). Este documento es el contexto para el agente de código (Antigravity, Claude Code, etc.). Las decisiones de diseño ya están tomadas aquí: el agente las implementa, no las cambia.

## 0. Reglas para el agente

1. Trabaja **por fases** (sección 11). Al terminar cada fase, detente, muestra qué hiciste y espera confirmación.
2. No agregues funcionalidades que no estén en este documento.
3. Cada fase termina con `mvn test` en verde y la app corriendo.
4. Commits pequeños en inglés, formato `feat: ...`, `test: ...`, `docs: ...`.
5. Código, README y mensajes de error en **inglés**. Comentarios solo donde la lógica no sea obvia.
6. Los tests deben probar comportamiento real (casos borde de calendario incluidos), no solo que "no explote".
7. Nunca subas credenciales. Todo secreto va por variables de entorno.
8. Si algo es ambiguo, **pregunta** en lugar de inventar.

## 1. Requisitos originales

- Req1: CRUD de tasks: name, effort (horas), skills, status (done / not-done).
- Req2: CRUD de workers: name, availability (horas/día), skills.
- Req3: Asignación manual de tasks a workers (worker, task, start date).
- Req3a: Solo se permite si task.skills ⊆ worker.skills.
- Req4: Auto-asignación.
- Entregables: repo en Git, scripts para build / run / test, script para poblar datos de demo.
- Consideraciones: horas enteras; días laborales lunes a viernes.

## 2. Stack

- Backend: Java 21, Spring Boot 3, Maven, Spring Data JPA, Bean Validation.
- Base de datos: PostgreSQL (perfil `prod`, base desplegada) y H2 (perfil `dev`, sin instalar nada). Esquema con Flyway.
- Tests: JUnit 5, Mockito, MockMvc / `@SpringBootTest`.
- Frontend: React + Vite (TypeScript opcional), consume la API REST.
- Conexión a la base por variables de entorno: `DB_URL`, `DB_USER`, `DB_PASS`.

## 3. Estructura del repo

```
/backend
/frontend
/scripts     build.sh run.sh test.sh seed.sh (+ .bat equivalentes)
SPEC.md
README.md
```

Backend en capas: `controller` → `service` → `repository`, más `domain` y `dto`. La lógica de calendario y la de auto-asignación viven en servicios propios, sin depender de HTTP, para poder testearlas de forma unitaria.

## 4. Modelo de datos

- **Task**: `id`, `name` (obligatorio), `effortHours` (int > 0), `skills` (set de strings, normalizados a minúsculas), `status` (`NOT_DONE` | `DONE`).
- **Worker**: `id`, `name` (obligatorio), `hoursPerDay` (int entre 1 y 24), `skills` (set de strings en minúsculas).
- **Assignment**: `id`, `task` (única: una task tiene como máximo una asignación), `worker`, `startDate`, `endDate`.
- **AssignmentDay**: `id`, `assignment`, `date`, `hours`. Guarda cuántas horas de cada día consume una asignación. Con esto la carga de un worker por día se calcula con una simple suma.

## 5. Reglas de calendario (suposiciones documentadas)

1. Solo se trabaja de lunes a viernes. No hay festivos.
2. Si `startDate` cae en sábado o domingo, la asignación empieza el lunes siguiente. La respuesta incluye la fecha de inicio **efectiva**.
3. Un worker tiene capacidad `hoursPerDay` por día hábil. La capacidad libre de un día es `hoursPerDay` menos la suma de horas de sus `AssignmentDay` de ese día.
4. Para ubicar una task, se recorren los días hábiles desde la fecha de inicio. En cada día se asigna `min(horas restantes, capacidad libre)`. Los días con capacidad libre 0 se saltan. Termina cuando las horas restantes llegan a 0; `endDate` es el último día con horas.
5. Ejemplo: task de 10 h, worker de 4 h/día, sin carga previa, inicio un jueves → jueves 4, viernes 4, lunes 2. `endDate` = lunes.
6. Las asignaciones de tasks `DONE` siguen contando para la capacidad (historial). Se documenta como limitación.

## 6. Reglas de negocio de asignación

- Req3a: `worker.skills.containsAll(task.skills)`. Si no se cumple → error 422 con mensaje que liste las skills que faltan.
- Una task `DONE` no se puede asignar → 409.
- Una task ya asignada no se puede asignar otra vez → 409 (hay que desasignar primero).
- Una task sin skills requeridas es asignable a cualquier worker.
- Editar `effortHours` o `skills` de una task asignada → 409 (desasignar primero). Editar `name` o `status` está permitido.
- Borrar una task borra su asignación y sus `AssignmentDay`.
- Borrar un worker con asignaciones → 409. Editar `hoursPerDay` o `skills` de un worker con asignaciones → 409.
- Marcar una task como `DONE` conserva la asignación.

## 7. Auto-asignación (Req4)

Algoritmo greedy determinista:

1. Entrada: fecha de inicio opcional (por defecto, hoy). Se toman todas las tasks `NOT_DONE` sin asignación.
2. Para cada task, calcular sus workers elegibles (skills cubiertas).
3. Ordenar las tasks por: menos workers elegibles primero, luego mayor `effortHours`, luego `id` ascendente. Así las tasks más restringidas no se quedan sin worker.
4. Para cada task, en ese orden: simular la ubicación con cada worker elegible (regla 4 de la sección 5) y elegir el que **termine más temprano**. Desempates: menos horas totales asignadas, luego `id` ascendente.
5. Crear la asignación y sus `AssignmentDay`, de modo que la siguiente task ya vea la carga actualizada.
6. Si una task no tiene workers elegibles, queda sin asignar con motivo (por ejemplo: "No worker has skills: \[kotlin\]").
7. Salida: `{ assigned: [...], unassigned: [{ task, reason }] }`.

Limitaciones a documentar en el README: es greedy, no óptimo global; no reasigna tasks ya asignadas; no considera prioridades ni fechas límite.

La simulación de ubicación debe ser una función pura reutilizable (la misma para asignación manual y auto-asignación).

## 8. API REST

```
GET POST          /api/tasks
GET PUT DELETE    /api/tasks/{id}
GET POST          /api/workers
GET PUT DELETE    /api/workers/{id}
GET POST          /api/assignments        body: { taskId, workerId, startDate }
DELETE            /api/assignments/{id}
POST              /api/assignments/auto   body: { startDate? }
GET               /api/workers/{id}/schedule   carga por día
POST              /api/seed               solo perfil dev
```

Errores en formato JSON consistente `{ timestamp, status, error, message }`:

- 400 validación, 404 no encontrado, 409 conflicto de estado, 422 skills incompatibles.
- Un `@RestControllerAdvice` central.

## 9. Frontend

Tres vistas:

- **Tasks**: tabla, crear, editar, borrar. Campo de skills como chips.
- **Workers**: igual.
- **Assignments**: formulario manual (task, worker, fecha), botón **Auto-assign** con el reporte (asignadas / no asignables con motivo) y lista de asignaciones con inicio, fin y worker.

Mostrar los errores del backend con su mensaje. Diseño simple y limpio, sin librerías pesadas.

## 10. Tests mínimos obligatorios

**Calendario (unitarios):**

- Inicio en día hábil que cabe en un día.
- Cruce de fin de semana (el ejemplo de 10 h / 4 h por día).
- Inicio en sábado → lunes.
- Worker con carga previa: se llena solo la capacidad libre.
- Día totalmente ocupado se salta.

**Asignación:**

- Skills válidas (subconjunto exacto y estricto), skills insuficientes → 422, task sin skills.
- Task `DONE` → 409; task ya asignada → 409.

**Auto-asignación:**

- Orden por restricción, elección del worker que termina antes, reparto de carga entre dos workers equivalentes, task imposible reportada con motivo, resultado determinista.

**CRUD y reglas de borrado/edición:** al menos un test de API por recurso.

## 11. Fases

**Fase 0 – Setup.** Repo, backend Spring Boot con perfiles `dev`/`prod`, Flyway, estructura de capas, scripts vacíos. App arranca con H2.

**Fase 1 – CRUD.** Tasks y workers con validación, manejo de errores y tests de API.

**Fase 2 – Calendario y asignación manual.** Servicio de calendario puro + tests unitarios primero (TDD recomendado), luego endpoints, `AssignmentDay` y reglas de la sección 6.

**Fase 3 – Auto-asignación.** Algoritmo de la sección 7 con sus tests.

**Fase 4 – Scripts y seed.** `build`, `run`, `test`, `seed`. Datos de demo (sección 12). Probar clonando el repo en una carpeta limpia.

**Fase 5 – Frontend.** Las tres vistas de la sección 9.

**Fase 6 – README y despliegue.** Base en Neon o Supabase, backend en Render o Railway, front en Vercel o Netlify. Variables de entorno documentadas. README completo (sección 13).

**Capas opcionales (solo si lo anterior está sólido, en este orden):**

- **A. Login y roles.** JWT propio con Spring Security. `ADMIN`: todo el CRUD, asignación manual y auto-asignación; crea workers junto con su usuario y contraseña temporal. `WORKER`: ve solo sus asignaciones (`GET /api/me/assignments`) y marca sus tasks como `DONE`. Contraseñas con BCrypt. Credenciales de demo en el README.
- **B. IA.** Una sola función: el admin escribe la descripción de una task y la IA sugiere skills y esfuerzo; el admin revisa y confirma. La API key va por variable de entorno y, si no existe, la función se oculta. Validar siempre la salida (JSON válido, esfuerzo entero, skills como strings). La auto-asignación **nunca** usa IA.

## 12. Datos de demo

5 workers (con distintas skills y horas por día, por ejemplo 4, 6 y 8) y 10 a 12 tasks que cubran:

- Una asignación manual válida.
- Un intento rechazado por skills.
- Una task que cruza un fin de semana.
- Una task imposible de asignar (skill que nadie tiene).
- Una auto-asignación que reparta carga entre varios workers.

`seed.sh` debe ser idempotente: limpia y vuelve a poblar.

## 13. README (contenido obligatorio)

- Qué es y qué requisitos cubre.
- Cómo construir, correr y testear (un comando cada uno).
- Cómo cargar los datos de demo.
- URL del despliegue y credenciales de demo (si aplica).
- Suposiciones de calendario y reglas de negocio (secciones 5 y 6).
- Explicación del algoritmo de auto-asignación y sus limitaciones.
- Qué se probó y qué no.
- Qué mejoraría con más tiempo (reasignación, prioridades, fechas límite, festivos, optimización global).
- Nota de transparencia sobre el uso de herramientas de IA en el desarrollo.

## 14. Definición de terminado

- Un clon limpio corre con los scripts documentados, sin configuración manual.
- Todos los tests pasan.
- Las reglas de la sección 6 están implementadas y probadas.
- El link del repo se envía **antes del lunes 19 de octubre, 9:00 a. m. (hora de Colombia)**.
