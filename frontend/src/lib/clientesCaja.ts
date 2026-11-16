import { allBlocks, makeBlock, upsertRow, validateRow } from './sistema/core';
import type { ErroresFila, Fila, Sistema, Valor } from './sistema/tipos';

export function clientesCaja(sistema: Sistema | undefined) {
  return allBlocks(sistema)
    .filter((tabla) => tabla.type === 'clients')
    .flatMap((tabla) =>
      tabla.rows.filter((fila) => !fila.__example).map((fila) => ({ tabla, fila })),
    );
}

export function guardarClienteCaja(
  sistema: Sistema,
  tablaId: string,
  valores: Record<string, Valor>,
  clienteId: string | null,
): { errores: ErroresFila; cliente?: Fila } {
  let tabla = allBlocks(sistema).find((item) => item.id === tablaId && item.type === 'clients');
  const nueva = !tabla;
  if (!tabla) {
    if (tablaId) return { errores: { name: 'La tabla de clientes ya no está disponible.' } };
    tabla = makeBlock('clients');
    if (!sistema.sections[0])
      return { errores: { name: 'Primero creá una sección en Mi Negocio.' } };
  }
  const datos = { ...valores, name: String(valores.name ?? '').trim() };
  const errores = validateRow(tabla, datos, sistema, clienteId);
  if (Object.keys(errores).length) return { errores };
  const resultado = upsertRow(tabla, datos, clienteId, sistema);
  if (resultado.errors) return { errores: resultado.errors };
  if (nueva) sistema.sections[0]?.blocks.push(tabla);
  return { errores, cliente: resultado.row };
}
