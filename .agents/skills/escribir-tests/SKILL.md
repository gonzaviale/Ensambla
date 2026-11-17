---
name: escribir-tests
description: Escribe o reescribe los tests del backend de Ensambla al estilo del proyecto - nombre del test en español sin tildes como frase del negocio que se lee sola (unCampoUsadoPorUnaFormulaNoPuedeCambiarDeTipo), @DisplayName obligatorio en español en todo @Test y @ParameterizedTest, tres bloques de una línea cada uno con helpers dadoX/cuandoX/entoncesX, cuerpo sin repositorios ni mocks ni when/verify ni builders, datos con fábricas de dominio en clases de apoyo compartidas (support/CamposDePrueba), base abstracta por tipo, imports explícitos, un comportamiento por test - con plantillas por capa (caso de uso con TransaccionDirecta, DTO con fábrica de(), servicio compartido, modelo e ids tipados, mapper de presentación, controller con MockMvcDePrueba, repo impl, mapper de repositorio, JPA contra PostgreSQL con Testcontainers, integración HTTP sin H2, excepciones, config, ArchitectureTest). Aplica siempre el skill codigo-semantico (obligatorio). Usar cuando se pida "escribir/agregar/arreglar tests", "testear este caso de uso/servicio/controller/mapper", "cubrir X con tests" o al terminar una feature nueva. Para refactorizar el caso de uso en sí usar refactor-use-case; para crear una feature usar crear-use-case.
---

> Copia para Codex. La fuente es `.claude/skills/escribir-tests/SKILL.md`: si cambiás uno, cambiá los dos.

# Escribir tests del backend (estilo Ensambla)

Un test se lee como una frase del negocio: **dado → cuando → entonces**, en **tres líneas** (`dadoX(...)`,
`cuandoX(...)`, `entoncesX(...)`). La mecánica (stubs, ejecución, asserts, verifies, builders) va en **métodos
privados al final de la clase** o en fábricas de apoyo. Plantillas vivas: el módulo `plantilla` completo
(`ListarPlantillasTest`, `ListarPlantillasResponseTest`, `PlantillaPresentationMapperTest`,
`PlantillaRepositoryImplTest`, `PlantillaRepositoryMapperTest`, `PlantillaControllerTest`, `PlantillaUseCaseConfigTest`),
`RegistrarUsuarioTest` (caso de uso con transacción) e `IdentificadorTest`. Los tests nuevos o refactorizados siguen
las reglas de abajo aunque esas plantillas vivas todavía tengan el estilo anterior (`xShouldYWhenZ`).

Los tests replican el paquete de la clase probada, bajo `backend/src/test/java/com/ensambla/backend/`. Las reglas de
estructura que se prueban están en [`backend/ARQUITECTURA.md`](../../../backend/ARQUITECTURA.md).

## Reglas (todas obligatorias)

1. **Nombre en español, sin tildes, como una frase del negocio que se lee sola:**
   `unCampoUsadoPorUnaFormulaNoPuedeCambiarDeTipo`, `elProveedorNoPuedeCambiarElPrecioDeLaRenovacion`. camelCase, sin
   guiones bajos. **Prohibido** `executeShouldXWhenY`, `metodoShouldHacerXWhenY` y cualquier nombre en inglés.
2. **`@DisplayName` obligatorio en todo `@Test` y `@ParameterizedTest`, siempre** (nunca se quita): una frase completa
   en español, con tildes y puntuación normal, que explica el comportamiento en lenguaje de negocio
   (`@DisplayName("Un campo usado por una fórmula no puede cambiar de tipo")`). Va justo debajo de `@Test`, **además**
   del nombre del método (regla 1) y de los tres bloques `dado/cuando/entonces`.
3. **Un comportamiento por test.** No pruebes dos métodos ni dos endpoints en el mismo test.
4. **Tres bloques de UNA línea cada uno**, separados por línea en blanco, con prefijos en español: `dadoX(...)`
   (preparar), `cuandoX(...)` (actuar) y `entoncesX(...)` (verificar). Reemplazan a `givenX/whenX/thenX`. Si hace
   falta más de una verificación, son varias líneas `entonces…` seguidas (una por cosa). Los helpers propios del
   escenario van **privados** al final de la clase, agrupados bajo `// --- dado ---`, `// --- cuando ---`,
   `// --- entonces ---`.
