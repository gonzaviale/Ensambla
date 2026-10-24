# Arquitectura del backend

Fuente única de las reglas de estructura del backend. Los skills de [`../.claude/skills`](../.claude/skills), su copia
para Codex en [`../.agents/skills`](../.agents/skills) y los `AGENTS.md` resumen lo que está acá; si algo cambia, se
cambia primero acá. `ArchitectureTest` verifica las reglas que se pueden comprobar sin dependencias nuevas.

## Capas y flujo

Clean Architecture en dos capas. `domain` no conoce a `infrastructure` ni a Spring.

```
Controller → PresentationMapper → Caso de uso → (Servicio compartido) → Puerto → RepositoryImpl → JPA
```

```
com.ensambla.backend
├── domain                                   Java plano: sin Spring, sin JPA
│   ├── model/<area>/                        modelos, reglas puras, value objects, ids (<Modelo>Id) y enums del área
│   │   └── common/                          Identificador, Pagina, TextoNormalizado, Huella
│   ├── repository/<area>/                   puertos de persistencia
│   ├── port/                                puertos de salida: TransaccionPort, LectorCuadernoPort, SamiIaPort…
│   ├── service/<area>/                      solo lógica que comparten dos o más casos de uso
│   ├── useCase/<area>/
│   │   ├── <Area>Item.java                  DTOs que comparten varios casos de uso del área
│   │   └── <accion>/                        un subpaquete por caso de uso
│   │       ├── <Accion>.java                el caso de uso: un único execute()
│   │       ├── <Accion>Request.java         entrada (record), si hace falta
│   │       └── <Accion>Response.java        salida (record) con su fábrica de()
│   └── exception/                           ResourceNotFound, DuplicateResource, BusinessRule…
└── infrastructure
    ├── config/useCase/<Area>UseCaseConfig   los @Bean de casos de uso, servicios y reglas del dominio
    ├── config/IdentificadoresConfig         ids tipados ↔ JSON y parámetros HTTP
    ├── presentation/                        controller, dto web, mapper de presentación, GlobalExceptionHandler
    └── repository/                          entity, jpaRepository/{interfaces,implementation}, mapper, TransaccionAdapter
```

El módulo de referencia es `plantilla` para lecturas y `proyecto` para el CRUD con aislamiento por negocio.

## Las reglas

### 1. Los tests con base usan Testcontainers, nunca H2

**Por qué:** H2 en "modo PostgreSQL" no es PostgreSQL. Se saltea Flyway, no tiene `jsonb`, ni los mismos bloqueos ni
las mismas restricciones, y un test verde no garantiza nada en producción.

**Cómo:** todo test que levanta base o contexto extiende `support/PostgresRepositoryTestSupport` (perfil
`postgres-test`, Flyway y `ddl-auto=validate`). Los tests unitarios no levantan nada.

```java
// ✅
class BloqueSeguridadIntegrationTest extends PostgresRepositoryTestSupport { … }

// ❌
@SpringBootTest
@ActiveProfiles("h2-test")
class BloqueSeguridadIntegrationTest { … }
```

### 2. Los ids son value objects con tipo propio

**Por qué:** con `UUID` crudo nada impide pasar un `bloqueId` donde se espera un `campoId`; el compilador lo acepta y
el error aparece como un 404 raro. Con un tipo por entidad, el error sale al compilar.

**Cómo:**

- Un record por entidad, en el paquete de su modelo, que implementa `model/common/Identificador`:
  `public record ProyectoId(UUID valor) implements Identificador`. Rechaza `null` y ofrece `de(UUID)`, `de(String)`,
  `nuevo()` y un `toString()` que devuelve el UUID.
- Lo usan todas las capas: modelo, puertos, casos de uso, servicios, DTOs, controllers y DTOs web.
- Las `*Entity` y los `I*JpaRepository` siguen con `UUID`: el esquema no cambia. La conversión vive en los
  `*RepositoryMapper` (`Identificador.opcional(entity.getId(), ProyectoId::de)`, `Identificador.valorDe(proyecto.getId())`)
  y en los `*RepositoryImpl` (`proyectoId.valor()`, `Identificador.valores(ids)`).
- Los parámetros de `JdbcTemplate` llevan `.valor()`: el driver no conoce los ids tipados.
- `IdentificadoresConfig` serializa cualquier `Identificador` como el string del UUID y lo lee por tipo, y convierte
  `@PathVariable`/`@RequestParam`. El JSON no cambia y un id inválido sigue respondiendo 400.
- En el dominio solo los `*Id`, `Identificador`, `IdSistema` y `Huella` nombran `UUID`.

```java
// ✅
public Optional<Proyecto> findByIdAndNegocioId(ProyectoId proyectoId, NegocioId negocioId);

// ❌
public Optional<Proyecto> findByIdAndNegocioId(UUID proyectoId, UUID negocioId);
```

### 3. Los enums viven junto a su modelo

**Por qué:** un paquete `enums` agrupa por tipo de construcción, no por concepto; para entender `TipoCampo` hay que
saltar de paquete. Junto al modelo se lee todo el concepto en un lugar.

```
✅ domain/model/campo/TipoCampo.java
❌ domain/model/enums/TipoCampo.java
```

### 4. Los DTOs están en el subpaquete de su caso de uso

**Por qué:** el request y el response son parte del contrato del caso de uso; separarlos en `domain/dto` obliga a
navegar dos árboles paralelos y deja DTOs huérfanos cuando se borra el caso.

