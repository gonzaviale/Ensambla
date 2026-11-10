import { describe, expect, it } from 'vitest';
import type { Aviso, TipoDeAviso } from '@/lib/sistema/alertas';
import { sugerenciasDeLaHome } from '@/lib/sistema/sugerenciasDeLaHome';

const aviso = (id: string, tipo: TipoDeAviso, prioridad: 1 | 2 | 3): Aviso => ({
  id,
  tipo,
  grupo: 'stock',
  prioridad,
  sistemaId: 's1',
  sistema: 'Almacén',
  titulo: id,
  detalle: '',
  porque: [],
  acciones: [],
});

const ids = (avisos: Aviso[]) => avisos.map((item) => item.id);

describe('sugerenciasDeLaHome', () => {
  it('toma la más urgente de cada tema aunque otros avisos sean más urgentes', () => {
    const avisos = [
      aviso('falta-1', 'stock', 3),
      aviso('falta-2', 'stock', 3),
      aviso('falta-3', 'stock', 2),
      aviso('parado', 'estancado', 1),
      aviso('precio', 'precio_zona', 1),
    ];

    expect(ids(sugerenciasDeLaHome(avisos))).toEqual(['falta-1', 'parado', 'precio']);
  });

  it('«se vende mucho» y «revisar el precio» también son del tema de precio frente a la zona', () => {
    expect(
      ids(sugerenciasDeLaHome([aviso('falta', 'stock', 3), aviso('barato', 'muy_vendido', 1)])),
    ).toEqual(['falta', 'barato']);
    expect(
      ids(sugerenciasDeLaHome([aviso('falta', 'stock', 3), aviso('revisar', 'revisar_precio', 1)])),
    ).toEqual(['falta', 'revisar']);
  });

  it('si falta algún tema, completa con las siguientes más urgentes', () => {
    const avisos = [
      aviso('falta-1', 'stock', 3),
      aviso('falta-2', 'stock', 2),
      aviso('falta-3', 'stock', 2),
      aviso('falta-4', 'stock', 1),
    ];

    expect(ids(sugerenciasDeLaHome(avisos))).toEqual(['falta-1', 'falta-2', 'falta-3']);
  });

  it('respeta el orden de urgencia y no repite avisos', () => {
    const avisos = [
      aviso('combo', 'combo', 3),
      aviso('falta', 'stock', 2),
      aviso('parado', 'estancado', 2),
      aviso('precio', 'precio_zona', 1),
    ];

    expect(ids(sugerenciasDeLaHome(avisos))).toEqual(['falta', 'parado', 'precio']);
  });

  it('con pocos avisos devuelve los que hay, y sin avisos ninguno', () => {
    expect(ids(sugerenciasDeLaHome([aviso('solo', 'combo', 1)]))).toEqual(['solo']);
    expect(sugerenciasDeLaHome([])).toEqual([]);
  });

  it('el máximo se puede cambiar', () => {
    const avisos = [
      aviso('a', 'stock', 3),
      aviso('b', 'estancado', 2),
      aviso('c', 'precio_zona', 1),
    ];

    expect(ids(sugerenciasDeLaHome(avisos, 2))).toEqual(['a', 'b']);
  });
});
