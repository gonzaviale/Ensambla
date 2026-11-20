import {
  AGRUPACIONES_DE_METRICA,
  CALCULOS_DE_METRICA,
  FUENTES_DE_METRICA,
  PERIODOS_DE_METRICA,
  type AgrupacionDeMetrica,
  type CalculoDeMetrica,
  type DefinicionDeMetrica,
  type FuenteDeMetrica,
  type LecturaDelPedido,
  type ParteDeLaReceta,
  type PeriodoDeMetrica,
} from '@/types/metricas';
import diccionario from './diccionario-pedidos.json';
import { motivoParaRechazar } from './recetasDeMetricas';

// El mismo intérprete que el servidor (InterpreteDePedidos), con el mismo diccionario: diccionario-pedidos.json es
// copia del de backend y un test verifica que sigan iguales. Sin IA y sin números: el número lo pone el cálculo.

export const NO_SE_ENTIENDE_LA_TABLA =
  'No me di cuenta de qué querés mirar. Decime si es de tus ventas, tus gastos o tu stock, por ejemplo: «cuánto vendí esta semana».';

type Diccionario = {
  fuentes: Partial<Record<FuenteDeMetrica, string[]>>;
  calculos: Partial<Record<CalculoDeMetrica, string[]>>;
  agrupaciones: Partial<Record<AgrupacionDeMetrica, string[]>>;
  periodos: Partial<Record<PeriodoDeMetrica, string[]>>;
  fiado: string[];
  noSeCalculan: string[];
  palabrasVacias: string[];
};

const DICCIONARIO: Diccionario = diccionario;

const LARGO_MINIMO_PARA_RECORTAR = 4;

const sinTildes = (texto: string) => texto.normalize('NFD').replace(/\p{M}/gu, '');

const sinLosUltimos = (palabra: string, cantidad: number) => palabra.slice(0, -cantidad);

function sinPlural(palabra: string) {
  if (palabra.length > LARGO_MINIMO_PARA_RECORTAR && palabra.endsWith('es'))
    return sinLosUltimos(palabra, 2);
  if (palabra.length > LARGO_MINIMO_PARA_RECORTAR - 1 && palabra.endsWith('s'))
    return sinLosUltimos(palabra, 1);
  return palabra;
}

function sinVocalFinal(palabra: string) {
  const terminaEnVocal = 'aeiou'.includes(palabra.at(-1) ?? '');
  return palabra.length > LARGO_MINIMO_PARA_RECORTAR && terminaEnVocal
    ? sinLosUltimos(palabra, 1)
    : palabra;
}

/** Igual que TextoNormalizado.palabras: sin tildes, sin signos y con el plural y la vocal final recortados. */
export const formasComparables = (texto: string) =>
  sinTildes(texto.toLowerCase())
    .replace(/[^a-z0-9]+/g, ' ')
    .trim()
    .split(' ')
    .filter((palabra) => palabra !== '')
    .map((palabra) => sinVocalFinal(sinPlural(palabra)));

type Pedido = { texto: string; escritas: string[]; palabras: string[] };

function pedidoDe(texto: string): Pedido {
  const trozos = texto.split(/[^\p{L}\p{M}\p{N}]+/u).filter((trozo) => trozo !== '');
  return {
    texto,
    escritas: trozos.flatMap((trozo) => formasComparables(trozo).map(() => trozo.toLowerCase())),
    palabras: trozos.flatMap(formasComparables),
  };
}

const frasesComparables = (frases: string[]) =>
  frases.map(formasComparables).filter((frase) => frase.length > 0);

function comienzosDe(pedido: Pedido, frase: string[]) {
  const comienzos: number[] = [];
  for (let comienzo = 0; comienzo + frase.length <= pedido.palabras.length; comienzo++)
    if (frase.every((palabra, lugar) => pedido.palabras[comienzo + lugar] === palabra))
      comienzos.push(comienzo);
  return comienzos;
}

