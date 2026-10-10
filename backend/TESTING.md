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
- `postgres-test`: PostgreSQL real con Flyway y `ddl-auto=validate`; valida V1–V4 y las entidades.

## Lectura de cuadernos (ENS-120)

El worker está deshabilitado en ambos perfiles de pruebas. Las pruebas llaman explícitamente al
caso de uso o al repositorio y no dependen del reloj del scheduler.
`ProcesamientoPostgresIntegrationTest` cubre reclamos/reservas concurrentes con dos conexiones,
finalización idempotente, recuperación con token, cambio de día UTC y aislamiento.
`LecturaSeguridadIntegrationTest` usa la cadena de seguridad real y PostgreSQL para verificar
401/403/404, reintento y consumo del negocio autenticado.

Desde la raíz: `python -m unittest discover -s scripts/tests -v` verifica la medición con datos
sintéticos, filas extra, anotaciones, costos desconocidos y el impedimento de declarar precisión real
con simulación. El comando real es `python scripts/evaluar_cuadernos.py`; sin set, debe fallar sin métricas.

En Windows, si un jar está ejecutándose, se puede ejecutar el mismo `clean verify` con un POM temporal
en backend que sólo cambie `build.directory` a una carpeta dentro de target. No requiere detener el servidor.
`ArchitectureTest` resuelve las clases del directorio de compilación usado, sin asumir target/classes.
  Lo usa `PostgresRepositoryTestSupport`.

## Estilo

Arrange-Act-Assert con helpers privados `givenX` / acción / `thenX`, nombres `metodoShouldXWhenY`, `@DisplayName`
en español, un comportamiento por test, imports explícitos. Guía completa: skill `escribir-tests`.
