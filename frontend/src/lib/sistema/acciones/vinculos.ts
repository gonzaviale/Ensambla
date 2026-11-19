import { CATALOG } from '../catalogo';
import { LARGO_DE_VALOR } from '../comportamiento/ComportamientoDeCampo';
import { allBlocks, camposDeVinculo, findBlock, id, normalize } from '../core';
import type { Bloque, Campo, Fila, Sistema, TipoCampo, Valor } from '../tipos';
import { estaVacio } from './argumentos';
import { borrarElDatoDeLasFilas, datoPrincipal, esVinculo } from './piezasDelSistema';
import { sincronizarStock } from './stock';
import type { FilaPendiente, SinStock, VinculoPendiente } from './tipos';

const ES_ID_DE_FILA = /^e[0-9a-f]{8}-/;

type Pendientes = Map<string, VinculoPendiente>;

type CampoAResolver = {
  bloque: Bloque;
  campo: Campo;
  registrosNuevos: ReadonlySet<string>;
  pendientes: Pendientes;
};

type Destino = { bloque: Bloque; principal: string };

/**
 * `removeBlock` quita la pieza pero no los vínculos que apuntaban a ella. Un vínculo sin destino vuelve a ser como en
 * el catálogo (el «Cliente» de una venta, texto) y pierde los valores, que eran ids de filas que ya no están; si
 * después hay con qué, `wireSystem` lo vuelve a conectar.
 */
export function soltarVinculosSinDestino(sistema: Sistema) {
  const ids = new Set(allBlocks(sistema).map((bloque) => bloque.id));
  for (const bloque of allBlocks(sistema)) {
    for (const campo of bloque.fields.filter((item) => item.target && !ids.has(item.target))) {
      soltarElVinculo(bloque, campo);
    }
  }
}

function soltarElVinculo(bloque: Bloque, campo: Campo) {
  delete campo.target;
  campo.type = tipoDeCatalogo(bloque, campo);
  borrarElDatoDeLasFilas(bloque.rows, campo.key);
}

function tipoDeCatalogo(bloque: Bloque, campo: Campo): TipoCampo {
  const base = CATALOG[bloque.type].fields.find((item) => item.key === campo.key);
  return base && esVinculo(base) ? 'relation' : 'text';
}

// En las filas de ejemplo y en los registros que Sami acaba de cargar, un vínculo llega con el nombre del registro
// («Lucía Pérez»): se busca en la pieza a la que apunta y se guarda su id. Un registro real sólo se vincula con otro
// real (los ejemplos no se guardan). Si la pieza no lo tiene, en una fila de ejemplo se suma ahí como ejemplo (sólo
// sirve para imaginar el sistema); en un registro real NO se crea nada: el vínculo queda vacío y el nombre vuelve como
// pendiente, para que la persona confirme si lo suma (un cliente o un producto nuevo no se inventa por una foto). Si
// lo que hay es el id de un registro que ya no existe, queda vacío. Las demás filas reales no se tocan.
export function resolverVinculosPorNombre(
  sistema: Sistema,
  registrosNuevos: ReadonlySet<string>,
): VinculoPendiente[] {
  const pendientes: Pendientes = new Map();
  for (const bloque of allBlocks(sistema)) {
    for (const campo of camposDeVinculo(bloque)) {
      resolverElCampo(sistema, { bloque, campo, registrosNuevos, pendientes });
    }
  }
  return [...pendientes.values()];
}

function resolverElCampo(sistema: Sistema, aResolver: CampoAResolver) {
  const destino = findBlock(sistema, aResolver.campo.target);
  const porResolver = aResolver.bloque.rows.filter(
    (fila) => fila.__example || aResolver.registrosNuevos.has(fila.id),
  );
  for (const fila of porResolver) resolverLaFila(aResolver, fila, destino);
}

const candidatasPara = (fila: Fila, destino: Bloque | undefined) =>
  destino?.rows.filter((otra) => fila.__example || !otra.__example) ?? [];

function resolverLaFila(aResolver: CampoAResolver, fila: Fila, destino: Bloque | undefined) {
  const { campo } = aResolver;
  const valor = fila[campo.key];
  if (estaVacio(valor) || candidatasPara(fila, destino).some((otra) => otra.id === valor)) return;
  const principal = datoPrincipal(destino);
  if (!destino || !principal) {
    fila[campo.key] = '';
    return;
  }
  const nombre = String(valor).slice(0, LARGO_DE_VALOR);
  fila[campo.key] =
    vinculadaPorNombre(aResolver, fila, { bloque: destino, principal }, nombre)?.id ?? '';
}

