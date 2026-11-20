import { formatoPesos } from '@/lib/sistema/formato';
import type {
  AgrupacionDeMetrica,
  CalculoDeMetrica,
  DefinicionDeMetrica,
  FuenteDeMetrica,
  PeriodoDeMetrica,
  ResultadoDeMetrica,
} from '@/types/metricas';

// Palabras de la persona para armar y leer una métrica. Las combinaciones que valen son las mismas que valida el
// servidor (DefinicionMetrica.validar): acá sólo se ofrecen las posibles para no mandar una que va a rechazar.

export const ETIQUETA_DE_FUENTE: Record<FuenteDeMetrica, string> = {
  VENTAS: 'Ventas',
  GASTOS: 'Gastos',
  INVENTARIO: 'Productos en stock',
};

const NOMBRE_DE_LAS_FILAS: Record<
  FuenteDeMetrica,
  { uno: string; varios: string; cuantos: string }
> = {
  VENTAS: { uno: 'venta', varios: 'ventas', cuantos: 'Cuántas ventas' },
  GASTOS: { uno: 'gasto', varios: 'gastos', cuantos: 'Cuántos gastos' },
  INVENTARIO: { uno: 'producto', varios: 'productos', cuantos: 'Cuántos productos' },
};

const TABLA_DE: Record<FuenteDeMetrica, string> = {
  VENTAS: 'tus ventas',
  GASTOS: 'tus gastos',
  INVENTARIO: 'tu stock',
};

export const ETIQUETA_DE_AGRUPACION: Record<AgrupacionDeMetrica, string> = {
  NINGUNA: 'Todo junto',
  PRODUCTO: 'Por producto',
  CLIENTE: 'Por cliente',
  CATEGORIA: 'Por categoría',
  DIA_DE_LA_SEMANA: 'Por día de la semana',
};

export const ETIQUETA_DE_PERIODO: Record<PeriodoDeMetrica, string> = {
  HOY: 'Hoy',
  SEMANA: 'Esta semana',
  MES: 'Este mes',
  TODO: 'Todo lo cargado',
};

export function etiquetaDeCalculo(fuente: FuenteDeMetrica, calculo: CalculoDeMetrica) {
  const filas = NOMBRE_DE_LAS_FILAS[fuente].varios;
  switch (calculo) {
    case 'SUMA':
      return fuente === 'INVENTARIO' ? 'Valor del stock (a costo)' : `Total de ${filas}`;
    case 'PROMEDIO':
      return `Promedio por ${NOMBRE_DE_LAS_FILAS[fuente].uno}`;
    case 'CONTEO':
      return `${NOMBRE_DE_LAS_FILAS[fuente].cuantos} hay`;
    case 'UNIDADES':
      return fuente === 'INVENTARIO' ? 'Unidades en stock' : 'Unidades vendidas';
    case 'GANANCIA':
      return 'Ganancia';
    case 'COSTO':
      return 'Costo de lo que vendiste';
  }
}

const CALCULOS_DE: Record<FuenteDeMetrica, CalculoDeMetrica[]> = {
  VENTAS: ['SUMA', 'PROMEDIO', 'CONTEO', 'UNIDADES', 'GANANCIA', 'COSTO'],
  GASTOS: ['SUMA', 'PROMEDIO', 'CONTEO'],
  INVENTARIO: ['SUMA', 'PROMEDIO', 'CONTEO', 'UNIDADES'],
};

export const calculosDe = (fuente: FuenteDeMetrica): CalculoDeMetrica[] => CALCULOS_DE[fuente];

export const agrupacionesDe = (fuente: FuenteDeMetrica): AgrupacionDeMetrica[] => {
  if (fuente === 'VENTAS') return ['NINGUNA', 'PRODUCTO', 'CLIENTE', 'DIA_DE_LA_SEMANA'];
  if (fuente === 'GASTOS') return ['NINGUNA', 'CATEGORIA', 'DIA_DE_LA_SEMANA'];
  return ['NINGUNA', 'PRODUCTO'];
};

// Contar productos repartidos por producto daría siempre 1: el stock tiene una fila por producto.
export const cuentaCadaProducto = ({ fuente, calculo, agrupar }: DefinicionDeMetrica) =>
  fuente === 'INVENTARIO' && calculo === 'CONTEO' && agrupar === 'PRODUCTO';