const menciona = (pedido: Pedido, frases: string[] = []) =>
  frasesComparables(frases).some((frase) => comienzosDe(pedido, frase).length > 0);

const loPrimeroQueMenciona = <T extends string>(
  pedido: Pedido,
  opciones: readonly T[],
  frasesDe: Partial<Record<T, string[]>>,
) => opciones.find((opcion) => menciona(pedido, frasesDe[opcion]));

function posicionesDe(pedido: Pedido, frases: string[]) {
  return new Set(
    frasesComparables(frases).flatMap((frase) =>
      comienzosDe(pedido, frase).flatMap((comienzo) =>
        frase.map((_palabra, lugar) => comienzo + lugar),
      ),
    ),
  );
}

function sinLasFrases(pedido: Pedido, frases: string[]): Pedido {
  const quitadas = posicionesDe(pedido, frases);
  const queda = (_valor: string, posicion: number) => !quitadas.has(posicion);
  return {
    texto: pedido.texto,
    escritas: pedido.escritas.filter(queda),
    palabras: pedido.palabras.filter(queda),
  };
}

const loQueEscribioDe = (pedido: Pedido, frases: string[]) =>
  frasesComparables(frases).flatMap((frase) =>
    comienzosDe(pedido, frase).map((comienzo) =>
      pedido.escritas.slice(comienzo, comienzo + frase.length).join(' '),
    ),
  );

function palabrasFueraDe(pedido: Pedido, conocidas: string[]) {
  const reconocidas = posicionesDe(pedido, conocidas);
  return pedido.escritas.filter((_escrita, posicion) => !reconocidas.has(posicion));
}

const frasesConocidas = () =>
  [
    ...Object.values(DICCIONARIO.fuentes),
    ...Object.values(DICCIONARIO.calculos),
    ...Object.values(DICCIONARIO.agrupaciones),
    ...Object.values(DICCIONARIO.periodos),
    DICCIONARIO.fiado,
    DICCIONARIO.palabrasVacias,
  ].flat();

const nombreSugerido = (texto: string) => {
  const limpio = texto
    .replace(/^[\s¿¡?!.,;:]+|[\s¿¡?!.,;:]+$/g, '')
    .slice(0, 80)
    .trim();
  return limpio.charAt(0).toUpperCase() + limpio.slice(1);
};

type LoQueDice = {
  fuente: FuenteDeMetrica;
  calculo?: CalculoDeMetrica;
  agrupar?: AgrupacionDeMetrica;
  periodo?: PeriodoDeMetrica;
  soloFiado: boolean;
};

const STOCK_SIN_PERIODO: Record<FuenteDeMetrica, boolean> = {
  VENTAS: false,
  GASTOS: false,
  INVENTARIO: true,
};

// Igual que el servidor: en las ventas, «ganancia por venta» pide la ganancia, no el promedio.
const CALCULOS_QUE_SE_LEEN_PRIMERO_EN_VENTAS: readonly CalculoDeMetrica[] = ['GANANCIA', 'COSTO'];

const calculosEnOrdenDeLectura = (fuente: FuenteDeMetrica): readonly CalculoDeMetrica[] =>
  fuente === 'VENTAS'
    ? [
        ...CALCULOS_QUE_SE_LEEN_PRIMERO_EN_VENTAS,
        ...CALCULOS_DE_METRICA.filter(
          (calculo) => !CALCULOS_QUE_SE_LEEN_PRIMERO_EN_VENTAS.includes(calculo),
        ),
      ]
    : CALCULOS_DE_METRICA;

