import { normalize } from '../core';
import type { Bloque, Fila, Sistema } from '../tipos';
import { aDiez, numero, palabras, parecido, pesos } from './medidas';
import { ventasDe, type Rotacion } from './rotaciones';
import type { AvisoSinGrupo } from './tipos';

const VENTAS_DE_POCA_SALIDA = 8;
const VECES_PARA_SER_COMBO = 3;
const DESCUENTO_DEL_COMBO = 0.9;

type Canastas = Map<string, Set<string>>;

/** 5. Dos productos de poca salida que se venden juntos: un combo. */
export function avisosDeCombo(
  sistema: Sistema,
  inventario: Bloque,
  porProducto: Map<string, Rotacion>,
): AvisoSinGrupo[] {
  const juntas = vecesQueSeLlevanJuntos(canastasDe(sistema), porProducto);
  return [...juntas]
    .filter(([, veces]) => veces >= VECES_PARA_SER_COMBO)
    .flatMap(([clave, veces]) => avisoDelPar(sistema, inventario, clave, veces) ?? []);
}

// Una canasta es lo que un mismo cliente se llevó el mismo día.
function canastasDe(sistema: Sistema): Canastas {
  const canastas: Canastas = new Map();
  for (const venta of ventasDe(sistema).filter(esVentaConClienteYFecha)) {
    const clave = `${String(venta.date)}|${String(venta.customer)}`;
    canastas.set(clave, (canastas.get(clave) ?? new Set<string>()).add(String(venta.product)));
  }
  return canastas;
}

const esVentaConClienteYFecha = (venta: Fila) =>
  Boolean(venta.customer && venta.date && venta.product);

function vecesQueSeLlevanJuntos(canastas: Canastas, porProducto: Map<string, Rotacion>) {
  const juntas = new Map<string, number>();
  for (const productos of canastas.values()) {
    const dePocaSalida = [...productos].filter(
      (id) => (porProducto.get(id)?.u30 ?? 0) <= VENTAS_DE_POCA_SALIDA,
    );
    for (const clave of paresDe(dePocaSalida.sort()))
      juntas.set(clave, (juntas.get(clave) ?? 0) + 1);
  }
  return juntas;
}

const paresDe = (ids: string[]) =>
  ids.flatMap((primero, indice) => ids.slice(indice + 1).map((segundo) => `${primero}|${segundo}`));

function avisoDelPar(sistema: Sistema, inventario: Bloque, clave: string, veces: number) {
  const [idA, idB] = clave.split('|');
  // El orden lo da el inventario (no el id), así el aviso se lee siempre igual.
  const [a, b] = inventario.rows.filter((fila) => fila.id === idA || fila.id === idB);
  if (!a || !b || yaHayUnCombo(inventario, String(a.name), String(b.name))) return undefined;
  return avisoDeCombo(sistema, inventario, [a, b], veces);
}

const yaHayUnCombo = (inventario: Bloque, nombreA: string, nombreB: string) =>
  inventario.rows.some(
    (fila) =>
      /combo/.test(normalize(fila.name)) &&
      parecido(nombreA, String(fila.name)) > 0 &&
      normalize(fila.name).includes(normalize(palabras(nombreB)[0] ?? '')),
  );

function avisoDeCombo(
  sistema: Sistema,
  inventario: Bloque,
  [a, b]: [Fila, Fila],
  veces: number,
): AvisoSinGrupo {
  const [nombreA, nombreB] = [String(a.name), String(b.name)];
  const porSeparado = numero(a.price) + numero(b.price);
  const precio = aDiez(porSeparado * DESCUENTO_DEL_COMBO);
  return {
    id: `combo:${a.id}:${b.id}`,
    tipo: 'combo',
    prioridad: 1,
    sistemaId: sistema.id,
    sistema: sistema.name,
    titulo: `${nombreA} y ${nombreB} se venden juntos`,
    detalle: `Los dos salen poco por separado, pero ${veces} veces se los llevó el mismo cliente el mismo día.`,
    porque: [
      `Por separado suman ${pesos(porSeparado)}; como combo, ${pesos(precio)} (10% menos).`,
      'Se crea como un producto nuevo; después cargás cuántos armás.',
    ],
    acciones: [
      {
        tipo: 'crear',
        texto: 'Crear el combo',
        sistemaId: sistema.id,
        bloqueId: inventario.id,
        valores: {
          name: `Combo ${nombreA} + ${nombreB}`,
          quantity: 0,
          price: precio,
          cost: costoDelCombo(a, b),
          minimum: '',
        },
        confirmacion: `Creamos el combo a ${pesos(precio)}.`,
      },
    ],
  };
}

const costoDelCombo = (a: Fila, b: Fila) => numero(a.cost) + numero(b.cost) || '';
