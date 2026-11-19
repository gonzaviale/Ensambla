import { CATALOG, TEMPLATES } from './catalogo';
import { comportamientoDe } from './comportamiento';
import type {
  Ancho,
  Bloque,
  Campo,
  ClavePlantilla,
  Distribucion,
  ErroresFila,
  Fila,
  OpcionesPlantilla,
  Seccion,
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
const VENDEN_A_CLIENTES: TipoBloque[] = ['orders', 'sales'];
const ANCHO_MEDIO: TipoBloque[] = [
  'clients',
  'orders',
  'sales',
  'entries',
  'exits',
  'summary',
  'note',
];
const ANCHO_COMPLETO_EN_MOSAICO: TipoBloque[] = ['inventory', 'summary'];
const ESTADOS_CERRADOS = ['Entregado'];
const SUMAN_STOCK: Partial<Record<TipoBloque, 1>> = { entries: 1 };
const CONTENIDO_INICIAL: Partial<Record<TipoBloque, string>> = {
  note: 'Este espacio es tuyo. Agregá instrucciones, ideas o lo que quieras tener a mano.',
};
const MOVIMIENTOS_DE_PLANTILLA: Partial<Record<ClavePlantilla, TipoBloque[]>> = {
  business: ['entries', 'exits'],
  shop: ['entries'],
};
const ANCHO_POR_DISTRIBUCION: Record<Distribucion, (bloque: Bloque) => Ancho> = {
  single: () => 'full',
  two: () => 'half',
  three: () => 'third',
  mosaic: (bloque) => (ANCHO_COMPLETO_EN_MOSAICO.includes(bloque.type) ? 'full' : 'half'),
};
const ORDEN_DE_LIMPIEZA: Partial<Record<TipoBloque, number>> = { exits: 0, sales: 0, entries: 1 };

export const numero = (valor: Valor) => Number(valor ?? 0) || 0;

export const allBlocks = (sistema: Sistema | undefined): Bloque[] =>
  sistema?.sections.flatMap((seccion) => seccion.blocks) ?? [];

export const findBlock = (sistema: Sistema | undefined, bloqueId: string | undefined) =>
  allBlocks(sistema).find((bloque) => bloque.id === bloqueId);

export const camposDeVinculo = (bloque: Bloque): Campo[] =>
  bloque.fields.filter((campo) => comportamientoDe(campo.type).esVinculo());

const esMovimiento = (bloque: Bloque) => MOVIMIENTOS.includes(bloque.type);

const esVenta = (bloque: Bloque) => bloque.type === 'sales';

const primeroDelTipo = (bloques: Bloque[], tipo: TipoBloque) =>
  bloques.find((bloque) => bloque.type === tipo);

const filasDelTipo = (bloques: Bloque[], tipo: TipoBloque) =>
  bloques.filter((bloque) => bloque.type === tipo).flatMap((bloque) => bloque.rows);

const campoDe = (bloque: Bloque, clave: string) =>
  bloque.fields.find((campo) => campo.key === clave);

const filaDe = (bloque: Bloque, filaId: string | null) =>
  bloque.rows.find((fila) => fila.id === filaId);

const sinErrores = (errores: ErroresFila) => !Object.keys(errores).length;

const sumar = (filas: Fila[], valorDe: (fila: Fila) => number) =>
  filas.reduce((suma, fila) => suma + valorDe(fila), 0);

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
    rows: examples ? ejemplosDe(type) : [],
    content: CONTENIDO_INICIAL[type] ?? '',
  };
}

const ejemplosDe = (tipo: TipoBloque): Fila[] =>
  CATALOG[tipo].examples.map((fila) => ({ ...clone(fila), id: id(), __example: true }));

export function makeSystem(name: string, types: TipoBloque[] = [], examples = false): Sistema {
  const blocks = types.filter((tipo) => CATALOG[tipo]).map((tipo) => makeBlock(tipo, examples));
  vincularConLosClientes(blocks);
  const ahora = new Date().toISOString();
  return {
    id: id(),
    name: nombreDeSistema(name),
    createdAt: ahora,
    updatedAt: ahora,
    sections: [{ id: id(), name: 'Vista general', blocks }],
  };
}

