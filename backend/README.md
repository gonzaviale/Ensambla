# Ensambla · Backend

API REST de Ensambla. Spring Boot 4 · Java 21 · PostgreSQL 17 · Flyway · Spring Security con sesión por cookie.

## Cómo correrlo

Requisitos: JDK 21 y Docker.

```bash
docker compose up -d               # PostgreSQL 17 en localhost:5435 y MinIO (S3) en localhost:9000
./mvnw spring-boot:run             # perfil dev, http://localhost:8080
```

- Swagger: <http://localhost:8080/swagger-ui.html> (sólo con el perfil `dev`).
- MinIO: consola en <http://localhost:9001> (usuario `ensambla_minio`, contraseña `ensambla_minio_pass`). Para
  cambiar puertos o credenciales del compose copiá `.env.example` a `.env`.
- Los valores por defecto del perfil `dev` apuntan a la base del `docker-compose.yml`. Para cambiarlos copiá
  `src/main/resources/application-ensambla-secrets.properties.example` a `application-ensambla-secrets.properties`
  (está en `.gitignore`) o usá variables de entorno (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
  `SPRING_DATASOURCE_PASSWORD`, `FRONTEND_URL`, `PORT`).
- Los tests (`./mvnw verify`) levantan PostgreSQL con Testcontainers: necesitan Docker corriendo.

## Arquitectura

Clean Architecture en dos capas; `domain` **no** depende de `infrastructure` (lo verifica `ArchitectureTest`).

```
com.ensambla.backend
├── domain
│   ├── model/<area>/        modelos (Lombok) y model/enums
│   ├── repository/<area>/   puertos de persistencia
│   ├── service/<area>/      reglas que tocan repositorio
│   ├── useCase/<area>/      un caso de uso por clase, execute()
│   ├── dto/<area>/          request/response de los use cases (records)
│   ├── mapper/<area>/       un mapper por use case
│   ├── port/                puertos de salida (LectorCuadernoPort, AlmacenamientoArchivosPort…)
│   └── exception/           ResourceNotFound, DuplicateResource, BusinessRule, CredencialesInvalidas
└── infrastructure
    ├── config/              SecurityConfig, CorsFilter, SwaggerConfig, useCase/<Area>UseCaseConfig
    ├── ia/                  adapter de lectura con IA (LectorCuadernoIaAdapter, IaConfig)
    ├── almacenamiento/      S3/MinIO y URLs firmadas (AlmacenamientoS3Adapter, AlmacenamientoConfig)
    ├── security/            SesionAdapter, UsuarioActivoFilter, SecurityErrorHandler
    ├── presentation/        controller, dto web, mapper, exception (GlobalExceptionHandler)
    └── repository/          entity, jpaRepository/{interfaces,implementation}, mapper
```

Flujo: `Controller → PresentationMapper → UseCase → Service → Repository (puerto) → RepositoryImpl → JPA`.
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

## Almacenamiento de archivos

Las fotos del cuaderno se guardan en un bucket privado compatible con S3 (MinIO en desarrollo). El dominio usa
`domain.port.AlmacenamientoArchivosPort`, que devuelve URLs firmadas de subida (`PUT`, con el `Content-Type` firmado) y
de lectura (`GET`); vencen según `ensambla.almacenamiento.expiracion-url` (15 min por defecto). Sin URL firmada vigente
MinIO responde 403. La implementación está en `infrastructure/almacenamiento`:

- `AlmacenamientoConfig` arma el cliente y el firmador del SDK de S3 en modo *path-style* (lo exige MinIO).
- `InicializadorBucket` crea el bucket al arrancar si no existe, sin política pública. Se apaga con
  `ENSAMBLA_ALMACENAMIENTO_INICIALIZAR_BUCKET=false` (así está en `prod` hasta definir el almacenamiento de producción).
- Variables: `ENSAMBLA_ALMACENAMIENTO_ENDPOINT`, `_ACCESS_KEY`, `_SECRET_KEY`, `_BUCKET`, `_REGION`, `_EXPIRACION_URL`.
  Los valores por defecto apuntan al MinIO del `docker-compose.yml`.

**Subida de fotos (T-090).** `POST /api/lotes` recibe hasta 10 archivos de hasta 10 MB cada uno. El tipo se detecta
por los primeros bytes (JPEG, PNG, WEBP, HEIC o PDF), no por lo que declara el navegador; un archivo vacío, grande o de
otro tipo responde 400 con un mensaje que dice qué foto falló. El backend sube cada foto a `negocios/{negocioId}/fotos/`
y guarda el lote; si algo falla a mitad de camino borra del bucket lo que ya había subido. Un lote de otro negocio
responde 404, así que sus URLs firmadas nunca llegan a otro negocio.

La imagen es `cgr.dev/chainguard/minio`: MinIO dejó de publicar imágenes oficiales y Chainguard la compila desde el
código de MinIO. Los tests (`AlmacenamientoS3IntegrationTest`) levantan la misma imagen con Testcontainers.

## Seguridad

- Sesión por cookie `SESSION` (Spring Session JDBC), `HttpOnly`, `SameSite=Lax`, `Secure` fuera de `dev`.
- CSRF: las escrituras (`POST/PUT/PATCH/DELETE`) exigen el header `X-CSRF-TOKEN`; el token se pide a `GET /api/auth/csrf`.
- CORS: sólo el origen de `frontend.url`, con credenciales.
- Rutas públicas (ver `SecurityConfig`): `GET /api/plantillas`, `GET /api/auth/csrf`, `POST /api/auth/{registro,login,logout}`
  y Swagger. Todo lo demás exige sesión (401 con el cuerpo `ErrorResponse` estándar).

## Endpoints

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `GET /api/auth/csrf` | público | Token CSRF y nombre del header |
| `GET /api/plantillas` | público | Plantillas activas con su estructura, ordenadas por nombre |
| `GET /api/proyectos` | sesión | Proyectos del negocio del usuario autenticado |
| `GET /api/proyectos/{proyectoId}` | sesión | Proyecto con sus bloques y campos ordenados |
| `POST /api/proyectos` | sesión + CSRF | Crear en blanco: `nombre` y `descripcion` opcional |
| `POST /api/proyectos/desde-plantilla/{plantillaId}` | sesión + CSRF | Copiar una plantilla activa con ids nuevos |
| `PATCH /api/proyectos/{proyectoId}` | sesión + CSRF | Renombrar: `nombre` |
| `DELETE /api/proyectos/{proyectoId}` | sesión + CSRF | Eliminar proyecto y sus bloques, campos y registros |
| `POST /api/lotes` | sesión + CSRF | Subir fotos del cuaderno (multipart, campo `fotos`): crea un lote `PENDIENTE` |
| `GET /api/lotes/{loteId}` | sesión | Lote con sus fotos y una URL firmada de lectura por foto |

V3 carga Inventario, Pedidos y Clientes. La copia de una plantilla se guarda en una única transacción.
El negocio se resuelve desde el dueño autenticado (primero por creación e id, hasta el selector de T-061);
no se acepta un `negocioId` del cliente. Consultar, renombrar o eliminar un proyecto ajeno devuelve 404.

Los de acceso (`/api/auth/register`, `/login`, `/logout`, `/me`) los implementan T-005 y T-006; el contrato que espera el
frontend está en `frontend/src/api/auth.ts`.

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

`./mvnw verify` corre los tests y exige **100 % de líneas y ramas por clase** (JaCoCo). Detalle en [TESTING.md](TESTING.md).
