# Backlog del MVP 2 — "el núcleo vendible"

Fuente: `Ensambla_Backlog_por_MVP.xlsx` (hojas *Tareas*, *Historias* y *Trazabilidad*). El MVP 1 fueron las pantallas
(entregado). El MVP 2 son 38 tareas en 10 historias. Talle: **S** ≈ hasta 1 día · **M** ≈ 2–3 días · **L** ≈ 4+ días.
Antes de tomar una tarea, verificá que sus dependencias estén hechas.

Columna **Base** = qué deja resuelto esta estructura inicial y qué falta.

## H-00 · Base técnica

| ID | Área | Talle | Tarea | Depende de | Base |
|---|---|---|---|---|---|
| T-001 | Infra | S | Estructura del repositorio | — | ✅ `backend/` + `frontend/`, README, `.env.example` |
| T-002 | Infra | S | CI en pull requests | T-001 | ✅ workflows; falta activar *branch protection* (ver CONTRIBUTING) |
| T-003 | Infra | S | Docker y docker-compose local | T-001 | ✅ `backend/Dockerfile` + `docker-compose.yml` (BD); falta contenedor del front si se quiere |
| T-004 | BD | M | Modelo de datos del core | T-001 | ✅ `V1__esquema_inicial.sql` + [modelo-datos.md](modelo-datos.md) |

## H-01 · Acceso y sesión

| ID | Área | Talle | Tarea | Depende de | Base |
|---|---|---|---|---|---|
| T-005 | BE | M | `POST /auth/register` (usuario + negocio en una transacción) | T-004 | ✅ registro, BCrypt, duplicados y rollback verificado en PostgreSQL |
| T-006 | BE | M | Login, logout y sesión actual (`/auth/me`) | T-005 | ✅ sesión JDBC, rotación, expiración y revocación en logout |
| T-007 | BE | S | Middleware de autenticación | T-006 | ✅ rutas privadas y escrituras sin sesión responden 401; tests parametrizados |
| T-008 | FE | M | Pantallas de login y registro | T-006 | ✅ formularios reales con nombre de negocio, validación y errores del servidor |
| T-009 | FE | S | Sesión en el cliente y rutas protegidas | T-008 | ✅ revalidación por servidor, logout a login y descarte de datos privados |

## H-02 · Espacio de negocio y aislamiento

| ID | Área | Talle | Tarea | Depende de | Base |
|---|---|---|---|---|---|
| T-010 | BE | M | Filtro por negocio en todas las consultas | T-007 | 🟡 esquema con FKs compuestas; falta resolver `negocio_id` desde la sesión |
| T-011 | QA | S | Tests de aislamiento entre negocios | T-010, T-027 | 🟡 hay tests a nivel base (`EsquemaPostgresIntegrationTest`); faltan los de API |

## H-03 · Crear proyecto desde plantilla o en blanco

| ID | Área | Talle | Tarea | Depende de | Base |
|---|---|---|---|---|---|
| T-012 | BE | M | CRUD de proyectos | T-010 | ⏳ falta (entidad y repo JPA ya existen) |
| T-013 | BE | S | Plantillas base como seeds | T-004 | 🟡 `GET /api/plantillas` listo; faltan los seeds (migración `V2`) |
| T-014 | BE | M | Crear proyecto desde plantilla | T-012, T-013 | ⏳ falta |
| T-015 | FE | M | Inicio con listado de proyectos | T-012, T-009 | 🟡 `ProyectosPage` placeholder que ya lista plantillas |
| T-016 | FE | S | Catálogo de plantillas | T-014, T-015 | ⏳ falta |

## H-04 · Constructor de bloques y campos

| ID | Área | Talle | Tarea | Depende de | Base |
|---|---|---|---|---|---|
| T-017 | BE | S | CRUD de bloques | T-012 | 🟡 `GET`/`POST /api/proyectos/{id}/bloques`, `PATCH`/`DELETE /api/bloques/{id}`; nombre único por proyecto (409) y borrado en cascada probado contra PostgreSQL |
| T-018 | BE | M | CRUD de campos tipados | T-017 | 🟡 `GET`/`POST /api/bloques/{id}/campos`, `PATCH`/`DELETE /api/campos/{id}`; `ConfiguracionCampo` valida tipo, opciones y valor inicial (400 con `errors` por dato) y `Bloque` el nombre único (409) |
| T-019 | FE | M | Layout del constructor | T-015 | 🟡 ruta `/proyectos/$proyectoId/construir` placeholder |
| T-020 | FE | S | Agregar, renombrar y eliminar bloques | T-019, T-017 | ⏳ falta |
| T-021 | FE | M | Editor de campos | T-019, T-018 | ⏳ falta |

