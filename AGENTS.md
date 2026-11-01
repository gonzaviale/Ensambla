# Guía para agentes (Codex y similares) — Ensambla

Monorepo con `backend/` (Spring Boot 4, Java 21, PostgreSQL) y `frontend/` (React). Antes de tocar el backend leé
[backend/ARQUITECTURA.md](backend/ARQUITECTURA.md) y [backend/AGENTS.md](backend/AGENTS.md); para las convenciones del
equipo, [.github/CONTRIBUTING.md](.github/CONTRIBUTING.md).

## Skills

Los skills del repo están en [`.agents/skills`](.agents/skills) (copia de [`.claude/skills`](.claude/skills), que es la
fuente; si cambiás uno, cambiá los dos):

| Skill | Cuándo |
|---|---|
| `reglas-arquitectura` | al escribir, mover o revisar cualquier clase del backend |
| `crear-use-case` | un endpoint o una feature nueva del backend |
| `refactor-use-case` | ordenar un caso de uso existente o disolver un servicio o un mapper |
| `escribir-tests` | tests del backend |

## Reglas del backend (resumen de ARQUITECTURA.md)

1. Los tests con base usan Testcontainers (`PostgresRepositoryTestSupport`), nunca H2.
2. Los ids son value objects (`ProyectoId`, `NegocioId`…) en todas las capas; las entidades JPA siguen con `UUID`.
3. Los enums viven junto a su modelo (`domain/model/campo/TipoCampo`).
4. Los DTOs están en el subpaquete de su caso de uso (`domain/useCase/<area>/<accion>/`).
5. Los casos de uso no llevan el sufijo `UseCase` (`CrearProyecto`).
6. Los DTOs se mapean con su fábrica `de()`; no hay mappers de dominio.
7. En `domain/service` solo va la lógica de varios pasos que repiten dos o más casos de uso (hoy son cinco servicios);
   las búsquedas de una línea son métodos `default` del puerto y las reglas puras van al modelo. Un caso de uso nunca
   llama a otro caso de uso. El dominio no usa Spring: beans manuales en `*UseCaseConfig` y transacciones con
   `TransaccionPort`.

`ArchitectureTest` verifica las reglas que se pueden comprobar automáticamente. Siguen valiendo el aislamiento por
negocio (el `NegocioId` sale de la sesión; recurso ajeno → 404), que una migración Flyway aplicada no se edita, los
nombres de dominio en español y el 100 % de cobertura por clase: `./mvnw clean verify` en verde antes de cerrar.

## Commits

Conventional Commits en minúscula, en imperativo y sin punto final (`refactor(backend): …`), solo cuando se pidan.