**Cómo:** `domain/useCase/<area>/<accion>/` contiene el caso de uso y sus DTOs. Un DTO que usan varios casos del área
sube a `domain/useCase/<area>/` (por ejemplo `ProyectoItem`). Si lo usan varias áreas, queda en el área dueña.

```
✅ domain/useCase/proyecto/crearProyecto/CrearProyectoRequest.java
❌ domain/dto/proyecto/CrearProyectoRequest.java
```

### 5. Los casos de uso no llevan el sufijo `UseCase`

**Por qué:** el paquete `useCase` ya dice qué es; el sufijo solo alarga los nombres.

**Cómo:** la clase se llama con la acción (`CrearProyecto`), su test `CrearProyectoTest` y su `@Bean` `crearProyecto()`.

```java
// ✅
public class CrearProyecto { public ProyectoItem execute(CrearProyectoRequest request) { … } }

// ❌
public class CrearProyectoUseCase { … }
```

### 6. Los DTOs se mapean solos; no hay mappers de dominio

**Por qué:** un mapper por caso de uso era una clase, un bean y un test más para lo que es una fábrica de una línea.

**Cómo:** dominio → DTO es una fábrica estática del record (`ProyectoItem.de(proyecto)`,
`ListarPlantillasResponse.de(plantillas)`). DTO → dominio, si hace falta, es un método del request. Los mappers de
infraestructura (`*PresentationMapper`, `*RepositoryMapper`) se mantienen: traducen entre capas, no dentro del dominio.

```java
// ✅
public record ListarPlantillasResponse(List<PlantillaItem> content) {

    public static ListarPlantillasResponse de(List<Plantilla> plantillas) {
        return new ListarPlantillasResponse(plantillas.stream().map(PlantillaItem::de).toList());
    }
}

// ❌
public class ListarPlantillasMapper { public ListarPlantillasResponse toResponse(List<Plantilla> plantillas) { … } }
```

### 7. Servicio solo si se comparte, y el dominio sin Spring

**Por qué:** un servicio que usa un único caso de uso es una indirección: obliga a abrir dos archivos para leer un
flujo y a mockear una capa más. Y un dominio con `@Service`/`@Transactional` depende del framework.

**Cómo:**

- Un método va a un servicio de `domain/service/<area>` solo si lo usan dos o más casos de uso (o otro servicio).
  Si lo usa uno solo, es un método privado de ese caso de uso, que llama directo al puerto. Comprobalo con grep.
- Una regla pura, sin I/O, va al modelo (método, función estática o value object).
- Sin `@Service`, `@Component`, `@Transactional` ni ningún import de `org.springframework` en `domain`. Casos de uso,
  servicios y reglas se registran a mano como `@Bean` en el `<Area>UseCaseConfig`; el orden de los argumentos es el
  orden de los campos (`@RequiredArgsConstructor`).
- La frontera de la transacción la pone el caso de uso con `TransaccionPort` (lo implementa `TransaccionAdapter` con un
  `TransactionTemplate`, propagación REQUIRED). En los tests unitarios se usa `support/TransaccionDirecta`.

```java
// ✅
@RequiredArgsConstructor
public class RegistrarUsuario {

    private final UsuarioRepository usuarioRepository;

    private final TransaccionPort transaccionPort;

    public UsuarioResponse execute(RegistrarUsuarioRequest request) {
        return UsuarioResponse.de(transaccionPort.ejecutar(() -> registrar(request)));
    }
}

// ❌
@Service
@Transactional
public class RegistroService { … }   // un solo caso de uso lo llama
```

## Otras reglas que siguen vigentes

- **Aislamiento por negocio:** el `NegocioId` sale de la sesión, jamás del body, la URL ni un query param. Un recurso
  ajeno responde 404, igual que uno inexistente.
- **Tiempo:** solo los casos de uso tienen `Clock`; los servicios reciben el instante por parámetro.
- **Flyway:** una migración aplicada nunca se edita.
- **Nombres de dominio en español.**
- **Cobertura:** `./mvnw clean verify` exige 100 % de líneas y ramas por clase.

## Qué verifica `ArchitectureTest`

| Regla | Cómo |
|---|---|
| `domain` no depende de `infrastructure` ni de Spring | bytecode de `domain` sin `com/ensambla/backend/infrastructure` ni `org/springframework` |
| Sin sufijo `UseCase` | ninguna clase de `domain/useCase` termina en `UseCase` |
| Sin `domain/dto`, `domain/mapper` ni `domain/model/enums` | los paquetes no existen |
| Ids tipados | en los fuentes de `domain`, solo los `*Id`, `Identificador`, `IdSistema` y `Huella` nombran `UUID` |
| Sin H2 | no hay `application-h2-test.properties` ni `org.h2.Driver` en el classpath |

## Controles rápidos

Desde `backend/`, todos deberían salir vacíos:

```bash
grep -rl "org.springframework" src/main/java/com/ensambla/backend/domain
find src/main/java/com/ensambla/backend/domain -name "*UseCase.java"
ls -d src/main/java/com/ensambla/backend/domain/{dto,mapper,model/enums} 2>/dev/null
grep -rlw "UUID" src/main/java/com/ensambla/backend/domain | grep -v "Id.java$\|Identificador\|IdSistema\|Huella"
grep -n "h2database" pom.xml; ls src/test/resources/application-h2-test.properties 2>/dev/null
```
