---
name: codigo-frontend
description: OBLIGATORIO siempre que se escriba, modifique o revise código del frontend de Ensambla (React 19, TypeScript, Vitest, carpeta frontend/src), componentes, hooks, funciones de lib y tests, sin excepción. Impone código semántico - componentes y funciones cortos (≈10 líneas, un solo nivel de abstracción), hooks con nombre de intención, JSX sin lógica inline, sin comentarios que repitan el código, cláusulas de guarda y SIEMPRE polimorfismo en lugar de switch o cadenas de if/ternarios por tipo de campo (campo.type - text, number, currency, date, time, email, tel, select, checkbox, relation, textarea) usando una clase base ComportamientoDeCampo con una subclase por tipo y un registro comportamientoDe(tipo). En tests exige it en español como frase de comportamiento, helpers dado/cuando/entonces (dadoX, cuandoX, entoncesX) compartidos en src/test, sin setTimeout ni getElementById y describe.each con una base compartida por tipo. Usar al crear, tocar o revisar cualquier archivo de frontend/src y antes de cerrar una tarea del frontend. Si tocás un archivo, lo dejás semántico.
---

> Copia para Codex. La fuente es `.claude/skills/codigo-frontend/SKILL.md`: si cambiás uno, cambiá los dos.

# Código semántico en el frontend (React/TypeScript y Vitest)

Este skill es **obligatorio, no una sugerencia**. Es el equivalente de `codigo-semantico` (backend) para `frontend/`.
Nos criticaron código espagueti, falta de polimorfismo y código que no se lee solo: ningún agente puede cerrar una
tarea del frontend con un `switch` o una cadena de ternarios por tipo, funciones largas ni tests ilegibles **en el
código que tocó**. La calidad va por encima de la velocidad.

Las convenciones de capas (`routes > pages > layouts > components > hooks > api/stores > types/lib`, ESLint), llamadas a
la API desde `api/` y zod están en [`.github/CONTRIBUTING.md`](../../../.github/CONTRIBUTING.md); este skill es cómo se
escribe cada archivo por dentro.

## Los pilares (todos obligatorios)

1. **Cortos.** Una función o un componente tiene ≈10 líneas como máximo y **un solo nivel de abstracción**. Si no
   entra, se parte **por significado**. Un componente que supera ≈10 líneas de lógica se parte en componentes y hooks.
2. **Semánticos.** Cada función o componente se lee como una frase y lo entiende alguien que no programa
   (`validarValorDelCampo`, `<TablaDeRegistros />`). El detalle va a funciones privadas del módulo con nombre de
   intención; nada de `handle`, `process`, `data`, `aux`, `doStuff`.
3. **Hooks con nombre de intención.** `useAvisosDeStock()`, no `useData()`. Un hook hace una cosa y devuelve un objeto
   con nombres de dominio; la lógica compleja va dentro del hook, no del componente. Los hooks viven en `hooks/` y
   consumen `api/` (nunca `fetch` directo en componentes).
4. **JSX sin lógica inline.** Nada de cadenas de `&&`, ternarios anidados ni `map` con cuerpo largo dentro del JSX. Se
   extraen a variables con nombre (`const hayAvisos = …`), a sub-componentes o a una función `renderX`/`componenteDe`.
   Un ternario simple de dos ramas está bien; dos ternarios anidados, no.
5. **Sin comentarios que expliquen lo que el código ya dice.** Solo el porqué no obvio. Ni comentarios de sección ni
   código comentado.
6. **Los menos condicionales posibles.** Cláusulas de guarda (`if (!campo) return null;`) al principio en lugar de
   `if` anidados; sin `else` después de `return`.
7. **Siempre polimorfismo, nunca `switch`/ternarios por tipo.** Cuando el comportamiento depende de `campo.type`, vive
   en un objeto por tipo, no en quien lo consulta.
8. **Nombres de dominio en español** para lo del negocio (`campo`, `registro`, `bloque`), igual que el backend.
9. **Las funciones y componentes públicos no llaman a `api/` ni al store directo.** Una función pública
   (`guardarRegistro`, un handler, un hook) se lee como pasos con nombre de intención; cada llamada a `api/` o al store
   va a una función con nombre de intención (`obtenerRegistro(id)`, `persistirRegistro(registro)`).
10. **La UI no conoce formatos de transporte.** Nada de armar prefijos de clave, parsear JSON ni leer `Record<string,
    unknown>` con claves de texto fuera de `api/` y del mapeo con zod: los componentes reciben objetos tipados.

## Regla de oro: si tocás un archivo, lo dejás semántico

Si abrís un archivo para modificarlo y encontrás una función larga, un `switch` por tipo, una cadena de ternarios o
un `it` ilegible, **lo dejás semántico antes de cerrar** (tests verdes, sin cambiar el comportamiento). Si excede la
tarea, lo hacés para lo que tocaste y anotás el resto en la deuda conocida; nunca lo ignorás en silencio.

## Polimorfismo por tipo de campo

