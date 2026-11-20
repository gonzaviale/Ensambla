import { describe, expect, it } from 'vitest';
import { alcanceDeLaMeta, leerObjetivo } from '@/lib/lecturaObjetivo';

const NEGOCIO = { hayVentas: true, vendiste: 1_000_000, queda: 250_000 };
const NEGOCIO_CHICO = { hayVentas: true, vendiste: 83_000, queda: 28_000 };

type Lectura = ReturnType<typeof leerObjetivo>;
type Alcance = ReturnType<typeof alcanceDeLaMeta>;

let lectura: Lectura;
let alcance: Alcance;

describe('leerObjetivo', () => {
  it.each([
    ['Quiero ganar $400.000 por mes sin subir demasiado los precios', 400_000],
    ['Quiero ganar 400 mil', 400_000],
    ['quiero ganar 400k', 400_000],
    ['Quiero ganar 1,5 millones', 1_500_000],
    ['quiero ganar un palo por mes', 1_000_000],
    ['quiero ganar medio millón', 500_000],
    ['quiero ganar 300 lucas', 300_000],
    ['Quiero ganar 400000', 400_000],
    ['Quiero poder retirar $150.000 por mes sin afectar el negocio', 150_000],
    ['Quiero ganar $100.000 por semana', 429_000],
    ['ganar 20 mil por día', 600_000],
    ['ganar 6 millones por año', 500_000],
    ['Para 2026 quiero ganar 400 mil', 400_000],
    ['Quiero subir la yerba a $4.000 y ganar $500.000', 500_000],
    ['Quiero ganar $500.000 sin subir la yerba a más de $4.000', 500_000],
    ['Vender 30 unidades por día y ganar $250K', 250_000],
    ['Subir 15 % y llegar a 300 mil antes del día 20', 300_000],
    ['Quiero que el negocio me deje $300.000', 300_000],
    ['ganar 400 mil por mes vendiendo 30 unidades por día', 400_000],
  ])('«%s» → meta %d', (texto, meta) => {
    cuandoSeLee(texto);

    entoncesLaMetaEs(meta);
  });

  it('una meta de ventas es un objetivo de vender', () => {
    cuandoSeLee('Quiero vender $2.000.000 por mes', NEGOCIO);

    entoncesElTipoEs('VENDER');
  });

  it('una meta de ventas la pasa a ganancia con lo que queda hoy de cada venta', () => {
    cuandoSeLee('Quiero vender $2.000.000 por mes', NEGOCIO);

    entoncesLaMetaEs(500_000);
    entoncesLaExplicacionDice(/25 %/);
  });

  it('«en ventas» después del monto también es vender', () => {
    cuandoSeLee('Quiero llegar a 1 millón en ventas', NEGOCIO);

    entoncesLaMetaEs(250_000);
  });

  it('una meta de ventas sin datos no inventa la ganancia', () => {
    cuandoSeLee('Quiero vender $2.000.000 por mes');

    entoncesLaMetaEs(null);
    entoncesAvisa('Escribí abajo cuánto querés que te quede por mes, en pesos.');
  });

  it.each([
    ['Quiero ganar un 20 % más', NEGOCIO, 300_000],
    ['Quiero duplicar lo que gano', NEGOCIO, 500_000],
    ['Quiero ganar un 20 % más', undefined, null],
  ])('crecer «%s» se calcula sobre lo que queda hoy', (texto, negocio, meta) => {
    cuandoSeLee(texto, negocio);

    entoncesLaMetaEs(meta);
  });

  it.each(['Quiero recuperar mis gastos antes del día 20', 'Quiero crecer', 'Quiero $5'])(
    'sin monto no inventa una meta: «%s»',
    (texto) => {
      cuandoSeLee(texto);

      entoncesLaMetaEs(null);
    },
  );

  it('recuperar los gastos es un objetivo de cubrir gastos', () => {
    cuandoSeLee('Quiero recuperar mis gastos');

    entoncesElTipoEs('CUBRIR_GASTOS');
  });

  it('avisa si había más de un monto y cuál tomó', () => {
    cuandoSeLee('Quiero ganar $400.000 o $500.000');

    entoncesLaMetaEs(400_000);
    entoncesElPrimerAvisoDice(/También escribiste \$500\.000/);
  });

  it('avisa si la meta ya se cumple hoy', () => {
    cuandoSeLee('Quiero ganar $100.000', NEGOCIO);

    entoncesElPrimerAvisoDice(/ya la cumplís/);
  });

  it.each([
    ['Quiero retirar $150.000', /^Retirar \$150\.000 por mes/],
    ['Quiero cubrir $300.000 de alquiler', /para tus gastos/],
    ['Quiero ganar $100.000 por semana', /unos \$429\.000 por mes/],
  ])('explica «%s» con sus palabras', (texto, explicacion) => {
    cuandoSeLee(texto);

    entoncesLaExplicacionDice(explicacion);
  });
});

