# Acceso y sesión — ENS-2

Implementación de ENS-33 a ENS-37 (T-005 a T-009). Las rutas HTTP usan el prefijo `/api`.
ENS-8 (modo construir/usar) no forma parte de este cambio.

## Contrato

| Endpoint | Resultado |
|---|---|
| GET /api/auth/csrf | 200 con `token` y `headerName`; crea sesión anónima si falta |
| POST /api/auth/register | 201 con `id, nombre, email`; no autentica |
| POST /api/auth/login | 200 con identidad pública y cookie SESSION; credenciales incorrectas: 401 |
| GET /api/auth/me | 200 con identidad pública; sesión inexistente, vencida o suspendida: 401 |
| POST /api/auth/logout | 204; elimina sesión JDBC y cookie. La cookie anterior ya no autentica |

Registro recibe `nombre`, `nombreNegocio`, `email` y `contrasenia`. Login recibe `email` y `contrasenia`.
Las escrituras requieren el token obtenido por GET /csrf en el header `X-CSRF-TOKEN` y la cookie de esa sesión.
Login rota el id de sesión y descarta el token CSRF anterior; el cliente pide uno nuevo para la próxima escritura.

Nombre y negocio no pueden quedar vacíos; máximos: 100 y 120 caracteres. Email válido, máximo 254 caracteres,
normalizado en minúsculas. Registro exige al menos 8 caracteres de contraseña. Registro y login rechazan
contraseñas que superen los 72 bytes UTF-8 admitidos por BCrypt (incluye caracteres Unicode).

Usuario y negocio se persisten dentro de una única transacción. Un fallo al guardar negocio revierte usuario.
El email duplicado devuelve 409 con “Ya existe una cuenta con ese email”, incluso cuando la restricción única
detecta una escritura concurrente. La contraseña se guarda como BCrypt con sal, nunca en texto plano.
Las respuestas no contienen ni contraseña ni hash.

## Sesión y protección

Spring Session persiste sesiones en PostgreSQL y usa cookie SESSION HttpOnly, SameSite=Lax.
Secure está habilitado fuera de desarrollo. La expiración por inactividad se configura con
`SESSION_TIMEOUT` (30 minutos por defecto). La sesión contiene identidad y rol, sin credenciales.

Las rutas privadas exigen sesión para cualquier método HTTP. Sin sesión responden 401 antes de evaluar CSRF.
Las escrituras autenticadas sin CSRF responden 403. Son públicas únicamente las rutas de acceso indicadas,
el catálogo de plantillas y la documentación de desarrollo. /auth/me siempre exige sesión.
Una cuenta suspendida invalida también las sesiones que ya tenía abiertas.

## Cliente

El modo por defecto es `VITE_API_MOCKS=ninguno`. El registro primero crea la cuenta, luego inicia sesión.
Login respeta la URL privada solicitada, aceptando solo destinos internos.
TanStack Query mantiene la identidad en memoria; la cookie pertenece al servidor y no se usa localStorage
para credenciales ni sesiones. Cada navegación privada revalida /me para evitar autorizar con datos cacheados.

Logout espera la confirmación del servidor, vuelve a /login y descarta los datos privados. Si el servidor falla,
muestra un aviso y permite reintentar sin afirmar que la sesión fue revocada.

## Validación automatizada

- Tests de dominio, mappers, adaptadores, controller y configuración; 100% de líneas y ramas por clase.
- AccesoIntegrationTest usa PostgreSQL real y un contexto web con puerto aleatorio: registro, hash, duplicados,
  rollback, login, cookie, rotación, expiración, suspensión, logout y replay de la cookie anterior.
- Tests HTTP parametrizados para lectura y escritura privada sin sesión, y CSRF con sesión.
- Frontend: validación de formularios, registro completo, email repetido, errores por campo, contraseña incorrecta,
  logout y rechazo de sesión vencida aunque hubiera una identidad cacheada.
- El entorno de tests conserva los AbortSignal de Node para que MSW y fetch funcionen con las consultas cancelables.

Ejecutar backend: `./mvnw.cmd clean verify` con Docker disponible.
Ejecutar frontend: `npm run lint`, `npm run typecheck`, `npm test`, `npm run build`.

