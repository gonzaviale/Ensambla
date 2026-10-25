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
| Caso de uso (`CrearProyecto`) | `CrearProyectoTest` | `@Mock` de puertos y servicios compartidos, `TransaccionDirecta` |
| DTO con fábrica `de()` | `*ResponseTest`, `*ItemTest` | sin mocks, todos los campos y nulos |
| Método `default` de un puerto | `<Puerto>Test` en `domain/repository` | `mock(Puerto.class, CALLS_REAL_METHODS)`; los `when` van sobre los métodos abstractos |
| `*Service` (compartido) | `*ServiceTest` | `@Mock` de repositorios + `@InjectMocks` |
| Model con reglas, ids | `*Test`, `IdentificadorTest` | sin mocks |
| `*PresentationMapper` | `*PresentationMapperTest` | sin mocks |
| `*Controller` | `*ControllerTest` | `support/MockMvcDePrueba.standalone(...)` (convierte los ids tipados) |
| `*RepositoryImpl` | `*RepositoryImplTest` | `@Mock` del JPA repo, mapper real |
| `*RepositoryMapper` | `*RepositoryMapperTest` | sin mocks; ids comparados con `support/IdsDePrueba` |
| Esquema y consultas | `EsquemaPostgresIntegrationTest`, `I*JpaRepositoryTest` | PostgreSQL real, `@Transactional` (rollback) |
| Seguridad HTTP | `SeguridadIntegrationTest`, `*SeguridadIntegrationTest` | contexto completo contra PostgreSQL (`PostgresRepositoryTestSupport`) |
| Arquitectura | `ArchitectureTest` | las reglas de [ARQUITECTURA.md](ARQUITECTURA.md) |

## Perfil de test

No se usa H2: todo test que levanta base o contexto extiende `support/PostgresRepositoryTestSupport`, que arranca
PostgreSQL con Testcontainers y el perfil `postgres-test` (Flyway y `ddl-auto=validate`, valida las migraciones y las
entidades). Los tests unitarios no levantan nada; para la transacción usan `support/TransaccionDirecta`.

## Lectura de cuadernos (ENS-120)

El worker está deshabilitado en el perfil de pruebas. Las pruebas llaman explícitamente al
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

- `SuscripcionServiceTest`, `RegistrarUsuarioTest` y los tests de DTOs/adaptadores verifican
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

## Consulta de planes y pantalla (ENS-97)

`ConsultaPlanesServiceTest` verifica catálogo, elegibilidad, prueba total, mes UTC, reservas,
premium vigente y vencimiento antes del job. Los tests de caso de uso, mappers y configuración
comprueban el contrato y el reloj por capa. `PlanSeguridadIntegrationTest` agrega la consulta privada
con PostgreSQL y seguridad reales: sesión, negocio ajeno ignorado, contadores, historial y límites.

`mvnw.cmd -B clean verify`: 1.512 pruebas, cero fallos, errores u omitidas; 456 clases,
4.351 líneas y 1.036 ramas cubiertas al 100 %. Evaluador: 39 pruebas aprobadas con el jar recién
compilado. Frontend: 288 pruebas aprobadas, lint, tipos y build. El circuito real en navegador usó
una base local independiente; no se aplicaron migraciones a la base habitual. No se invocó IA externa.
Detalle: [plan-ENS-97.md](../docs/plan-ENS-97.md).

## Renovación automática y pagos (ENS-98)

- `RenovacionPremiumIntegrationTest`: PostgreSQL y seguridad reales, preparación concurrente única,
  respeto de la prueba, autorización sin pago, aprobación idempotente, pendientes/rechazos,
  recuperación de creación incierta, cancelación confirmada o pendiente, períodos separados,
  reembolso, versiones antiguas, conservación de datos, sesión/CSRF y webhook con firma validada.
- `RenovacionPremiumServiceTest`: condiciones comerciales inválidas, catálogo/suscripción ausentes,
  aislamiento de referencias, identidad del proveedor, frecuencia, fechas, redondeo hacia arriba a segundos,
  reasignación de facturas y protección de Premium heredado.
- `MercadoPagoSuscripcionesAdapterTest`: contrato HTTP, verificación de receptor/aplicación/ambiente,
  importes numéricos o representados como texto, paginación completa, pagos ajenos, JSON inválido,
  redirecciones no admitidas, fallas de red, interrupciones, HMAC y rutas de recursos fijas.