Los datos son JSON plano en el store (`campo.type`: `text`, `number`, `currency`, `date`, `time`, `email`, `tel`,
`select`, `checkbox`, `relation`, `textarea`), así que se modela con una **clase base** y **una subclase por tipo**,
más un registro que las resuelve:

```
lib/sistema/comportamiento/
  ComportamientoDeCampo.ts     clase base abstracta
  campoNumero.ts, campoFecha.ts, campoMoneda.ts, …   una subclase por tipo
  index.ts                     export function comportamientoDe(tipo): ComportamientoDeCampo
```

Cada subclase define lo que cambia por tipo: `validar`, `coercionar`, `valorDeEjemplo` y `textoPlano`. Lo común queda
en la base; los tipos que solo cambian un detalle cuelgan de una intermedia.

❌ **Antes:** el tipo se ramifica en varios lugares (`core.ts` validación y coerción, `acciones.ts` `valorDeEjemplo`,
`lectura.ts` ternario de siete niveles):

```ts
function textoPlano(campo: Campo, valor: unknown): string {
  return campo.type === 'checkbox' ? (valor ? 'Sí' : 'No')
    : campo.type === 'currency' ? formatearMoneda(valor)
    : campo.type === 'date' ? formatearFecha(valor)
    : campo.type === 'select' ? etiquetaDeOpcion(campo, valor)
    : String(valor ?? '');
}
```

✅ **Después:** cada tipo sabe cómo mostrarse y quien consulta solo delega:

```ts
export abstract class ComportamientoDeCampo {
  abstract validar(valor: unknown, campo: Campo): string | null;
  abstract coercionar(valor: unknown): unknown;
  abstract valorDeEjemplo(campo: Campo): unknown;
  abstract textoPlano(valor: unknown, campo: Campo): string;
}

class CampoFecha extends ComportamientoDeCampo {
  validar(valor: unknown) {
    return esFechaIso(valor) ? null : 'Debe ser una fecha válida';
  }
  textoPlano(valor: unknown) {
    return formatearFecha(valor);
  }
}

const comportamientos: Record<TipoDeCampo, ComportamientoDeCampo> = {
  date: new CampoFecha(),
  number: new CampoNumero(),
  // …uno por tipo
};

export const comportamientoDe = (tipo: TipoDeCampo) => comportamientos[tipo];

// quien consulta
const texto = comportamientoDe(campo.type).textoPlano(valor, campo);
```

`Record<TipoDeCampo, …>` hace que agregar un tipo sin su comportamiento **no compile**: ese es el objetivo. Para otros
ejes de variación (estado de un aviso, tipo de acción) se usa el mismo patrón: una clase o un objeto por variante y un
registro, nunca un `switch`.

## Funciones públicas sin llamadas a api/store dentro

```ts
// ❌ la función pública habla con la api y el store
export async function guardarRegistro(id: string, valores: Valores) {
  const registro = await api.registros.obtener(id);
  useRegistrosStore.getState().actualizar({ ...registro, ...valores });
  await api.registros.guardar(id, valores);
}

// ✅ pasos con nombre de intención; el detalle en funciones privadas
export async function guardarRegistro(id: string, valores: Valores) {
  const registro = await obtenerRegistro(id);
  actualizarEnPantalla(registro, valores);
  await persistirRegistro(id, valores);
}

const obtenerRegistro = (id: string) => api.registros.obtener(id);
const persistirRegistro = (id: string, valores: Valores) => api.registros.guardar(id, valores);
```

## Cómo se ve una función o un componente semántico

```tsx
// ❌ lógica inline y varios niveles mezclados
return (
  <ul>
    {avisos.filter((a) => a.stock < a.minimo && !a.silenciado).map((a) => (
      <li key={a.id}>{a.stock === 0 ? 'Sin stock' : a.stock < a.minimo / 2 ? 'Crítico' : 'Bajo'}: {a.nombre}</li>
    ))}
  </ul>
);

// ✅ el componente se lee como una frase
const avisosPendientes = avisos.filter(estaPendiente);
return <ListaDeAvisos avisos={avisosPendientes} />;
```

## Anti-patrones (prohibidos)

- `switch (campo.type)` o ternarios/`if` encadenados por tipo.
- Funciones o componentes de más de ≈10 líneas de lógica o con niveles de abstracción mezclados.
- JSX con lógica inline (`&&` encadenados, ternarios anidados, `map` largo).
- Hooks con nombres genéricos (`useData`) o que hacen varias cosas no relacionadas.
- `fetch`/llamadas a la API directo en componentes.
- Comentarios que traducen el código, de sección o código comentado.
- Fragmentar por fragmentar: una función de una línea sin significado propio.
- Helpers de test redefinidos en cada archivo (van compartidos en `src/test/`).

## Tests (Vitest y Testing Library)

1. **`it` en español como frase de comportamiento:** `it('avisa cuando el stock de gaseosa baja del mínimo')`, no
   `it('works')` ni `it('test 1')`. Un comportamiento por `it`. En el frontend el título del `it('…')` cumple el rol del
   `@DisplayName` del backend (frase completa en español, con tildes y puntuación normal): es obligatorio y nunca vacío.
