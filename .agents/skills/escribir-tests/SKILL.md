---
name: escribir-tests
description: Escribe o reescribe los tests del backend de Ensambla al estilo del proyecto - Arrange-Act-Assert con helpers privados givenX/thenX, métodos camelCase xShouldYWhenZ, @DisplayName en español, imports explícitos, builders en vez de reflexión, un comportamiento por test - con plantillas por capa (caso de uso con TransaccionDirecta, DTO con fábrica de(), servicio compartido, modelo e ids tipados, mapper de presentación, controller con MockMvcDePrueba, repo impl, mapper de repositorio, JPA contra PostgreSQL con Testcontainers, integración HTTP sin H2, excepciones, config, ArchitectureTest). Aplica siempre el skill codigo-semantico (obligatorio): tests dado/cuando/entonces con helpers givenX/whenX/thenX, sin lógica ni ternarios y con base abstracta por tipo. Usar cuando se pida "escribir/agregar/arreglar tests", "testear este caso de uso/servicio/controller/mapper", "cubrir X con tests" o al terminar una feature nueva. Para refactorizar el caso de uso en sí usar refactor-use-case; para crear una feature usar crear-use-case.
---

> Copia para Codex. La fuente es `.claude/skills/escribir-tests/SKILL.md`: si cambiás uno, cambiá los dos.

# Escribir tests del backend (estilo Ensambla)

Un test se lee como una frase: **dado → cuando → entonces** (`givenX()` → `whenX()` → `thenX()`). La mecánica (stubs, ejecución, asserts, verifies) va en
**métodos privados al final de la clase**. Plantillas vivas: el módulo `plantilla` completo (`ListarPlantillasTest`,
`ListarPlantillasResponseTest`, `PlantillaPresentationMapperTest`, `PlantillaRepositoryImplTest`,
`PlantillaRepositoryMapperTest`, `PlantillaControllerTest`, `PlantillaUseCaseConfigTest`), `RegistrarUsuarioTest` (caso
de uso con transacción) e `IdentificadorTest`.

Los tests replican el paquete de la clase probada, bajo `backend/src/test/java/com/ensambla/backend/`. Las reglas de
estructura que se prueban están en [`backend/ARQUITECTURA.md`](../../../backend/ARQUITECTURA.md).

## Reglas (todas obligatorias)

1. **Nombre** `metodoShouldHacerXWhenY` en camelCase, **sin guiones bajos** y sin frases en español.
2. **`@DisplayName` en español** describiendo el comportamiento ("Rechaza al usuario suspendido con credenciales inválidas").
3. **Un comportamiento por test.** No pruebes dos métodos ni dos endpoints en el mismo test.
4. **Cuerpo de 3 bloques**, separados por línea en blanco, de pocas líneas. Los helpers se agrupan al final bajo
   `// --- arrange ---`, `// --- act ---`, `// --- assert ---` (y `// --- helpers ---` para builders de datos).
5. **Imports explícitos**: ningún `*`, ni en estáticos. Sin nombres totalmente calificados dentro del código. Sin imports ni variables sin usar.
6. **Una sentencia por línea.** Prohibido `@Test void x() { ...; ...; }` en una línea.
7. **Datos con builders** (`Proyecto.builder()...`) o con el constructor del record. Cada campo mapeado se verifica con un
   `assertThat` explícito (el único `usingRecursiveComparison` aceptado es entidad↔modelo en un mapper bidireccional,
   comparando los ids con `support/IdsDePrueba`). Valores en `private static final` con nombre (`USUARIO_ID`, `NOMBRE`)
   y del tipo de id que corresponde (`ProyectoId.de("…")`); datos compartidos en `support/*TestData`.
8. **Mockito estricto**: cada `givenX()` debe usarse en el camino de ese test, o salta `UnnecessaryStubbingException`.
9. Comentarios solo donde no es obvio.
10. **Cobertura 100 % de líneas y ramas por clase** (el `verify` falla si no): cada `if`, `orElseThrow` y `null` del código probado tiene su test.
    Los getters/setters/builders de Lombok no cuentan (`lombok.addLombokGeneratedAnnotation = true`).