5. **Imports explícitos**: ningún `*`, ni en estáticos. Sin nombres totalmente calificados dentro del código. Sin imports ni variables sin usar.
6. **Una sentencia por línea.** Prohibido `@Test void x() { ...; ...; }` en una línea.
7. **El cuerpo del `@Test` nunca nombra repositorios, mocks, `when(...)`, `verify(...)`, builders ni UUID sueltos.**
   Todo va a helpers con nombre de negocio. Cada campo mapeado se verifica con un `assertThat` explícito dentro de un
   helper `entonces…` (el único `usingRecursiveComparison` aceptado es entidad↔modelo en un mapper bidireccional,
   comparando los ids con `support/IdsDePrueba`). Las constantes van en `private static final` con nombre y del tipo de
   id que corresponde (`ProyectoId.de("…")`).
8. **Datos de prueba con fábricas de dominio** (`unCampoNumerico("Precio")`, `unaFormulaQueUsa(precio)`) en clases de
   apoyo compartidas por tema en `src/test/.../support/` (`CamposDePrueba`, `AvisosDePrueba`, `RenovacionesDePrueba`),
   usadas con `import static`. Los helpers `dado/cuando/entonces` propios del escenario quedan privados en la clase del
   test; lo que se repite en dos clases sube a la clase de apoyo del tema. Nada de armar el mismo builder en cada test.
9. **Mockito estricto**: cada `dadoX()` con stubs debe usarse en el camino de ese test, o salta `UnnecessaryStubbingException`.
10. Comentarios solo donde no es obvio (si hace falta uno para entender *qué* hace el test, falta un nombre o un helper).
11. **Cobertura 100 % de líneas y ramas por clase** (el `verify` falla si no): cada `if`, `orElseThrow` y `null` del código probado tiene su test.
    Los getters/setters/builders de Lombok no cuentan (`lombok.addLombokGeneratedAnnotation = true`).
12. **Nunca H2.** Lo que levanta base o contexto `extends PostgresRepositoryTestSupport` (Testcontainers); `ArchitectureTest`
    falla si vuelve `h2`.

### Tests semánticos (obligatorios, skill `codigo-semantico`)

13. **Sin lógica en el test.** Nada de `if`, `for` con aserciones, `switch` ni ternarios (`caso == 6 ? … : …`). Si un
    `@ParameterizedTest` decide qué esperar según el caso, se reemplaza por un test por escenario con su `dado`.
14. **Tests por tipo: clase base abstracta con nombres de negocio.** `CampoPorTipoTest` define
    `aceptaLosValoresDeSuTipo`, `rechazaLosValoresQueNoSonDeSuTipo` y `aceptaUnValorInicialDeSuTipo`, que usan métodos
    abstractos (`valorValido()`, `valorInvalido()`, `mensajeDeError()`); hay **una subclase por tipo que solo aporta
    datos**. Cada subclase delgada queda cubierta por la base (100 % por clase) y agregar un tipo es agregar una clase
    de test de tres métodos.
15. **Helpers cortos** (≈10 líneas, un solo nivel) y sin repetir el mismo setup en cada test.
16. **Los helpers se reutilizan**: si dos tests arman el mismo dato o la misma verificación, es un helper o una fábrica
    de apoyo, nunca copiar y pegar.

### Antes / después real: `EditarCampoTest`

❌ **Antes:** nombre en inglés, y el cuerpo habla con el repositorio y arma datos inline:

```java
@Test
@DisplayName("Rechaza cambiar el tipo de un campo usado por una fórmula")
void executeShouldRejectChangingTypeOfFieldUsedByFormula() {
    givenCamposDelBloque();

    assertThatThrownBy(() -> editarCampo.execute(request))
            .isInstanceOf(BusinessRuleException.class);

    verify(campoRepository, never()).save(any());
}
```

✅ **Después:** `@Test` + `@DisplayName` + nombre de método del negocio, tres bloques de una línea y fábricas de dominio:

```java
@Test
@DisplayName("Un campo usado por una fórmula no puede cambiar de tipo")
void unCampoUsadoPorUnaFormulaNoPuedeCambiarDeTipo() {
    Campo cantidad = unCampoNumerico("Cantidad");
    dadoUnBloqueCon(cantidad, unaFormulaQueUsa(cantidad, "Doble"));

    cuandoSeIntentaConvertirEnTexto(cantidad);

    entoncesSeRechazaPorque("No se puede cambiar el tipo de «Cantidad»: la fórmula «Doble» lo usa");
    entoncesNoSeGuardaNingunCampo();
}

// --- dado ---
private void dadoUnBloqueCon(Campo... campos) {
    when(campoRepository.findByBloqueId(BLOQUE_ID)).thenReturn(List.of(campos));
}

// --- cuando ---
private void cuandoSeIntentaConvertirEnTexto(Campo campo) {
    excepcion = catchThrowable(() -> editarCampo.execute(pedidoDeCambioATexto(campo)));
}

// --- entonces ---
private void entoncesSeRechazaPorque(String mensaje) {
    assertThat(excepcion).isInstanceOf(BusinessRuleException.class).hasMessage(mensaje);
}

private void entoncesNoSeGuardaNingunCampo() {
    verify(campoRepository, never()).save(any());
}
```

