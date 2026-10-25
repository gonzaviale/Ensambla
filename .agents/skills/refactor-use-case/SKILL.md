---
name: refactor-use-case
description: Refactoriza un caso de uso del backend de Ensambla (com.ensambla.backend.domain.useCase.*) a la estructura objetivo del proyecto, dejando execute() como una lista de pasos, moviendo las reglas puras al model, la lógica de un solo caso de uso a métodos privados y a servicios de dominio solo lo que comparten dos o más casos de uso; con sus DTOs en el subpaquete del caso, fábricas de() en lugar de mappers, ids tipados, sin sufijo UseCase y sin Spring en el dominio; y sus tests al estilo Arrange-Act-Assert con helpers privados, nombres camelCase y @DisplayName en español; sin cambiar el comportamiento y manteniendo los tests verdes. Usar cuando se pida "refactorizar este caso de uso", "dejar SOLID", "extraer a private methods/al model/al service", "eliminar este servicio/mapper", limpiar un execute() con lógica inline, o "refactorizar/limpiar los tests" (extraer when/then y validaciones a métodos privados, camelCase, DisplayName).
---

> Copia para Codex. La fuente es `.claude/skills/refactor-use-case/SKILL.md`: si cambiás uno, cambiá los dos.

# Refactor de casos de uso (estilo Ensambla)

Lleva un caso de uso de `com.ensambla.backend.domain.useCase.*` a la **forma objetivo**: `execute()` se lee como los
pasos del caso de uso; las reglas puras viven en el model; lo que usa solo este caso de uso son métodos privados; y en
un servicio de dominio queda solo lo que comparten dos o más casos de uso. **El comportamiento no cambia y los tests
existentes deben seguir pasando sin modificar sus aserciones.**

Las reglas de estructura están en [`backend/ARQUITECTURA.md`](../../../backend/ARQUITECTURA.md) (checklist en el skill
`reglas-arquitectura`). Nombres de dominio **en español**; vocabulario en `docs/modelo-datos.md`.

## Arquitectura de referencia

- `domain/useCase/<area>/<accion>/` — el caso de uso (`CrearProyecto`, sin sufijo, sin anotaciones de Spring,
  `@RequiredArgsConstructor` y un único `execute(<Accion>Request)`) junto a sus `<Accion>Request`/`<Accion>Response`.
  Los DTOs que comparten varios casos del área quedan en `domain/useCase/<area>/`.
- `domain/service/<area>/` — Java plano, solo con lógica compartida por dos o más casos de uso.
- `domain/model/<area>/` — modelos, reglas puras, ids tipados (`ProyectoId`) y enums del área.
- `domain/port/` — `TransaccionPort` y los puertos de salida.
- `domain/exception/` — `ResourceNotFoundException`, `DuplicateResourceException`, `BusinessRuleException`,
  `CredencialesInvalidasException`.
- `infrastructure/config/useCase/<Area>UseCaseConfig` — `@Bean` de cada caso de uso, servicio y regla.

## Forma objetivo de `execute()`

`execute()` solo orquesta. Cada línea es una de estas cosas:

1. Una llamada a un **método privado** del caso de uso (`resolverX`, `cargarX`, `validarX`, `aplicarX`).
2. Una llamada a un **método del model** (regla pura).
3. Una llamada a un **puerto** o a un **servicio compartido**.
4. `transaccionPort.ejecutar(() -> …)` alrededor de los pasos que tienen que confirmarse juntos.
5. La fábrica del DTO de salida (`ProyectoItem.de(proyecto)`).

No debe quedar en `execute()`: validaciones inline con `throw`, matemática de dominio, cadenas
`repository.findX().filter(...).isPresent()`, ni armado de varios valores.

Caso simple — `ListarPlantillas`:

```java
public ListarPlantillasResponse execute() {
    return ListarPlantillasResponse.de(plantillaRepository.findAllActivas());
}
```

Caso con más pasos — `CrearProyectoDesdePlantilla`:

```java
public ProyectoItem execute(CrearProyectoDesdePlantillaRequest request) {
    NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
    PlantillaProyecto plantilla = plantillaRepository.obtenerActiva(request.plantillaId());
    Proyecto proyecto = crearDesdePlantilla(negocioId, plantilla, OffsetDateTime.now(clock));
    return ProyectoItem.de(proyecto);
}
```

## Dónde va cada cosa (regla de decisión)

- **Función pura del estado de dominio, sin I/O** → al **model**: un método o predicado (`usuario.estaActivo()`), una
  función estática si no tiene estado (`Usuario.normalizarEmail`), o un value object (record) si deriva varios valores.
- **Toca un puerto y la usa solo este caso de uso** → **método privado** del caso de uso, que llama directo al puerto.
  Si arma varios valores, devolvé un **record privado**.
