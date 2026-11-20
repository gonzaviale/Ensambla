import { describe, expect, it } from 'vitest';
import { makeBlock, makeSystem } from '@/lib/sistema/core';
import {
  clavesReservadasDe,
  esColumnaFija,
  motivoParaNoQuitarLaColumna,
  motivoParaNoQuitarLaSeccion,
  problemaAlCambiarLaColumna,
  sePuedeQuitarLaSeccion,
  tieneColumnasFijas,
} from '@/lib/sistema/estructuraFija';
import type { Bloque, Campo, Sistema } from '@/lib/sistema/tipos';

let sistema: Sistema;
let bloque: Bloque;
let resultado: unknown;

const columnaDe = (clave: string) => bloque.fields.find((campo) => campo.key === clave) as Campo;

function dadoUnNegocioConLasSeccionesObligatorias() {
  sistema = makeSystem('Mi negocio', ['inventory', 'sales', 'clients', 'expenses']);
}

function dadoUnNegocioConDosSeccionesDeStock() {
  sistema = makeSystem('Mi negocio', ['inventory', 'inventory']);
}

function dadaLaSeccionDe(tipo: Bloque['type']) {
  bloque = sistema.sections.flatMap((seccion) => seccion.blocks).find((item) => item.type === tipo) as Bloque;
}

function dadaUnaNotaAparte() {
  bloque = makeBlock('note');
}

function cuandoSePideSiSePuedeQuitarLaSeccion() {
  resultado = sePuedeQuitarLaSeccion(sistema, bloque);
}

function cuandoSeCambiaElTipoDeLaColumna(clave: string, tipo: Campo['type']) {
  resultado = problemaAlCambiarLaColumna(columnaDe(clave), { ...columnaDe(clave), type: tipo });
}

function cuandoSeDejaDeExigirLaColumna(clave: string) {
  resultado = problemaAlCambiarLaColumna(columnaDe(clave), { ...columnaDe(clave), required: false });
}

function cuandoSeQuitanLasOpcionesDeLaColumna(clave: string) {
  resultado = problemaAlCambiarLaColumna(columnaDe(clave), { ...columnaDe(clave), options: ['Otros'] });
}

function cuandoSeRenombraLaColumna(clave: string) {
  resultado = problemaAlCambiarLaColumna(columnaDe(clave), { ...columnaDe(clave), label: 'Otro nombre' });
}

function entoncesSePuedeQuitar() {
  expect(resultado).toBe(true);
}

function entoncesNoSePuedeQuitar() {
  expect(resultado).toBe(false);
}

function entoncesNoHayProblema() {
  expect(resultado).toBeNull();
}

function entoncesElProblemaEs(mensaje: string) {
  expect(resultado).toBe(mensaje);
}

describe('la estructura fija del negocio', () => {
  it('no deja quitar la única sección de Stock', () => {
    dadoUnNegocioConLasSeccionesObligatorias();
    dadaLaSeccionDe('inventory');

    cuandoSePideSiSePuedeQuitarLaSeccion();

    entoncesNoSePuedeQuitar();
  });

  it('deja quitar una sección de Stock si hay otra', () => {
    dadoUnNegocioConDosSeccionesDeStock();
    dadaLaSeccionDe('inventory');

    cuandoSePideSiSePuedeQuitarLaSeccion();

    entoncesSePuedeQuitar();
  });

  it('deja quitar una sección que el negocio no necesita', () => {
    dadoUnNegocioConLasSeccionesObligatorias();
    dadaUnaNotaAparte();

    cuandoSePideSiSePuedeQuitarLaSeccion();

    entoncesSePuedeQuitar();
  });

  it('explica por qué no se puede quitar una sección o una columna', () => {
    dadoUnNegocioConLasSeccionesObligatorias();
    dadaLaSeccionDe('inventory');

    expect(motivoParaNoQuitarLaSeccion(bloque)).toBe(
      'No se puede quitar la sección «Inventario»: Ensambla la necesita para funcionar.',
    );
    expect(motivoParaNoQuitarLaColumna(columnaDe('cost'))).toBe(
      'No se puede quitar «Costo»: Ensambla la necesita para funcionar. Podés cambiarle el nombre.',
    );
  });

  it('marca como fijas las columnas que Ensambla necesita en el stock', () => {
    dadoUnNegocioConLasSeccionesObligatorias();
    dadaLaSeccionDe('inventory');

    expect(tieneColumnasFijas(bloque)).toBe(true);
    expect(bloque.fields.every(esColumnaFija)).toBe(true);
  });

  it('no marca como fija una columna que Ensambla no necesita', () => {
    dadoUnNegocioConLasSeccionesObligatorias();
    dadaLaSeccionDe('clients');

    expect(esColumnaFija(columnaDe('phone'))).toBe(false);
    expect(esColumnaFija(columnaDe('name'))).toBe(true);
  });

  it('no deja cambiar el tipo de dato de una columna fija', () => {
    dadoUnNegocioConLasSeccionesObligatorias();
    dadaLaSeccionDe('inventory');

    cuandoSeCambiaElTipoDeLaColumna('price', 'text');

    entoncesElProblemaEs(
      'No se puede cambiar «Precio»: Ensambla la necesita para funcionar. Podés cambiarle el nombre.',
    );
  });

  it('no deja dejar de exigir una columna fija obligatoria', () => {
    dadoUnNegocioConLasSeccionesObligatorias();
    dadaLaSeccionDe('inventory');

    cuandoSeDejaDeExigirLaColumna('quantity');

    entoncesElProblemaEs(
      'No se puede cambiar «Cantidad»: Ensambla la necesita para funcionar. Podés cambiarle el nombre.',
    );
  });

  it('no deja quitar la mercadería de las categorías de gasto', () => {
    dadoUnNegocioConLasSeccionesObligatorias();
    dadaLaSeccionDe('expenses');

    cuandoSeQuitanLasOpcionesDeLaColumna('category');

    entoncesElProblemaEs(
      'No se puede cambiar «Categoría»: Ensambla la necesita para funcionar. Podés cambiarle el nombre.',
    );
  });

  it('deja cambiar el nombre de una columna fija', () => {
    dadoUnNegocioConLasSeccionesObligatorias();
    dadaLaSeccionDe('inventory');

    cuandoSeRenombraLaColumna('price');

    entoncesNoHayProblema();
  });

  it('no pone reparos a una columna que no existe todavía', () => {
    expect(problemaAlCambiarLaColumna(undefined, { key: 'x', label: 'X', type: 'text', required: false })).toBeNull();
  });

  it('reserva las claves de las columnas fijas de cada tipo de sección', () => {
    dadoUnNegocioConLasSeccionesObligatorias();
    dadaLaSeccionDe('clients');

    expect(clavesReservadasDe(bloque)).toEqual(['name']);
  });
});
