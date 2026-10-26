# Ensambla · Backend

API REST de Ensambla. Spring Boot 4 · Java 21 · PostgreSQL 17 · Flyway · Spring Security con sesión por cookie.

## Cómo correrlo

Requisitos: JDK 21 y Docker.

```bash
docker compose up -d               # PostgreSQL 17 en localhost:5435
./mvnw spring-boot:run             # perfil dev, http://localhost:8080
```

- Swagger: <http://localhost:8080/swagger-ui.html> (sólo con el perfil `dev`).
- Para cambiar el puerto o las credenciales de PostgreSQL del compose copiá `.env.example` a `.env`.
- Los valores por defecto del perfil `dev` apuntan a la base del `docker-compose.yml`. Para cambiarlos copiá
  `src/main/resources/application-ensambla-secrets.properties.example` a `application-ensambla-secrets.properties`
  (está en `.gitignore`) o usá variables de entorno (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
  `SPRING_DATASOURCE_PASSWORD`, `FRONTEND_URL`, `PORT`).
- Los tests (`./mvnw verify`) levantan PostgreSQL con Testcontainers: necesitan Docker corriendo.

## Arquitectura

Clean Architecture en dos capas; `domain` **no** depende de `infrastructure` ni de Spring (lo verifica
`ArchitectureTest`). Las reglas, con su porqué y ejemplos, están en [ARQUITECTURA.md](ARQUITECTURA.md).

```
com.ensambla.backend
├── domain
│   ├── model/<area>/        modelos, reglas puras, ids tipados (<Modelo>Id) y enums del área
│   ├── repository/<area>/   puertos de persistencia
│   ├── service/<area>/      sólo lógica que comparten dos o más casos de uso
│   ├── useCase/<area>/      un subpaquete por caso de uso: la clase (sin sufijo, un execute()) y sus DTOs
│   ├── port/                puertos de salida (TransaccionPort, LectorCuadernoPort…)
│   └── exception/           ResourceNotFound, DuplicateResource, BusinessRule, CredencialesInvalidas
└── infrastructure
    ├── config/              SecurityConfig, CorsFilter, SwaggerConfig, IdentificadoresConfig, useCase/<Area>UseCaseConfig
    ├── ia/                  adapter de lectura con IA (LectorCuadernoIaAdapter, IaConfig)
    ├── security/            SesionAdapter, UsuarioActivoFilter, SecurityErrorHandler
    ├── presentation/        controller, dto web, mapper, exception (GlobalExceptionHandler)
    └── repository/          entity, jpaRepository/{interfaces,implementation}, mapper, TransaccionAdapter
```

Flujo: `Controller → PresentationMapper → Caso de uso → (Servicio compartido) → Puerto → RepositoryImpl → JPA`.
Para sumar un endpoint seguí el skill `crear-use-case` (`../.claude/skills`); el módulo `plantilla` es la referencia.

## Modelo de datos

`usuario`, `negocio`, `plantilla`, `proyecto`, `bloque`, `campo`, `registro` (V1) y `lote_subida`, `foto_cuaderno`,
`lectura_ia` para la digitalización del cuaderno (V2), más las tablas de sesión. Ver
[../docs/modelo-datos.md](../docs/modelo-datos.md). Migraciones en `src/main/resources/db/migration`; una migración
aplicada **nunca se edita**.

## Lectura del cuaderno con IA

El dominio pide una lectura a través de `domain.port.LectorCuadernoPort`; la implementación vive en `infrastructure/ia`:

- `PromptLecturaCuaderno` arma la petición con `resources/ia/prompt-lectura-cuaderno.md` y el JSON Schema
  `resources/ia/esquema-lectura-cuaderno.json` (salida estructurada). Si cambian, subir `PromptLecturaCuaderno.VERSION`.
- `ClienteModeloVision` es el único punto que habla con el proveedor. Hoy existe `ClienteModeloVisionSimulado`, que
  devuelve `resources/ia/lectura-simulada.json` sin costo (desarrollo, E2E y demo).
- `LectorCuadernoIaAdapter` valida la respuesta (tipos, claves, rangos de confianza, páginas y que el bloque sugerido sea
  del negocio), la convierte al dominio y registra tokens y duración. Cualquier falla sale como `LecturaCuadernoException`.

Para conectar el proveedor real (después del spike T-094): implementar `ClienteModeloVision`, registrarlo en `IaConfig`
con `@ConditionalOnProperty(prefix = "ensambla.ia", name = "proveedor", havingValue = "<nombre>")` y definir
`ENSAMBLA_IA_PROVEEDOR`, `ENSAMBLA_IA_MODELO` y `ENSAMBLA_IA_API_KEY`. Sin esas variables se usa `simulado`.

