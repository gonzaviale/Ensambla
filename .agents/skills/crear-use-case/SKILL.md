---
name: crear-use-case
description: Crea un caso de uso nuevo (endpoint) del backend de Ensambla de punta a punta siguiendo la estructura de capas del proyecto - model e id tipado, repositorio (puerto), caso de uso con sus DTOs en su subpaquete (fábricas de(), sin mapper de dominio), servicio solo si se comparte, bean manual en *UseCaseConfig, entity, JPA, RepositoryImpl, mapper de repositorio, DTO web, mapper de presentación y controller - más sus tests. Usar cuando se pida "agregar un endpoint", "crear el caso de uso de X", "nuevo módulo/feature del backend", "exponer GET/POST/PATCH/DELETE de X", "agregar una consulta" o "sumar una entidad". Para limpiar o reordenar un caso de uso que ya existe usar refactor-use-case; para los tests usar escribir-tests; las reglas de estructura están en reglas-arquitectura.
---

> Copia para Codex. La fuente es `.claude/skills/crear-use-case/SKILL.md`: si cambiás uno, cambiá los dos.

# Crear un caso de uso nuevo (backend Ensambla)

Lleva una funcionalidad nueva a la estructura de capas del proyecto, sin saltearse ninguna.
Las reglas de estructura están en [`backend/ARQUITECTURA.md`](../../../backend/ARQUITECTURA.md) (checklist en el skill
`reglas-arquitectura`). Los nombres de dominio van **en español** (clases, campos, métodos, enums, tablas).
Vocabulario y tablas: `docs/modelo-datos.md`. Tareas: `docs/backlog.md`.

## Antes de escribir código

1. **Leé el módulo de referencia:** `plantilla` para una lectura (`ListarPlantillas`, `ListarPlantillasResponse`,
   `PlantillaItem`, `PlantillaController`, `PlantillaRepositoryImpl`) y `proyecto` para un CRUD con aislamiento por
   negocio (`CrearProyecto`, `ProyectoItem`, `ProyectoController`).
2. **Verificá que la tabla y la entidad existan** (`db/migration`, `infrastructure/repository/entity`). Si hay tablas o
   columnas nuevas van en una **migración Flyway nueva**: nunca se edita una ya aplicada.
3. **Definí la regla de aislamiento** (ver más abajo) antes de escribir la consulta: es lo que más se olvida.
4. **Decidí si hace falta un servicio:** solo si la lógica ya la usa otro caso de uso (buscala con grep). Si no, va
   como método privado del caso de uso.

## Estructura: qué archivo va en cada capa

Todo bajo `backend/src/main/java/com/ensambla/backend/`. Ejemplo con la feature `proyecto` / `Crear`:

| Orden | Archivo | Qué contiene |
|---:|---|---|
| 1 | `domain/model/proyecto/ProyectoId.java` | `record ProyectoId(UUID valor) implements Identificador`: rechaza `null`, `de(UUID)`, `de(String)`, `nuevo()`, `toString()` = UUID. Solo si la entidad es nueva. |
| 2 | `domain/model/proyecto/Proyecto.java` | Modelo (`@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`) con ids tipados. Reglas puras sin I/O como métodos. Sus enums van en el mismo paquete. |
| 3 | `domain/repository/proyecto/ProyectoRepository.java` | Puerto de persistencia, solo tipos de dominio e ids tipados. |
| 4 | `domain/useCase/proyecto/crearProyecto/CrearProyectoRequest.java` | Record de entrada con ids tipados. |
| 5 | `domain/useCase/proyecto/ProyectoItem.java` o `crearProyecto/CrearProyectoResponse.java` | Record de salida con `static … de(Proyecto)`. **Nunca modelos de dominio dentro de un DTO.** Si lo comparten varios casos del área, va en `domain/useCase/proyecto/`. |
| 6 | `domain/useCase/proyecto/crearProyecto/CrearProyecto.java` | Sin anotaciones de Spring ni sufijo. `@RequiredArgsConstructor`, un único `execute(...)`. Recibe puertos, servicios compartidos, `TransaccionPort` y `Clock` si los usa. |
| 7 | `domain/service/proyecto/ProyectoService.java` | **Solo si la lógica la usan dos o más casos de uso.** Java plano, `@RequiredArgsConstructor`, sin `@Service`/`@Transactional`. |
| 8 | `infrastructure/config/useCase/ProyectoUseCaseConfig.java` | `@Bean crearProyecto(...)` (y del servicio, si es nuevo). El orden de los argumentos es el de los campos. |
| 9 | `infrastructure/repository/entity/ProyectoEntity.java` | Entidad JPA con `UUID` (`@Entity @Table`, enums con `@Enumerated(EnumType.STRING)` + `columnDefinition = "text"`, JSON con `@JdbcTypeCode(SqlTypes.JSON)`). |
| 10 | `infrastructure/repository/jpaRepository/interfaces/IProyectoJpaRepository.java` | Spring Data con `UUID`. Consultas derivadas o `@Query` con javadoc de la regla. |
| 11 | `infrastructure/repository/mapper/ProyectoRepositoryMapper.java` | `@Component`, `toDomain`/`toEntity` con builders. Convierte ids: `Identificador.opcional(entity.getId(), ProyectoId::de)` y `Identificador.valorDe(proyecto.getId())`. |
| 12 | `infrastructure/repository/jpaRepository/implementation/ProyectoRepositoryImpl.java` | `@Component`, implementa el puerto; pasa `proyectoId.valor()` al JPA repo y `.valor()` a `JdbcTemplate`. |
| 13 | `infrastructure/presentation/dto/proyecto/ProyectoResponse.java` | Record del JSON HTTP, con ids tipados (se serializan como el UUID). Validaciones (`@NotBlank`…) en los request web. |
| 14 | `infrastructure/presentation/mapper/proyecto/ProyectoPresentationMapper.java` | `@Component`. Parámetros HTTP ↔ DTO del caso de uso. **No importa `domain.model.*`** salvo los ids. |
| 15 | `infrastructure/presentation/controller/ProyectoController.java` | `@RestController`, `@Tag`, `@Operation`. Sin lógica. `@PathVariable ProyectoId`. |

Flujo de un request: `Controller → PresentationMapper → Caso de uso → (Servicio compartido) → Puerto → RepositoryImpl → JPA`.

## Plantillas (copiá la forma, cambiá los nombres)

Caso de uso: orquesta; la lógica que solo él usa va en métodos privados, y la escritura dentro de la transacción.

```java
@RequiredArgsConstructor
public class CrearProyecto {

    private final NegocioService negocioService;

    private final ProyectoRepository proyectoRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public ProyectoItem execute(CrearProyectoRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Proyecto proyecto = transaccionPort.ejecutar(() -> crear(negocioId, request, OffsetDateTime.now(clock)));
        return ProyectoItem.de(proyecto);
    }

    private Proyecto crear(NegocioId negocioId, CrearProyectoRequest request, OffsetDateTime ahora) {
        return proyectoRepository.save(Proyecto.nuevo(negocioId, request.nombre(), request.descripcion(), ahora));
    }
}
```

DTO con su fábrica:

```java
public record ProyectoItem(ProyectoId id, String nombre, OffsetDateTime creadoEn) {

    public static ProyectoItem de(Proyecto proyecto) {
        return new ProyectoItem(
                proyecto.getId(),
                proyecto.getNombre(),
                proyecto.getCreadoEn());
    }
}
```

Bean manual:

```java
@Bean
public CrearProyecto crearProyecto(NegocioService negocioService, ProyectoRepository proyectoRepository,
        TransaccionPort transaccionPort, Clock clock) {
    return new CrearProyecto(negocioService, proyectoRepository, transaccionPort, clock);
}
```

Controller: delega todo, devuelve `ResponseEntity`.

```java
@GetMapping
@Operation(summary = "Listar proyectos del negocio de la sesión")
public ResponseEntity<List<ProyectoResponse>> listar(Authentication authentication) {
    return ResponseEntity.ok(proyectoPresentationMapper.toResponse(listarProyectos.execute(
            proyectoPresentationMapper.toListarRequest(UsuarioId.de(authentication.getName())))));
}
```

