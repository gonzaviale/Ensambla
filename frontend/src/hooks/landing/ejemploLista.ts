import {
  costoUnitario,
  margenSobreCosto,
  precioSegunRegla,
  variacionPct,
} from '@/lib/red/calculos';

// El ejemplo del aceite que cuentan «El dolor» y la demo «Llegó la lista»: un solo juego de números
// (ilustrativos) para que las dos secciones cuenten la misma historia.
export const EJEMPLO = {
  proveedor: 'Distribuidora Sur',
  producto: 'Aceite Natura 900 ml',
  textoDeLista: 'ACEITE NATURA 900 ML X12',
  precioDeLaCaja: 26520,
  unidadesPorCaja: 12,
  costoAnterior: 1900,
  precioActual: 2450,
  ventasPorSemana: 20,
  margenMinimo: 12,
  redondeo: 10,
} as const;

/** Reglas de ganancia (sobre el costo) que se pueden probar en la demo. */
export const REGLAS_DEL_DUENO = [20, 25, 30] as const;
export const REGLA_INICIAL = 25;

export type ResultadoDemo = {
  costoNuevo: number;
  /** Cuánto subió el costo por unidad, en pesos. */
  subioPesos: number;
  /** Cuánto subió el costo por unidad, en %. */
  subioPorcentaje: number;
  /** Margen que queda hoy con el precio actual. */
  margenHoy: number;
  enRiesgo: boolean;
  precioSugerido: number;
  margenNuevo: number;
};

export function calcularDemo(reglaMargen: number): ResultadoDemo {
  const costoNuevo = costoUnitario(EJEMPLO.precioDeLaCaja, 'BULTO', EJEMPLO.unidadesPorCaja);
  const precioSugerido = precioSegunRegla(costoNuevo, {
    margen: reglaMargen,
    redondeo: EJEMPLO.redondeo,
  });
  const margenHoy = margenSobreCosto(costoNuevo, EJEMPLO.precioActual);
  return {
    costoNuevo,
    subioPesos: costoNuevo - EJEMPLO.costoAnterior,
    subioPorcentaje: variacionPct(EJEMPLO.costoAnterior, costoNuevo),
    margenHoy,
    enRiesgo: margenHoy < EJEMPLO.margenMinimo,
    precioSugerido,
    margenNuevo: margenSobreCosto(costoNuevo, precioSugerido),
  };
}

/** Lo que dejaba cada botella antes y ahora, y lo que se pierde al mes si nadie avisa. */
export function calcularDolor() {
  const { costoNuevo } = calcularDemo(REGLA_INICIAL);
  const gananciaAntes = EJEMPLO.precioActual - EJEMPLO.costoAnterior;
  const gananciaHoy = EJEMPLO.precioActual - costoNuevo;
  const perdidaPorBotella = gananciaAntes - gananciaHoy;
  return {
    costoNuevo,
    gananciaAntes,
    gananciaHoy,
    perdidaPorBotella,
    perdidaPorMes: perdidaPorBotella * EJEMPLO.ventasPorSemana * 4,
    /** Cuánto le falta subir al costo para que el precio actual quede por debajo. */
    faltaParaPerder: EJEMPLO.precioActual - costoNuevo,
  };
}
