---
name: refactor-use-case
description: Refactoriza un use case del backend de Ensambla (com.ensambla.backend.domain.useCase.*) a la estructura SOLID objetivo del proyecto, dejando execute() como una lista de pasos, moviendo reglas/validaciones de dominio al model y la lógica compartida o con I/O a servicios de dominio, y sus tests al estilo Arrange-Act-Assert con helpers privados, nombres camelCase y @DisplayName en español; sin cambiar el comportamiento y manteniendo los tests verdes. Usar cuando se pida "refactorizar este use case", "dejar SOLID", "extraer a private methods/al model/al service", limpiar un execute() con lógica inline, o "refactorizar/limpiar los tests" (extraer when/then y validaciones a métodos privados, camelCase, DisplayName).
---

# Refactor de Use Cases (estilo SOLID de Ensambla)

Lleva un use case de `com.ensambla.backend.domain.useCase.*` a la **forma objetivo**:
`execute()` se lee como los pasos del caso de uso; las reglas de dominio viven en el
model; la lógica compartida o con I/O vive en servicios de dominio. **El comportamiento
no cambia y los tests existentes deben seguir pasando sin modificación de sus assertions.**

Recordá la convención del proyecto: **nombres de dominio en español** (clases, campos,
métodos de servicio, enums, tablas). Ver `docs/modelo-datos.md` para el vocabulario.

## Arquitectura de referencia

Clean Architecture en dos capas (`domain` no depende de `infrastructure`):

- `domain/useCase/<area>/` — use cases planos (sin anotaciones de Spring), con
  `@RequiredArgsConstructor` y un único `execute(<X>Request)`.
- `domain/service/<area>/` — servicios `@Service` con las reglas que tocan repositorio
  (existencia, unicidad, pertenencia al negocio, normalización).
- `domain/model/<area>/` — modelos de dominio (Lombok `@Builder`/`@Getter`/`@Setter`).
- `domain/dto/<area>/` y `domain/mapper/<area>/` — request/response del use case y su mapper.
- `domain/exception/` — `ResourceNotFoundException`, `DuplicateResourceException`,
  `BusinessRuleException`, `CredencialesInvalidasException`.
- `infrastructure/config/useCase/<Area>UseCaseConfig` — wiring de mappers y use cases como `@Bean`.

## Forma objetivo de `execute()`

`execute()` solo orquesta. Cada línea es una de estas tres cosas:

1. Una llamada a un **método privado** del use case (`resolverX`, `cargarX`, `validarX`, `aplicarX`).
2. Una llamada a un **método del model** (regla de dominio pura).
3. Una llamada a un **servicio colaborador** (lógica compartida / con I/O) o al **mapper**.

No debe quedar en `execute()`: validaciones inline con `throw`, matemática de dominio,
cadenas `repository.findX().filter(...).isPresent()`, ni armado de varios valores.

Caso simple, todo delegado al servicio — `ListarPlantillasUseCase`:

```java
public ListarPlantillasResponse execute() {
    return mapper.toResponse(plantillaService.listarActivas());
}
```

Para un caso con más pasos, la forma esperada es (ilustrativo, T-014 crear proyecto desde plantilla):

```java
public CrearProyectoDesdePlantillaResponse execute(CrearProyectoDesdePlantillaRequest request) {
    Plantilla plantilla = plantillaService.obtenerActiva(request.plantillaId());
    Proyecto proyecto = Proyecto.desde(plantilla, request.negocioId(), OffsetDateTime.now(clock));
    Proyecto creado = proyectoService.registrarConEstructura(proyecto, plantilla.getEstructura());
    return mapper.toResponse(creado);
}
```

## Dónde va cada cosa (regla de decisión)

- **Función pura del estado de dominio, sin I/O** → al **model**: un método o predicado
  (`usuario.estaActivo()`), una función estática si no tiene estado (`Usuario.normalizarEmail`),
  o un value object (record propio) si deriva **varios** valores.