## Reglas del proyecto que hay que respetar

- **Aislamiento por negocio (T-010).** Todo dato de un negocio (proyecto, bloque, campo, registro) se filtra por
  `negocio_id`. El `NegocioId` **sale de la sesión en el servidor**, jamás del body, la URL ni un query param. Un
  recurso de otro negocio responde `404`, igual que un id inexistente. Usá consultas `findByIdAndNegocioId(...)`.
  Las **plantillas** son globales (sin `negocio_id`) y de solo lectura.
- **Errores.** `ResourceNotFoundException` → 404, `DuplicateResourceException` → 409, `BusinessRuleException` → 400;
  violación de restricción de la base → 409; body inválido (`@Valid`) o id/parámetro inválido → 400;
  `CredencialesInvalidasException` → 401; cualquier otra → 500 (todo lo traduce `GlobalExceptionHandler`).
  Los errores de validación de campos van como `{ campo: mensaje }` en `errors`. No armes respuestas de error a mano.
- **Sin N+1.** Si una lista trae datos relacionados, cargalos **en lote** con `IN (...)` y agrupá en memoria.
- **Reglas de negocio en el dominio**, no en SQL ni en mappers.
- **Tiempo.** Solo el caso de uso tiene `Clock`: pasa `OffsetDateTime.now(clock)` a los servicios y al modelo.
  Las entidades no tienen `@PrePersist`: `creado_en` y `actualizado_en` los completa el dominio.
- **Dominio sin Spring.** Sin `@Service`, `@Component` ni `@Transactional` en `domain`; la transacción la abre el caso
  de uso con `TransaccionPort`. No crees interfaces para casos de uso con una sola implementación ni devuelvas
  entidades JPA o modelos de dominio por HTTP.
- **Un controller y un `*UseCaseConfig` por área** (`<Area>Controller`, `<Area>UseCaseConfig`).
- **Seguridad.** Todo endpoint es privado salvo los listados en `SecurityConfig`. Las escrituras exigen el header
  `X-CSRF-TOKEN` (`GET /api/auth/csrf`).

## Estilo de código

Una sentencia por línea, una anotación por línea, línea en blanco después de `{` de la clase y entre miembros, imports
explícitos (solo se toleran `lombok.*` y `jakarta.persistence.*`), ordenados proyecto/libs, línea en blanco, `java.*`;
líneas ≤ 120 columnas; argumentos y builders uno por línea; campos con el nombre del tipo (`proyectoRepository`,
`transaccionPort`), nunca `repository`/`service`.

## Tests (obligatorios)

Cada clase nueva con comportamiento lleva su test: usá el skill **escribir-tests**. El build exige **100 % de líneas y
ramas por clase** (JaCoCo en `verify`) y `ArchitectureTest` falla si se rompe una regla de estructura.

## Verificación

Desde `backend/`, con JDK 21 y Docker corriendo:

```powershell
.\mvnw.cmd test "-Dtest=CrearProyectoTest,ProyectoItemTest,ArchitectureTest"
.\mvnw.cmd clean verify
```

Después abrí Swagger (`/swagger-ui.html`, solo con el perfil `dev`) y probá el endpoint.

## Checklist

- [ ] Aislamiento por negocio definido y cubierto con test (recurso ajeno → 404).
- [ ] Ids tipados en todas las capas; `UUID` solo en entidades y JPA.
- [ ] Caso de uso sin sufijo, con sus DTOs en su subpaquete y fábricas `de()`; sin mapper de dominio.
- [ ] Servicio nuevo solo si lo usan dos o más casos de uso; nada de Spring en `domain`.
- [ ] `execute()` es una lista de pasos; las escrituras dentro de `transaccionPort.ejecutar(...)`.
- [ ] Bean agregado al `*UseCaseConfig` con el orden correcto de argumentos.
- [ ] `@Tag`/`@Operation` en el controller y ruta agregada a `SecurityConfig` solo si es pública.
- [ ] Migración Flyway nueva si cambió el esquema (y `docs/modelo-datos.md` actualizado).
- [ ] Tests por capa y `clean verify` en verde.