const nombreDeSistema = (nombre: string) =>
  String(nombre || 'Mi sección')
    .trim()
    .slice(0, 100);

function vincularConLosClientes(bloques: Bloque[]) {
  const clientes = primeroDelTipo(bloques, 'clients');
  if (!clientes) return;
  for (const bloque of queVendenAClientes(bloques)) vincularElCliente(bloque, clientes, porNombre);
}

const queVendenAClientes = (bloques: Bloque[]) =>
  bloques.filter((bloque) => VENDEN_A_CLIENTES.includes(bloque.type));

const porNombre = (cliente: Fila, valor: Valor) => cliente.name === valor;

const porIdONombre = (cliente: Fila, valor: Valor) =>
  cliente.id === valor || cliente.name === valor;

function vincularElCliente(
  bloque: Bloque,
  clientes: Bloque,
  esElCliente: (cliente: Fila, valor: Valor) => boolean,
) {
  const campo = campoDe(bloque, 'customer');
  if (!campo) return;
  campo.type = 'relation';
  campo.target = clientes.id;
  for (const fila of bloque.rows) {
    fila.customer = clientes.rows.find((cliente) => esElCliente(cliente, fila.customer))?.id ?? '';
  }
}

export function moveBlock(
  sistema: Sistema,
  bloqueId: string,
  seccionDestinoId: string,
  antesDeId: string | null = null,
) {
  const origen = seccionQueContiene(sistema, bloqueId);
  const destino = sistema.sections.find((seccion) => seccion.id === seccionDestinoId);
  if (!origen || !destino || bloqueId === antesDeId) return false;
  if (antesDeId && !contieneElBloque(destino, antesDeId)) return false;
  const bloque = sacarDeLaSeccion(origen, bloqueId);
  if (!bloque) return false;
  destino.blocks.splice(posicionParaInsertar(destino, antesDeId), 0, bloque);
  return true;
}

const contieneElBloque = (seccion: Seccion, bloqueId: string) =>
  seccion.blocks.some((bloque) => bloque.id === bloqueId);

const seccionQueContiene = (sistema: Sistema, bloqueId: string) =>
  sistema.sections.find((seccion) => contieneElBloque(seccion, bloqueId));

const sacarDeLaSeccion = (seccion: Seccion, bloqueId: string) =>
  seccion.blocks.splice(
    seccion.blocks.findIndex((bloque) => bloque.id === bloqueId),
    1,
  )[0];

const posicionParaInsertar = (seccion: Seccion, antesDeId: string | null) =>
  antesDeId ? seccion.blocks.findIndex((bloque) => bloque.id === antesDeId) : seccion.blocks.length;

function valorGuardable(campo: Campo, valor: Valor): Valor {
  return comportamientoDe(campo.type).coercionar(valor);
}

function errorDeCampo(campo: Campo, valor: Valor, sistema: Sistema): string | undefined {
  const comportamiento = comportamientoDe(campo.type);
  const hayValor = valor !== '' && valor != null;
  const errorDeValor = hayValor ? comportamiento.validar(campo, valor, sistema) : undefined;
  if (errorDeValor) return errorDeValor;
  return campo.required && comportamiento.falta(valor) ? 'Completá este dato.' : undefined;
}

export function validateRow(
  bloque: Bloque,
  valores: Record<string, Valor>,
  sistema: Sistema,
  filaId: string | null = null,
): ErroresFila {
  const errores = erroresDeLosCampos(bloque, valores, sistema);
  if (esVentaFiadaSinCliente(bloque, valores, errores)) errores.customer = 'Elegí a quién le fiás.';
  if (esMovimiento(bloque) && sinErrores(errores)) {
    Object.assign(
      errores,
      movementPlan(bloque, valores, filaIdDe(valores, filaId), sistema).errors,
    );
  }
  return errores;
}

function erroresDeLosCampos(bloque: Bloque, valores: Record<string, Valor>, sistema: Sistema) {
  const errores: ErroresFila = {};
  for (const campo of bloque.fields) {
    const error = errorDeCampo(campo, valores[campo.key], sistema);
    if (error) errores[campo.key] = error;
  }
  return errores;
}