11. **Nunca H2.** Lo que levanta base o contexto `extends PostgresRepositoryTestSupport` (Testcontainers); `ArchitectureTest`
    falla si vuelve `h2`.

### Tests semánticos (obligatorios, skill `codigo-semantico`)

12. **Dado / cuando / entonces con helpers.** El cuerpo del test es `givenX()`, `whenX()`, `thenX()` con nombres que se
    leen sin pensar (`givenContratoConPrecioAjeno()`, `whenSeActualizaElContrato()`, `thenSeRechazaElContrato()`).
    Los `assertThat`, `verify` y stubs no van sueltos en el cuerpo: van en esos helpers.
13. **Sin lógica en el test.** Nada de `if`, `for` con aserciones, `switch` ni ternarios (`caso == 6 ? … : …`). Si un
    `@ParameterizedTest` decide qué esperar según el caso, se reemplaza por un test por escenario con su `given`.
14. **Base abstracta cuando varios tipos comparten comportamiento.** Una clase abstracta (`CampoPorTipoTest`) con los
    tests semánticos (`aceptaUnValorValido`, `rechazaUnValorInvalido`, `aceptaUnValorInicialValido`) que usa métodos
    abstractos (`valorValido()`, `valorInvalido()`, `mensajeDeError()`), y un test concreto por subclase que **solo aporta
    datos**. Cada subclase delgada queda cubierta por la base (100 % por clase) y agregar un tipo es agregar una clase de
    test de tres métodos.
15. **Nombres de datos que se leen solos** (`PRECIO_AJENO`, `OTRA_REFERENCIA`), no `X1`, `dto`, `valor2`. El test no
    necesita comentarios: si hace falta uno, falta un helper o un nombre.
16. **Helpers cortos** (≈10 líneas, un solo nivel) y agrupados al final, sin repetir el mismo setup en cada test.
17. **Un `@Test` no usa `when(...)`, `verify(...)`, `assertThat` ni arma datos inline.** Todo va a `givenX` /
    `whenX` / `thenX` (extract method). El test queda en tres líneas que se leen como una frase:

```java
// ❌ mecánica dentro del @Test
@Test
void editarShouldSaveCampo() {
    when(campoRepository.obtener(CAMPO_ID, NEGOCIO_ID)).thenReturn(campo);
    when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);

    CampoItem item = editarCampo.execute(new EditarCampoRequest(USUARIO_ID, CAMPO_ID, "Precio"));

    assertThat(item.nombre()).isEqualTo("Precio");
    verify(campoRepository).save(campo);
}

// ✅ dado / cuando / entonces
@Test
void editarShouldSaveCampo() {
    givenUnCampoDelNegocio();

    CampoItem item = whenSeRenombraElCampo("Precio");

    thenElCampoQuedaGuardadoConNombre(item, "Precio");
}
```
18. **Los mismos helpers se reutilizan**: si dos tests arman el mismo dato o la misma verificación, es un helper (o un
    `support/*TestData`), nunca copiar y pegar.

## Qué probar en cada capa

