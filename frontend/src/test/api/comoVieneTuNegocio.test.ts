import { describe, expect, it } from 'vitest';
import { obtenerComoVieneTuNegocio } from '@/api/comoVieneTuNegocio';
import { negocioDe } from '@/lib/objetivosNegocio';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { comoVieneTuNegocioDe } from '@/mocks/handlers/comoVieneTuNegocio';
import { setupUsuarioConSesion } from '@/test/testHelpers';

const HOY = '2026-11-08';
const SEMANAS_QUE_TERMINAN_EN_HOY = [
  'Hace 5 sem.',
  'Hace 4 sem.',
  'Hace 3 sem.',
  'Hace 2 sem.',
  'Hace 1 sem.',
  'Hoy',
];

type Negocio = Awaited<ReturnType<typeof obtenerComoVieneTuNegocio>>;

let almacen: ReturnType<typeof crearAlmacenCargado>;
let negocio: Negocio;

describe('Cómo viene tu negocio', () => {
  it('sin secciones el servidor responde sin ventas, en cero y sin semanas', async () => {
    dadoUnUsuarioSinSecciones();

    await cuandoSePideComoVieneElNegocio();

    entoncesElNegocioEstaEnCeroYSinSemanas();
  });

  it('con el almacén cargado cuenta las ventas en pesos enteros', () => {
    dadoElAlmacenCargado();

    cuandoSeCalculaComoVieneElNegocio();

    entoncesVendioLoQueDiceElCalculoDelCliente();
    entoncesQuedaLoVendidoMenosMercaderiaYGastos();
  });

  it('con el almacén cargado trae seis semanas que terminan en Hoy', () => {
    dadoElAlmacenCargado();

    cuandoSeCalculaComoVieneElNegocio();

    entoncesTraeSeisSemanasQueTerminanEnHoy();
    entoncesLaUltimaSemanaQuedaIgualAlTotal();
    entoncesCadaSemanaQuedaEnPesosEnteros();
  });
});

function dadoUnUsuarioSinSecciones() {
  setupUsuarioConSesion();
}

function dadoElAlmacenCargado() {
  almacen = crearAlmacenCargado(HOY);
}

async function cuandoSePideComoVieneElNegocio() {
  negocio = await obtenerComoVieneTuNegocio();
}

function cuandoSeCalculaComoVieneElNegocio() {
  negocio = comoVieneTuNegocioDe([almacen], HOY);
}

function entoncesElNegocioEstaEnCeroYSinSemanas() {
  expect(negocio).toEqual({
    hayVentas: false,
    vendiste: 0,
    mercaderia: 0,
    gastos: 0,
    queda: 0,
    teDeben: 0,
    semanas: [],
  });
}

function entoncesVendioLoQueDiceElCalculoDelCliente() {
  expect(negocio.hayVentas).toBe(true);
  expect(negocio.vendiste).toBe(Math.round(negocioDe([almacen], HOY).vendiste));
}

function entoncesQuedaLoVendidoMenosMercaderiaYGastos() {
  expect(negocio.queda).toBe(negocio.vendiste - negocio.mercaderia - negocio.gastos);
}

function entoncesTraeSeisSemanasQueTerminanEnHoy() {
  expect(negocio.semanas.map((semana) => semana.semana)).toEqual(SEMANAS_QUE_TERMINAN_EN_HOY);
}

function entoncesLaUltimaSemanaQuedaIgualAlTotal() {
  expect(negocio.semanas.at(-1)?.queda).toBe(negocio.queda);
}

function entoncesCadaSemanaQuedaEnPesosEnteros() {
  expect(negocio.semanas.map((semana) => Number.isInteger(semana.queda))).not.toContain(false);
}