## H-05 · Formulario de carga generado automáticamente

| ID | Área | Talle | Tarea | Depende de | Base |
|---|---|---|---|---|---|
| T-022 | BE | M | Validador dinámico de registros | T-018 | 🟡 `Registro.validar` y `Campo.errorDe`; falta el CRUD de campos (T-018) para armarlos desde la API |
| T-023 | BE | S | Crear registro (`POST /blocks/:id/records`) | T-022, T-010 | 🟡 `POST /api/bloques/{id}/registros`; el negocio sale del dueño de la sesión hasta que T-010 lo generalice |
| T-024 | FE | M | `FormRenderer` dinámico | T-021 | ⏳ falta; `InputField`/`TextareaField` ya existen |
| T-025 | FE | S | Validación y feedback del formulario | T-024, T-023 | ⏳ falta; patrón en `LoginForm` |

## H-06 · Tabla de registros con gestión completa

| ID | Área | Talle | Tarea | Depende de | Base |
|---|---|---|---|---|---|
| T-026 | BE | M | Listado de registros (paginación, búsqueda, orden) | T-023 | 🟡 `GET /api/bloques/{id}/registros?q&orden&direccion&pagina&tamanio` |
| T-027 | BE | S | Ver, editar y eliminar registro | T-023 | 🟡 `GET`, `PATCH` y `DELETE /api/registros/{id}` |
| T-028 | FE | M | Tabla dinámica de registros | T-026, T-024 | ⏳ falta |
| T-029 | FE | S | Búsqueda y orden en la tabla | T-028 | ⏳ falta |
| T-030 | FE | M | Ficha de registro con editar y eliminar | T-028, T-027 | ⏳ falta |

## H-07 · Modo construir / modo usar

| ID | Área | Talle | Tarea | Depende de | Base |
|---|---|---|---|---|---|
| T-031 | FE | M | Modo usar | T-030 | 🟡 ruta `/proyectos/$proyectoId/usar` placeholder |
| T-032 | FE | S | Alternar construir/usar y vista previa | T-031 | ⏳ falta |

## H-08 · Interfaz base usable y adaptable

| ID | Área | Talle | Tarea | Depende de | Base |
|---|---|---|---|---|---|
| T-033 | FE | M | Layout general y navegación | T-009 | 🟡 `AppLayout`, `Navbar` y tokens de marca; faltan breadcrumbs |
| T-034 | FE | S | Componentes de vacío, carga y error | T-033 | 🟡 `EmptyState`, `ErrorState`, `Spinner`; falta *skeleton* y aplicarlos |
| T-035 | FE | M | Responsive del flujo principal (360 px) | T-030, T-032 | ⏳ falta |

## H-09 · Despliegue y prueba del flujo principal

| ID | Área | Talle | Tarea | Depende de | Base |
|---|---|---|---|---|---|
| T-036 | Infra | M | Deploy en entorno público | T-003, T-002 | 🟡 `render.yaml` + `deploy-main.yml`; falta servicio del front y secretos |
| T-037 | QA | M | Test E2E del flujo principal | T-035, T-036 | ⏳ falta (Playwright o Cypress) |
| T-038 | QA | S | Datos de demo para la venta | T-014, T-023 | ⏳ falta |

Leyenda: ✅ resuelto por la base · 🟡 parcialmente · ⏳ por hacer.

## Orden sugerido para arrancar en paralelo

1. **Backend:** T-005 → T-006 → T-010 (desbloquea todo) · en paralelo T-013 (seeds).
2. **Frontend:** T-008 (registro) y T-033/T-034 (shell y estados) mientras el backend avanza, con `VITE_API_MOCKS=parcial`.
3. **Después:** T-012 → T-017 → T-018 (estructura) y T-019 → T-021 (constructor); luego T-022 → T-023 → T-026/T-027.
