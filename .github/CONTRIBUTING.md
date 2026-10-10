# Guía de Contribución

## Ramas

```
tipo/descripcion-corta
```

| Tipo | Cuándo |
|------|--------|
| `feat` | Nueva funcionalidad |
| `fix` | Bug |
| `hotfix` | Urgencia en producción |
| `refactor` | Cambio interno sin alterar el comportamiento |
| `docs` | Solo documentación |
| `test` | Agregar o corregir tests |
| `chore` | Config, dependencias, build |

```bash
✅ feat/crud-de-proyectos
✅ fix/orden-de-campos
❌ Feat/CRUD-Proyectos      # mayúsculas
❌ feat-crud-de-proyectos   # guion en vez de /
```

Opcional pero recomendado: incluir el código de la tarea, `feat/T-012-crud-de-proyectos`.

---

## Commits

Seguimos [Conventional Commits](https://www.conventionalcommits.org/):

```
tipo(scope): descripcion
```

- `scope` es opcional: el módulo afectado (`auth`, `proyectos`, `bloques`, `campos`, `registros`, `frontend`, `backend`).
- Descripción en imperativo, en minúscula y sin punto final.
- Tipos: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `build`, `ci`.

```bash
✅ feat(proyectos): agregar crud de proyectos
✅ fix(campos): rechazar opcion sin lista de opciones
✅ test(backend): completar cobertura de usuario
❌ Agregue cosas nuevas               # sin tipo
❌ feat(proyectos): Agregar crud.     # mayúscula y punto final
```

---

## Código

**Java (backend)**
- Clases `PascalCase`, métodos y variables `camelCase`, constantes `UPPER_SNAKE_CASE`
- Clean Architecture: `domain` no depende de `infrastructure` (`ArchitectureTest` lo verifica)
- Lógica de negocio solo en `domain/service`, nunca en los controllers
- Un caso de uso por clase, con `execute()`
- Migraciones Flyway `V{n}__{descripcion}.sql`: **nunca se edita una ya aplicada**, se crea una nueva
- **Aislamiento:** el `negocio_id` sale siempre de la sesión, nunca del body ni de la URL
- Cobertura 100 % de líneas y ramas por clase (JaCoCo en `verify`)

**React (frontend)**
- Componentes `PascalCase`, hooks `useCamelCase`, todo lo demás `camelCase`
- Capas: `routes > pages > layouts > components > hooks > api/stores > types/lib` (ESLint lo verifica)
- Llamadas a la API solo desde `api/`, consumidas desde `hooks/`, nunca directo en componentes
- Un componente por archivo
- Respuestas de la API validadas con zod

---

## Flujo Git

```bash
git checkout dev && git pull
git checkout -b feat/nombre-corto
# ... commits ...
git rebase origin/dev
git push origin feat/nombre-corto
# Abrir PR → dev
```

**Ramas base:** `main` (producción) ← `stg` (staging) ← `dev` (integración) ← tus ramas

---

## Pull Requests

- Título con el mismo formato que un commit: `feat(proyectos): agregar crud de proyectos`
- Completar la plantilla de [PULL_REQUEST_TEMPLATE.md](PULL_REQUEST_TEMPLATE.md)
- Mínimo 1 aprobación, el autor no se aprueba a sí mismo
- Resolver todos los comentarios antes de mergear
- Usar **Squash and merge**; el mensaje del squash también sigue Conventional Commits
- **Branch protection (T-002):** en GitHub, activar en `dev`, `stg` y `main` "Require status checks to pass"
  con `ci-backend` y `ci-frontend`, para que un PR con lint o tests rotos no se pueda mergear.
