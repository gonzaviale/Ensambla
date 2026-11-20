import { describe, expect, it } from 'vitest';
import { makeSystem } from '@/lib/sistema/core';
import {
  armarSeccion,
  moverSeccionEn,
  quitarSeccionDe,
  renombrarSeccionesEn,
  sumarBloque,
} from '@/lib/sistema/secciones';
import type { Bloque, Seccion, Sistema } from '@/lib/sistema/tipos';

let sistema: Sistema;
let primera: Seccion;
let segunda: Seccion;
let creado: Bloque | undefined;
let receptora: string | undefined;
let idsDeLosBloques: string[];

describe('pestañas y bloques de un sistema', () => {
  it('sumarBloque agrega la pieza al final de la pestaña', () => {
    dadoUnSistemaConDosSecciones();

    cuandoSeSumaUnInventarioConEjemplosALaSegunda();

    entoncesLaPiezaQuedaAlFinalDeLaSegunda();
  });

  it('sumarBloque trae ejemplos si se piden', () => {
    dadoUnSistemaConDosSecciones();

    cuandoSeSumaUnInventarioConEjemplosALaSegunda();

    entoncesLaPiezaTraeFilasDeEjemplo();
  });

  it('sumarBloque no hace nada si la pestaña no existe', () => {
    dadoUnSistemaConDosSecciones();

    cuandoSeSumaUnInventarioAUnaPestanaQueNoExiste();

    entoncesNoSeCreoNingunaPieza();
  });

  it('armarSeccion usa el nombre pedido', () => {
    cuandoSeArmaLaSeccion('Ventas');

    entoncesLaSeccionSeLlama('Ventas');
  });

  it('armarSeccion, sin nombre, usa el del preset', () => {
    cuandoSeArmaLaSeccion('');

    entoncesLaSeccionSeLlama('Nueva pestaña');
  });

  it('armarSeccion arranca sin bloques', () => {
    cuandoSeArmaLaSeccion('Ventas');

    entoncesLaSeccionNoTieneBloques();
  });

  it('renombrarSeccionesEn ignora nombres vacíos', () => {
    dadoUnSistemaConDosSecciones();

    cuandoSeRenombranLasSeccionesConUnNombreVacio();

    entoncesLosNombresSon('Stock', 'Ventas');
  });

  it('moverSeccionEn no se pasa del extremo de arriba', () => {
    dadoUnSistemaConDosSecciones();

    cuandoSeMueveLaPrimeraHacia('arriba');

    entoncesElOrdenEs('primera', 'segunda');
  });

  it('moverSeccionEn cambia el orden hacia abajo', () => {
    dadoUnSistemaConDosSecciones();

    cuandoSeMueveLaPrimeraHacia('abajo');

    entoncesElOrdenEs('segunda', 'primera');
  });

  it('quitarSeccionDe pasa los bloques a otra pestaña', () => {
    dadoUnSistemaConDosSecciones();

    cuandoSeQuitaLaPrimeraSeccion();

    entoncesLaSegundaRecibeLosBloquesDeLaPrimera();
  });

  it('quitarSeccionDe deja una sola pestaña', () => {
    dadoUnSistemaConDosSecciones();

    cuandoSeQuitaLaPrimeraSeccion();

    entoncesQuedaUnaSolaSeccion();
  });

  it('no deja quitar la única pestaña', () => {
    dadoUnSistemaConUnaSolaSeccion();

    cuandoSeQuitaLaUnicaSeccion();

    entoncesSigueHabiendoUnaSolaSeccionYNadieRecibeNada();
  });
});

function dadoUnSistemaConDosSecciones() {
  sistema = makeSystem('Mi sección', ['clients']);
  const inicial = sistema.sections[0];
  if (!inicial) throw new Error('Falta la primera pestaña');
  primera = inicial;
  segunda = armarSeccion('Ventas', 'empty');
  sistema.sections.push(segunda);
  idsDeLosBloques = primera.blocks.map((bloque) => bloque.id);
}

function dadoUnSistemaConUnaSolaSeccion() {
  sistema = makeSystem('Mi sección', ['clients']);
}

function cuandoSeSumaUnInventarioConEjemplosALaSegunda() {
  creado = sumarBloque(sistema, 'inventory', segunda.id, { conEjemplos: true });
}

function cuandoSeSumaUnInventarioAUnaPestanaQueNoExiste() {
  creado = sumarBloque(sistema, 'inventory', 'no-existe');
}

let seccionArmada: Seccion;

function cuandoSeArmaLaSeccion(nombre: string) {
  seccionArmada = armarSeccion(nombre, 'empty');
}

function cuandoSeRenombranLasSeccionesConUnNombreVacio() {
  renombrarSeccionesEn(sistema, [
    [primera.id, 'Stock'],
    [segunda.id, ''],
  ]);
}

function cuandoSeMueveLaPrimeraHacia(sentido: 'arriba' | 'abajo') {
  moverSeccionEn(sistema, primera.id, sentido);
}

function cuandoSeQuitaLaPrimeraSeccion() {
  receptora = quitarSeccionDe(sistema, primera.id);
}

function cuandoSeQuitaLaUnicaSeccion() {
  receptora = quitarSeccionDe(sistema, sistema.sections[0]?.id ?? '');
}

function entoncesLaPiezaQuedaAlFinalDeLaSegunda() {
  expect(creado?.type).toBe('inventory');
  expect(sistema.sections[1]?.blocks.at(-1)?.id).toBe(creado?.id);
}

function entoncesLaPiezaTraeFilasDeEjemplo() {
  expect(creado?.rows.some((fila) => fila.__example)).toBe(true);
}

function entoncesNoSeCreoNingunaPieza() {
  expect(creado).toBeUndefined();
}

function entoncesLaSeccionSeLlama(nombre: string) {
  expect(seccionArmada.name).toBe(nombre);
}

function entoncesLaSeccionNoTieneBloques() {
  expect(seccionArmada.blocks).toEqual([]);
}

function entoncesLosNombresSon(...nombres: string[]) {
  expect(sistema.sections.map((seccion) => seccion.name)).toEqual(nombres);
}

function entoncesElOrdenEs(...orden: ('primera' | 'segunda')[]) {
  const ids = { primera: primera.id, segunda: segunda.id };
  expect(sistema.sections.map((seccion) => seccion.id)).toEqual(orden.map((clave) => ids[clave]));
}

function entoncesLaSegundaRecibeLosBloquesDeLaPrimera() {
  expect(receptora).toBe(segunda.id);
  expect(sistema.sections[0]?.blocks.map((bloque) => bloque.id)).toEqual(idsDeLosBloques);
}

function entoncesQuedaUnaSolaSeccion() {
  expect(sistema.sections).toHaveLength(1);
}

function entoncesSigueHabiendoUnaSolaSeccionYNadieRecibeNada() {
  expect(receptora).toBeUndefined();
  expect(sistema.sections).toHaveLength(1);
}
