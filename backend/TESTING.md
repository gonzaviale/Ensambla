# Tests del backend

```bash
./mvnw test      # sólo tests
./mvnw verify    # tests + reporte JaCoCo + chequeo de cobertura (lo que corre el CI)
```

Necesitan Docker (Testcontainers levanta `postgres:17-alpine`). Resúmenes en `target/surefire-reports/*.txt` y
cobertura en `target/site/jacoco/index.html`.

## Regla de cobertura

`verify` falla si alguna **clase** queda por debajo de 100 % de líneas o de ramas. Lombok no cuenta
(`lombok.addLombokGeneratedAnnotation = true`). Para ver qué clase falta mirá `target/site/jacoco/jacoco.csv`
(columnas `INSTRUCTION_MISSED`, `BRANCH_MISSED`, `LINE_MISSED`).

## Qué se prueba en cada capa

| Clase | Test | Cómo |
|---|---|---|
| `*UseCase` | `*UseCaseTest` | `@Mock` del servicio, mapper real |
| `*Service` | `*ServiceTest` | `@Mock` de repositorios + `@InjectMocks` |
| `*Mapper` (dominio) | `*MapperTest` | sin mocks, todos los campos y nulos |
| `*PresentationMapper` | `*PresentationMapperTest` | sin mocks |
| `*Controller` | `*ControllerTest` | `MockMvcBuilders.standaloneSetup` + `GlobalExceptionHandler` |
| `*RepositoryImpl` | `*RepositoryImplTest` | `@Mock` del JPA repo, mapper real |
| `*RepositoryMapper` | `*RepositoryMapperTest` | sin mocks |
| Esquema y consultas | `EsquemaPostgresIntegrationTest`, `I*JpaRepositoryTest` | PostgreSQL real, `@Transactional` (rollback) |
| Seguridad HTTP | `SeguridadIntegrationTest` | contexto completo con H2 (`h2-test`) |
| Arquitectura | `ArchitectureTest` | `domain` no referencia `infrastructure` |

## Perfiles de test

- `h2-test`: H2 en memoria (`MODE=PostgreSQL`), `ddl-auto=create-drop`, sin Flyway. Para el contexto y la seguridad.
- `postgres-test`: PostgreSQL real con Flyway y `ddl-auto=validate`; así **V1 y las entidades deben coincidir**.
  Lo usa `PostgresRepositoryTestSupport`.

## Estilo

Arrange-Act-Assert con helpers privados `givenX` / acción / `thenX`, nombres `metodoShouldXWhenY`, `@DisplayName`
en español, un comportamiento por test, imports explícitos. Guía completa: skill `escribir-tests`.