const esVentaFiadaSinCliente = (
  bloque: Bloque,
  valores: Record<string, Valor>,
  errores: ErroresFila,
) => esVenta(bloque) && valores.credit === true && !valores.customer && !errores.customer;

const filaIdDe = (valores: Record<string, Valor>, filaId: string | null) =>
  filaId ?? (valores.id as string | undefined) ?? null;

// Al elegir un producto en una venta, el precio sugerido es el del inventario (como el cobro sugerido
// del mockup). Un precio escrito a mano se conserva.
export function resolveValues(bloque: Bloque, valores: Record<string, Valor>, sistema: Sistema) {
  const resultado = { ...valores };
  if (!esVenta(bloque) || tienePrecio(resultado)) return resultado;
  const precio = precioDelProducto(bloque, resultado.product, sistema);
  if (precio !== undefined && precio !== '') resultado.price = precio;
  return resultado;
}

const tienePrecio = (valores: Record<string, Valor>) =>
  valores.price !== '' && valores.price != null;

const precioDelProducto = (bloque: Bloque, productoId: Valor, sistema: Sistema) =>
  findBlock(sistema, campoDe(bloque, 'product')?.target)?.rows.find(
    (fila) => fila.id === productoId,
  )?.price;

export type ResultadoFila = { row: Fila; errors?: never } | { errors: ErroresFila; row?: never };

type ResultadoDeQuitar =
  { removed: true; errors?: never } | { errors: ErroresFila; removed?: never };

const esVentaDeCaja = (bloque: Bloque, filaId: string) =>
  Boolean(filaDe(bloque, filaId)?.__source?.startsWith('Caja '));

const errorDeVentaDeCaja = () => ({
  errors: { _form: 'Esta venta se confirmó en Caja y conserva su comprobante.' },
});

export function upsertRow(
  bloque: Bloque,
  valoresIniciales: Record<string, Valor>,
  filaId: string | null = null,
  sistema: Sistema,
): ResultadoFila {
  if (filaId && esVentaDeCaja(bloque, filaId)) return errorDeVentaDeCaja();
  const valores = resolveValues(bloque, valoresIniciales, sistema);
  const errores = validateRow(bloque, valores, sistema, filaId);
  if (!sinErrores(errores)) return { errors: errores };
  const indice = filaId ? bloque.rows.findIndex((fila) => fila.id === filaId) : -1;
  if (filaId && indice < 0) return { errors: { _form: 'El registro ya no existe.' } };
  const plan = movementPlan(bloque, valores, filaId, sistema);
  const fila = guardarLaFila(bloque, valores, indice);
  aplicarLosCambiosDeStock(plan.changes, true);
  adoptarLasFilasVinculadas(bloque, fila, sistema);
  return { row: fila };
}

function guardarLaFila(bloque: Bloque, valores: Record<string, Valor>, indice: number) {
  const fila: Fila = indice >= 0 ? { ...(bloque.rows[indice] as Fila) } : { id: id() };
  for (const campo of bloque.fields) fila[campo.key] = valorGuardable(campo, valores[campo.key]);
  if (indice < 0) bloque.rows.push(fila);
  else reemplazarLaFila(bloque, fila, indice);
  return fila;
}

// Un ejemplo editado ya pertenece al usuario y se conserva al quitar ejemplos.
function reemplazarLaFila(bloque: Bloque, fila: Fila, indice: number) {
  delete fila.__example;
  bloque.rows[indice] = fila;
}

function aplicarLosCambiosDeStock(cambios: CambioDeStock[], adoptaLosProductos: boolean) {
  for (const cambio of cambios) {
    cambio.product.quantity = numero(cambio.product.quantity) + cambio.delta;
    if (adoptaLosProductos) delete cambio.product.__example;
  }
}

const filaVinculada = (sistema: Sistema, campo: Campo, fila: Fila) =>
  findBlock(sistema, campo.target)?.rows.find((otra) => otra.id === fila[campo.key]);