| Clase probada | Test | Cómo | Casos mínimos |
|---|---|---|---|
| Caso de uso (`CrearProyecto`) | `CrearProyectoTest` | `@ExtendWith(MockitoExtension.class)`, `@Mock` de puertos y servicios compartidos, `new TransaccionDirecta()` si abre transacción, caso de uso en `@BeforeEach` | respuesta armada · delega con los argumentos correctos · cada rama de sus métodos privados · propaga la excepción sin convertirla |
| DTO con `de()` | `*ResponseTest`, `*ItemTest` | sin mocks | **todos** los campos · orden conservado · nulos · lista vacía |
| Método `default` de un puerto | `<Puerto>Test` en `domain/repository/<area>` | `mock(Puerto.class, CALLS_REAL_METHODS)`; los `when` van sobre los métodos abstractos (`findByIdAndNegocioId`), nunca sobre el `default` | camino feliz · el 404 con su mensaje |
| `*Service` (compartido) | `*ServiceTest` | `@Mock` de repositorios/otros servicios + `@InjectMocks` | camino feliz · cada rama de error · **orden** de validaciones (`InOrder`) · que no consulta si la validación falla (`verifyNoInteractions`) |
| Model con reglas, ids | `*Test` | sin mocks | cada regla, casos borde; un id nuevo ya lo cubre `IdentificadorTest` si se suma a su lista |
| `*PresentationMapper` | `*PresentationMapperTest` | sin mocks | un test por método · lista vacía |
| `*Controller` | `*ControllerTest` | `mock()` de los casos de uso + `MockMvcDePrueba.standalone(controller).setControllerAdvice(new GlobalExceptionHandler())` | 200 con JSON · el request llega bien al caso de uso (`ArgumentCaptor`) · 200 `[]` · 404 · 400 por id inválido (`verifyNoInteractions`) · 400 por validación del body |
| `*RepositoryImpl` | `*RepositoryImplTest` | `@Mock` del JPA repo; mapper real | mapeo completo · orden · vacío · `Optional` presente/ausente · el JPA recibe `id.valor()` |
| `*RepositoryMapper` | `*RepositoryMapperTest` | sin mocks | `toDomain`/`toEntity` con todos los campos · entidad vacía (nulos) · enums |
| `I*JpaRepository` / esquema | `I*JpaRepositoryTest`, `EsquemaPostgresIntegrationTest` | `extends PostgresRepositoryTestSupport` + `@Transactional` (rollback) | la consulta real: filtros, orden, **aislamiento entre negocios**, constraints |
| Seguridad HTTP | `SeguridadIntegrationTest`, `*SeguridadIntegrationTest` | `extends PostgresRepositoryTestSupport` + `@AutoConfigureMockMvc` | 401 sin sesión · 403 sin CSRF · rutas públicas · recurso ajeno 404 |
| Excepciones, config, arranque, arquitectura | `DomainExceptionTest`, `*UseCaseConfigTest`, `IdentificadoresConfigTest`, `BackendApplicationTests`, `ArchitectureTest` | ver los existentes | mensaje/causa/herencia · beans · contexto carga · reglas de estructura |

`MockMvcDePrueba` arma el MockMvc standalone con los conversores de ids tipados y el Jackson del proyecto: sin él, un
`@PathVariable ProyectoId` responde 500.

## Plantillas

Método `default` de un puerto (los `when` van sobre el método abstracto):

```java
class UsuarioRepositoryTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("Rechaza al usuario inexistente con credenciales inválidas")
    void obtenerActivoShouldThrowWhenMissing() {
        givenRepositoryHasNoUsuario();

        thenSeRechazaConCredencialesInvalidas();
    }

    // --- arrange ---
    private void givenRepositoryHasNoUsuario() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());
    }

    // --- assert ---
    private void thenSeRechazaConCredencialesInvalidas() {
        assertThatThrownBy(this::obtenerActivo).isInstanceOf(CredencialesInvalidasException.class);
    }

    // --- act ---
    private Usuario obtenerActivo() {
        return usuarioRepository.obtenerActivo(USUARIO_ID);
    }
}
```

Caso de uso (puertos mockeados, transacción directa):

```java
@BeforeEach
void setUp() {
    registrarUsuario = new RegistrarUsuario(usuarioRepository, negocioRepository, planRepository,
            suscripcionRepository, contraseniaPort, new TransaccionDirecta(), CLOCK);
}
```

DTO con su fábrica:

