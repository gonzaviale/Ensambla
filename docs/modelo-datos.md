# Modelo de datos (MVP 2 · T-004)

Fuente de verdad: `backend/src/main/resources/db/migration/V1__esquema_inicial.sql`. Este documento explica el
porqué; si cambian, se actualizan juntos.

## Convenciones

- Nombres en **español, `snake_case`, sin tildes**; tablas en **singular**.
- Clave primaria `id uuid DEFAULT gen_random_uuid()`. Texto siempre `text` (nunca `varchar`).
- Estados y tipos: `text` + `CHECK (... IN (...))` en **MAYÚSCULAS**, espejo de un `enum` Java con `EnumType.STRING`.
- `creado_en` y `actualizado_en` (`timestamptz`) en todas las tablas. Los completa el dominio (el use case pasa el
  `Clock`), no hay `@PrePersist`; el `DEFAULT now()` existe para seeds y scripts.
- Índices al final del script: `ux_` únicos, `ix_` comunes. Constraints con nombre: `ck_`, `fk_`, `ux_`.
- Las migraciones aplicadas **no se editan**: cada cambio es una `V{n}` nueva.

## Aislamiento entre negocios (T-010, T-011)

Todo lo que construye un usuario cuelga de un **negocio**. Para que sea imposible —aun con un bug en la aplicación—
que un registro apunte a un bloque de otro negocio, las tablas hijas llevan `negocio_id` **denormalizado** y la clave
foránea es **compuesta**: `(negocio_id, bloque_id) → bloque (negocio_id, id)`. La base rechaza cualquier cruce y
`EsquemaPostgresIntegrationTest` lo verifica. En la aplicación, el `negocio_id` sale de la sesión, nunca del request.

## Diagrama

```mermaid
erDiagram
    usuario ||--o{ negocio : "es dueño de"
    negocio ||--o{ proyecto : tiene
    plantilla ||--o{ proyecto : "origen de"
    proyecto ||--o{ bloque : contiene
    bloque ||--o{ campo : define
    bloque ||--o{ registro : almacena

    usuario {
        uuid id PK
        text email UK "normalizado: minúsculas, sin espacios"
        text nombre
        text hash_contrasenia
        text estado "ACTIVO | SUSPENDIDO"
    }
    negocio {
        uuid id PK
        text nombre
        uuid duenio_id FK
    }
    plantilla {
        uuid id PK
        text nombre UK
        text descripcion
        text icono
        jsonb estructura "bloques y campos"
        boolean activa
    }
    proyecto {
        uuid id PK
        uuid negocio_id FK
        text nombre
        text descripcion
        uuid plantilla_id FK "SET NULL"
    }
    bloque {
        uuid id PK
        uuid negocio_id FK
        uuid proyecto_id FK
        text nombre "único por proyecto"
        smallint posicion
    }
    campo {
        uuid id PK
        uuid negocio_id FK
        uuid bloque_id FK
        text nombre "único por bloque"
        text tipo "TEXTO | NUMERO | FECHA | OPCION | SI_NO"
        boolean obligatorio
        text valor_inicial
        jsonb opciones "lista; obligatoria si tipo = OPCION"
        smallint posicion
    }
    registro {
        uuid id PK
        uuid negocio_id FK
        uuid bloque_id FK
        jsonb valores "{ campo_id: valor }"
    }
```

## Tablas

| Tabla | Para qué | Notas |
|---|---|---|
| `usuario` | Cuenta de acceso | `email` único y normalizado por `CHECK`. MVP 2: un único dueño por negocio. |
| `negocio` | Espacio de trabajo y límite de aislamiento | Se crea junto con el usuario en el registro (T-005). `duenio_id` `RESTRICT`. Miembros y roles (dueño / editor / operador) llegan en MVP 4 (T-060) con una tabla nueva. |
| `plantilla` | Estructuras listas para copiar (T-013) | Global: sin `negocio_id`. `estructura` = `{ "bloques": [ { "nombre", "campos": [ { "nombre", "tipo", "obligatorio", "valorInicial", "opciones" } ] } ] }`. Crear un proyecto desde una plantilla **copia** la estructura; editar el proyecto no toca la plantilla (T-014). |
| `proyecto` | Un sistema armado por el usuario | `plantilla_id` guarda el origen y pasa a `NULL` si la plantilla se borra. Borrar un proyecto borra en cascada bloques, campos y registros (T-012). |
| `bloque` | Una "pieza": Productos, Clientes, Pedidos… | Nombre único por proyecto; `posicion` ordena la lista del constructor. |
| `campo` | Un dato de un bloque, con tipo y reglas | Tipos del MVP 2: texto, número, fecha, opción, sí/no. `ck_campo_opciones`: `opciones` es un arreglo y, para `OPCION`, no está vacío (T-018). `valor_inicial` es texto y se interpreta según el tipo. |
| `registro` | Una fila cargada en un bloque | `valores` está indexado por **id de campo**, así renombrar un campo no rompe los datos viejos. El validador dinámico (T-022) controla tipo, obligatoriedad y opciones. |
| `spring_session`, `spring_session_attributes` | Sesiones por cookie (Spring Session JDBC) | DDL oficial; Flyway es su único dueño (`initialize-schema=never`). |

## Índices relevantes

- `ix_proyecto_negocio (negocio_id, creado_en DESC)`: listado de proyectos del negocio.
- `ix_registro_bloque (negocio_id, bloque_id, creado_en DESC)`: paginación de registros (T-026).
- `ix_registro_valores` GIN `jsonb_path_ops` sobre `valores`: búsqueda y filtros por contenido (T-026, T-050).

## Qué queda para después

| Necesidad | Cuándo | Cómo se resolvería |
|---|---|---|
| Borrador vs. versión publicada de la estructura | MVP 3 · T-047 | Tablas o columnas de versión en una migración nueva |
| Columnas visibles por bloque y dispositivo | MVP 3 · T-049 | Tabla de preferencias por bloque |
| Membresías y roles, invitaciones, auditoría | MVP 4 · T-060, T-063, T-065 | `membresia`, `invitacion`, `auditoria` |
| Planes, suscripciones, límites | MVP 4 · T-066 | `plan`, `suscripcion` |
| Relación entre bloques, stock, recetas | MVP 4 · T-073 a T-075 | Nuevos tipos de campo y tablas de movimientos |
| Archivos adjuntos | MVP 4 · T-077, T-078 | Almacenamiento S3-compatible + tipo de campo `ARCHIVO` |
| Recuperación de contraseña | MVP 3 · T-039 | Tabla de tokens temporales |