- **Traer una entidad o responder 404 (`findX().orElseThrow(...)`)** → método `default` del puerto (`bloqueRepository.obtener`),
  con su test (`mock(Puerto.class, CALLS_REAL_METHODS)`); nunca un servicio ni la misma línea copiada en cada caso.
- **Una llamada que sólo pasa al puerto** → el caso de uso llama directo al puerto.
- **Lógica de varios pasos que repiten dos o más casos de uso** (comprobalo con grep antes de decidir) → **servicio** en
  `domain/service/<area>`, Java plano, registrado como `@Bean` en el `*UseCaseConfig`. Hoy son cinco; no sumes uno
  por una o dos líneas.
- **Un servicio que queda con un único llamador, o con líneas sueltas** → se disuelve: la búsqueda va al puerto, la
  regla pura al modelo y lo demás al caso de uso; se borran el servicio, su bean y su test.
- **Un caso de uso que llama a otro caso de uso** → está prohibido: lo compartido se baja a un servicio, a un método
  `default` del puerto o al modelo.
- **Un mapper de dominio** → se reemplaza por la fábrica `de()` del DTO; su test pasa a ser el test del DTO.
- **Un `UUID` en una firma** → el id tipado de esa entidad.

Para mantener los servicios agnósticos del tiempo y los tests deterministas: el caso de uso mantiene su `Clock` y le
pasa `OffsetDateTime.now(clock)` al servicio o al modelo.

## Procedimiento

1. **Leer**: el caso de uso, sus DTOs, sus clases de model, los servicios que usa y sus tests.
2. **Buscar los llamadores** antes de extraer o disolver (`grep -rln "servicio.metodo(" src/main/java`).
3. **Cambiar en orden de dependencia**: model → servicios → caso de uso → `*UseCaseConfig` → tests.
4. **Wiring**: si cambian las dependencias, actualizá el `@Bean` en `<Area>UseCaseConfig`. El orden de los argumentos
   del constructor coincide con el de los campos (`@RequiredArgsConstructor`).
5. **Tests**: los del caso de uso no cambian sus aserciones; solo la construcción. Si un servicio se disolvió, mockeá el
   puerto en lugar del servicio y pasá `new TransaccionDirecta()` si el caso abre una transacción. Agregá tests para
   **toda** regla movida al model (un caso por comportamiento): el build exige 100 % por clase.
6. **Compilar y correr** los tests afectados, `ArchitectureTest` y después `clean verify`.

## Anti-patrones (NO hacer)

- Un método por cada write o por cada línea: eso es fragmentar, no ordenar.
- Crear un servicio para lógica que usa un solo caso de uso, o un mapper de dominio para lo que es una fábrica.
- Crear interfaces para los casos de uso cuando hay una sola implementación (YAGNI).
- Mover I/O (repositorios) dentro del model.
- Anotar con `@Service`/`@Component`/`@Transactional` cualquier clase de `domain`.
- Cambiar el comportamiento o relajar aserciones para que "entre" la extracción.
- Inyectar `Clock` en los servicios de dominio.
- Nombres de dominio en inglés (`Project`, `create`).
- Sacar el filtro por `negocio_id` "para simplificar": el aislamiento no se negocia.

## Refactor de los tests del caso de uso (estilo Arrange-Act-Assert)

Cada `@Test` queda en tres bloques (arrange / act / assert) de pocas líneas; toda la mecánica (stubs, ejecución,
asserts y verifies) se extrae a **métodos privados** agrupados al final de la clase, bajo `// --- arrange ---`,
`// --- act ---`, `// --- assert ---`.

- **Arrange** → métodos `givenX(...)` con los `when(...).thenReturn(...)` y builders de fixtures.
- **Act** → un método que ejecuta el caso de uso con el verbo del caso (`listar()`, `crear()`).
- **Assert** → métodos `thenX(...)` que agrupan los `assertThat` y `verify`.

Convenciones obligatorias: nombres en **camelCase** sin guiones bajos (`executeShouldReturnPlantillasActivas`),
`@DisplayName` en **español**, comentarios solo en los casos no obvios y cuidado con el **strict stubbing** de Mockito.

Plantillas ya hechas: `ListarPlantillasTest`, `ListarPlantillasResponseTest`, `PlantillaRepositoryTest`.

## Build & test

Desde `backend/` (JDK 21 en `JAVA_HOME` y Docker corriendo: los tests con base usan Testcontainers, nunca H2):

```bash
./mvnw test -Dtest=ListarPlantillasTest,PlantillaRepositoryTest,ArchitectureTest
./mvnw clean verify
```

En Windows/PowerShell: `.\mvnw.cmd test "-Dtest=ListarPlantillasTest,PlantillaRepositoryTest,ArchitectureTest"`.