const loQueDiceDe = (pedido: Pedido, fuente: FuenteDeMetrica): LoQueDice => ({
  fuente,
  calculo: loPrimeroQueMenciona(pedido, calculosEnOrdenDeLectura(fuente), DICCIONARIO.calculos),
  agrupar: loPrimeroQueMenciona(pedido, AGRUPACIONES_DE_METRICA, DICCIONARIO.agrupaciones),
  periodo: STOCK_SIN_PERIODO[fuente]
    ? undefined
    : loPrimeroQueMenciona(pedido, PERIODOS_DE_METRICA, DICCIONARIO.periodos),
  soloFiado: menciona(pedido, DICCIONARIO.fiado),
});

// El stock es lo que hay hoy y lo que te deben se acumula: sin un período dicho, miran todo lo cargado.
const periodoDeSiempre = ({ fuente, soloFiado }: LoQueDice): PeriodoDeMetrica =>
  STOCK_SIN_PERIODO[fuente] || soloFiado ? 'TODO' : 'MES';

const recetaDe = (texto: string, loQueDice: LoQueDice): DefinicionDeMetrica => ({
  nombre: nombreSugerido(texto),
  fuente: loQueDice.fuente,
  calculo: loQueDice.calculo ?? 'SUMA',
  agrupar: loQueDice.agrupar ?? 'NINGUNA',
  periodo: loQueDice.periodo ?? periodoDeSiempre(loQueDice),
  soloFiado: loQueDice.soloFiado,
});

const LO_DIJO: Record<ParteDeLaReceta, (loQueDice: LoQueDice) => boolean> = {
  FUENTE: () => true,
  CALCULO: (loQueDice) => loQueDice.calculo !== undefined,
  AGRUPACION: (loQueDice) => loQueDice.agrupar !== undefined,
  PERIODO: (loQueDice) => loQueDice.periodo !== undefined,
  SOLO_FIADO: (loQueDice) => loQueDice.soloFiado,
};

const SE_TOMO_POR_DEFECTO: Record<ParteDeLaReceta, (loQueDice: LoQueDice) => boolean> = {
  FUENTE: () => false,
  CALCULO: (loQueDice) => !LO_DIJO.CALCULO(loQueDice),
  AGRUPACION: (loQueDice) => !LO_DIJO.AGRUPACION(loQueDice),
  PERIODO: (loQueDice) => !LO_DIJO.PERIODO(loQueDice) && !STOCK_SIN_PERIODO[loQueDice.fuente],
  SOLO_FIADO: () => false,
};

const PARTES = Object.keys(LO_DIJO) as ParteDeLaReceta[];

const noEntendidasDe = (escrito: Pedido, calculable: Pedido) => [
  ...new Set([
    ...loQueEscribioDe(escrito, DICCIONARIO.noSeCalculan),
    ...palabrasFueraDe(calculable, frasesConocidas()),
  ]),
];

const lecturaDe = (loQueDice: LoQueDice, noEntendidas: string[]): LecturaDelPedido => ({
  delPedido: PARTES.filter((parte) => LO_DIJO[parte](loQueDice)),
  porDefecto: PARTES.filter((parte) => SE_TOMO_POR_DEFECTO[parte](loQueDice)),
  noEntendidas,
});

export type PedidoEntendido = { definicion: DefinicionDeMetrica; lectura: LecturaDelPedido };

/** La receta que pide el texto con cómo se lo entendió, o el motivo por el que no se entendió. */
export function interpretarPedido(texto: string): PedidoEntendido | { motivo: string } {
  const escrito = pedidoDe(texto);
  const calculable = sinLasFrases(escrito, DICCIONARIO.noSeCalculan);
  const fuente = loPrimeroQueMenciona(calculable, FUENTES_DE_METRICA, DICCIONARIO.fuentes);
  if (!fuente) return { motivo: NO_SE_ENTIENDE_LA_TABLA };
  const loQueDice = loQueDiceDe(calculable, fuente);
  const definicion = recetaDe(texto, loQueDice);
  const rechazo = motivoParaRechazar(definicion);
  if (rechazo) return { motivo: rechazo };
  return { definicion, lectura: lecturaDe(loQueDice, noEntendidasDe(escrito, calculable)) };
}
