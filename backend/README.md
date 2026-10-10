# Ensambla · Backend

API REST de Ensambla. Spring Boot 4 · Java 21 · PostgreSQL 17 · Flyway · Spring Security con sesión por cookie.

## Cómo correrlo

Requisitos: JDK 21 y Docker.

```bash
docker compose up -d postgres      # PostgreSQL 17 en localhost:5435 (db ensambla, user ensambla_user)
./mvnw spring-boot:run             # perfil dev, http://localhost:8080
```

- Swagger: <http://localhost:8080/swagger-ui.html> (sólo con el perfil `dev`).
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
│   ├── port/                puertos de salida (contraseñas, etc.)
│   └── exception/           ResourceNotFound, DuplicateResource, BusinessRule, CredencialesInvalidas
└── infrastructure
    ├── config/              SecurityConfig, CorsFilter, SwaggerConfig, useCase/<Area>UseCaseConfig
    ├── security/            SesionAdapter, UsuarioActivoFilter, SecurityErrorHandler
    ├── presentation/        controller, dto web, mapper, exception (GlobalExceptionHandler)
    └── repository/          entity, jpaRepository/{interfaces,implementation}, mapper
```

Flujo: `Controller → PresentationMapper → UseCase → Service → Repository (puerto) → RepositoryImpl → JPA`.
Para sumar un endpoint seguí el skill `crear-use-case` (`../.claude/skills`); el módulo `plantilla` es la referencia.

## Modelo de datos

`usuario`, `negocio`, `plantilla`, `proyecto`, `bloque`, `campo`, `registro` (+ tablas de sesión). Ver
[../docs/modelo-datos.md](../docs/modelo-datos.md). Migraciones en `src/main/resources/db/migration`; una migración
aplicada **nunca se edita**.

## Seguridad

- Sesión por cookie `SESSION` (Spring Session JDBC), `HttpOnly`, `SameSite=Lax`, `Secure` fuera de `dev`.
- CSRF: las escrituras (`POST/PUT/PATCH/DELETE`) exigen el header `X-CSRF-TOKEN`; el token se pide a `GET /api/auth/csrf`.
- CORS: sólo el origen de `frontend.url`, con credenciales.
- Rutas públicas (ver `SecurityConfig`): `GET /api/plantillas`, `GET /api/auth/csrf`, `POST /api/auth/{register,login,logout}`
  y Swagger. Todo lo demás exige sesión (401 con el cuerpo `ErrorResponse` estándar).

## Endpoints

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `GET /api/auth/csrf` | público | Token CSRF y nombre del header |
| `POST /api/auth/register` | público + CSRF | Crea usuario y negocio; 201, 400 o 409 |
| `POST /api/auth/login` | público + CSRF | Abre sesión; 200, 400 o 401 |
| `POST /api/auth/logout` | público + CSRF | Invalida sesión y cookie; 204 |
| `GET /api/auth/me` | sesión | Identidad actual sin contraseña ni hash; 200 o 401 |
| `GET /api/plantillas` | público | Plantillas activas con su estructura, ordenadas por nombre |

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

`./mvnw verify` corre los tests y exige **100 % de líneas y ramas por clase** (JaCoCo). Detalle en [TESTING.md](TESTING.md).