- `MercadoPagoConfigTest` y `ConciliacionPagosTest`: arranque deshabilitado, configuración parcial,
  conciliación sin webhook, rotación de intentos fallidos y notificaciones de contratos desconocidos.
- `MigracionPlanesPostgresIntegrationTest`: V14→V15 conserva datos, suscripciones y pruebas activas;
  configura un precio pendiente, conserva uno existente y no crea contratos ni cobros automáticamente.

Verificación inicial: `mvnw.cmd -B clean verify`, **1.644 pruebas** sin fallos, errores u omitidas.
JaCoCo: **486 clases, 4.818 líneas y 1.229 ramas**, 100 % de líneas y ramas por clase.
Evaluador: 39 pruebas aprobadas con el jar nuevo. Las pruebas del proveedor usan dobles controlados
y un servidor HTTP ficticio local; esa verificación inicial no usó una cuenta externa de Mercado Pago.
Diseño, circuito en navegador y límites: [plan-ENS-98.md](../docs/plan-ENS-98.md).

Verificación final tras contrastar la API real con cuentas de prueba: **1.648 pruebas**, cero fallos,
errores u omitidas; 486 clases, 4.821 líneas y 1.229 ramas al 100 %. Se cubren el redondeo del primer
cobro a segundos, la representación de fecha con tres decimales y la paginación de facturas sin un
`limit` explícito rechazado por el proveedor. El circuito externo comprobó autorización, retorno y
cancelación antes del primer cobro, con prueba intacta; no comprobó facturas pagadas ni renovaciones.

El circuito posterior sin prueba gratuita comprobó una factura externa de prueba aprobada, la
activación de Premium por 720 horas y la cancelación conservando el período pagado. Se agregaron
regresiones del formato real: aplicación desde el contrato, vínculo a la suscripción, vendedor de
prueba verificado con `/users/me` cuando el pago informa `live_mode=true`, y referencia interna de
factura estable frente al id público opcional. Cuentas ajenas, compradores no admitidos, referencias
ambiguas y ambientes incorrectos siguen rechazados. El adaptador pasa 74 casos; la suite completa
aprueba **1.669 pruebas**, 486 clases, 4.866 líneas y 1.267 ramas al 100 %, sin omitidas.

La corrección del rechazo sin correo agregó veinte casos: omisión, nulo, vacío y espacios;
exclusión de estados aprobados/pendientes/reembolsados, correo real, comprador ausente o distinto,
contrato sin comprador y vendedor no verificado. Se reprodujeron cuatro errores antes del cambio.
El adaptador aprueba ahora **94 casos**; `clean verify` pasó **1.689 pruebas**, sin fallos, errores
u omitidas, con **486 clases, 4.873 líneas y 1.273 ramas**, todas cubiertas al 100 % por clase.
La consulta del pago externo ya existente y dos actualizaciones con sesión/CSRF devolvieron
HTTP 204, conservaron `GRATUITO` y dejaron una única fila `rejected`, sin pagos aprobados.
No hubo nuevos intentos de pago, cobros reales ni commits en esta comprobación.

Antes de la publicación del 21/10/2026 se incorporaron los cambios remotos de ubicación del negocio
y lectura de imágenes. El conjunto integrado pasó `clean verify`: **1.817 pruebas**, cero fallos,
errores u omitidas, con **537 clases, 5.127 líneas y 1.322 ramas**, al 100 % por clase.
Incluye los cinco casos adicionales de siguientes facturas impagas y reembolsos entre períodos.
El evaluador pasó **39 pruebas** con el jar recién construido. Frontend: **344 pruebas en 32 archivos**,
además de lint, tipos y build de producción. Estos resultados no reemplazan las comprobaciones
externas pendientes de segundo cobro, reembolso y notificaciones firmadas por Mercado Pago.

Frontend: 297 pruebas en 29 archivos aprobadas; lint, tipos y build de producción. Se cubren
contratación única con CSRF, estados de autorización y cancelación, respeto de la prueba,
conservación del período pagado, rechazo de enlaces ajenos y confirmaciones accesibles.
El test del tutorial ahora espera a que la navegación anterior termine antes de pulsar su logo.

## Estilo de los tests

Arrange-Act-Assert con helpers privados `givenX` / acción / `thenX`, nombres `metodoShouldXWhenY`, `@DisplayName`
en español, un comportamiento por test, imports explícitos. Guía completa: skill `escribir-tests`.
