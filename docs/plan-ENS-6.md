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
4. Selección de bloque e integración del formulario en modo usar, acceso desde el constructor del proyecto,
   pruebas del flujo, revisión visual y documentación para integrar con el backend de Facundo.

Cada incremento se verifica antes del commit. Verificación final: lint, TypeScript, tests y build de frontend.
Sin cambios en Java ni migraciones. La demo usa MSW completo hasta integrar backend y login reales.

## Decisiones de interfaz e integración

- Sí/no usa un selector con tres estados: sin responder, Sí y No. Así un obligatorio no obliga a
  responder Sí, y `false` no se confunde con un valor vacío.
- Los borradores de cada bloque se mantienen montados y ocultos al cambiar de selección. El selector
  se bloquea durante el envío; los valores se restablecen únicamente después de una respuesta válida.
- El guardado pasa por `components → hooks → api`, usando el cliente HTTP y CSRF existentes. La UI
  recibe un resultado normalizado, sin importar errores ni clientes de red.
- Un bloqueo inmediato con `useRef` complementa el botón deshabilitado. No se reintenta automáticamente
  una creación ante un error de red; no se agregó idempotencia de servidor fuera del alcance asignado.
- El hook invalida `['registros', bloqueId]` para que el listado de ENS-7 pueda actualizarse al integrarlo.

## Verificación y límites

Resultado: `npm test` con 84 pruebas en 10 archivos, `npm run lint` y `npm run build` correctos
(el build incluye TypeScript). Revisión en navegador de Inventario y Pedidos: obligatorio vacío,
guardado con stock cero, fecha y No inicial; aviso de éxito y restablecimiento del formulario.
Revisión a 360 px sin desbordamiento horizontal. Capturas fuera del repositorio en el espacio de
artefactos de Codex: `ens-6-inventario.png` y `ens-6-pedidos-movil.png`.

Las pruebas cubren los cinco controles, orden, valores iniciales, obligatorios, límites de texto,
números finitos, fechas reales, opciones, cero/falso, errores por UUID del servidor, conservación de
datos, bloqueo de envíos inmediatos, cambio de bloque y acceso con sesión/propiedad.

Las pruebas de API y navegador usan MSW completo. La persistencia real y la validación Java pertenecen
a ENS-50/51; falta comprobar la integración contra PostgreSQL después de incorporar ese backend y la
autenticación real en `dev`. No se modificaron sus archivos ni se fusionó la rama de Facundo.
