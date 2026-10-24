# Guía para agentes (Claude Code y similares) — backend de Ensambla

Antes de tocar código leé [ARQUITECTURA.md](ARQUITECTURA.md), [README.md](README.md), [TESTING.md](TESTING.md),
[../docs/modelo-datos.md](../docs/modelo-datos.md) y los skills de [`../.claude/skills`](../.claude/skills):
`reglas-arquitectura`, `crear-use-case`, `escribir-tests`, `refactor-use-case` (Codex los lee en `../.agents/skills`).

## Reglas del equipo

- Explicá el plan antes de implementar y esperá confirmación en cambios que no sean triviales.
- **Nunca edites una migración Flyway ya aplicada**: creá una `V{n}__descripcion.sql` nueva y actualizá `docs/modelo-datos.md`.
- **Aislamiento por negocio:** toda consulta de proyectos, bloques, campos o registros filtra por el `negocio_id` de la
  sesión. Jamás lo tomes del body, la URL ni un query param. Un recurso ajeno responde 404.
- Commits sólo cuando se pidan, con Conventional Commits (`tipo(scope): descripcion`).
- Nombres de dominio en español (clases, campos, métodos de servicio, enums, tablas).
- `domain` no depende de `infrastructure` ni de Spring: casos de uso, servicios y reglas se registran como `@Bean` en
  `*UseCaseConfig` y la transacción la abre el caso de uso con `TransaccionPort`.
- Las 7 reglas de [ARQUITECTURA.md](ARQUITECTURA.md): Testcontainers (sin H2), ids tipados (`ProyectoId`…), enums junto
  a su modelo, DTOs en el subpaquete del caso de uso, sin sufijo `UseCase`, DTOs con fábrica `de()` (sin mappers de
  dominio) y servicios sólo para lo que comparten dos o más casos de uso.
- El `Clock` sólo vive en los use cases; los servicios reciben el instante por parámetro.
- Antes de cerrar una tarea: `./mvnw clean verify` en verde (100 % de cobertura por clase).

## Mapa rápido

| Quiero… | Miro… |
|---|---|
| Un endpoint nuevo | skill `crear-use-case`, módulo `plantilla` |
| Cambiar el esquema | `db/migration`, `docs/modelo-datos.md` |
| Rutas públicas o reglas de acceso | `infrastructure/config/SecurityConfig` |
| Traducción de errores a HTTP | `infrastructure/presentation/exception/GlobalExceptionHandler` |
| Tareas pendientes | `../docs/backlog.md` |
