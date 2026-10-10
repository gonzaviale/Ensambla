---
name: crear-use-case
description: Crea un use case nuevo (endpoint) del backend de Ensambla de punta a punta siguiendo la estructura de capas del proyecto - model, repositorio (puerto), servicio, DTOs, mapper de dominio, use case, wiring en *UseCaseConfig, entity, JPA, RepositoryImpl, mapper de repositorio, DTO web, mapper de presentación y controller - más sus tests. Usar cuando se pida "agregar un endpoint", "crear el use case de X", "nuevo módulo/feature del backend", "exponer GET/POST/PATCH/DELETE de X", "agregar una consulta" o "sumar una entidad". Para limpiar o reordenar un use case que ya existe usar refactor-use-case; para los tests usar escribir-tests.
---

# Crear un use case nuevo (backend Ensambla)

Lleva una funcionalidad nueva a la estructura de capas del proyecto, sin saltearse ninguna.
Los nombres de dominio van **en español** (clases, campos, métodos de servicio, enums, tablas).
Vocabulario y tablas: `docs/modelo-datos.md`. Tareas del MVP2: `docs/backlog.md`.

## Antes de escribir código

1. **Leé el módulo de referencia**: `plantilla` es el único completo hoy (listado de solo lectura):
   `ListarPlantillasUseCase`, `PlantillaService`, `PlantillaController`, `PlantillaRepositoryImpl`.
   Cuando el equipo termine `proyecto` (T-012) pasa a ser la referencia para CRUD con aislamiento por negocio.
2. **Verificá que la tabla y la entidad existan** (`V1__esquema_inicial.sql`, `infrastructure/repository/entity`).
   Si hay tablas o columnas nuevas van en una **migración Flyway nueva** (`V2__...sql`): nunca se edita una ya aplicada.
3. **Definí la regla de aislamiento** (ver más abajo) antes de escribir la consulta: es lo que más se olvida.

## Estructura: qué archivo va en cada capa

Todo bajo `backend/src/main/java/com/ensambla/backend/`. `domain` **no** depende de `infrastructure`
(`ArchitectureTest` lo verifica). Ejemplo con la feature `plantilla` / `Listar`:

| Orden | Archivo | Qué contiene |
|---:|---|---|
| 1 | `domain/model/plantilla/Plantilla.java` | Modelo de dominio: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`. Reglas puras sin I/O van como métodos acá. |
| 2 | `domain/repository/plantilla/PlantillaRepository.java` | Interfaz (puerto) de persistencia. Solo tipos de dominio. |
| 3 | `domain/service/plantilla/PlantillaService.java` | `@Service @RequiredArgsConstructor @Transactional(readOnly = true)`. Reglas que tocan repositorio: existencia, unicidad, pertenencia al negocio. Lanza `ResourceNotFoundException` / `DuplicateResourceException` / `BusinessRuleException`. |
| 4 | `domain/dto/plantilla/` | `ListarPlantillasResponse`, `PlantillaItem` (records). **Nunca modelos de dominio dentro de un DTO.** |
| 5 | `domain/mapper/plantilla/ListarPlantillasMapper.java` | Clase plana (sin anotaciones), `toResponse(...)`. Un mapper **por use case**. |
| 6 | `domain/useCase/plantilla/ListarPlantillasUseCase.java` | Sin anotaciones de Spring. `@RequiredArgsConstructor`, un único `execute(...)`. |
| 7 | `infrastructure/config/useCase/<Area>UseCaseConfig.java` | `@Bean` de cada mapper y cada use case. El orden de los argumentos del constructor es el orden de los campos. |
| 8 | `infrastructure/repository/entity/PlantillaEntity.java` | Entidad JPA (`@Entity @Table`, enums con `@Enumerated(EnumType.STRING)` + `columnDefinition = "text"`, JSON con `@JdbcTypeCode(SqlTypes.JSON)`). |
| 9 | `infrastructure/repository/jpaRepository/interfaces/IPlantillaJpaRepository.java` | Spring Data. Consultas derivadas o `@Query` con javadoc de la regla. |
| 10 | `infrastructure/repository/mapper/PlantillaRepositoryMapper.java` | `@Component`, `toDomain(entity)` (y `toEntity` si se escribe) con builders, un campo por línea. |
| 11 | `infrastructure/repository/jpaRepository/implementation/PlantillaRepositoryImpl.java` | `@Component`, implementa el puerto, usa el JPA repo y el mapper. |
| 12 | `infrastructure/presentation/dto/plantilla/PlantillaResponse.java` | Record del JSON HTTP. Validaciones (`@NotBlank`, etc.) en los request web. |
| 13 | `infrastructure/presentation/mapper/plantilla/PlantillaPresentationMapper.java` | `@Component`. Convierte parámetros HTTP ↔ DTO de dominio. **No importa `domain.model.*`.** |
| 14 | `infrastructure/presentation/controller/PlantillaController.java` | `@RestController`, `@Tag`, `@Operation`. Sin lógica. |

Flujo de un request: `Controller → PresentationMapper → UseCase → Service → Repository (puerto) → RepositoryImpl → JPA`.

## Plantillas (copiá la forma, cambiá los nombres)

Use case: solo orquesta, el resultado en una variable local.

```java
@RequiredArgsConstructor
public class ListarProyectosUseCase {