2. **Helpers `dado/cuando/entonces` compartidos en `src/test/`**, con prefijos en español, nombrados por intención y
   no redefinidos por archivo: `dadoUnNegocioConStockDe(...)`, `cuandoBajaElStockA(...)`,
   `entoncesApareceUnAvisoPendienteDe(...)`. Dentro del `it`, **una línea por bloque** (dado → cuando → entonces); el
   detalle va al helper. Referencia de estilo: `src/test/components/PreviewBoard.test.tsx`.
3. **Cortos:** un `it` de ≈10 líneas; si encadena acciones y tiene muchos `expect`, son tests distintos o falta un helper
   `then…` que agrupe las aserciones.
4. **Sin `setTimeout` ni `getElementById`.** Se espera con `await screen.findBy…`/`waitFor` de **un solo nivel** y se
   consulta por rol, etiqueta o texto (`getByRole`, `getByLabelText`), no por id ni por selector CSS. Nada de `waitFor`
   anidados.
5. **Sin lógica en el test:** ni `if`, ni `for` con `expect`, ni ternarios.
5b. **El `it` no arma datos ni llama a mocks inline:** nada de `vi.mock`/`mockResolvedValue`, `render(...)` con
    props armadas a mano ni `expect` sueltos; todo va a helpers `dado/cuando/entonces`:

```ts
// ❌ mecánica dentro del it
it('guarda', async () => {
  vi.mocked(api.registros.obtener).mockResolvedValue({ id: '1', nombre: 'Coca' });
  await guardarRegistro('1', { nombre: 'Sprite' });
  expect(api.registros.guardar).toHaveBeenCalledWith('1', { nombre: 'Sprite' });
});

// ✅ dado / cuando / entonces
it('guarda el registro con el nombre nuevo', async () => {
  dadoUnRegistroLlamadoCoca();

  await cuandoSeRenombraASprite();

  entoncesSeGuardaConNombreSprite();
});
```
6. **Tests por tipo con base compartida:** `describe.each` con los datos de cada tipo (`valorValido`, `valorInvalido`,
   `mensajeDeError`), y las mismas tres frases para todos (`acepta un valor válido`, `rechaza un valor inválido`…).

```ts
describe.each(casosPorTipo)('el campo $tipo', ({ tipo, valorValido, valorInvalido, mensajeDeError }) => {
  it('acepta un valor válido', () => {
    expect(comportamientoDe(tipo).validar(valorValido, campoDe(tipo))).toBeNull();
  });

  it('rechaza un valor inválido', () => {
    expect(comportamientoDe(tipo).validar(valorInvalido, campoDe(tipo))).toBe(mensajeDeError);
  });
});
```

## Checklist de revisión

- [ ] ¿Ninguna función ni componente supera ≈10 líneas ni mezcla niveles de abstracción?
- [ ] ¿Se lee como una frase (nombres de intención, hooks con nombre de intención)?
- [ ] ¿El JSX no tiene lógica inline?
- [ ] ¿Las funciones públicas llaman a `api/` y al store solo a través de funciones con nombre de intención?
- [ ] ¿La UI recibe objetos tipados (sin armar claves, parsear JSON ni leer mapas de texto)?
- [ ] ¿No hay comentarios que repitan el código?
- [ ] ¿No hay `switch` ni cadenas de ternarios/`if` por `campo.type` o estado? ¿Se usa `comportamientoDe(tipo)`?
- [ ] ¿Cada `it` es una frase en español, con helpers compartidos, sin `setTimeout`, `getElementById` ni lógica?
- [ ] ¿Lo que toqué quedó semántico (regla de oro)?
- [ ] ¿`npm run lint && npm run typecheck && npm test` en verde?

## Greps de control

Desde `frontend/`:

```bash
grep -rn "switch (" src --include=*.ts --include=*.tsx          # revisar cada resultado
grep -rnE "\.type === '" src --include=*.ts --include=*.tsx     # ramas por tipo de campo
grep -rnE "setTimeout|getElementById" src/test                  # vacío
```

Cada resultado de los dos primeros o se convierte en polimorfismo o se justifica por escrito. "Ya estaba así" no es una
justificación.

## Verificación

```bash
cd frontend && npm run lint && npm run typecheck && npm test
```

## Deuda conocida

- `lib/sistema/core.ts` (validación y coerción), `lib/sistema/acciones.ts` (`valorDeEjemplo`) y `lib/sistema/lectura.ts`
  (texto plano) ramifican por `campo.type`: se llevan a `comportamientoDe(tipo)`. Quitalos de esta lista cuando estén.
- Tests de peor lectura: `src/test/routes/avisos.test.tsx`, `src/test/lib/sistema/acciones.test.ts` (1800+ líneas) y
  `src/test/routes/editor.test.tsx`. Si tocás un `it`, lo dejás como frase con helpers compartidos.
