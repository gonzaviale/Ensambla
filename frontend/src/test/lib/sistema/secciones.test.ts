import { describe, expect, it } from 'vitest';
import { makeSystem } from '@/lib/sistema/core';
import {
  armarSeccion,
  moverSeccionEn,
  quitarSeccionDe,
  renombrarSeccionesEn,
  sumarBloque,
} from '@/lib/sistema/secciones';

const conDosSecciones = () => {
  const sistema = makeSystem('Mi cuaderno', ['clients']);
  const primera = sistema.sections[0];
  if (!primera) throw new Error('Falta la primera sección');
  const segunda = armarSeccion('Ventas', 'empty');
  sistema.sections.push(segunda);
  return { sistema, primera, segunda };
};

describe('secciones y bloques de un sistema', () => {
  it('sumarBloque agrega la pieza al final de la sección, con ejemplos si se piden', () => {
    const { sistema, segunda } = conDosSecciones();

    const creado = sumarBloque(sistema, 'inventory', segunda.id, { conEjemplos: true });

    expect(creado?.type).toBe('inventory');
    expect(sistema.sections[1]?.blocks.at(-1)?.id).toBe(creado?.id);
    expect(creado?.rows.some((fila) => fila.__example)).toBe(true);
  });

  it('sumarBloque no hace nada si la sección no existe', () => {
    const { sistema } = conDosSecciones();

    expect(sumarBloque(sistema, 'inventory', 'no-existe')).toBeUndefined();
  });

  it('armarSeccion usa el nombre pedido y, sin nombre, el del preset', () => {
    expect(armarSeccion('Ventas', 'empty').name).toBe('Ventas');
    expect(armarSeccion('', 'empty').name).toBe('Nueva sección');
    expect(armarSeccion('Ventas', 'empty').blocks).toEqual([]);
  });

  it('renombrarSeccionesEn ignora nombres vacíos', () => {
    const { sistema, primera, segunda } = conDosSecciones();

    renombrarSeccionesEn(sistema, [
      [primera.id, 'Stock'],
      [segunda.id, ''],
    ]);

    expect(sistema.sections.map((seccion) => seccion.name)).toEqual(['Stock', 'Ventas']);
  });

  it('moverSeccionEn cambia el orden y no se pasa de los extremos', () => {
    const { sistema, primera, segunda } = conDosSecciones();

    moverSeccionEn(sistema, primera.id, 'arriba');
    expect(sistema.sections.map((seccion) => seccion.id)).toEqual([primera.id, segunda.id]);

    moverSeccionEn(sistema, primera.id, 'abajo');
    expect(sistema.sections.map((seccion) => seccion.id)).toEqual([segunda.id, primera.id]);
  });

  it('quitarSeccionDe pasa los bloques a otra sección y conserva sus datos', () => {
    const { sistema, primera, segunda } = conDosSecciones();
    const bloques = primera.blocks.map((bloque) => bloque.id);

    const receptora = quitarSeccionDe(sistema, primera.id);

    expect(receptora).toBe(segunda.id);
    expect(sistema.sections).toHaveLength(1);
    expect(sistema.sections[0]?.blocks.map((bloque) => bloque.id)).toEqual(bloques);
  });

  it('no deja quitar la única sección', () => {
    const sistema = makeSystem('Mi cuaderno', ['clients']);

    expect(quitarSeccionDe(sistema, sistema.sections[0]?.id ?? '')).toBeUndefined();
    expect(sistema.sections).toHaveLength(1);
  });
});