    private final ProyectoService proyectoService;
    private final ListarProyectosMapper mapper;

    public ListarProyectosResponse execute(ListarProyectosRequest request) {
        List<Proyecto> proyectos = proyectoService.listar(request.negocioId());
        return mapper.toResponse(proyectos);
    }
}
```

Controller: delega todo, devuelve `ResponseEntity`.

```java
@GetMapping
@Operation(summary = "Listar los proyectos del negocio de la sesión")
public ResponseEntity<List<ProyectoResponse>> listar(Authentication authentication) {
    ListarProyectosResponse response = listarProyectosUseCase.execute(
            proyectoPresentationMapper.toRequest(UUID.fromString(authentication.getName())));
    return ResponseEntity.ok(proyectoPresentationMapper.toResponse(response));
}
```

## Reglas del proyecto que hay que respetar

- **Aislamiento por negocio (T-010).** Todo dato de un negocio (proyecto, bloque, campo, registro) se filtra por `negocio_id`.
  El `negocioId` **sale de la sesión en el servidor**, jamás del body, la URL ni un query param.
  Un recurso de otro negocio responde `404`, igual que un id inexistente (no se filtra que existe).
  Usá consultas `findByIdAndNegocioId(...)` y no repitas la validación inline: va en el servicio de la entidad padre.
  Las **plantillas** son globales (no tienen `negocio_id`) y son de solo lectura para los usuarios.
- **Errores.** `ResourceNotFoundException` → 404, `DuplicateResourceException` → 409, `BusinessRuleException` → 400;
  violación de restricción de la base → 409; body inválido (`@Valid`) o UUID/parámetro inválido → 400;
  `CredencialesInvalidasException` → 401; cualquier otra → 500 (todo lo traduce `GlobalExceptionHandler`).
  Los errores de validación de campos van como `{ campo: mensaje }` en `errors` (T-023). No armes respuestas de error a mano.
- **Sin N+1.** Si una lista trae datos relacionados (campos de cada bloque, por ejemplo), cargalos **en lote** con `IN (...)` y agrupá en memoria.
- **Reglas de negocio en el dominio**, no en SQL ni en mappers.
- **Tiempo.** Los servicios no inyectan `Clock`: el use case pasa `OffsetDateTime.now(clock)` al servicio. Las entidades no tienen `@PrePersist`: `creado_en` y `actualizado_en` los completa el dominio.
- **No** anotar use cases con `@Service`/`@Component` (se registran en el `*UseCaseConfig`), **no** crear interfaces para use cases con una sola implementación, **no** devolver entidades JPA ni modelos de dominio por HTTP.
- **Un controller y un `*UseCaseConfig` por área** (`<Area>Controller`, `<Area>UseCaseConfig`).
- **Seguridad.** Todo endpoint es privado salvo los listados en `SecurityConfig` (`GET /api/plantillas`, `/api/auth/*`, Swagger). Las escrituras exigen el header `X-CSRF-TOKEN` (`GET /api/auth/csrf`).

## Estilo de código

Una sentencia por línea, una anotación por línea, línea en blanco después de `{` de la clase y entre miembros, imports explícitos
(solo se toleran `lombok.*` y `jakarta.persistence.*`), ordenados proyecto/libs, línea en blanco, `java.*`; líneas ≤ 120 columnas;
argumentos y builders uno por línea; campos con el nombre del tipo (`plantillaService`, `listarPlantillasMapper`), nunca `service`/`mapper`.

## Tests (obligatorios)

Cada clase nueva con comportamiento lleva su test: usá el skill **escribir-tests**. El build exige **100 % de líneas y ramas por clase** (JaCoCo en `verify`).

## Verificación

Desde `backend/`, con JDK 21 y Docker corriendo:

```powershell
.\mvnw.cmd test "-Dtest=Listar*Test,PlantillaServiceTest"
.\mvnw.cmd clean verify
```

Después abrí Swagger (`/swagger-ui.html`, solo con el perfil `dev`) y probá el endpoint.

## Checklist

- [ ] Aislamiento por negocio definido y cubierto con test (recurso ajeno → 404).
- [ ] DTOs de dominio y web sin modelos de dominio; presentación no importa `domain.model`.
- [ ] `execute()` es una lista de pasos; nada de `throw` ni consultas inline.
- [ ] Bean agregado al `*UseCaseConfig` con el orden correcto de argumentos.
- [ ] `@Tag`/`@Operation` en el controller y ruta agregada a `SecurityConfig` solo si es pública.
- [ ] Migración Flyway nueva si cambió el esquema (y `docs/modelo-datos.md` actualizado).
- [ ] Tests por capa y `clean verify` en verde.