function vinculadaPorNombre(
  aResolver: CampoAResolver,
  fila: Fila,
  destino: Destino,
  nombre: string,
): Fila | undefined {
  const encontrada = candidatasPara(fila, destino.bloque).find(
    (otra) => normalize(otra[destino.principal]) === normalize(nombre),
  );
  if (encontrada || ES_ID_DE_FILA.test(nombre)) return encontrada;
  if (fila.__example) return sumarUnEjemploLlamado(destino, nombre);
  const pendiente = {
    bloqueId: aResolver.bloque.id,
    filaId: fila.id,
    campoKey: aResolver.campo.key,
  };
  anotarPendiente(aResolver.pendientes, destino.bloque.id, nombre, pendiente);
  return undefined;
}

function sumarUnEjemploLlamado({ bloque, principal }: Destino, nombre: string): Fila {
  const ejemplo: Fila = { id: id(), [principal]: nombre, __example: true };
  bloque.rows.push(ejemplo);
  return ejemplo;
}

/** Junta por pieza destino y nombre (sin importar mayúsculas ni tildes): dos ventas de «Marta» piden una sola vez. */
function anotarPendiente(
  pendientes: Pendientes,
  destinoId: string,
  nombre: string,
  fila: FilaPendiente,
) {
  const clave = `${destinoId}:${normalize(nombre)}`;
  const pendiente = pendientes.get(clave) ?? { destinoId, nombre, filas: [] };
  pendiente.filas.push(fila);
  pendientes.set(clave, pendiente);
}

const filaQueEspera = (sistema: Sistema, fila: FilaPendiente) =>
  findBlock(sistema, fila.bloqueId)?.rows.find((item) => item.id === fila.filaId);

const sigueVacia = (sistema: Sistema, fila: FilaPendiente) => {
  const vinculada = filaQueEspera(sistema, fila);
  return Boolean(vinculada) && estaVacio(vinculada?.[fila.campoKey] as Valor);
};

/**
 * Los pendientes que siguen valiendo en `sistema`: las filas que todavía existen y tienen el vínculo vacío, y cuya
 * pieza destino sigue estando. Los grupos (los de antes y los del turno) se juntan por destino y nombre.
 */
export function pendientesVigentes(
  sistema: Sistema,
  ...grupos: VinculoPendiente[][]
): VinculoPendiente[] {
  const juntos: Pendientes = new Map();
  for (const pendiente of grupos.flat().filter((item) => findBlock(sistema, item.destinoId))) {
    for (const fila of pendiente.filas.filter((item) => sigueVacia(sistema, item))) {
      anotarPendiente(juntos, pendiente.destinoId, pendiente.nombre, fila);
    }
  }
  return [...juntos.values()];
}

/**
 * Suma (en el lugar) en cada pieza destino un registro real con el nombre pendiente y lo vincula en las filas que lo
 * nombraban. Si ya hay uno con ese nombre (se cargó a mano mientras tanto), usa ese. Las ventas que quedan vinculadas
 * a un producto descuentan su stock; devuelve a cuáles no les alcanzó.
 */
export function sumarPendientes(sistema: Sistema, pendientes: VinculoPendiente[]): SinStock[] {
  for (const pendiente of pendientesVigentes(sistema, pendientes))
    sumarElPendiente(sistema, pendiente);
  return sincronizarStock(sistema, new Set());
}

function sumarElPendiente(sistema: Sistema, pendiente: VinculoPendiente) {
  const destino = findBlock(sistema, pendiente.destinoId);
  const principal = datoPrincipal(destino);
  if (!destino || !principal) return;
  const registro = registroRealLlamado({ bloque: destino, principal }, pendiente.nombre);
  for (const fila of pendiente.filas) {
    const vinculada = filaQueEspera(sistema, fila);
    if (vinculada) vinculada[fila.campoKey] = registro.id;
  }
}

function registroRealLlamado({ bloque, principal }: Destino, nombre: string): Fila {
  const existente = bloque.rows.find(
    (fila) => !fila.__example && normalize(fila[principal]) === normalize(nombre),
  );
  if (existente) return existente;
  const nuevo: Fila = { id: id(), [principal]: nombre };
  bloque.rows.push(nuevo);
  return nuevo;
}
