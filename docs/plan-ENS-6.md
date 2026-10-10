# ENS-6 · Formulario de carga generado automáticamente

Rama: `codex/ens-6-formularios`, creada desde `origin/dev`.

## Alcance autorizado

- ENS-52 / T-024: FormRenderer con texto, número, fecha, opción y sí/no; valores iniciales y orden de campos.
- ENS-53 / T-025: validación en cliente, errores del servidor por campo, avisos de éxito/error, bloqueo de doble envío
  y conservación de lo escrito cuando falla el guardado.
- ENS-50 / T-022 y ENS-51 / T-023 están asignadas a Facundo Palleros. No modificar su backend.
- Commits locales chicos y completos, con pruebas por incremento. El usuario hará el push y el PR.

## Contrato de integración

Fuente: comentario de Facundo en ENS-51 y código de `origin/feat/ens-7-registros-backend`.

- `POST /api/bloques/{bloqueId}/registros`, body `{ "valores": { "<idCampo>": valor } }`.
- Los valores son texto, número o booleano JSON; las fechas usan `YYYY-MM-DD`.
- Respuesta 201: `id`, `bloqueId`, `valores`, `creadoEn`, `actualizadoEn`.
- Error 400 estándar con `errors: { "<idCampo>": "mensaje" }`; bloque ajeno: 404; sesión y CSRF obligatorios.
- Texto hasta 1000 caracteres y sin espacios en los bordes; números finitos; fechas reales; opción perteneciente
  a la lista. Cero y `false` son valores válidos, incluso para campos obligatorios.
- Los campos vienen del detalle de proyecto existente. No implementar el editor de ENS-49 / T-021 ni la tabla
  de registros de ENS-56 / T-028. El modo usar sólo incorporará la carga correspondiente a estas subtareas.

## Incrementos y commits

1. FormRenderer reutilizable con cinco controles, valores iniciales, orden y serialización; pruebas de componente.
2. Validación de registros y errores accesibles junto al campo; pruebas de casos inválidos.
3. Cliente API y hook de guardado, simulación del contrato, avisos y bloqueo inmediato de envíos repetidos.
4. Selección de bloque e integración del formulario en modo usar, enlaces desde constructor/proyectos,
   pruebas del flujo, revisión visual y documentación para integrar con el backend de Facundo.

Cada incremento se verifica antes del commit. Verificación final: lint, TypeScript, tests y build de frontend.
Sin cambios en Java ni migraciones. La demo usa MSW completo hasta integrar backend y login reales.