El `when(...)`, el `verify(...)` y los builders quedaron en helpers; el `@Test` se lee solo, como una frase.

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

Todas con el estilo nuevo: `@DisplayName` en español, nombre de método del negocio, tres bloques de una línea.

Método `default` de un puerto (los `when` van sobre el método abstracto):

```java
class UsuarioRepositoryTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class, CALLS_REAL_METHODS);

    @Test
    void unUsuarioInexistenteSeRechazaConCredencialesInvalidas() {
        dadoQueNoExisteElUsuario();

        entoncesSeRechazaConCredencialesInvalidas();
    }

    // --- dado ---
    private void dadoQueNoExisteElUsuario() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---
    private Usuario cuandoSeObtieneElUsuarioActivo() {
        return usuarioRepository.obtenerActivo(USUARIO_ID);
    }

    // --- entonces ---
    private void entoncesSeRechazaConCredencialesInvalidas() {
        assertThatThrownBy(this::cuandoSeObtieneElUsuarioActivo).isInstanceOf(CredencialesInvalidasException.class);
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
void laRespuestaCopiaTodosLosDatosDeLaPlantilla() {
    ListarPlantillasResponse respuesta = cuandoSeArmaLaRespuestaDeUnaPlantilla();

    entoncesCopiaTodosLosDatosDeLaPlantilla(respuesta);
}

// --- cuando ---
private ListarPlantillasResponse cuandoSeArmaLaRespuestaDeUnaPlantilla() {
    return ListarPlantillasResponse.de(List.of(unaPlantilla()));
}

// --- entonces ---
private void entoncesCopiaTodosLosDatosDeLaPlantilla(ListarPlantillasResponse respuesta) {
    assertThat(respuesta.content()).singleElement().satisfies(item -> {
        assertThat(item.id()).isEqualTo(PLANTILLA_ID);
        assertThat(item.nombre()).isEqualTo(NOMBRE);
    });
}
```

Controller (el 400 no debe llegar al caso de uso):

```java
@Test
void unIdQueNoEsUnUuidSeRechazaSinConsultarElProyecto() throws Exception {
    ResultActions respuesta = cuandoSeConsultaUnProyectoConIdInvalido();

    entoncesRespondeBadRequestSinLlegarAlCasoDeUso(respuesta);
}

// --- cuando ---
private ResultActions cuandoSeConsultaUnProyectoConIdInvalido() throws Exception {
    return mockMvc.perform(get("/api/proyectos/id-invalido"));
}

// --- entonces ---
private void entoncesRespondeBadRequestSinLlegarAlCasoDeUso(ResultActions respuesta) throws Exception {
    respuesta.andExpect(status().isBadRequest());
    verifyNoInteractions(obtenerProyecto);
}
```

Aislamiento entre negocios contra PostgreSQL (rollback por test; las entidades JPA siguen con `UUID`):

```java
@Transactional
class IProyectoJpaRepositoryTest extends PostgresRepositoryTestSupport {

    @Test
    void unNegocioNoVeLosProyectosDeOtroNegocio() {
        UUID proyectoDelNegocioA = dadoUnProyectoDelNegocioA();

        Optional<ProyectoEntity> proyecto = cuandoElNegocioBBuscaElProyecto(proyectoDelNegocioA);

        entoncesNoEncuentraNada(proyecto);
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
- Un nombre en inglés o con `Should`, un `@Test` o `@ParameterizedTest` sin `@DisplayName`, un nombre con `_` o un
  `import ...*;` son errores de estilo aunque el test pase.

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
grep -rn "Should[A-Z]" backend/src/test                # solo código legado, ningún test nuevo
grep -rL "@DisplayName" --include=*Test.java backend/src/test   # archivos de test sin ningún @DisplayName: revisar
grep -rA1 -E "@(Test|ParameterizedTest)\b" backend/src/test | grep -vE "@(Test|ParameterizedTest)\b|@DisplayName|^--"   # vacío: ningún @Test sin su @DisplayName justo debajo
```

El de `Should` solo puede mostrar tests legados que todavía no se tocaron, y los de `@DisplayName` no deben encontrar
nada: si aparece un test que escribiste o refactorizaste, renombralo a una frase del negocio y agregale su `@DisplayName`. Si tocás un test legado, lo dejás con el
estilo nuevo (regla de oro de `codigo-semantico`).
