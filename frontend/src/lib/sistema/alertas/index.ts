import { allBlocks } from '../core';
import type { Bloque, Sistema } from '../tipos';
import type { Zona } from '../zona';
import { avisosDeCombo } from './combos';
import { avisosDeConsumoDeZona } from './consumoDeZona';
import {
  analizarElProducto,
  nombreDelProducto,
  type ContextoDelSistema,
  type ProductoAnalizado,
} from './producto';
import { avisoDeStock } from './reglaDeStock';
import { avisosDePrecio } from './reglasDePrecio';
import { rotaciones } from './rotaciones';
import { GRUPO_DE, ORDEN, type Aviso, type AvisoSinGrupo } from './tipos';

// Los avisos de Sami: reglas transparentes sobre los datos del comercio y sobre los datos agregados y
// anónimos de la zona. Cada aviso dice por qué aparece y de dónde sale cada número; nada se inventa ni
// se envía. El motor es puro: la interfaz y las acciones viven en otro lado.

export {
  GRUPOS,
  type AccionDeAviso,
  type Aviso,
  type GrupoDeAviso,
  type TipoDeAviso,
} from './tipos';
export { palabras, parecido, pesos, productoDeZona } from './medidas';
export { rotaciones } from './rotaciones';
export { responder, type Respuesta } from './preguntas';

const SIN_DATOS_DE_ZONA: Zona = { nombre: '', comercios: 0, productos: [] };

type Entrada = { sistemas?: Sistema[]; zona?: Zona; hoy: string; grupos?: Record<string, boolean> };

type Recolector = { avisos: Aviso[]; sumar: (aviso: AvisoSinGrupo) => void };

export function analizar({ sistemas = [], zona, hoy, grupos = {} }: Entrada): Aviso[] {
  const recolector = nuevoRecolector(grupos);
  for (const sistema of sistemas) {
    avisosDelSistema(sistema, zona ?? SIN_DATOS_DE_ZONA, hoy).forEach(recolector.sumar);
  }
  return recolector.avisos.sort(porUrgencia);
}

/** Junta los avisos sin repetir y deja afuera los de los grupos que la persona apagó. */
function nuevoRecolector(grupos: Record<string, boolean>): Recolector {
  const avisos: Aviso[] = [];
  const vistos = new Set<string>();
  const sumar = (aviso: AvisoSinGrupo) => {
    const grupo = GRUPO_DE[aviso.tipo];
    if (vistos.has(aviso.id) || grupos[grupo] === false) return;
    vistos.add(aviso.id);
    avisos.push({ ...aviso, grupo });
  };
  return { avisos, sumar };
}

const porUrgencia = (a: Aviso, b: Aviso) =>
  b.prioridad - a.prioridad || ORDEN.indexOf(a.tipo) - ORDEN.indexOf(b.tipo);

function avisosDelSistema(sistema: Sistema, zona: Zona, hoy: string): AvisoSinGrupo[] {
  const bloques = allBlocks(sistema);
  const inventarios = bloques.filter((bloque) => bloque.type === 'inventory');
  const contexto: ContextoDelSistema = {
    ...rotaciones(sistema, hoy),
    sistema,
    zona,
    entradas: bloques.find((bloque) => bloque.type === 'entries'),
  };
  return [
    ...inventarios.flatMap((inventario) => avisosDelInventario(contexto, inventario)),
    ...avisosDeConsumoDeZona(sistema, inventarios, zona),
  ];
}

function avisosDelInventario(contexto: ContextoDelSistema, inventario: Bloque): AvisoSinGrupo[] {
  const productos = inventario.rows
    .filter((fila) => nombreDelProducto(fila))
    .map((fila) => analizarElProducto(contexto, inventario, fila));
  return [
    ...productos.flatMap(avisosDelProducto),
    ...avisosDeCombo(contexto.sistema, inventario, contexto.porProducto),
  ];
}

const avisosDelProducto = (producto: ProductoAnalizado): AvisoSinGrupo[] =>
  [avisoDeStock(producto), ...avisosDePrecio(producto)].filter(
    (aviso): aviso is AvisoSinGrupo => aviso !== undefined,
  );