function adoptarLasFilasVinculadas(bloque: Bloque, fila: Fila, sistema: Sistema) {
  let adopto = false;
  for (const campo of camposDeVinculo(bloque)) {
    const vinculada = filaVinculada(sistema, campo, fila);
    if (!vinculada?.__example) continue;
    delete vinculada.__example;
    adopto = true;
  }
  return adopto;
}

const claveDuplicado = (fila: Record<string, Valor>, bloque: Bloque) => {
  const primera = bloque.fields[0]?.key;
  return normalize(primera ? fila[primera] : '');
};

type ResumenDeImportacion = { added: number; skipped: number; errors: ErroresFila[] };

export function importRows(
  bloque: Bloque,
  filas: Record<string, Valor>[],
  sistema: Sistema,
  omitirDuplicados = true,
) {
  const resumen: ResumenDeImportacion = { added: 0, skipped: 0, errors: [] };
  for (const entrada of filas) importarLaFila(bloque, entrada, sistema, omitirDuplicados, resumen);
  return resumen;
}

function importarLaFila(
  bloque: Bloque,
  entrada: Record<string, Valor>,
  sistema: Sistema,
  omitirDuplicados: boolean,
  resumen: ResumenDeImportacion,
) {
  if (omitirDuplicados && yaEstaCargada(bloque, entrada)) {
    resumen.skipped++;
    return;
  }
  const resultado = upsertRow(bloque, entrada, null, sistema);
  if (resultado.errors) resumen.errors.push(resultado.errors);
  else marcarComoLeida(resultado.row, entrada, resumen);
}

const yaEstaCargada = (bloque: Bloque, entrada: Record<string, Valor>) =>
  bloque.rows.some((fila) => claveDuplicado(fila, bloque) === claveDuplicado(entrada, bloque));

function marcarComoLeida(
  fila: Fila,
  entrada: Record<string, Valor>,
  resumen: ResumenDeImportacion,
) {
  fila.__source = 'Lectura simulada';
  fila.__example = entrada.__example !== false;
  resumen.added++;
}

export function wireSystem(sistema: Sistema) {
  const bloques = allBlocks(sistema);
  vincularLosClientesSinVinculo(bloques);
  vincularLosMovimientosConElInventario(bloques);
  return sistema;
}

function vincularLosClientesSinVinculo(bloques: Bloque[]) {
  const clientes = primeroDelTipo(bloques, 'clients');
  if (!clientes) return;
  for (const bloque of queVendenAClientes(bloques)) {
    if (!campoDe(bloque, 'customer')?.target) vincularElCliente(bloque, clientes, porIdONombre);
  }
}

function vincularLosMovimientosConElInventario(bloques: Bloque[]) {
  const inventario = primeroDelTipo(bloques, 'inventory');
  if (!inventario) return;
  for (const bloque of bloques.filter(esMovimiento)) {
    const campo = campoDe(bloque, 'product');
    if (campo && !campo.target) campo.target = inventario.id;
    bloque.stockDirection = direccionDelStock(bloque);
  }
}

const direccionDelStock = (bloque: Bloque): 1 | -1 => SUMAN_STOCK[bloque.type] ?? -1;

