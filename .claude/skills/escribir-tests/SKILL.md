---
name: escribir-tests
description: Escribe o reescribe los tests del backend de Ensambla al estilo del proyecto - Arrange-Act-Assert con helpers privados givenX/thenX, métodos camelCase xShouldYWhenZ, @DisplayName en español, imports explícitos, builders en vez de reflexión, un comportamiento por test - con plantillas por capa (use case, servicio, mapper de dominio, mapper de presentación, controller MockMvc, repo impl, mapper de repositorio, JPA contra PostgreSQL con Testcontainers, integración HTTP, excepciones, config). Usar cuando se pida "escribir/agregar/arreglar tests", "testear este use case/servicio/controller/mapper", "cubrir X con tests" o al terminar una feature nueva. Para refactorizar el use case en sí usar refactor-use-case; para crear una feature usar crear-use-case.
---

# Escribir tests del backend (estilo Ensambla)

Un test se lee como una frase: **given → acción → then**. La mecánica (stubs, ejecución, asserts, verifies)
va en **métodos privados al final de la clase**. Plantillas vivas: el módulo `plantilla` completo
(`PlantillaServiceTest`, `ListarPlantillasUseCaseTest`, `ListarPlantillasMapperTest`,
`PlantillaPresentationMapperTest`, `PlantillaRepositoryImplTest`, `PlantillaRepositoryMapperTest`,
`PlantillaControllerTest`, `PlantillaUseCaseConfigTest`) y `UsuarioServiceTest`.

Los tests replican el paquete de la clase probada, bajo `backend/src/test/java/com/ensambla/backend/`.

## Reglas (todas obligatorias)

1. **Nombre** `metodoShouldHacerXWhenY` en camelCase, **sin guiones bajos** y sin frases en español.
2. **`@DisplayName` en español** describiendo el comportamiento ("Rechaza al usuario suspendido con credenciales inválidas").
3. **Un comportamiento por test.** No pruebes dos métodos ni dos endpoints en el mismo test.
4. **Cuerpo de 3 bloques**, separados por línea en blanco, de pocas líneas. Los helpers se agrupan al final bajo
   `// --- arrange ---`, `// --- act ---`, `// --- assert ---` (y `// --- helpers ---` para builders de datos).
5. **Imports explícitos**: ningún `*`, ni en estáticos. Sin nombres totalmente calificados dentro del código. Sin imports ni variables sin usar.
6. **Una sentencia por línea.** Prohibido `@Test void x() { ...; ...; }` en una línea.
7. **Datos con builders** (`Proyecto.builder()...`) o con el constructor del record. Cada campo mapeado se verifica con un
   `assertThat` explícito (el único `usingRecursiveComparison` aceptado es entidad↔modelo en un mapper bidireccional).
   Valores en `private static final` con nombre (`USUARIO_ID`, `NOMBRE`); datos compartidos en `support/*TestData`.
8. **Mockito estricto**: cada `givenX()` debe usarse en el camino de ese test, o salta `UnnecessaryStubbingException`.
9. Comentarios solo donde no es obvio.
10. **Cobertura 100 % de líneas y ramas por clase** (el `verify` falla si no): cada `if`, `orElseThrow` y `null` del código probado tiene su test.
    Los getters/setters/builders de Lombok no cuentan (`lombok.addLombokGeneratedAnnotation = true`).

## Qué probar en cada capa

