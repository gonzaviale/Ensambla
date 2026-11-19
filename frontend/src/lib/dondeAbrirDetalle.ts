import { FACTORES, type Aporte, type Filtros } from './dondeAbrir';

const moneda = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  maximumFractionDigits: 0,
});

const conUnidad = (texto: string) => (valor: number) => `${valor.toLocaleString('es-AR')} ${texto}`;

const FORMATO_POR_UNIDAD: Record<string, (valor: number) => string> = {
  ARS_mensuales: (valor) => moneda.format(valor),
  indice_0_100: conUnidad('de 100'),
  comercios_500m: conUnidad('comercios a 500 m'),
  lugares_500m: conUnidad('lugares a 500 m'),
  habitantes_km2: conUnidad('personas por km²'),
};

/** El dato observado de un factor, con su unidad, listo para mostrar. */
export function formatoDato(valor: number | undefined, unidad: string | undefined): string {
  if (valor === undefined) return 'Sin dato suficiente';
  const formato = unidad ? FORMATO_POR_UNIDAD[unidad] : undefined;
  return formato ? formato(valor) : valor.toLocaleString('es-AR');
}

type FuenteDelDato = NonNullable<Aporte['fuente']>;

/** Las fuentes de los datos usados, una sola vez cada una. */
export function fuentesDeLosDatos(aportes: Aporte[]): FuenteDelDato[] {
  const porCodigo = new Map<string, FuenteDelDato>();
  aportes.forEach(({ fuente }) => fuente && porCodigo.set(fuente.codigo, fuente));
  return Array.from(porCodigo.values());
}

/** Los factores que la persona dijo que no le importan, en minúscula para escribirlos en una frase. */
export const factoresSinMirar = (filtros: Filtros): string[] =>
  FACTORES.filter((factor) => filtros.importancia[factor.clave] === 0).map((factor) =>
    factor.etiqueta.toLowerCase(),
  );