## Subida de fotos

`POST /api/lotes` (T-090) recibe hasta 10 archivos de hasta 10 MB cada uno. El tipo se detecta por los primeros bytes
(JPEG, PNG, WEBP, HEIC o PDF), no por lo que declara el navegador; un archivo vacío, grande o de otro tipo responde 400
con un mensaje que dice qué foto falló. Las fotos se guardan con `ArchivoCuadernoLocalAdapter` (ver
[lectura-cuaderno-ia.md](../docs/lectura-cuaderno-ia.md)); si algo falla a mitad de camino se borra lo que ya se había
guardado. Un lote de otro negocio responde 404.

## Planes, suscripciones, prueba y renovación (ENS-94 a ENS-98)

V12 agrega el catálogo `plan` y la suscripción actual `suscripcion`, asigna el gratuito activo a
negocios existentes y lo crea junto con cada cuenta nueva en una sola transacción.
V13 configura los límites aprobados: gratuito de un sistema, 1.000 registros totales y diez páginas
de IA por mes calendario UTC; premium sin límite comercial de sistemas/registros y cien páginas mensuales.

`limites_configurados=true` habilita el control de capacidades; `configurado` identifica precio/moneda.
V15 configura los catálogos Premium pendientes a ARS 14.900 finales cada treinta días, conservando
una configuración comercial que ya existiera. Recetas/costos, reportes filtrados y XLSX requieren premium activo.
Las altas se validan dentro de la transacción, con bloqueo por negocio; las dos vías de IA comparten
el consumo mensual, incluidas reservas, y conservan el límite diario técnico.

`POST /api/importaciones/lectura` identifica el negocio desde la sesión y cuenta el intento inmediato
antes de llamar a IA. Si hay lecturas inmediatas, el resumen diario marca tokens/costo como incompletos.
Un rechazo de plan devuelve 403 con `message` y `codigo`; un recurso ajeno sigue devolviendo 404.
Superar una cuota no borra los datos ni bloquea su lectura/edición básica.

V14 incorpora una única prueba premium por negocio. `POST /api/planes/prueba` requiere sesión y CSRF;
inicia siete días exactos de capacidades premium y **diez páginas de IA totales**, sin renovación mensual.
Las dos vías de IA comparten el contador persistido, incluidas las reservas admitidas; fallos y timeouts
conservan el cargo del intento. Una reserva rechazada no lo consume. Repetir el inicio no renueva la prueba.
Al vencer se usa el gratuito sin borrar datos, incluso antes de ejecutar el job automático de cada minuto.
El job conserva cualquier premium ya activado por un pago. Puede deshabilitarse con `ENSAMBLA_PRUEBAS_WORKER=false`.
`GET /api/planes` requiere sesión y devuelve una vista coherente del catálogo, el plan efectivo,
la elegibilidad e historial de prueba y los contadores del negocio. Responde con `Cache-Control: no-store`;
el negocio se resuelve exclusivamente desde la sesión. `consultadoEn` es la hora del servidor.
`suscripcion` distingue prueba activa y posibilidad de iniciarla; `uso` incluye sistemas, registros,
páginas, límite y período (`PRUEBA` total o `MENSUAL` UTC). `prueba` es nulo si nunca se usó.
El precio se identifica con `precioConfigurado`, separado de `limitesConfigurados`.
La consulta respeta el vencimiento exacto aunque el job todavía no haya persistido el gratuito.
ENS-98 agrega contratación mediante Suscripciones de Mercado Pago, primer cobro al terminar la
prueba vigente, conciliación, pagos idempotentes y cancelación de renovaciones conservando el
período pagado. Autorizar un contrato o volver del checkout no concede acceso pago. El adaptador
verifica cuenta, aplicación, ambiente, importe, moneda y referencia de cada pago consultado.
Sin configurar una aplicación del proveedor, los cobros permanecen deshabilitados.
Con cuentas de Mercado Pago de prueba se verificaron creación, autorización, retorno y cancelación
desde Planes, conservando los días y las diez páginas de la prueba. El checkout validó la tarjeta por
importe cero. Un segundo circuito sin prueba gratuita verificó una factura aprobada de ARS 14.900,
Premium durante 720 horas, actualización idempotente y cancelación conservando el período pagado.
El adaptador valida la aplicación desde el contrato cuando falta en el pago y comprueba `/users/me`
antes de aceptar el formato live de cuentas de prueba. También se sincronizó un pago rechazado
sin correo de comprador, exigiendo vendedor de prueba y coincidencia del comprador con el contrato:
quedó registrado una sola vez y el negocio conservó Gratuito. Esta excepción no admite pagos aprobados.
Siguen pendientes las renovaciones posteriores, el reembolso externo y las notificaciones firmadas
del proveedor. Las pruebas automatizadas usan
una pasarela ficticia; los circuitos externos descritos usaron cuentas y tarjetas de prueba.
Detalle y configuración:
[renovaciones y pagos](../docs/plan-ENS-98.md),
[guía local de Mercado Pago](../docs/mercado-pago-configuracion-local.md),
[pantalla y consulta de planes](../docs/plan-ENS-97.md),
[prueba premium](../docs/plan-ENS-96.md), [control por plan](../docs/plan-ENS-95.md)
y [modelo de datos](../docs/modelo-datos.md).

