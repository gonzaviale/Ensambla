import { describe, expect, it } from 'vitest';
import {
  META_POR_DEFECTO,
  aportes,
  avance,
  brecha,
  caminos,
  conHistorialReal,
  metaDesdeTexto,
  nuevoObjetivo,
  objetivoDeEjemplo,
  objetivoDesdeServidor,
  resultadoProyectado,
  valoresRecomendados,
  variacion,
} from '@/lib/objetivos';

describe('objetivos', () => {
  it('lee el importe que la persona escribió', () => {
    expect(metaDesdeTexto('Quiero ganar $400.000 por mes')).toBe(400_000);
    expect(metaDesdeTexto('Quiero retirar $ 150.000')).toBe(150_000);
  });

  it('entiende el importe como lo escribe la gente: con k, con mil, con millones o con puntos', () => {
    expect(metaDesdeTexto('Quiero ganar $400k este mes')).toBe(400_000);
    expect(metaDesdeTexto('Quiero ganar 400 mil por mes')).toBe(400_000);
    expect(metaDesdeTexto('Quiero ganar $400.000')).toBe(400_000);
    expect(metaDesdeTexto('Quiero ganar $400,000')).toBe(400_000);
    expect(metaDesdeTexto('Quiero ganar 1,5 millones')).toBe(1_500_000);
    expect(metaDesdeTexto('Quiero ganar 400000')).toBe(400_000);
  });

  it('prefiere el importe con $ o multiplicador antes que otros números del texto', () => {
    expect(metaDesdeTexto('Vender 30 unidades por día y ganar $250K')).toBe(250_000);
    expect(metaDesdeTexto('Subir 15 % y llegar a $300 mil antes del día 20')).toBe(300_000);
  });

  it('usa la meta de ejemplo cuando no hay un importe razonable', () => {
    expect(metaDesdeTexto('Quiero recuperar mis gastos antes del día 20')).toBe(META_POR_DEFECTO);
    expect(metaDesdeTexto('Quiero ganar 400 mildos')).toBe(META_POR_DEFECTO);
    expect(metaDesdeTexto('Quiero crecer')).toBe(META_POR_DEFECTO);
    expect(metaDesdeTexto('Quiero $5')).toBe(META_POR_DEFECTO);
  });

  it('un objetivo nuevo arranca sin historia ni números de ejemplo', () => {
    const objetivo = nuevoObjetivo('  Quiero ganar $400.000  ');
    expect(objetivo.texto).toBe('Quiero ganar $400.000');
    expect(objetivo.meta).toBe(400_000);
    expect(objetivo.historial).toEqual([]);
    expect(variacion(objetivo)).toBe(0);
    expect(brecha(objetivo)).toBe(400_000);
  });

  it('un objetivo guardado en el servidor arranca sin historia, como uno nuevo', () => {
    const objetivo = objetivoDesdeServidor({ id: 'abc', texto: 'Quiero crecer', meta: 300_000 });

    expect(objetivo).toEqual({ id: 'abc', texto: 'Quiero crecer', meta: 300_000, historial: [] });
  });

  it('el historial es el resultado real de las últimas semanas del negocio', () => {
    const guardado = objetivoDesdeServidor({ id: 'abc', texto: 'Quiero crecer', meta: 300_000 });

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
    const objetivo = conHistorialReal(nuevoObjetivo('Quiero ganar $100.000'), [
      { semana: 'Hoy', queda: 180_000 },
    ]);
    expect(avance(objetivo)).toBe(1);
    expect(brecha(objetivo)).toBe(0);
  });

  it('con las palancas recomendadas el resultado llega a la meta', () => {
    for (const texto of [
      'Quiero ganar $500.000',
      'Quiero ganar $300.000',
      'Quiero ganar $900.000',
    ]) {
      const objetivo = nuevoObjetivo(texto);
      const proyectado = resultadoProyectado(objetivo, valoresRecomendados(objetivo));
      // El redondeo de cada palanca deja un margen chico.
      expect(Math.abs(proyectado - objetivo.meta)).toBeLessThan(objetivo.meta * 0.05);
    }
  });

  it('sin aplicar ninguna palanca el resultado no cambia', () => {
    const objetivo = nuevoObjetivo('Quiero ganar $500.000');
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
