# Guía para agentes (Claude Code, Codex y similares) — frontend de Ensambla

SPA con Vite, React 19, TypeScript, TanStack Router/Query/Form, Tailwind 4, zod, MSW y Vitest. Antes de tocar código
leé [README.md](README.md), [../.github/CONTRIBUTING.md](../.github/CONTRIBUTING.md) y el skill
[`codigo-frontend`](../.claude/skills/codigo-frontend/SKILL.md) (Codex lo lee en
[`../.agents/skills/codigo-frontend`](../.agents/skills/codigo-frontend/SKILL.md)).

## Reglas obligatorias

El skill `codigo-frontend` es **obligatorio**, no una sugerencia: se aplica siempre que se escriba, modifique o revise
código o tests de `src/`. Resumen:

- Funciones y componentes cortos (≈10 líneas, un solo nivel de abstracción) que se leen como una frase.
- Hooks con nombre de intención y JSX sin lógica inline.
- Sin comentarios que expliquen lo que el código ya dice (solo el porqué no obvio).
- **Nunca `switch` ni cadenas de ternarios/`if` por tipo de campo** (`campo.type`): una clase base
  `ComportamientoDeCampo` con una subclase por tipo y el registro `comportamientoDe(tipo)`.
- Tests: `it` en español como frase de comportamiento, helpers `dadoX/cuandoX/entoncesX` compartidos en `src/test/`, sin
  `setTimeout` ni `getElementById`.
- Las funciones públicas no llaman a `api/` ni al store directo: cada llamada va a una función con nombre de
  intención; la UI recibe objetos tipados (sin armar claves ni parsear JSON). En los tests, el `it` no usa mocks ni
  arma datos inline: todo en helpers `dadoX/cuandoX/entoncesX`.
- Si tocás un archivo, lo dejás semántico.
- Capas `routes > pages > layouts > components > hooks > api/stores > types/lib` (ESLint) y llamadas a la API solo desde
  `api/`, consumidas desde `hooks/`.
- Commits solo cuando se pidan, con Conventional Commits.

## Antes de cerrar una tarea

```bash
npm run lint && npm run typecheck && npm test
```