/** Las maneras de repartir que tienen sentido para la receta elegida hasta ahora. */
export const agrupacionesPara = (definicion: DefinicionDeMetrica): AgrupacionDeMetrica[] =>
  agrupacionesDe(definicion.fuente).filter(
    (agrupar) => !cuentaCadaProducto({ ...definicion, agrupar }),
  );

export const periodosDe = (fuente: FuenteDeMetrica): PeriodoDeMetrica[] =>
  fuente === 'INVENTARIO' ? ['TODO'] : ['HOY', 'SEMANA', 'MES', 'TODO'];

export const permiteFiado = (fuente: FuenteDeMetrica) => fuente === 'VENTAS';

export const definicionInicial = (): DefinicionDeMetrica => ({
  nombre: '',
  fuente: 'VENTAS',
  calculo: 'SUMA',
  agrupar: 'NINGUNA',
  periodo: 'MES',
  soloFiado: false,
});

/** Al cambiar la tabla, lo que ya no vale para ella vuelve a un valor posible; lo que sigue valiendo se conserva. */
export function ajustarALaFuente(
  definicion: DefinicionDeMetrica,
  fuente: FuenteDeMetrica,
): DefinicionDeMetrica {
  const periodos = periodosDe(fuente);
  return {
    ...definicion,
    fuente,
    calculo: calculosDe(fuente).includes(definicion.calculo) ? definicion.calculo : 'SUMA',
    agrupar: agrupacionesDe(fuente).includes(definicion.agrupar) ? definicion.agrupar : 'NINGUNA',
    periodo: periodos.includes(definicion.periodo)
      ? definicion.periodo
      : (periodos.at(-1) ?? 'TODO'),
    soloFiado: permiteFiado(fuente) && definicion.soloFiado,
  };
}

const numeros = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 });

/** El número como lo lee la persona: en pesos o como cantidad. */
export const formatoDelValor = (valor: number, formato: ResultadoDeMetrica['formato']) =>
  formato === 'MONEDA' ? formatoPesos(valor) : numeros.format(valor);

const fechaCorta = (iso: string) => {
  const [, mes = '', dia = ''] = iso.split('-');
  return `${Number(dia)}/${Number(mes)}`;
};

const cantidadDe = (fuente: FuenteDeMetrica, cantidad: number) => {
  const nombre = NOMBRE_DE_LAS_FILAS[fuente];
  return `${cantidad} ${cantidad === 1 ? nombre.uno : nombre.varios}`;
};

/** «De dónde sale» el número, en palabras: la tabla, el período, cuántas filas entraron y cuántas quedaron afuera. */
export function textoDeDondeSale({ deDondeSale }: ResultadoDeMetrica) {
  const { fuente, filasUsadas, filasSinDato } = deDondeSale;
  const partes = [`Sale de ${TABLA_DE[fuente]}${periodoMirado(deDondeSale)}.`];
  if (filasUsadas > 0)
    partes.push(
      `${filasUsadas === 1 ? 'Se usó' : 'Se usaron'} ${cantidadDe(fuente, filasUsadas)}.`,
    );
  if (filasSinDato > 0)
    partes.push(`${cantidadDe(fuente, filasSinDato)} ${lasQueQuedaronAfuera(filasSinDato)}.`);
  return partes.join(' ');
}

function periodoMirado({ desde, hasta }: ResultadoDeMetrica['deDondeSale']) {
  if (!desde || !hasta) return ', con todo lo cargado';
  if (desde === hasta) return ` de hoy (${fechaCorta(hasta)})`;
  return `, del ${fechaCorta(desde)} al ${fechaCorta(hasta)}`;
}

const lasQueQuedaronAfuera = (cantidad: number) =>
  cantidad === 1
    ? 'quedó afuera porque le falta un dato'
    : 'quedaron afuera porque les falta un dato';

/** La receta en una frase corta, para la lista de sugeridas: «Total de ventas · Por producto · Este mes». */
export function resumenDeLaReceta(definicion: DefinicionDeMetrica) {
  return [
    definicion.soloFiado
      ? `${etiquetaDeCalculo(definicion.fuente, definicion.calculo)} (fiado)`
      : etiquetaDeCalculo(definicion.fuente, definicion.calculo),
    ETIQUETA_DE_AGRUPACION[definicion.agrupar],
    ETIQUETA_DE_PERIODO[definicion.periodo],
  ].join(' · ');
}