export function applyLayout(
  sistema: Sistema,
  distribucion: Distribucion = 'mosaic',
  seccionId: string | null = null,
) {
  for (const seccion of sistema.sections.filter((item) => !seccionId || item.id === seccionId)) {
    for (const bloque of seccion.blocks) {
      delete bloque.startRow;
      bloque.width = ANCHO_POR_DISTRIBUCION[distribucion](bloque);
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
  const opts: OpcionesPlantilla = { movements: false, expenses: false, ...opciones };
  const sistema = makeSystem(
    TEMPLATES[clave].defaultName,
    tiposDeLaPlantilla(clave, opts),
    examples,
  );
  sistema.template = clave;
  sistema.options = opts;
  wireSystem(sistema);
  applyLayout(sistema, 'mosaic');
  return sistema;
}

function tiposDeLaPlantilla(clave: ClavePlantilla, opciones: OpcionesPlantilla): TipoBloque[] {
  const tipos = [...TEMPLATES[clave].types];
  if (opciones.movements) tipos.push(...(MOVIMIENTOS_DE_PLANTILLA[clave] ?? []));
  if (opciones.expenses) tipos.push('expenses');
  return tipos;
}

export function splitBlocks(
  sistema: Sistema,
  moviendoId: string,
  destinoId: string,
  lado: 'left' | 'right' = 'right',
) {
  if (moviendoId === destinoId) return false;
  const seccionDestino = seccionQueContiene(sistema, destinoId);
  const moviendo = findBlock(sistema, moviendoId);
  const destino = findBlock(sistema, destinoId);
  if (!seccionDestino || !moviendo || !destino) return false;
  const antes = lado === 'left' ? destinoId : bloqueQueSigue(seccionDestino, moviendoId, destinoId);
  if (!moveBlock(sistema, moviendoId, seccionDestino.id, antes)) return false;
  ponerLadoALado(moviendo, destino, lado);
  return true;
}

function bloqueQueSigue(seccion: Seccion, moviendoId: string, destinoId: string) {
  const resto = seccion.blocks.filter((bloque) => bloque.id !== moviendoId);
  const posicion = resto.findIndex((bloque) => bloque.id === destinoId);
  return resto[posicion + 1]?.id ?? null;
}

function ponerLadoALado(moviendo: Bloque, destino: Bloque, lado: 'left' | 'right') {
  destino.width = 'half';
  moviendo.width = 'half';
  delete destino.startRow;
  delete moviendo.startRow;
  (lado === 'left' ? moviendo : destino).startRow = true;
}

type CambioDeStock = { product: Fila; delta: number };

type PlanDeStock = { changes: CambioDeStock[]; errors: ErroresFila };

export function movementPlan(
  bloque: Bloque,
  valores: Record<string, Valor>,
  filaId: string | null,
  sistema: Sistema,
  quitando = false,
): PlanDeStock {
  if (!esMovimiento(bloque)) return { changes: [], errors: {} };
  const inventario = findBlock(sistema, campoDe(bloque, 'product')?.target);
  if (!inventario) {
    return {
      changes: [],
      errors: { product: 'Elegí un bloque de productos para este movimiento.' },
    };
  }
  return planSobreElInventario(inventario, deltasDeStock(bloque, valores, filaId, quitando));
}

function deltasDeStock(
  bloque: Bloque,
  valores: Record<string, Valor>,
  filaId: string | null,
  quitando: boolean,
) {
  const direccion = direccionDelStock(bloque);
  const deltas = new Map<Valor, number>();
  const anterior = filaId ? filaDe(bloque, filaId) : undefined;
  if (anterior) deltas.set(anterior.product, -direccion * numero(anterior.quantity));
  if (quitando) return deltas;
  const acumulado = deltas.get(valores.product) ?? 0;
  return deltas.set(valores.product, acumulado + direccion * numero(valores.quantity));
}

function planSobreElInventario(inventario: Bloque, deltas: Map<Valor, number>) {
  const plan: PlanDeStock = { changes: [], errors: {} };
  for (const [productoId, delta] of deltas) {
    sumarAlPlan(
      plan,
      inventario.rows.find((fila) => fila.id === productoId),
      delta,
    );
  }
  return plan;
}

function sumarAlPlan(plan: PlanDeStock, producto: Fila | undefined, delta: number) {
  if (!producto) {
    plan.errors.product = 'El producto ya no está disponible.';
    return;
  }
  if (numero(producto.quantity) + delta < 0) {
    plan.errors.quantity = 'No hay stock suficiente para este movimiento.';
  }
  plan.changes.push({ product: producto, delta });
}

export function removeRow(bloque: Bloque, filaId: string, sistema: Sistema): ResultadoDeQuitar {
  if (esVentaDeCaja(bloque, filaId)) return errorDeVentaDeCaja();
  const plan = movementPlan(bloque, {}, filaId, sistema, true);
  if (!sinErrores(plan.errors)) return { errors: plan.errors };
  // Quitar un movimiento de ejemplo no vuelve real al producto que tocó: sólo adopta el que usó un movimiento real.
  aplicarLosCambiosDeStock(plan.changes, !filaDe(bloque, filaId)?.__example);
  bloque.rows = bloque.rows.filter((fila) => fila.id !== filaId);
  return { removed: true };
}

// Quitar un bloque con movimientos revierte el stock; si algún movimiento no se puede revertir, no se quita nada.
export function removeBlock(sistema: Sistema, bloqueId: string): ResultadoDeQuitar {
  const objetivo = findBlock(sistema, bloqueId);
  if (!objetivo) return { errors: { _form: 'El bloque ya no existe.' } };
  const error = errorAlVaciarUnaCopia(sistema, bloqueId);
  if (error) return error;
  for (const fila of [...objetivo.rows]) removeRow(objetivo, fila.id, sistema);
  for (const seccion of sistema.sections) {
    seccion.blocks = seccion.blocks.filter((bloque) => bloque.id !== bloqueId);
  }
  return { removed: true };
}

function errorAlVaciarUnaCopia(sistema: Sistema, bloqueId: string) {
  const ensayo = clone(sistema);
  const candidato = findBlock(ensayo, bloqueId);
  return candidato && primerErrorAlVaciar(candidato, ensayo);
}

function primerErrorAlVaciar(bloque: Bloque, sistema: Sistema): ResultadoDeQuitar | undefined {
  for (const fila of [...bloque.rows]) {
    const resultado = removeRow(bloque, fila.id, sistema);
    if (resultado.errors) return resultado;
  }
  return undefined;
}

// Quita los ejemplos. Lo que la persona ya usó (o eligió como vínculo) se conserva.
export function clearExamples(sistema: Sistema) {
  const ordenados = enOrdenDeLimpieza(allBlocks(sistema));
  while (adoptarLoQueUsanLasFilasReales(ordenados, sistema));
  return quitarLosEjemplos(ordenados, sistema);
}

const enOrdenDeLimpieza = (bloques: Bloque[]) =>
  [...bloques].sort((a, b) => (ORDEN_DE_LIMPIEZA[a.type] ?? 2) - (ORDEN_DE_LIMPIEZA[b.type] ?? 2));

function adoptarLoQueUsanLasFilasReales(bloques: Bloque[], sistema: Sistema) {
  let adopto = false;
  for (const bloque of bloques) {
    for (const fila of bloque.rows.filter((item) => !item.__example)) {
      adopto = adoptarLasFilasVinculadas(bloque, fila, sistema) || adopto;
    }
  }
  return adopto;
}

type ResultadoDeLimpieza = { removed: string[]; retained: string[] };

function quitarLosEjemplos(bloques: Bloque[], sistema: Sistema) {
  const resultado: ResultadoDeLimpieza = { removed: [], retained: [] };
  for (const bloque of bloques) {
    for (const fila of bloque.rows.filter((item) => item.__example)) {
      quitarElEjemplo(bloque, fila, sistema, resultado);
    }
  }
  return resultado;
}

function quitarElEjemplo(
  bloque: Bloque,
  fila: Fila,
  sistema: Sistema,
  resultado: ResultadoDeLimpieza,
) {
  if (!removeRow(bloque, fila.id, sistema).errors) {
    resultado.removed.push(fila.id);
    return;
  }
  delete fila.__example;
  resultado.retained.push(fila.id);
}

export function totals(sistema: Sistema) {
  const bloques = allBlocks(sistema);
  const pedidos = filasDelTipo(bloques, 'orders');
  return {
    records: bloques.flatMap((bloque) => bloque.rows).length,
    clients: filasDelTipo(bloques, 'clients').length,
    stock: sumar(filasDelTipo(bloques, 'inventory'), (fila) => numero(fila.quantity)),
    pending: pedidos.filter((fila) => !ESTADOS_CERRADOS.includes(String(fila.status))).length,
    income:
      sumar(pedidos, (fila) => numero(fila.total)) +
      sumar(filasDelTipo(bloques, 'sales'), (fila) => numero(fila.quantity) * numero(fila.price)),
  };
}

export type { Campo };
