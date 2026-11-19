import { ETIQUETA_DE_AGRUPACION, ETIQUETA_DE_PERIODO } from '@/lib/metricas/metricasAMedida';
import {
  PARTES_DE_LA_RECETA,
  type CalculoDeMetrica,
  type DefinicionDeMetrica,
  type FuenteDeMetrica,
  type LecturaDelPedido,
  type ParteDeLaReceta,
} from '@/types/metricas';

/** Una línea del recuadro «Esto entendí»: «Período: este mes», con el aviso si se tomó por defecto. */
export type LineaDeLoQueEntendi = {
  parte: ParteDeLaReceta;
  titulo: string;
  valor: string;
  porDefecto: string | null;
};

type ComoSeCuenta = {
  titulo: string;
  valor: (definicion: DefinicionDeMetrica) => string;
  siNoLoDijo: string | null;
  seMuestra: (definicion: DefinicionDeMetrica) => boolean;
};

const enMinuscula = (texto: string) => texto.charAt(0).toLowerCase() + texto.slice(1);

const siempre = () => true;

const QUE_MIRA: Record<FuenteDeMetrica, string> = {
  VENTAS: 'tus ventas',
  GASTOS: 'tus gastos',
  INVENTARIO: 'tu stock',
};

const QUE_CUENTA: Record<FuenteDeMetrica, Record<CalculoDeMetrica, string>> = {
  VENTAS: {
    SUMA: 'la plata que entró',
    PROMEDIO: 'el promedio por venta',
    CONTEO: 'cuántas ventas hay',
    UNIDADES: 'las unidades vendidas',
  },
  GASTOS: {
    SUMA: 'la plata que salió',
    PROMEDIO: 'el promedio por gasto',
    CONTEO: 'cuántos gastos hay',
    UNIDADES: 'las unidades',
  },
  INVENTARIO: {
    SUMA: 'la plata parada en stock (a costo)',
    PROMEDIO: 'el promedio por producto',
    CONTEO: 'cuántos productos hay',
    UNIDADES: 'las unidades en stock',
  },
};

// El stock no tiene período: es lo que hay hoy, así que no se muestra el que trae la receta.
const PERIODO_DE: Record<FuenteDeMetrica, (definicion: DefinicionDeMetrica) => string> = {
  VENTAS: (definicion) => enMinuscula(ETIQUETA_DE_PERIODO[definicion.periodo]),
  GASTOS: (definicion) => enMinuscula(ETIQUETA_DE_PERIODO[definicion.periodo]),
  INVENTARIO: () => 'lo que tenés hoy',
};

const PARTES: Record<ParteDeLaReceta, ComoSeCuenta> = {
  FUENTE: {
    titulo: 'Mira',
    valor: (definicion) => QUE_MIRA[definicion.fuente],
    siNoLoDijo: null,
    seMuestra: siempre,
  },
  CALCULO: {
    titulo: 'Cuenta',
    valor: (definicion) => QUE_CUENTA[definicion.fuente][definicion.calculo],
    siNoLoDijo: 'no dijiste qué contar',
    seMuestra: siempre,
  },
  AGRUPACION: {
    titulo: 'Reparte',
    valor: (definicion) => enMinuscula(ETIQUETA_DE_AGRUPACION[definicion.agrupar]),
    siNoLoDijo: 'no dijiste cómo repartirlo',
    seMuestra: siempre,
  },
  PERIODO: {
    titulo: 'Período',
    valor: (definicion) => PERIODO_DE[definicion.fuente](definicion),
    siNoLoDijo: 'no dijiste cuándo',
    seMuestra: siempre,
  },
  SOLO_FIADO: {
    titulo: 'Solo lo fiado',
    valor: () => 'lo que te deben',
    siNoLoDijo: null,
    seMuestra: (definicion) => definicion.soloFiado,
  },
};

/** Lo que se entendió del pedido, parte por parte y en palabras simples. */
export function lineasDeLoQueEntendi(
  definicion: DefinicionDeMetrica,
  lectura: LecturaDelPedido,
): LineaDeLoQueEntendi[] {
  return PARTES_DE_LA_RECETA.filter((parte) => PARTES[parte].seMuestra(definicion)).map((parte) =>
    lineaDe(parte, definicion, lectura),
  );
}

function lineaDe(
  parte: ParteDeLaReceta,
  definicion: DefinicionDeMetrica,
  lectura: LecturaDelPedido,
): LineaDeLoQueEntendi {
  const { titulo, valor, siNoLoDijo } = PARTES[parte];
  const seTomoPorDefecto = lectura.porDefecto.includes(parte);
  return {
    parte,
    titulo,
    valor: valor(definicion),
    porDefecto: seTomoPorDefecto ? siNoLoDijo : null,
  };
}

/** «finde, feria»: las palabras que no se entendieron, como las escribió la persona. */
export const palabrasQueNoEntendi = (lectura: LecturaDelPedido) =>
  lectura.noEntendidas.map((palabra) => `«${palabra}»`).join(', ');
