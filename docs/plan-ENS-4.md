# ENS-4 · Crear proyecto: foto, plantilla o en blanco

Rama: `feat/ENS-4-crear-proyecto`. Fuente: https://gonzaloleonelviale.atlassian.net/browse/ENS-4

## Plan autorizado

1. ENS-40 / T-012: CRUD privado en `/api/proyectos`, negocio resuelto desde el usuario de la sesión; 404 para recursos ajenos; borrado en cascada.
2. ENS-41 / T-013: migración V3 con Inventario, Pedidos y Clientes, conservando V1 y V2; listado público existente.
3. ENS-42 / T-014: copia transaccional de bloques y campos, con ids nuevos y sin modificar la plantilla global.
4. ENS-43 / T-015: inicio con entrada principal para fotos, creación en blanco y listado navegable de proyectos.
5. ENS-44 / T-016: catálogo con descripción, creación desde plantilla y navegación al constructor; mostrar la estructura persistida y orientar al crear en blanco.
6. Validar backend (`clean verify`, 100 % por clase), frontend (lint, tipos, tests y build) y flujo visual.

## Dependencias y límites

- T-005/T-006 (registro y login reales) siguen pendientes. Se conserva su simulación existente; en modo completo se puede probar el flujo de proyectos sin backend. El modo parcial no crea una sesión real para acceder a proyectos privados.
- Para MVP2 se resuelve el primer negocio del dueño autenticado, ordenado por creación e id. El cambio entre negocios corresponde a T-061. No se acepta negocioId desde el cliente.
- La subida efectiva de fotos corresponde a ENS-124 / T-090 y las historias de digitalización. Esta historia presenta la entrada principal y comunica su disponibilidad sin simular una subida exitosa.
- El editor de bloques y campos corresponde a T-019/T-021; esta historia permite inspeccionar los bloques y campos copiados.
- Sin commits, push ni cambios de estado en Jira hasta que se pidan.

## Implementación local · 10 de octubre de 2026

| Subtarea | Resultado |
|---|---|
| ENS-40 | CRUD privado, negocio de la sesión, recursos ajenos con 404 y cascada en PostgreSQL |
| ENS-41 | V3 con Inventario, Pedidos y Clientes; V1 y V2 intactas |
| ENS-42 | Copia transaccional con ids independientes; rollback completo ante un fallo |
| ENS-43 | Inicio con entrada para fotos, creación en blanco, listado, apertura, renombrado y confirmación de eliminación |
| ENS-44 | Catálogo, creación desde plantilla y apertura del constructor con la estructura copiada |

ENS-6 queda fuera del alcance. Los cambios están guardados en `feat/ENS-4-crear-proyecto`, sin commit ni push.
La tabla describe el código local; no modifica el estado de Jira.

## Validación

- Backend: `mvnw.cmd clean verify` completo, **196 tests**, sin fallos ni pruebas omitidas. JaCoCo verificó
  **100 % de líneas y ramas por clase**. Incluye arquitectura, HTTP, sesión, CSRF, aislamiento y PostgreSQL 17 real.
- PostgreSQL: se aplicaron V1–V3; verificados seeds, copia independiente, cascada de bloques/campos/registros
  y rollback de toda la copia si falla un campo. Docker quedó funcionando después del reinicio.
- Frontend: ESLint, TypeScript y build de producción correctos; **37 tests** en cinco suites.
  Se verifican creación, copia, renombrado, confirmación de eliminación, errores y aislamiento.
- Vitest: se compatibilizó AbortSignal de jsdom con fetch nativo de Node 24 en el setup de tests.
- Revisión visual: ingreso demo, inicio, copia de Inventario, apertura de sus campos y conservación de la
  estructura después de renombrar. La vista se adapta al ancho del navegador integrado.

## Probar localmente (PowerShell)

Backend, desde `backend`:

```powershell
docker compose up -d postgres
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
.\mvnw.cmd spring-boot:run
```

Frontend, desde `frontend`, para revisar ENS-4 mientras falta el login real:

```powershell
$env:VITE_API_MOCKS = 'completo'
npm.cmd run dev -- --host 127.0.0.1
```

Abrir http://127.0.0.1:5173/login con `demo@ensambla.local` / `EnsamblaDemo2026!`.
La cuenta existe sólo en la API simulada del navegador; sus proyectos están en memoria y se reinician al recargar.
El backend y PostgreSQL mantienen su propia persistencia, comprobada mediante los tests de integración.

Para repetir las verificaciones: `mvnw.cmd clean verify` en backend; `npm.cmd run lint`, `npm.cmd test` y
`npm.cmd run build` (incluye TypeScript) en frontend.
