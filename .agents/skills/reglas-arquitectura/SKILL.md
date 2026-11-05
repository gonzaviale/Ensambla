---
name: reglas-arquitectura
description: Checklist de las reglas de estructura del backend de Ensambla (backend/ARQUITECTURA.md) para aplicar al escribir o revisar cualquier código Java del backend - tests con Testcontainers y nunca H2, ids tipados (ProyectoId, NegocioId…) en lugar de UUID, enums junto a su modelo, DTOs en el subpaquete de su caso de uso, casos de uso sin sufijo UseCase, DTOs con fábrica de() y sin mappers de dominio, servicios solo para lo compartido y dominio sin Spring (beans manuales, TransaccionPort), variantes por tipo con polimorfismo (interfaz o clase base abstracta, nunca switch ni if por tipo) y métodos que se explican solos (execute() como lista de pasos, todo el detalle en métodos privados). Usar siempre que se cree, mueva, renombre o revise una clase de backend/src, cuando se pregunte "dónde va esto", "¿hace falta un servicio/mapper?", "¿cómo tipo este id?", o antes de cerrar una tarea o un PR del backend. Para crear una feature completa usar crear-use-case; para reordenar un caso de uso existente, refactor-use-case; para tests, escribir-tests.
---

> Copia para Codex. La fuente es `.claude/skills/reglas-arquitectura/SKILL.md`: si cambiás uno, cambiá los dos.

# Reglas de arquitectura del backend (Ensambla)

La fuente es [`backend/ARQUITECTURA.md`](../../../backend/ARQUITECTURA.md), con el porqué y los ejemplos de cada regla.
Este skill es el checklist para aplicarlas. Si una regla choca con lo que pide la tarea, decilo antes de romperla.

## Las 9 reglas

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
7. **Servicio solo para lógica compartida; dominio sin Spring; ningún caso de uso llama a otro.**
   - Traer una entidad o responder 404 va como método `default` del puerto (`negocioRepository.obtenerIdDelUsuario`), con
     su test. Una llamada que sólo pasa al puerto se hace directo desde el caso de uso. Una regla pura va al modelo.
   - Un servicio de `domain/service/<area>` existe sólo si su lógica es de varios pasos y la repiten **dos o más** casos de
     uso (comprobalo con grep). Hoy son cinco: `ControlPlanService`, `RenovacionPremiumService`, `RegistroDatosService`,
     `ReporteService` y `RecetaService`. Si la usa uno solo, es un método privado de ese caso de uso.
   - Un caso de uso nunca llama a otro caso de uso (`ArchitectureTest` lo verifica).
   - En `domain` no hay `@Service`, `@Component`, `@Transactional` ni imports de `org.springframework`. Casos de uso,
     servicios y reglas se registran a mano como `@Bean` en `infrastructure/config/useCase/<Area>UseCaseConfig`, con
     los argumentos en el orden de los campos.
   - La transacción la abre el caso de uso con `TransaccionPort` (`transaccionPort.ejecutar(() -> …)`); en los tests
     unitarios se pasa `new TransaccionDirecta()`.
8. **Variantes con polimorfismo, no con `switch`.** Si el comportamiento cambia según un tipo (campo, aviso, canal,
   comando, formato), el tipo es una **interfaz con una clase por variante**; si comparten el flujo y solo cambia un paso,
   una **clase base abstracta con método plantilla**. El enum solo identifica a la variante: el comportamiento se le pide
   a la clase, no se pregunta con `switch (tipo)`. Las variantes se registran como `@Bean` y quien las usa recibe la
   `List<Interfaz>` o un catálogo (modelo: `ReglaAviso` y sus reglas). Una cadena de `if` por tipo (`tipo == …`,
   `instanceof`) vale lo mismo que un `switch`; pasar un valor a otro se hace con un `Map` o un método del enum. Sus tests
   también: contrato abstracto heredado por un test por variante. `ArchitectureTest` falla ante un `switch` que no esté en
   la «Deuda conocida» de `ARQUITECTURA.md` (lo viejo que se acomodará después).
9. **Métodos que se explican solos.** El método público (`execute()`) es una lista corta de pasos con nombres que se
   leen como una frase; **todo** el detalle va en métodos privados con nombre de dominio, cada uno en un solo nivel de
   abstracción. Sin bloques inline, sin `if` anidados (guardas, predicados con nombre o polimorfismo), sin números ni
   textos mágicos (constantes con nombre), sin comentarios que expliquen lo que el nombre debería decir.

Siguen valiendo: el `NegocioId` sale de la sesión (recurso ajeno → 404), solo los casos de uso tienen `Clock`, una
migración aplicada no se edita, nombres de dominio en español y 100 % de cobertura por clase.

## Dónde va cada cosa

| Tengo… | Va en… |
|---|---|
| Una regla sin I/O | el modelo (`domain/model/<area>`) |
| Comportamiento que cambia según un tipo | una interfaz y **una clase por variante** (o base abstracta con método plantilla); nunca un `switch` |
| Una variante nueva | una clase nueva y su `@Bean` en el `*UseCaseConfig`; no se edita a quien las usa |
| El detalle de un paso de `execute()` | un método privado con nombre de dominio |
| Traer o responder 404 (`find…orElseThrow`) | método `default` del puerto, con su test |
| Lógica con I/O que usa un solo caso de uso | método privado del caso de uso |
| Lógica de varios pasos que repiten dos o más casos de uso | `domain/service/<area>/<Area>Service`, bean en el `*UseCaseConfig` |
| Entrada / salida de un caso de uso | records en `domain/useCase/<area>/<accion>/` |
| Un id nuevo | `domain/model/<area>/<Modelo>Id` |
| Un enum | el paquete de su modelo |
| Un proveedor externo o la transacción | un puerto en `domain/port`, su adaptador en `infrastructure` |

## Verificación

`ArchitectureTest` controla las reglas 1, 2, 3, 4, 5, 6, de la 7 que el dominio no use Spring y que un caso de uso no
dependa de otro, y de la 8 que no haya `switch` fuera de la «Deuda conocida». Que un servicio sea realmente compartido,
que cada variante tenga su test de contrato y la regla 9 (`execute()` como lista de pasos, detalle en privados) no se
pueden verificar automáticamente: revisalos a mano. Desde `backend/`, estos controles deberían
salir vacíos (el del `switch` solo debe listar los archivos de la «Deuda conocida»):

```bash
grep -rl "org.springframework" src/main/java/com/ensambla/backend/domain
find src/main/java/com/ensambla/backend/domain -name "*UseCase.java"
ls -d src/main/java/com/ensambla/backend/domain/{dto,mapper,model/enums} 2>/dev/null
grep -rlw "UUID" src/main/java/com/ensambla/backend/domain | grep -v "Id.java$\|Identificador\|IdSistema\|Huella"
grep -rn "h2-test\|h2database" pom.xml src/test
grep -rlE "\bswitch\s*\(" src/main/java
```

Para saber cuántos casos de uso usan un método de servicio:

```bash
grep -rln "nombreDelServicio.metodo(" src/main/java/com/ensambla/backend/domain/useCase
```

Al final, `./mvnw clean verify` tiene que pasar en verde.
