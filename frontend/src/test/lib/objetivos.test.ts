import { describe, expect, it } from 'vitest';
import * as objetivos from '@/lib/objetivos';
import {
  aportes,
  avance,
  brecha,
  caminos,
  conHistorialReal,
  objetivoDeEjemplo,
  objetivoDesdeServidor,
  resultadoProyectado,
  tieneMeta,
  ultimoValor,
  valoresRecomendados,
  variacion,
  type ObjetivoConMeta,
} from '@/lib/objetivos';

const conMeta = (texto: string, meta: number) =>
  objetivoDesdeServidor({
    id: 'abc',
    texto,
    tipo: 'MONETARIO',
    meta,
    armadoConSami: false,
  }) as ObjetivoConMeta;

const sinMeta = (texto: string) =>
  objetivoDesdeServidor({ id: 'abc', texto, tipo: 'SIN_DATOS', meta: null, armadoConSami: true });

describe('objetivos', () => {
  it('un objetivo sin importe no recibe una meta inventada', () => {
    expect(objetivos).not.toHaveProperty('META_POR_DEFECTO');
    expect(objetivos).not.toHaveProperty('metaDesdeTexto');
    expect(objetivos).not.toHaveProperty('nuevoObjetivo');

    const objetivo = sinMeta('Quiero que más gente conozca mi local');

    expect(objetivo.meta).toBeNull();
    expect(tieneMeta(objetivo)).toBe(false);
  });

  it('sin meta no hay avance ni brecha: no se compara con ninguna cifra', () => {
    const objetivo = conHistorialReal(sinMeta('Quiero atender mejor'), [
      { semana: 'Hoy', queda: 120_000 },
    ]);

    expect(ultimoValor(objetivo)).toBe(120_000);
    expect(avance(objetivo)).toBe(0);
    expect(brecha(objetivo)).toBe(0);
  });

  it('un objetivo guardado en el servidor arranca sin historia, como uno nuevo', () => {
    const objetivo = conMeta('Quiero crecer', 300_000);

    expect(objetivo).toEqual({
      id: 'abc',
      texto: 'Quiero crecer',
      tipo: 'MONETARIO',
      meta: 300_000,
      armadoConSami: false,
      historial: [],
    });
  });

  it('el historial es el resultado real de las últimas semanas del negocio', () => {
    const guardado = conMeta('Quiero crecer', 300_000);

    const objetivo = conHistorialReal(guardado, [
      { semana: 'Hace 1 sem.', queda: 90_000 },
      { semana: 'Hoy', queda: 120_000 },
    ]);

    expect(objetivo.historial).toEqual([
      { semana: 'Hace 1 sem.', valor: 90_000 },
      { semana: 'Hoy', valor: 120_000 },
    ]);
    expect(brecha(objetivo)).toBe(180_000);
    expect(variacion(objetivo)).toBe(30_000);
  });

  it('mide el avance y lo que falta', () => {
    const objetivo = objetivoDeEjemplo();
    expect(avance(objetivo)).toBeCloseTo(0.36);
    expect(brecha(objetivo)).toBe(320_000);
    expect(variacion(objetivo)).toBe(60_000);
  });

  it('el avance nunca pasa de 100 % ni la brecha baja de cero', () => {
    const objetivo = conHistorialReal(conMeta('Quiero ganar $100.000', 100_000), [
      { semana: 'Hoy', queda: 180_000 },
    ]);
    expect(avance(objetivo)).toBe(1);
    expect(brecha(objetivo)).toBe(0);
  });

  it('con las palancas recomendadas el resultado llega a la meta', () => {
    for (const meta of [500_000, 300_000, 900_000]) {
      const objetivo = conMeta(`Quiero ganar $${meta}`, meta);
      const proyectado = resultadoProyectado(objetivo, valoresRecomendados(objetivo));
      // El redondeo de cada palanca deja un margen chico.
      expect(Math.abs(proyectado - objetivo.meta)).toBeLessThan(objetivo.meta * 0.05);
    }
  });

  it('sin aplicar ninguna palanca el resultado no cambia', () => {
    const objetivo = conMeta('Quiero ganar $500.000', 500_000);
    const ceros = { ventas: 0, margen: 0, gastos: 0 };
    expect(resultadoProyectado(objetivo, ceros)).toBe(0);
    expect(Object.values(aportes(objetivo, ceros))).toEqual([0, 0, 0]);
  });

  it('recomienda un solo camino, el combinado', () => {
    const lista = caminos(objetivoDeEjemplo());
    expect(lista).toHaveLength(3);
    expect(lista.filter((camino) => camino.recomendado).map((camino) => camino.clave)).toEqual([
      'combinado',
    ]);
  });

  it('el camino de referencia pide +12 % de ventas, −$80.000 de gastos y +3 puntos de margen', () => {
    const [combinado] = caminos(objetivoDeEjemplo());
    expect(combinado?.datos.map((dato) => dato.valor)).toEqual([
      '+12%',
      '−$80.000',
      '+3 pts de margen',
    ]);
  });
});
