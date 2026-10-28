import { CATALOG, TEMPLATES } from './catalogo';
import type {
  Bloque,
  Campo,
  ClavePlantilla,
  Distribucion,
  ErroresFila,
  Fila,
  OpcionesPlantilla,
  Sistema,
  TipoBloque,
  Valor,
} from './tipos';

// Motor local de Ensambla: define bloques, campos y registros. Es independiente de la
// interfaz. Igual que en el mockup, muchas funciones modifican el sistema que reciben (los tests y la
// pantalla dependen de eso): se pasa una copia cuando hace falta ensayar.

export const clone = <T>(valor: T): T => JSON.parse(JSON.stringify(valor)) as T;

export const id = () => `e${crypto.randomUUID()}`;

export const normalize = (valor: unknown) =>
  String(valor ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim();

const MOVIMIENTOS: TipoBloque[] = ['entries', 'exits', 'sales'];
const ANCHO_MEDIO: TipoBloque[] = [
  'clients',
  'orders',
  'sales',
  'entries',
  'exits',
  'summary',
  'note',
];
const ESTADOS_CERRADOS = ['Entregado'];

export function makeBlock(type: TipoBloque, examples = false): Bloque {
  const def = CATALOG[type];
  return {
    id: id(),
    type,
    name: def.name,
    description: def.purpose,
    color: def.color,
    width: ANCHO_MEDIO.includes(type) ? 'half' : 'full',
    fields: clone(def.fields),
    rows: examples
      ? def.examples.map((fila) => ({ ...clone(fila), id: id(), __example: true }))
      : [],
    content:
      type === 'note'
        ? 'Este espacio es tuyo. Agregá instrucciones, ideas o lo que quieras tener a mano.'
        : '',
  };
}

export function makeSystem(name: string, types: TipoBloque[] = [], examples = false): Sistema {
  const blocks = types.filter((tipo) => CATALOG[tipo]).map((tipo) => makeBlock(tipo, examples));
  const client = blocks.find((bloque) => bloque.type === 'clients');
  if (client) {
    blocks
      .filter((bloque) => ['orders', 'sales'].includes(bloque.type))
      .forEach((bloque) => {
        const campo = bloque.fields.find((item) => item.key === 'customer');
        if (!campo) return;
        campo.type = 'relation';
        campo.target = client.id;
        bloque.rows.forEach((fila) => {
          fila.customer = client.rows.find((cliente) => cliente.name === fila.customer)?.id ?? '';
        });
      });
  }
  return {
    id: id(),
    name: String(name || 'Mi cuaderno')
      .trim()
      .slice(0, 100),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    sections: [{ id: id(), name: 'Vista general', blocks }],
  };
}

export const allBlocks = (sistema: Sistema | undefined): Bloque[] =>
  sistema?.sections.flatMap((seccion) => seccion.blocks) ?? [];

export const findBlock = (sistema: Sistema | undefined, bloqueId: string | undefined) =>
  allBlocks(sistema).find((bloque) => bloque.id === bloqueId);

export function moveBlock(
  sistema: Sistema,
  bloqueId: string,
  seccionDestinoId: string,
  antesDeId: string | null = null,
) {
  const origen = sistema.sections.find((seccion) =>
    seccion.blocks.some((bloque) => bloque.id === bloqueId),
  );
  const destino = sistema.sections.find((seccion) => seccion.id === seccionDestinoId);
  if (!origen || !destino || bloqueId === antesDeId) return false;
  if (antesDeId && !destino.blocks.some((bloque) => bloque.id === antesDeId)) return false;
  const [bloque] = origen.blocks.splice(
    origen.blocks.findIndex((item) => item.id === bloqueId),
    1,
  );
  if (!bloque) return false;
  const indice = antesDeId
    ? destino.blocks.findIndex((item) => item.id === antesDeId)
    : destino.blocks.length;
  destino.blocks.splice(indice, 0, bloque);
  return true;
}

export const numero = (valor: Valor) => Number(valor ?? 0) || 0;

export function validateRow(
  bloque: Bloque,
  valores: Record<string, Valor>,
  sistema: Sistema,
  filaId: string | null = null,
): ErroresFila {
  const errores: ErroresFila = {};
  for (const campo of bloque.fields) {
    const valor = valores[campo.key];
    if (
      campo.required &&
      (valor == null ||
        (typeof valor === 'string' && !valor.trim()) ||
        (campo.type === 'checkbox' && valor !== true))
    ) {
      errores[campo.key] = 'Completá este campo.';
    }
    if (valor !== '' && valor != null) {
      const texto = String(valor);
      if (['number', 'currency'].includes(campo.type) && !Number.isFinite(Number(valor))) {
        errores[campo.key] = 'Ingresá un número válido.';
      }
      if (campo.min != null && Number(valor) < campo.min) {
        errores[campo.key] = `El mínimo es ${campo.min}.`;
      }
      if (campo.type === 'time' && !/^([01]\d|2[0-3]):[0-5]\d$/.test(texto)) {
        errores[campo.key] = 'Elegí un horario válido.';
      }
      if (campo.type === 'email' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(texto)) {
        errores[campo.key] = 'Revisá el correo.';
      }
      if (
        campo.type === 'date' &&
        (!/^\d{4}-\d{2}-\d{2}$/.test(texto) || Number.isNaN(Date.parse(texto)))
      ) {
        errores[campo.key] = 'Elegí una fecha válida.';
      }
      if (campo.type === 'select' && !campo.options?.includes(texto)) {
        errores[campo.key] = 'Elegí una de las opciones.';
      }
      if (
        campo.type === 'relation' &&
        !findBlock(sistema, campo.target)?.rows.some((fila) => fila.id === valor)
      ) {
        errores[campo.key] = 'Elegí un registro del bloque vinculado.';
      }
    }
  }
  // Una venta fiada tiene que decir a quién.
  if (
    bloque.type === 'sales' &&
    valores.credit === true &&
    !valores.customer &&
    !errores.customer
  ) {
    errores.customer = 'Elegí a quién le fiás.';
  }
  if (MOVIMIENTOS.includes(bloque.type) && !Object.keys(errores).length) {
    Object.assign(
      errores,
      movementPlan(bloque, valores, filaId ?? (valores.id as string | undefined) ?? null, sistema)
        .errors,
    );
  }
  return errores;
}

// Al elegir un producto en una venta, el precio sugerido es el del inventario (como el cobro sugerido
// del mockup). Un precio escrito a mano se conserva.
export function resolveValues(bloque: Bloque, valores: Record<string, Valor>, sistema: Sistema) {
  const resultado = { ...valores };
  if (bloque.type === 'sales' && (resultado.price === '' || resultado.price == null)) {
    const campo = bloque.fields.find((item) => item.key === 'product');
    const producto = findBlock(sistema, campo?.target)?.rows.find(
      (fila) => fila.id === resultado.product,
    );
    if (producto?.price !== undefined && producto.price !== '') resultado.price = producto.price;
  }
  return resultado;
}

export type ResultadoFila = { row: Fila; errors?: never } | { errors: ErroresFila; row?: never };

export function upsertRow(
  bloque: Bloque,
  valoresIniciales: Record<string, Valor>,
  filaId: string | null = null,
  sistema: Sistema,
): ResultadoFila {
  const valores = resolveValues(bloque, valoresIniciales, sistema);
  const errores = validateRow(bloque, valores, sistema, filaId);
  if (Object.keys(errores).length) return { errors: errores };
  const indice = filaId ? bloque.rows.findIndex((fila) => fila.id === filaId) : -1;
  if (filaId && indice < 0) return { errors: { _form: 'El registro ya no existe.' } };
  const plan = movementPlan(bloque, valores, filaId, sistema);
  const fila: Fila = indice >= 0 ? { ...(bloque.rows[indice] as Fila) } : { id: id() };
  for (const campo of bloque.fields) {
    const valor = valores[campo.key];
    fila[campo.key] =
      ['number', 'currency'].includes(campo.type) && valor !== '' && valor != null
        ? Number(valor)
        : campo.type === 'checkbox'
          ? Boolean(valor)
          : (valor ?? '');
  }
  // Un ejemplo editado ya pertenece al usuario y se conserva al quitar ejemplos.
  if (indice >= 0) {
    delete fila.__example;
    bloque.rows[indice] = fila;
  } else {
    bloque.rows.push(fila);
  }
  for (const cambio of plan.changes) {
    cambio.product.quantity = numero(cambio.product.quantity) + cambio.delta;
    delete cambio.product.__example;
  }
  for (const campo of bloque.fields.filter((item) => item.type === 'relation')) {
    const vinculada = findBlock(sistema, campo.target)?.rows.find(
      (otra) => otra.id === fila[campo.key],
    );
    if (vinculada) delete vinculada.__example;
  }
  return { row: fila };
}

const claveDuplicado = (fila: Record<string, Valor>, bloque: Bloque) => {
  const primera = bloque.fields[0]?.key;
  return normalize(primera ? fila[primera] : '');
};

export function importRows(
  bloque: Bloque,
  filas: Record<string, Valor>[],
  sistema: Sistema,
  omitirDuplicados = true,
) {
  let added = 0;
  let skipped = 0;
  const errors: ErroresFila[] = [];
  for (const entrada of filas) {
    if (
      omitirDuplicados &&
      bloque.rows.some((fila) => claveDuplicado(fila, bloque) === claveDuplicado(entrada, bloque))
    ) {
      skipped++;
      continue;
    }
    const resultado = upsertRow(bloque, entrada, null, sistema);
    if (resultado.errors) {
      errors.push(resultado.errors);
      continue;
    }
    resultado.row.__source = 'Lectura simulada';
    resultado.row.__example = entrada.__example !== false;
    added++;
  }
  return { added, skipped, errors };
}

// Vincula los bloques que se conocen: clientes con pedidos y ventas, productos con movimientos y ventas.
export function wireSystem(sistema: Sistema) {
  const bloques = allBlocks(sistema);
  const clientes = bloques.find((bloque) => bloque.type === 'clients');
  const inventario = bloques.find((bloque) => bloque.type === 'inventory');
  for (const bloque of bloques) {
    if (clientes && ['orders', 'sales'].includes(bloque.type)) {
      const campo = bloque.fields.find((item) => item.key === 'customer');
      if (campo && !campo.target) {
        campo.type = 'relation';
        campo.target = clientes.id;
        for (const fila of bloque.rows) {
          fila.customer =
            clientes.rows.find(
              (cliente) => cliente.id === fila.customer || cliente.name === fila.customer,
            )?.id ?? '';
        }
      }
    }
    if (inventario && MOVIMIENTOS.includes(bloque.type)) {
      const campo = bloque.fields.find((item) => item.key === 'product');
      if (campo && !campo.target) campo.target = inventario.id;
      bloque.stockDirection = bloque.type === 'entries' ? 1 : -1;
    }
  }
  return sistema;
}

export function applyLayout(
  sistema: Sistema,
  distribucion: Distribucion = 'mosaic',
  seccionId: string | null = null,
) {
  for (const seccion of sistema.sections.filter((item) => !seccionId || item.id === seccionId)) {
    for (const bloque of seccion.blocks) {
      delete bloque.startRow;
      bloque.width =
        distribucion === 'single'
          ? 'full'
          : distribucion === 'two'
            ? 'half'
            : distribucion === 'three'
              ? 'third'
              : ['inventory', 'summary'].includes(bloque.type)
                ? 'full'
                : 'half';
    }
  }
  sistema.layout = distribucion;
  return sistema;
}

export function makeTemplate(
  clave: ClavePlantilla,
  opciones: Partial<OpcionesPlantilla> = {},
  examples = true,
): Sistema {
  const def = TEMPLATES[clave];
  const opts: OpcionesPlantilla = { movements: false, expenses: false, ...opciones };
  const types = [...def.types];
  if (opts.movements && clave === 'business') types.push('entries', 'exits');
  if (opts.movements && clave === 'shop') types.push('entries');
  if (opts.expenses) types.push('expenses');
  const sistema = makeSystem(def.defaultName, types, examples);
  sistema.template = clave;
  sistema.options = opts;
  wireSystem(sistema);
  applyLayout(sistema, 'mosaic');
  return sistema;
}

export function splitBlocks(
  sistema: Sistema,
  moviendoId: string,
  destinoId: string,
  lado: 'left' | 'right' = 'right',
) {
  if (moviendoId === destinoId) return false;
  const seccionDestino = sistema.sections.find((seccion) =>
    seccion.blocks.some((bloque) => bloque.id === destinoId),
  );
  const moviendo = findBlock(sistema, moviendoId);
  const destino = findBlock(sistema, destinoId);
  if (!seccionDestino || !moviendo || !destino) return false;
  const resto = seccionDestino.blocks.filter((bloque) => bloque.id !== moviendoId);
  const en = resto.findIndex((bloque) => bloque.id === destinoId);
  const antes = lado === 'left' ? destinoId : (resto[en + 1]?.id ?? null);
  if (!moveBlock(sistema, moviendoId, seccionDestino.id, antes)) return false;
  destino.width = 'half';
  moviendo.width = 'half';
  delete destino.startRow;
  delete moviendo.startRow;
  (lado === 'left' ? moviendo : destino).startRow = true;
  return true;
}

type CambioDeStock = { product: Fila; delta: number };

// Qué le pasa al stock con un movimiento (entrada, salida o venta) y si alcanza el stock.
export function movementPlan(
  bloque: Bloque,
  valores: Record<string, Valor>,
  filaId: string | null,
  sistema: Sistema,
  quitando = false,
) {
  const changes: CambioDeStock[] = [];
  const errors: ErroresFila = {};
  if (!MOVIMIENTOS.includes(bloque.type)) return { changes, errors };
  const campo = bloque.fields.find((item) => item.key === 'product');
  const inventario = campo && findBlock(sistema, campo.target);
  const direccion = bloque.type === 'entries' ? 1 : -1;
  if (!inventario) {
    return { changes, errors: { product: 'Elegí un bloque de productos para este movimiento.' } };
  }
  const deltas = new Map<Valor, number>();
  const anterior = filaId ? bloque.rows.find((fila) => fila.id === filaId) : undefined;
  if (anterior) deltas.set(anterior.product, -direccion * numero(anterior.quantity));
  if (!quitando) {
    deltas.set(
      valores.product,
      (deltas.get(valores.product) ?? 0) + direccion * numero(valores.quantity),
    );
  }
  for (const [productoId, delta] of deltas) {
    const producto = inventario.rows.find((fila) => fila.id === productoId);
    if (!producto) {
      errors.product = 'El producto ya no está disponible.';
      continue;
    }
    if (numero(producto.quantity) + delta < 0) {
      errors.quantity = 'No hay stock suficiente para este movimiento.';
    }
    changes.push({ product: producto, delta });
  }
  return { changes, errors };
}

export function removeRow(
  bloque: Bloque,
  filaId: string,
  sistema: Sistema,
): { removed: true; errors?: never } | { errors: ErroresFila; removed?: never } {
  const plan = movementPlan(bloque, {}, filaId, sistema, true);
  if (Object.keys(plan.errors).length) return { errors: plan.errors };
  for (const cambio of plan.changes) {
    cambio.product.quantity = numero(cambio.product.quantity) + cambio.delta;
    delete cambio.product.__example;
  }
  bloque.rows = bloque.rows.filter((fila) => fila.id !== filaId);
  return { removed: true };
}

// Quitar un bloque con movimientos revierte el stock; si algún movimiento no se puede revertir, no se quita nada.
export function removeBlock(
  sistema: Sistema,
  bloqueId: string,
): { removed: true; errors?: never } | { errors: ErroresFila; removed?: never } {
  const objetivo = findBlock(sistema, bloqueId);
  if (!objetivo) return { errors: { _form: 'El bloque ya no existe.' } };
  const ensayo = clone(sistema);
  const candidato = findBlock(ensayo, bloqueId);
  if (candidato) {
    for (const fila of [...candidato.rows]) {
      const resultado = removeRow(candidato, fila.id, ensayo);
      if (resultado.errors) return resultado;
    }
  }
  for (const fila of [...objetivo.rows]) removeRow(objetivo, fila.id, sistema);
  for (const seccion of sistema.sections) {
    seccion.blocks = seccion.blocks.filter((bloque) => bloque.id !== bloqueId);
  }
  return { removed: true };
}

const ORDEN_DE_LIMPIEZA: Partial<Record<TipoBloque, number>> = { exits: 0, sales: 0, entries: 1 };

// Quita los ejemplos. Lo que la persona ya usó (o eligió como vínculo) se conserva.
export function clearExamples(sistema: Sistema) {
  const removed: string[] = [];
  const retained: string[] = [];
  const ordenados = [...allBlocks(sistema)].sort(
    (a, b) => (ORDEN_DE_LIMPIEZA[a.type] ?? 2) - (ORDEN_DE_LIMPIEZA[b.type] ?? 2),
  );
  let cambio = true;
  while (cambio) {
    cambio = false;
    for (const bloque of ordenados) {
      for (const fila of bloque.rows.filter((item) => !item.__example)) {
        for (const campo of bloque.fields.filter((item) => item.type === 'relation')) {
          const relacionada = findBlock(sistema, campo.target)?.rows.find(
            (otra) => otra.id === fila[campo.key],
          );
          if (relacionada?.__example) {
            delete relacionada.__example;
            cambio = true;
          }
        }
      }
    }
  }
  for (const bloque of ordenados) {
    for (const fila of [...bloque.rows].filter((item) => item.__example)) {
      const resultado = removeRow(bloque, fila.id, sistema);
      if (resultado.errors) {
        delete fila.__example;
        retained.push(fila.id);
      } else {
        removed.push(fila.id);
      }
    }
  }
  return { removed, retained };
}

export function totals(sistema: Sistema) {
  const bloques = allBlocks(sistema);
  const pedidos = bloques
    .filter((bloque) => bloque.type === 'orders')
    .flatMap((bloque) => bloque.rows);
  const ventas = bloques
    .filter((bloque) => bloque.type === 'sales')
    .flatMap((bloque) => bloque.rows);
  return {
    records: bloques.reduce((suma, bloque) => suma + bloque.rows.length, 0),
    clients: bloques
      .filter((bloque) => bloque.type === 'clients')
      .reduce((suma, bloque) => suma + bloque.rows.length, 0),
    stock: bloques
      .filter((bloque) => bloque.type === 'inventory')
      .flatMap((bloque) => bloque.rows)
      .reduce((suma, fila) => suma + numero(fila.quantity), 0),
    pending: pedidos.filter((fila) => !ESTADOS_CERRADOS.includes(String(fila.status))).length,
    income:
      pedidos.reduce((suma, fila) => suma + numero(fila.total), 0) +
      ventas.reduce((suma, fila) => suma + numero(fila.quantity) * numero(fila.price), 0),
  };
}

export type { Campo };