- **Toca un repositorio/servicio, es específico de este use case y se usa una sola vez**
  → **método privado** del use case. Si arma varios valores para downstream, devolvé un **record privado**.
- **La misma lógica aparece en ≥2 lugares** (comprobalo con grep antes de decidir), o es
  una regla de persistencia del agregado (unicidad, existencia, pertenencia al negocio) →
  **servicio `@Service`** en `com.ensambla.backend.domain.service.<area>`, inyectado en el use case.

Para mantener los servicios agnósticos del tiempo y los tests deterministas: si el use case
necesita la fecha/hora, mantiene su `Clock` y le pasa `OffsetDateTime.now(clock)` al servicio
(no inyectes el `Clock` en el servicio).

## Procedimiento

1. **Leer**: el use case, su request/response/mapper, sus clases de model, el servicio que usa y sus tests.
2. **Buscar duplicación** antes de extraer, para decidir model vs privado vs servicio (Grep sobre `backend/src/main/java`).
3. **Cambiar en orden de dependencia**: model → servicios → use case → `*UseCaseConfig` → tests.
4. **Wiring**: si agregaste/quitaste dependencias del use case, actualizá el `@Bean` en
   `com.ensambla.backend.infrastructure.config.useCase.<Area>UseCaseConfig`. El orden de los
   argumentos del constructor debe coincidir con el orden de los campos (por `@RequiredArgsConstructor`).
5. **Tests**: los del use case no cambian sus assertions; solo adaptá la construcción (mocks). Si una
   dependencia pasó a servicio, mockeá el servicio en lugar del repo. Agregá tests unitarios para
   **toda** regla movida al model/servicio (un caso por comportamiento) — el build exige 100 % por clase.
6. **Compilar y correr** los tests afectados y después `clean verify`.

## Anti-patrones (NO hacer)

- Un método por cada write o por cada línea: eso es fragmentar, no es SOLID.
- Crear interfaces para los use cases cuando hay una sola implementación (YAGNI).
- Mover I/O (repositorios) dentro del model.
- Anotar los use cases con `@Service`/`@Component`: se registran en el `*UseCaseConfig`.
- Cambiar el comportamiento o relajar assertions para que "entre" la extracción.
- Inyectar `Clock` en los servicios de dominio (pasá el instante por parámetro).
- Nombres de dominio en inglés (`Project`, `create`): el dominio va en español.
- Sacar el filtro por `negocio_id` "para simplificar": el aislamiento no se negocia.

## Refactor de los tests del use case (estilo Arrange-Act-Assert)

Cada `@Test` queda en tres bloques (arrange / act / assert) de pocas líneas; toda la mecánica
(stubs, ejecución, asserts y verifies) se extrae a **métodos privados** agrupados al final de
la clase, bajo `// --- arrange ---`, `// --- act ---`, `// --- assert ---`.

- **Arrange** → métodos `givenX(...)` con los `when(...).thenReturn(...)` y builders de fixtures.
- **Act** → un método que ejecuta el use case con el verbo del caso (`listar()`, `crear()`).
- **Assert** → métodos `thenX(...)` que agrupan los `assertThat` y `verify`.

Convenciones obligatorias: nombres en **camelCase** sin guiones bajos (`executeShouldReturnMappedPlantillas`),
`@DisplayName` en **español**, comentarios solo en los casos no obvios y cuidado con el
**strict stubbing** de Mockito (cada `givenX()` debe usarse en el camino de ese test).

Plantilla ya hecha: `ListarPlantillasUseCaseTest`, `PlantillaServiceTest`.

## Build & test

Desde `backend/` (JDK 21 en `JAVA_HOME`; los tests de contexto usan H2 con el perfil `h2-test`, sin Flyway):

```bash
./mvnw test -Dtest=ListarPlantillasUseCaseTest,PlantillaServiceTest
./mvnw clean verify
```

En Windows/PowerShell: `.\mvnw.cmd test "-Dtest=ListarPlantillasUseCaseTest,PlantillaServiceTest"`.