```java
@Test
@DisplayName("Copia todos los datos de la plantilla")
void deShouldCopyAllFields() {
    ListarPlantillasResponse respuesta = whenSeArmaLaRespuestaDeUnaPlantilla();

    thenCopiaTodosLosDatosDeLaPlantilla(respuesta);
}

// --- act ---
private ListarPlantillasResponse whenSeArmaLaRespuestaDeUnaPlantilla() {
    return ListarPlantillasResponse.de(List.of(plantilla()));
}

// --- assert ---
private void thenCopiaTodosLosDatosDeLaPlantilla(ListarPlantillasResponse respuesta) {
    assertThat(respuesta.content()).singleElement().satisfies(item -> {
        assertThat(item.id()).isEqualTo(PLANTILLA_ID);
        assertThat(item.nombre()).isEqualTo(NOMBRE);
    });
}
```

Controller (el 400 no debe llegar al caso de uso):

```java
@Test
@DisplayName("GET devuelve 400 cuando el id no es un UUID")
void obtenerShouldReturn400WhenIdIsNotUuid() throws Exception {
    ResultActions respuesta = whenSeConsultaConUnIdInvalido();

    thenRespondeBadRequestSinLlegarAlCasoDeUso(respuesta);
}

// --- act ---
private ResultActions whenSeConsultaConUnIdInvalido() throws Exception {
    return mockMvc.perform(get("/api/proyectos/id-invalido"));
}

// --- assert ---
private void thenRespondeBadRequestSinLlegarAlCasoDeUso(ResultActions respuesta) throws Exception {
    respuesta.andExpect(status().isBadRequest());
    verifyNoInteractions(obtenerProyecto);
}
```

Aislamiento entre negocios contra PostgreSQL (rollback por test; las entidades JPA siguen con `UUID`):

```java
@Transactional
class IProyectoJpaRepositoryTest extends PostgresRepositoryTestSupport {

    @Test
    @DisplayName("No devuelve un proyecto de otro negocio")
    void findByIdAndNegocioIdShouldReturnEmptyWhenProyectoBelongsToOtherNegocio() {
        UUID proyectoDeA = givenProyecto(negocioA);

        Optional<ProyectoEntity> proyecto = whenSeBuscaDesdeOtroNegocio(proyectoDeA);

        thenNoEncuentraNada(proyecto);
    }
}
```

## Detalles que suelen romper

- JSON numérico: Jackson devuelve `Integer`/`Double`. Si el modelo usa `Short` o `BigDecimal`, comparalo con
  `.value(POSICION.intValue())`; si no, falla con "expected 1 but was 1".
- Ids en JSON: se serializan como el string del UUID; en `jsonPath` compará con `PROYECTO_ID.toString()`.
- `JdbcTemplate` en tests de integración: pasale `id.valor()`, no el id tipado (falla con "bad SQL grammar").
- `ArgumentCaptor` del tipo de id (`ArgumentCaptor<ProyectoId>`), no de `UUID`.
- Los `I*JpaRepositoryTest` crean sus datos con los repos JPA dentro de la transacción del test (se revierten solos);
  no dependas de datos que dejó otro test.
- Una violación de constraint dentro de un test transaccional deja la transacción abortada: probá una sola por test.
- Un `@Test` sin `@DisplayName`, un nombre con `_` o un `import ...*;` son errores de estilo aunque el test pase.

## Ejecutar

Desde `backend/`, con JDK 21 y Docker corriendo:

```powershell
.\mvnw.cmd test "-Dtest=ListarPlantillasTest,PlantillaRepositoryTest,ArchitectureTest"
.\mvnw.cmd clean verify
```

Resúmenes en `target/surefire-reports/*.txt`; reporte de cobertura en `target/site/jacoco/index.html`.
Si `verify` falla por cobertura, `target/site/jacoco/jacoco.csv` lista las clases con líneas o ramas sin cubrir.

## Controles antes de cerrar

```bash
grep -rEn "^import .*\*;" backend/src/test             # sin wildcards
grep -rEn "void [a-z]+_[a-zA-Z_]+\(" backend/src/test  # sin guiones bajos
grep -rn "h2-test\|UseCaseTest\b" backend/src/test     # sin H2 ni nombres viejos
```

Y confirmá que cada `@Test` tiene su `@DisplayName`.