## Seguridad

- Sesión por cookie `SESSION` (Spring Session JDBC), `HttpOnly`, `SameSite=Lax`, `Secure` fuera de `dev`.
- CSRF: las escrituras (`POST/PUT/PATCH/DELETE`) exigen el header `X-CSRF-TOKEN`; el token se pide a `GET /api/auth/csrf`.
- La excepción exacta es `POST /api/pagos/mercadopago`: no usa sesión ni CSRF; valida firma HMAC
  y consulta la API del proveedor. Las demás escrituras de pago conservan sesión y CSRF.
- CORS: sólo el origen de `frontend.url`, con credenciales.
- Rutas públicas (ver `SecurityConfig`): `GET /api/planes/catalogo`, `GET /api/plantillas`, `GET /api/auth/csrf`, `POST /api/auth/{register,login,logout}`
  y Swagger, además de `POST /api/pagos/mercadopago` con firma. Todo lo demás exige sesión
  (401 con el cuerpo `ErrorResponse` estándar).

## Endpoints

La búsqueda privada de direcciones y el catálogo de rubros se documentan en
[Configuración, ubicación y rubro](../docs/ubicacion-negocio.md), incluidos ENS-69, persistencia y límites de Nominatim.

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `GET /api/auth/csrf` | público | Token CSRF y nombre del header |
| `POST /api/auth/register` | público + CSRF | Crea usuario y negocio; 201, 400 o 409 |
| `POST /api/auth/login` | público + CSRF | Abre sesión; 200, 400 o 401 |
| `POST /api/auth/logout` | público + CSRF | Invalida sesión y cookie; 204 |
| `GET /api/auth/me` | sesión | Identidad actual sin contraseña ni hash; 200 o 401 |
| `GET /api/business` | sesión del dueño | Configuración de su negocio, incluida ubicación y rubro; sin identificadores ajenos |
| `PATCH /api/business` | sesión del dueño + CSRF | Editar nombre, ubicación completa, rubro y participación en agregados; campos omitidos conservan su valor |
| `GET /api/planes/catalogo` | público | Catálogo global sin suscripción, consumo ni identificadores de negocio (ENS-244) |
| `GET /api/planes` | sesión | Catálogo, plan efectivo, prueba y uso del negocio |
| `POST /api/planes/prueba` | sesión + CSRF | Una prueba por negocio, siete días y diez páginas totales |
| `GET /api/planes/renovacion` | sesión | Disponibilidad de contratación y último contrato del negocio |
| `POST /api/planes/renovacion` | sesión + CSRF | Crea una contratación o recupera la existente, sin aceptar precio ni negocio del cliente |
| `POST /api/planes/renovacion/cancelar` | sesión + CSRF | Solicita la baja y conserva prueba/período pagado |
| `POST /api/planes/renovacion/actualizar` | sesión + CSRF | Concilia contrato y facturas con Mercado Pago; 204 |
| `POST /api/pagos/mercadopago` | firma del proveedor | Notificación `type` y `data.id`, HMAC y consulta autenticada a la API |
| `GET /api/plantillas` | público | Plantillas activas con su estructura, ordenadas por nombre |
| `GET /api/proyectos` | sesión | Proyectos del negocio del usuario autenticado |
| `GET /api/proyectos/{proyectoId}` | sesión | Proyecto con sus bloques y campos ordenados |
| `POST /api/proyectos` | sesión + CSRF | Crear en blanco: `nombre` y `descripcion` opcional |
| `POST /api/proyectos/desde-plantilla/{plantillaId}` | sesión + CSRF | Copiar una plantilla activa con ids nuevos |
| `PATCH /api/proyectos/{proyectoId}` | sesión + CSRF | Renombrar: `nombre` |
| `DELETE /api/proyectos/{proyectoId}` | sesión + CSRF | Eliminar proyecto y sus bloques, campos y registros |
| `GET /api/proyectos/{proyectoId}/bloques` | sesión | Bloques del proyecto ordenados por `posicion` (sin campos; para la estructura completa, `GET /api/proyectos/{id}`) |
| `POST /api/proyectos/{proyectoId}/bloques` | sesión + CSRF | Agrega un bloque al final: `nombre`. `201`, o `409` si el nombre ya existe en el proyecto (sin distinguir mayúsculas) |
| `PATCH /api/bloques/{bloqueId}` | sesión + CSRF | Renombrar: `nombre`. `409` si lo usa otro bloque del proyecto |
| `DELETE /api/bloques/{bloqueId}` | sesión + CSRF | Elimina el bloque y, en cascada, sus campos y registros. `204` |
| `GET /api/bloques/{bloqueId}/campos` | sesión | Campos del bloque ordenados por `posicion` |
| `POST /api/bloques/{bloqueId}/campos` | sesión + CSRF | Agrega un campo al final: `nombre`, `tipo` (`TEXTO`, `NUMERO`, `FECHA`, `OPCION`, `SI_NO`), `obligatorio` (por defecto `false`), `valorInicial` y `opciones` (obligatorias y sin repetir si es `OPCION`; se ignoran en los demás tipos). `201` con `Location: /api/campos/{id}`, `400` con `errors` por dato o `409` si el nombre ya existe en el bloque (sin distinguir mayúsculas) |
| `PATCH /api/campos/{campoId}` | sesión + CSRF | Reemplaza toda la configuración (mismo body que crear). `400` o `409` como al crear; el campo puede conservar su nombre |
| `DELETE /api/campos/{campoId}` | sesión + CSRF | Elimina el campo; los registros se conservan (sus valores de ese campo quedan sin usarse). `204` |
| `POST /api/lotes` | sesión + CSRF | Subir fotos del cuaderno (multipart, campo `fotos`): crea un lote `PENDIENTE` |
| `GET /api/lotes/{loteId}` | sesión | Lote con sus fotos |
| `POST /api/bloques/{bloqueId}/registros` | sesión + CSRF | Carga un registro; `valores` va indexado por id de campo. `201`, o `400` con `errors` por campo |
| `GET /api/bloques/{bloqueId}/registros` | sesión | Registros del bloque. Parámetros: `q` (texto entre los valores), `orden` (id de campo o `creadoEn`), `direccion` (`asc`/`desc`, por defecto `desc`), `pagina` (desde 0), `tamanio` (1 a 100, por defecto 20). Además de la página, devuelve `totales`: `cantidad` y `sumas` (suma de cada campo numérico, por id de campo) de **todos** los registros que cumplen `q`, no sólo de la página |
| `GET /api/registros/{id}` | sesión | Un registro |
| `PATCH /api/registros/{id}` | sesión + CSRF | Edita sólo los campos enviados; un valor `null` o vacío borra el dato. Revalida el registro completo |
| `DELETE /api/registros/{id}` | sesión + CSRF | Elimina el registro. `204` |
| `GET /api/lotes/{loteId}/lectura` | sesión | Estado, motivo y propuesta tipada del lote |
| `POST /api/lotes/{loteId}/reintentar` | sesión + CSRF | Reencolar ERROR del negocio; 202 |
| `GET /api/ia/consumo` | sesión | Consumo, reservas y disponibilidad diaria UTC |

