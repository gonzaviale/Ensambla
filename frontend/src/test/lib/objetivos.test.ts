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
  resultadoDelMes,
  resultadoProyectado,
  tieneMeta,
  ultimoValor,
  valoresRecomendados,
  variacion,
  type ObjetivoConMeta,
} from '@/lib/objetivos';
import {
  cuandoSeCalcula,
  entoncesElResultadoCoincideCon,
  entoncesElResultadoEs,
} from '@/test/calculo';

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

const HISTORIAL_DE_DOS_SEMANAS = [
  { semana: 'Hace 1 sem.', queda: 90_000 },
  { semana: 'Hoy', queda: 120_000 },
];

describe('objetivos', () => {
  it('el módulo ya no inventa metas por defecto', () => {
    cuandoSeCalcula(() => Object.keys(objetivos));

    entoncesNoExporta('META_POR_DEFECTO', 'metaDesdeTexto', 'nuevoObjetivo');
  });

  it('un objetivo sin importe no recibe una meta inventada', () => {
    cuandoSeCalcula(() => sinMeta('Quiero que más gente conozca mi local'));

    entoncesElResultadoCoincideCon({ meta: null });
  });

  it('un objetivo sin importe no tiene meta', () => {
    cuandoSeCalcula(() => tieneMeta(sinMeta('Quiero que más gente conozca mi local')));

    entoncesElResultadoEs(false);
  });

  it('sin meta se conoce el último valor real', () => {
    cuandoSeCalcula(() => ultimoValor(unObjetivoSinMetaConHistorial()));

    entoncesElResultadoEs(120_000);
  });

  it('sin meta no hay avance: no se compara con ninguna cifra', () => {
    cuandoSeCalcula(() => avance(unObjetivoSinMetaConHistorial()));

    entoncesElResultadoEs(0);
  });

  it('sin meta no hay brecha', () => {
    cuandoSeCalcula(() => brecha(unObjetivoSinMetaConHistorial()));

    entoncesElResultadoEs(0);
  });

  it('un objetivo guardado en el servidor arranca sin historia, como uno nuevo', () => {
    cuandoSeCalcula(() => conMeta('Quiero crecer', 300_000));

    entoncesElResultadoEs({
      id: 'abc',
      texto: 'Quiero crecer',
      tipo: 'MONETARIO',
      meta: 300_000,
      armadoConSami: false,
      historial: [],
    });
  });

  it('el historial es el resultado real de las últimas semanas del negocio', () => {
    cuandoSeCalcula(
      () => conHistorialReal(conMeta('Quiero crecer', 300_000), HISTORIAL_DE_DOS_SEMANAS).historial,
    );

    entoncesElResultadoEs([
      { semana: 'Hace 1 sem.', valor: 90_000 },
      { semana: 'Hoy', valor: 120_000 },
    ]);
  });

  it('con historial real la brecha es lo que falta para la meta', () => {
    cuandoSeCalcula(() =>
      brecha(conHistorialReal(conMeta('Quiero crecer', 300_000), HISTORIAL_DE_DOS_SEMANAS)),
    );

    entoncesElResultadoEs(180_000);
  });

  it('con ganancia dice cuánto ganás por mes', () => {
    cuandoSeCalcula(() =>
      resultadoDelMes(
        conHistorialReal(conMeta('Quiero crecer', 300_000), HISTORIAL_DE_DOS_SEMANAS),
      ),
    );

    entoncesElResultadoEs({ monto: '$120.000', etiqueta: 'ganás por mes' });
  });

  it('con pérdida dice cuánto perdés por mes, con el menos delante del signo $', () => {
    cuandoSeCalcula(() =>
      resultadoDelMes(
        conHistorialReal(conMeta('Quiero crecer', 300_000), [{ semana: 'Hoy', queda: -126_150 }]),
      ),
    );

    entoncesElResultadoEs({ monto: '−$126.150', etiqueta: 'perdés por mes' });
  });

  it('con historial real la variación es lo que cambió en la última semana', () => {
    cuandoSeCalcula(() =>
      variacion(conHistorialReal(conMeta('Quiero crecer', 300_000), HISTORIAL_DE_DOS_SEMANAS)),
    );

    entoncesElResultadoEs(30_000);
  });

  it('mide el avance', () => {
    cuandoSeCalcula(() => avance(objetivoDeEjemplo()));

    entoncesElAvanceEsCercanoA(0.36);
  });

  it('mide lo que falta y lo que cambió', () => {
    cuandoSeCalcula(() => [brecha(objetivoDeEjemplo()), variacion(objetivoDeEjemplo())]);

    entoncesElResultadoEs([320_000, 60_000]);
  });

  it('el avance nunca pasa de 100 %', () => {
    cuandoSeCalcula(() => avance(unObjetivoCumplido()));

    entoncesElResultadoEs(1);
  });

  it('la brecha nunca baja de cero', () => {
    cuandoSeCalcula(() => brecha(unObjetivoCumplido()));

    entoncesElResultadoEs(0);
  });

  it.each([500_000, 300_000, 900_000])(
    'con las palancas recomendadas el resultado llega a la meta de %d',
    (meta) => {
      cuandoSeCalcula(() => proyeccionRecomendada(meta));

      entoncesLaProyeccionQuedaCercaDeLaMeta(meta);
    },
  );

  it('sin aplicar ninguna palanca el resultado no cambia', () => {
    cuandoSeCalcula(() => resultadoProyectado(conMeta('Quiero ganar $500.000', 500_000), CEROS));

    entoncesElResultadoEs(0);
  });

  it('sin aplicar ninguna palanca no hay aportes', () => {
    cuandoSeCalcula(() => Object.values(aportes(conMeta('Quiero ganar $500.000', 500_000), CEROS)));

    entoncesElResultadoEs([0, 0, 0]);
  });

  it('ofrece tres caminos', () => {
    cuandoSeCalcula(() => caminos(objetivoDeEjemplo()).length);

    entoncesElResultadoEs(3);
  });

  it('recomienda un solo camino, el combinado', () => {
    cuandoSeCalcula(() =>
      caminos(objetivoDeEjemplo())
        .filter((camino) => camino.recomendado)
        .map((camino) => camino.clave),
    );

    entoncesElResultadoEs(['combinado']);
  });

  it('el camino de referencia pide +12 % de ventas, −$80.000 de gastos y +3 puntos de margen', () => {
    cuandoSeCalcula(() => caminos(objetivoDeEjemplo())[0]?.datos.map((dato) => dato.valor));

    entoncesElResultadoEs(['+12%', '−$80.000', '+3 pts de ganancia por venta']);
  });
});

const CEROS = { ventas: 0, margen: 0, gastos: 0 };
let proyectado = 0;

function unObjetivoSinMetaConHistorial() {
  return conHistorialReal(sinMeta('Quiero atender mejor'), [{ semana: 'Hoy', queda: 120_000 }]);
}

function unObjetivoCumplido() {
  return conHistorialReal(conMeta('Quiero ganar $100.000', 100_000), [
    { semana: 'Hoy', queda: 180_000 },
  ]);
}

function proyeccionRecomendada(meta: number) {
  const objetivo = conMeta(`Quiero ganar $${meta}`, meta);
  proyectado = resultadoProyectado(objetivo, valoresRecomendados(objetivo));
  return proyectado;
}

function entoncesNoExporta(...nombres: string[]) {
  for (const nombre of nombres) expect(objetivos).not.toHaveProperty(nombre);
}

function entoncesElAvanceEsCercanoA(esperado: number) {
  expect(avance(objetivoDeEjemplo())).toBeCloseTo(esperado);
}

// El redondeo de cada palanca deja un margen chico.
function entoncesLaProyeccionQuedaCercaDeLaMeta(meta: number) {
  expect(Math.abs(proyectado - meta)).toBeLessThan(meta * 0.05);
}