| Clase probada | Test | Cómo | Casos mínimos |
|---|---|---|---|
| `*UseCase` | `*UseCaseTest` | `@ExtendWith(MockitoExtension.class)`, `@Mock` del servicio, mapper **real**, use case en `@BeforeEach` | respuesta mapeada · delega con los argumentos correctos · propaga la excepción sin convertirla |
| `*Service` | `*ServiceTest` | `@Mock` de repositorios/otros servicios + `@InjectMocks` | camino feliz · cada rama de error · **orden** de validaciones (`InOrder`) · que no consulta si la validación falla (`verifyNoInteractions`) |
| Model con reglas | `*Test` | sin mocks | cada regla, casos borde |
| `*Mapper` (dominio) | `*MapperTest` | sin mocks, mapper en `@BeforeEach` | **todos** los campos · orden conservado · nulos · lista vacía |
| `*PresentationMapper` | `*PresentationMapperTest` | sin mocks | un test por método · lista vacía |
| `*Controller` | `*ControllerTest` | `mock()` de los use cases + `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler())` | 200 con JSON · el request llega bien al use case (`ArgumentCaptor`) · 200 `[]` · 404 · 400 por parámetro inválido (`verifyNoInteractions`) · 400 por validación del body |
| `*RepositoryImpl` | `*RepositoryImplTest` | `@Mock` del JPA repo; mapper real | mapeo completo · orden · vacío · `Optional` presente/ausente |
| `*RepositoryMapper` | `*RepositoryMapperTest` | sin mocks | `toDomain`/`toEntity` con todos los campos · entidad vacía (nulos) · enums |
| `I*JpaRepository` / esquema | `I*JpaRepositoryTest`, `EsquemaPostgresIntegrationTest` | `extends PostgresRepositoryTestSupport` + `@Transactional` (rollback) | la consulta real: filtros, orden, **aislamiento entre negocios**, constraints |
| Seguridad HTTP | `SeguridadIntegrationTest` | `@SpringBootTest @AutoConfigureMockMvc`, perfil `h2-test` | 401 sin sesión · 403 sin CSRF · rutas públicas |
| Excepciones, config, arranque | `DomainExceptionTest`, `CorsFilterTest`, `SwaggerConfigTest`, `BackendApplicationTests`, `ArchitectureTest` | ver los existentes | mensaje/causa/herencia · headers · contexto carga |

## Plantillas

Servicio:

```java
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("Rechaza al usuario inexistente con credenciales inválidas")
    void obtenerActivoShouldThrowWhenMissing() {
        givenRepositoryHasNoUsuario();

        assertThatThrownBy(this::obtenerActivo).isInstanceOf(CredencialesInvalidasException.class);
    }

    // --- arrange ---
    private void givenRepositoryHasNoUsuario() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());
    }

    // --- act ---
    private Usuario obtenerActivo() {
        return usuarioService.obtenerActivo(USUARIO_ID);
    }
}
```

Use case (mapper real, colaborador mockeado):

```java
@BeforeEach
void setUp() {
    listarPlantillasUseCase = new ListarPlantillasUseCase(plantillaService, new ListarPlantillasMapper());
}
```

Controller (MockMvc standalone; el 400 no debe llegar al use case):

```java
@Test
@DisplayName("GET devuelve 400 cuando el id no es un UUID")
void obtenerShouldReturn400WhenIdIsNotUuid() throws Exception {
    ResultActions result = mockMvc.perform(get("/api/proyectos/id-invalido"));

    result.andExpect(status().isBadRequest());
    verifyNoInteractions(obtenerProyectoUseCase);
}
```

Aislamiento entre negocios contra PostgreSQL (rollback por test):

```java
@Transactional
class IProyectoJpaRepositoryTest extends PostgresRepositoryTestSupport {

    @Test
    @DisplayName("No devuelve un proyecto de otro negocio")
    void findByIdAndNegocioIdShouldReturnEmptyWhenProyectoBelongsToOtherNegocio() {
        UUID proyectoDeA = givenProyecto(negocioA);

        Optional<ProyectoEntity> result = repository.findByIdAndNegocioId(proyectoDeA, negocioB);

        assertThat(result).isEmpty();
    }
}
```

## Detalles que suelen romper

- JSON numérico: Jackson devuelve `Integer`/`Double`. Si el modelo usa `Short` o `BigDecimal`, comparalo con
  `.value(POSICION.intValue())`; si no, falla con "expected 1 but was 1".
- Los `I*JpaRepositoryTest` crean sus datos con los repos JPA dentro de la transacción del test (se revierten solos);
  no dependas de datos que dejó otro test.
- Una violación de constraint dentro de un test transaccional deja la transacción abortada: probá una sola por test.
- Un `@Test` sin `@DisplayName`, un nombre con `_` o un `import ...*;` son errores de estilo aunque el test pase.

## Ejecutar

Desde `backend/`, con JDK 21 y Docker corriendo:

```powershell
.\mvnw.cmd test "-Dtest=PlantillaServiceTest,ListarPlantillasUseCaseTest"
.\mvnw.cmd clean verify
```

Resúmenes en `target/surefire-reports/*.txt`; reporte de cobertura en `target/site/jacoco/index.html`.
Si `verify` falla por cobertura, `target/site/jacoco/jacoco.csv` lista las clases con líneas o ramas sin cubrir.

## Controles antes de cerrar

```bash
grep -rEn "^import .*\*;" backend/src/test             # sin wildcards
grep -rEn "void [a-z]+_[a-zA-Z_]+\(" backend/src/test  # sin guiones bajos
```

Y confirmá que cada `@Test` tiene su `@DisplayName`.
