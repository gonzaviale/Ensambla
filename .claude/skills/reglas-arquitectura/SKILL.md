---
name: reglas-arquitectura
description: Checklist de las reglas de estructura del backend de Ensambla (backend/ARQUITECTURA.md) para aplicar al escribir o revisar cualquier código Java del backend - tests con Testcontainers y nunca H2, ids tipados (ProyectoId, NegocioId…) en lugar de UUID, enums junto a su modelo, DTOs en el subpaquete de su caso de uso, casos de uso sin sufijo UseCase, DTOs con fábrica de() y sin mappers de dominio, servicios solo para lo compartido y dominio sin Spring (beans manuales, TransaccionPort). Usar siempre que se cree, mueva, renombre o revise una clase de backend/src, cuando se pregunte "dónde va esto", "¿hace falta un servicio/mapper?", "¿cómo tipo este id?", o antes de cerrar una tarea o un PR del backend. Para crear una feature completa usar crear-use-case; para reordenar un caso de uso existente, refactor-use-case; para tests, escribir-tests.
---

# Reglas de arquitectura del backend (Ensambla)

La fuente es [`backend/ARQUITECTURA.md`](../../../backend/ARQUITECTURA.md), con el porqué y los ejemplos de cada regla.
Este skill es el checklist para aplicarlas. Si una regla choca con lo que pide la tarea, decilo antes de romperla.

## Las 7 reglas

1. **Sin H2.** Todo test que levanta base o contexto `extends PostgresRepositoryTestSupport` (Testcontainers, perfil
   `postgres-test`). Nada de `@ActiveProfiles("h2-test")` ni de la dependencia `com.h2database:h2`.
2. **Ids tipados.** Cada entidad tiene su `record <Modelo>Id(UUID valor) implements Identificador` en
   `domain/model/<area>/`, con `de(UUID)`, `de(String)`, `nuevo()` y `toString()` = UUID. Se usa en modelo, puertos,
   casos de uso, servicios, DTOs, controllers (`@PathVariable ProyectoId`) y DTOs web. Las `*Entity` y los
   `I*JpaRepository` siguen con `UUID`: se convierte en `*RepositoryMapper` (`Identificador.opcional`,
   `Identificador.valorDe`) y en `*RepositoryImpl` (`.valor()`, `Identificador.valores`). A `JdbcTemplate` se le pasa
   `.valor()`. El usuario de la sesión es `UsuarioId.de(authentication.getName())`.
3. **Enums junto a su modelo.** `domain/model/campo/TipoCampo`, nunca `domain/model/enums`.
4. **DTOs en el subpaquete del caso de uso.** `domain/useCase/<area>/<accion>/{<Accion>, <Accion>Request,
   <Accion>Response}`. Un DTO que usan varios casos del área sube a `domain/useCase/<area>/` (`ProyectoItem`).
5. **Sin sufijo `UseCase`.** `CrearProyecto`, su test `CrearProyectoTest`, su bean `crearProyecto()`.
6. **Sin mappers de dominio.** Dominio → DTO con una fábrica estática del record (`ProyectoItem.de(proyecto)`); DTO →
   dominio con un método del request. Los `*PresentationMapper` y `*RepositoryMapper` de infraestructura sí existen.
7. **Servicio solo si se comparte; dominio sin Spring.**
   - Un método va a `domain/service/<area>` solo si lo usan **dos o más** casos de uso u otro servicio (comprobalo con
     grep). Si lo usa uno solo, es un método privado del caso de uso que llama directo al puerto. Una regla pura va al
     modelo.
   - En `domain` no hay `@Service`, `@Component`, `@Transactional` ni imports de `org.springframework`. Casos de uso,
     servicios y reglas se registran a mano como `@Bean` en `infrastructure/config/useCase/<Area>UseCaseConfig`, con
     los argumentos en el orden de los campos.
   - La transacción la abre el caso de uso con `TransaccionPort` (`transaccionPort.ejecutar(() -> …)`); en los tests
     unitarios se pasa `new TransaccionDirecta()`.

Siguen valiendo: el `NegocioId` sale de la sesión (recurso ajeno → 404), solo los casos de uso tienen `Clock`, una
migración aplicada no se edita, nombres de dominio en español y 100 % de cobertura por clase.

## Dónde va cada cosa

| Tengo… | Va en… |
|---|---|
| Una regla sin I/O | el modelo (`domain/model/<area>`) |
| Lógica con I/O que usa un solo caso de uso | método privado del caso de uso |
| Lógica con I/O que usan dos o más casos de uso | `domain/service/<area>/<Area>Service`, bean en el `*UseCaseConfig` |
| Entrada / salida de un caso de uso | records en `domain/useCase/<area>/<accion>/` |
| Un id nuevo | `domain/model/<area>/<Modelo>Id` |
| Un enum | el paquete de su modelo |
| Un proveedor externo o la transacción | un puerto en `domain/port`, su adaptador en `infrastructure` |

## Verificación

`ArchitectureTest` controla las reglas 1, 2, 3, 4, 5, 6 y la parte de Spring de la 7. La regla de servicios
compartidos no se puede verificar automáticamente: revisala a mano con grep. Desde `backend/`, estos controles deberían
salir vacíos:

```bash
grep -rl "org.springframework" src/main/java/com/ensambla/backend/domain
find src/main/java/com/ensambla/backend/domain -name "*UseCase.java"
ls -d src/main/java/com/ensambla/backend/domain/{dto,mapper,model/enums} 2>/dev/null
grep -rlw "UUID" src/main/java/com/ensambla/backend/domain | grep -v "Id.java$\|Identificador\|IdSistema\|Huella"
grep -rn "h2-test\|h2database" pom.xml src/test
```

Para saber cuántos casos de uso usan un método de servicio:

```bash
grep -rln "nombreDelServicio.metodo(" src/main/java/com/ensambla/backend/domain/useCase
```

Al final, `./mvnw clean verify` tiene que pasar en verde.