describe('alcanceDeLaMeta', () => {
  it('una meta mayor a lo que se vende no se alcanza', () => {
    cuandoSeMideElAlcanceDe(1_000_000, NEGOCIO_CHICO);

    entoncesElNivelEs('imposible');
  });

  it('una meta mayor a lo que se vende sugiere un primer escalón', () => {
    cuandoSeMideElAlcanceDe(1_000_000, NEGOCIO_CHICO);

    entoncesSeSugiere(36_000);
    entoncesElMensajeDice(/Hoy vendés unos \$83\.000 por mes/);
  });

  it('más del doble de lo que queda hoy está lejos pero se puede', () => {
    cuandoSeMideElAlcanceDe(70_000, NEGOCIO_CHICO);

    entoncesElNivelEs('lejos');
  });

  it('una meta cercana no avisa nada', () => {
    cuandoSeMideElAlcanceDe(40_000, NEGOCIO_CHICO);

    entoncesNoHayAlcance();
  });

  it('sin ventas no inventa un escalón', () => {
    cuandoSeMideElAlcanceDe(1_000_000, { hayVentas: false, vendiste: 0, queda: 0 });

    entoncesNoHayAlcance();
  });

  it('si hoy no queda nada no sugiere un escalón', () => {
    cuandoSeMideElAlcanceDe(1_000_000, { hayVentas: true, vendiste: 83_000, queda: -5_000 });

    entoncesElNivelEs('imposible');
    entoncesNoSeSugiereNada();
  });
});

function cuandoSeLee(texto: string, negocio?: typeof NEGOCIO) {
  lectura = leerObjetivo(texto, negocio);
}

function cuandoSeMideElAlcanceDe(meta: number, negocio: Parameters<typeof alcanceDeLaMeta>[1]) {
  alcance = alcanceDeLaMeta(meta, negocio);
}

function entoncesLaMetaEs(meta: number | null) {
  expect(lectura.meta).toBe(meta);
}

function entoncesElTipoEs(tipo: string) {
  expect(lectura.tipo).toBe(tipo);
}

function entoncesLaExplicacionDice(esperado: RegExp) {
  expect(lectura.explicacion).toMatch(esperado);
}

function entoncesAvisa(aviso: string) {
  expect(lectura.avisos).toContain(aviso);
}

function entoncesElPrimerAvisoDice(esperado: RegExp) {
  expect(lectura.avisos[0]).toMatch(esperado);
}

function entoncesElNivelEs(nivel: string) {
  expect(alcance?.nivel).toBe(nivel);
}

function entoncesSeSugiere(monto: number) {
  expect(alcance?.sugerida).toBe(monto);
}

function entoncesNoSeSugiereNada() {
  expect(alcance?.sugerida).toBeNull();
}

function entoncesElMensajeDice(esperado: RegExp) {
  expect(alcance?.mensaje).toMatch(esperado);
}

function entoncesNoHayAlcance() {
  expect(alcance).toBeNull();
}
