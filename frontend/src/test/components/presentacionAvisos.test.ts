import { describe, expect, it } from 'vitest';
import type { Aviso } from '@/lib/sistema/alertas';
import {
  agruparAvisos,
  cuandoSeDetecto,
  datosDelAviso,
  NOMBRE_DE_TIPO,
  razonDelAviso,
  restoDelAviso,
  resumenDelAviso,
  suavizarTexto,
  tituloDeGrupo,
} from '@/components/sistema/avisos/presentacion';

const avisoDeRevisarPrecio = (datos?: Record<string, unknown>): Aviso => ({
  id: 'servidor:1',
  tipo: 'revisar_precio',
  grupo: 'ventas',
  prioridad: 2,
  sistemaId: 'c0000000-0000-4000-8000-000000000001',
  sistema: 'Mi almacén',
  titulo: 'Yerba vende menos desde que subiste el precio',
  detalle: 'Revisá si el nuevo precio te conviene.',
  porque: [],
  acciones: [],
  datos,
});

describe('datosDelAviso · revisar el precio', () => {
  it('muestra el precio anterior y el actual', () => {
    const datos = datosDelAviso(avisoDeRevisarPrecio({ precioAnterior: 1000, precioNuevo: 1200 }));

    expect(datos.map((dato) => dato.etiqueta)).toEqual(['Precio anterior', 'Precio actual']);
  });

  it('no muestra nada si el aviso no trae los precios', () => {
    expect(datosDelAviso(avisoDeRevisarPrecio())).toEqual([]);
    expect(datosDelAviso(avisoDeRevisarPrecio({ precioAnterior: 1000 }))).toEqual([]);
  });

  it('tiene su nombre de tipo', () => {
    expect(NOMBRE_DE_TIPO.revisar_precio).toBe('Revisar el precio');
  });
});

describe('avisos de lectura rápida', () => {
  it('suaviza el «pedí» del motor de avisos', () => {
    expect(suavizarTexto('Te quedan 1. Para no quedarte corto, pedí 1,5.')).toBe(
      'Te quedan 1. Te conviene pedir 1,5 para no quedarte sin stock.',
    );
    expect(suavizarTexto('Sin cambios.')).toBe('Sin cambios.');
  });

  it('dice cuándo se detectó, con mayúscula', () => {
    const ahora = new Date('2026-10-21T12:00:00Z');
    expect(cuandoSeDetecto('2026-10-21T08:00:00Z', ahora)).toBe('Hoy');
    expect(cuandoSeDetecto('2026-10-19T12:00:00Z', ahora)).toBe('Hace 2 días');
    expect(cuandoSeDetecto(undefined, ahora)).toBeUndefined();
  });

  it('junta los avisos del mismo tipo y nivel, sin importar el negocio', () => {
    const uno = { ...avisoDeRevisarPrecio(), id: 'a' };
    const dos = { ...avisoDeRevisarPrecio(), id: 'b' };
    const otroNegocio = { ...avisoDeRevisarPrecio(), id: 'c', sistemaId: 'otro' };
    const otroNivel = { ...avisoDeRevisarPrecio(), id: 'd', prioridad: 1 as const };

    const grupos = agruparAvisos([uno, otroNegocio, otroNivel, dos]);

    expect(grupos.map((grupo) => grupo.avisos.map((aviso) => aviso.id))).toEqual([
      ['a', 'c', 'b'],
      ['d'],
    ]);
  });

  it('pone el número adelante en el título de cada grupo', () => {
    expect(tituloDeGrupo('stock', 3, 2)).toBe('2 productos sin stock');
    expect(tituloDeGrupo('stock', 2, 5)).toBe('5 productos bajo el mínimo');
    expect(tituloDeGrupo('precio_zona', 2, 3)).toBe('3 productos casi al costo');
    expect(tituloDeGrupo('precio_zona', 1, 3)).toBe(
      '3 productos con precio distinto al de la zona',
    );
    expect(tituloDeGrupo('estancado', 2, 2)).toBe('2 productos caros que no se mueven');
    expect(tituloDeGrupo('muy_vendido', 1, 2)).toBe('2 productos muy vendidos con precio bajo');
    expect(tituloDeGrupo('combo', 1, 2)).toBe('2 combos para armar');
    expect(tituloDeGrupo('consumo_zona', 1, 2)).toBe('2 productos de la zona que no tenés');
    expect(tituloDeGrupo('revisar_precio', 2, 4)).toBe('4 precios para revisar');
    expect(tituloDeGrupo('stock', 3, 1)).toBe('1 producto sin stock');
  });

  it('deja para el detalle lo que no dice la tarjeta, sin repetir el resumen ni el «Te conviene pedir»', () => {
    expect(
      restoDelAviso('No te quedan unidades y tu mínimo es 6. Para no quedarte corto, pedí 5.'),
    ).toBe('');
    expect(restoDelAviso('Vendiste 0 en 30 días. Mirá el precio.')).toBe('Mirá el precio.');
  });

  it('muestra una sola razón por aviso, sin la aclaración de que el dato es agregado', () => {
    const base = { ...avisoDeRevisarPrecio(), tipo: 'estancado' as const };
    expect(
      razonDelAviso({
        ...base,
        porque: [
          'En tu zona, 9 comercios lo venden entre $ 1.050 y $ 1.250. Es un dato agregado: no se sabe quién cobra qué.',
          'Tenés 25 en stock.',
        ],
      }),
    ).toBe('En tu zona, 9 comercios lo venden entre $ 1.050 y $ 1.250.');
    expect(
      razonDelAviso({ ...base, tipo: 'stock', porque: ['Te alcanza para 6 días.', 'Pedí 7.'] }),
    ).toBe('Te alcanza para 6 días.');
    expect(razonDelAviso({ ...base, porque: [] })).toBeUndefined();
  });

  it('resume el aviso en una sola idea corta', () => {
    expect(resumenDelAviso('Te quedan 4 y tu mínimo es 10. Para no quedarte corto, pedí 23.')).toBe(
      'Te quedan 4 y tu mínimo es 10.',
    );
    expect(
      resumenDelAviso(
        'Lo tenés a $ 2.600, 37 % más que el promedio de tu zona, y no lo vendiste en 30 días.',
      ),
    ).toBe('Lo tenés a $ 2.600.');
    expect(resumenDelAviso('Vendiste 0 en 30 días.')).toBe('Vendiste 0 en 30 días.');
    expect(
      resumenDelAviso(
        'Una frase larga sin comas que sigue y sigue hasta pasar el límite de caracteres',
      ),
    ).toBe('Una frase larga sin comas que sigue y sigue hasta pasar el límite de caracteres');
  });
});
