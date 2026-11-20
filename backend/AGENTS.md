# Guía para agentes (Claude Code y similares) — backend de Ensambla

Antes de tocar código leé [ARQUITECTURA.md](ARQUITECTURA.md), [README.md](README.md), [TESTING.md](TESTING.md),
[../docs/modelo-datos.md](../docs/modelo-datos.md) y los skills de [`../.claude/skills`](../.claude/skills):
`reglas-arquitectura`, `codigo-semantico`, `crear-use-case`, `escribir-tests`, `refactor-use-case` (Codex los lee en
`../.agents/skills`). `codigo-semantico` es obligatorio en toda tarea del backend.

## Reglas del equipo

- Explicá el plan antes de implementar y esperá confirmación en cambios que no sean triviales.
- **Nunca edites una migración Flyway ya aplicada**: creá una `V{n}__descripcion.sql` nueva y actualizá `docs/modelo-datos.md`.
- **Aislamiento por negocio:** toda consulta de proyectos, bloques, campos o registros filtra por el `negocio_id` de la
  sesión. Jamás lo tomes del body, la URL ni un query param. Un recurso ajeno responde 404.
- Commits sólo cuando se pidan, con Conventional Commits (`tipo(scope): descripcion`).
- Nombres de dominio en español (clases, campos, métodos de servicio, enums, tablas).
- `domain` no depende de `infrastructure` ni de Spring: casos de uso, servicios y reglas se registran como `@Bean` en
  `*UseCaseConfig` y la transacción la abre el caso de uso con `TransaccionPort`.
- Las reglas de estructura de [ARQUITECTURA.md](ARQUITECTURA.md): Testcontainers (sin H2), ids tipados (`ProyectoId`…), enums junto
  a su modelo, DTOs en el subpaquete del caso de uso, sin sufijo `UseCase`, DTOs con fábrica `de()` (sin mappers de
  dominio), servicios sólo para la lógica de varios pasos que repiten dos o más casos de uso (las búsquedas de una
  línea son métodos `default` del puerto) y ningún caso de uso llama a otro.
- **Código semántico y polimórfico (obligatorio, skill `codigo-semantico`):** métodos de ≈10 líneas que se leen como una
  frase, detalle en privados con nombre de intención, sin comentarios que repitan el código, y **nunca `switch` ni
  `if/else if` por tipo o estado**: herencia (clase base abstracta + subclases). Tests con `@DisplayName` en español en todo `@Test` (frase con tildes), nombre de método en español sin tildes que se lee solo y tres bloques de una línea
  `dadoX/cuandoX/entoncesX`, sin lógica
  ni ternarios. Si tocás un archivo, lo dejás semántico.
- **Los métodos públicos no llaman a repositorios, puertos ni servicios:** `execute()` y los públicos de un servicio
  llaman a privados con nombre de intención (`obtenerCampo(...)`); el cuerpo del `@Test` no nombra repositorios, mocks, `when/verify` ni builders (fábricas de dominio en `support/`).
- **El dominio no conoce formatos de persistencia ni de transporte** (`"servidor:"`, JSON, `Map<?, ?>` con claves de
  texto): la infraestructura convierte a objetos tipados; el dominio habla de `AvisoId`, `AccionDeAviso`.
- El `Clock` sólo vive en los use cases; los servicios reciben el instante por parámetro.
- Antes de cerrar una tarea: `./mvnw clean verify` en verde (100 % de cobertura por clase).

## Mapa rápido

| Quiero… | Miro… |
|---|---|
| Un endpoint nuevo | skill `crear-use-case`, módulo `plantilla` |
| Escribir o revisar cualquier clase | skill `codigo-semantico`, sección "Calidad de código" de [ARQUITECTURA.md](ARQUITECTURA.md) |
| Cambiar el esquema | `db/migration`, `docs/modelo-datos.md` |
| Rutas públicas o reglas de acceso | `infrastructure/config/SecurityConfig` |
| Traducción de errores a HTTP | `infrastructure/presentation/exception/GlobalExceptionHandler` |
| Tareas pendientes | `../docs/backlog.md` |
