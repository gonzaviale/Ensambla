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

## Planes y suscripciones (ENS-94)

- `SuscripcionServiceTest`, `RegistroServiceTest` y los tests de mappers/adaptadores verifican
  la asignación del gratuito, las fechas recibidas del caso de uso y la consulta por negocio.
- `PlanesPostgresIntegrationTest` comprueba el catálogo inicial pendiente de configuración,
  persistencia de condiciones, aislamiento, suscripción única, claves foráneas y restricciones.
- `MigracionPlanesPostgresIntegrationTest` crea un esquema aislado en V11, agrega un negocio anterior
  a V12 y aplica V12: todos reciben el gratuito y se conserva la cantidad de negocios y registros.
- `RegistroSuscripcionRollbackPostgresTest` provoca un fallo de la suscripción y comprueba en
  PostgreSQL que no quedan ni el usuario ni el negocio del registro fallido.
- Los valores de cuotas, moneda y funciones de los fixtures sólo son datos de tests; no establecen
  las condiciones comerciales del catálogo inicial.

Verificación local: `mvnw.cmd -B clean verify`, 1.364 tests sin fallos, errores ni omitidos;
100 % de líneas y ramas por clase (422 clases evaluadas). La migración se ejecuta en los contenedores
temporales de pruebas, sin necesidad de aplicar cambios a la base local habitual.

## Control por plan (ENS-95)

- `PlanTest`, `SuscripcionTest` y `ControlPlanServiceTest`: cuotas, funciones habilitadas, vigencia
  con inicio inclusivo y fin exclusivo, fallback al gratuito y mes calculado en UTC.
- `ControlPlanPostgresIntegrationTest`: dos altas simultáneas de sistemas y registros, lotes completos,
  conservación de datos, concurrencia entre lectura inmediata y cola, reservas pendientes, cambio de mes,
  finalización tardía y consumo inmediato con costo desconocido.
- `PlanSeguridadIntegrationTest`: HTTP con la cadena de seguridad real, 403 con código de plan,
  funciones premium, 404 de recursos ajenos, altas por cuota, edición conservada, lectura inmediata,
  subida y reintentos; el negocio siempre sale de la sesión.
- `ConfirmacionLecturaIntegrationTest` también verifica que un rechazo de cuota revierta la nueva
  estructura creada para una foto y deje el lote listo para revisar, sin guardar filas parciales.
- `MigracionPlanesPostgresIntegrationTest` cubre V11→V12 y V12→V13 sin perder registros, consumo
  ni un precio premium definido previamente. Los precios de ese test son datos ficticios.
- Los fixtures anteriores que usan servicios de escritura ahora asignan una suscripción; el control
  de producción no se deshabilita ni se simula en las pruebas de integración.

La suite completa sigue exigiendo 100 % de líneas y ramas por clase. Los tests del evaluador
se ejecutan con el jar compilado para incluir la prueba empaquetada sin omitirla.

Verificación local al cerrar ENS-95: 1.426 pruebas del backend aprobadas sin fallos, errores ni
omitidas, con 430 clases, 4.183 líneas y 992 ramas completamente cubiertas. El evaluador pasó
sus 39 pruebas sin omitidas. Las migraciones se probaron en PostgreSQL temporal.

## Prueba premium (ENS-96)

- `PruebaPremiumTest` y `SuscripcionTest`: 168 horas exactas, inicio incluido, fin excluido y
  máximo de diez páginas totales durante la prueba.
- `PruebaPremiumServiceTest`: activación, repetición, falta de catálogo/suscripción, premium ya
  vigente, vencimiento anticipado y conservación de una suscripción activada después de seleccionar candidatos.
- `PruebaPremiumPostgresIntegrationTest`: inicio concurrente único; páginas previas del mismo día;
  cruce de mes; competencia entre lectura inmediata y reserva; cuota diaria; reservas rechazadas y
  duplicadas; timeout finalizado luego del vencimiento; job repetible; datos por encima del gratuito;
  rollback del inicio si falla la actualización de suscripción; aislamiento, cascada y restricciones SQL.
- `MigracionPlanesPostgresIntegrationTest` agrega V13→V14 con suscripciones, registros y consumo existentes,
  comprobando que migrar no inicia pruebas automáticamente.
- `PlanSeguridadIntegrationTest` comprueba la ruta privada de inicio, sesión, CSRF, imposibilidad de elegir
  fechas/cuota/negocio desde el cliente, respuesta repetida y 403 `LIMITE_IA_PRUEBA` en la ruta inmediata.
- Los tests de casos de uso, mappers, repositorio, controller y configuración cubren el contrato por capa.
  `VencimientoPruebasWorkerTest` verifica programación automática y deshabilitación explícita.

El worker se deshabilita en ambos perfiles de tests y se ejecuta explícitamente con reloj controlado.
Las migraciones se ejecutan sólo en PostgreSQL temporal, sin usar un proveedor de IA real.
Detalle del contrato y resultado final: [plan-ENS-96.md](../docs/plan-ENS-96.md).

Verificación final de ENS-96: `mvnw.cmd -B clean verify` aprobado con 1.489 pruebas en 233 clases,
cero fallos, errores u omitidas; 444 clases con 4.277 líneas y 1.014 ramas cubiertas (100 % por clase).
El evaluador pasó 39 pruebas sin omitidas usando `ENSAMBLA_EVAL_JAR` con el jar recién compilado.

## Estilo de los tests

Arrange-Act-Assert con helpers privados `givenX` / acción / `thenX`, nombres `metodoShouldXWhenY`, `@DisplayName`
en español, un comportamiento por test, imports explícitos. Guía completa: skill `escribir-tests`.
