import { describe, it } from 'vitest';
import type { Aviso, TipoDeAviso } from '@/lib/sistema/alertas';
import { sugerenciasDeLaHome } from '@/lib/sistema/sugerenciasDeLaHome';
import { cuandoSeCalcula, entoncesElResultadoEs } from '@/test/calculo';

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

const sugeridas = (avisos: Aviso[], maximo?: number) =>
  sugerenciasDeLaHome(avisos, maximo).map((item) => item.id);

describe('sugerenciasDeLaHome', () => {
  it('toma la más urgente de cada tema aunque otros avisos sean más urgentes', () => {
    const avisos = [
      aviso('falta-1', 'stock', 3),
      aviso('falta-2', 'stock', 3),
      aviso('falta-3', 'stock', 2),
      aviso('parado', 'estancado', 1),
      aviso('precio', 'precio_zona', 1),
    ];

    cuandoSeCalcula(() => sugeridas(avisos));

    entoncesElResultadoEs(['falta-1', 'parado', 'precio']);
  });

  it('«se vende mucho» es del tema de precio frente a la zona', () => {
    const avisos = [aviso('falta', 'stock', 3), aviso('barato', 'muy_vendido', 1)];

    cuandoSeCalcula(() => sugeridas(avisos));

    entoncesElResultadoEs(['falta', 'barato']);
  });

  it('«revisar el precio» es del tema de precio frente a la zona', () => {
    const avisos = [aviso('falta', 'stock', 3), aviso('revisar', 'revisar_precio', 1)];

    cuandoSeCalcula(() => sugeridas(avisos));

    entoncesElResultadoEs(['falta', 'revisar']);
  });

  it('si falta algún tema, completa con las siguientes más urgentes', () => {
    const avisos = [
      aviso('falta-1', 'stock', 3),
      aviso('falta-2', 'stock', 2),
      aviso('falta-3', 'stock', 2),
      aviso('falta-4', 'stock', 1),
    ];

    cuandoSeCalcula(() => sugeridas(avisos));

    entoncesElResultadoEs(['falta-1', 'falta-2', 'falta-3']);
  });

  it('respeta el orden de urgencia y no repite avisos', () => {
    const avisos = [
      aviso('combo', 'combo', 3),
      aviso('falta', 'stock', 2),
      aviso('parado', 'estancado', 2),
      aviso('precio', 'precio_zona', 1),
    ];

    cuandoSeCalcula(() => sugeridas(avisos));

    entoncesElResultadoEs(['falta', 'parado', 'precio']);
  });

  it('con pocos avisos devuelve los que hay', () => {
    cuandoSeCalcula(() => sugeridas([aviso('solo', 'combo', 1)]));

    entoncesElResultadoEs(['solo']);
  });

  it('sin avisos no sugiere ninguno', () => {
    cuandoSeCalcula(() => sugeridas([]));

    entoncesElResultadoEs([]);
  });

  it('el máximo se puede cambiar', () => {
    const avisos = [
      aviso('a', 'stock', 3),
      aviso('b', 'estancado', 2),
      aviso('c', 'precio_zona', 1),
    ];

    cuandoSeCalcula(() => sugeridas(avisos, 2));

    entoncesElResultadoEs(['a', 'b']);
  });
});