V3 carga Inventario, Pedidos y Clientes. La copia de una plantilla se guarda en una única transacción.
El negocio se resuelve desde el dueño autenticado (primero por creación e id, hasta el selector de T-061);
no se acepta un `negocioId` del cliente. Consultar, renombrar o eliminar un proyecto ajeno devuelve 404.
Los campos y los registros se resuelven igual: un bloque, un campo o un registro de otro negocio responde 404.

Los endpoints de acceso están implementados. El contrato del cliente está en `frontend/src/api/auth.ts`.
Ver [acceso-sesion.md](../docs/acceso-sesion.md) para validaciones, sesión y pruebas.

### Formato de error

```json
{
  "timestamp": "2026-10-09T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Error de validación en los datos enviados",
  "path": "/api/proyectos",
  "traceId": "9c1d…",
  "errors": { "nombre": "no debe estar vacío" }
}
```

## Calidad

ENS-120 agrega una cola PostgreSQL con reclamos exclusivos, timeout, reintentos, recuperación y
reservas diarias de consumo. El worker persiste propuestas para revisión; la confirmación corresponde
a H-30. El modelo real y las fotos de evaluación siguen pendientes por acuerdo del usuario.
Configuración, integración con ENS-124 y evaluación:
[lectura-cuaderno-ia.md](../docs/lectura-cuaderno-ia.md).

`./mvnw verify` corre los tests y exige **100 % de líneas y ramas por clase** (JaCoCo). Detalle en [TESTING.md](TESTING.md).

<!-- prueba: dispara ci-backend sin Vercel -->
