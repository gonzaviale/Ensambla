import { afterEach, describe, expect, it, vi } from 'vitest';
import type { Aviso } from '@/lib/sistema/alertas';
import { destinoDeAviso } from '@/lib/sistema/destinoDeAviso';
import { resaltar } from '@/lib/sistema/resaltar';

const aviso = (parcial: Partial<Aviso>): Aviso => ({
  id: 'a',
  tipo: 'stock',
  grupo: 'stock',
  prioridad: 2,
  sistemaId: 's1',
  sistema: 'Almacén',
  titulo: 'Aviso',
  detalle: '',
  porque: [],
  acciones: [],
  ...parcial,
});

describe('resaltar', () => {
  afterEach(() => {
    vi.useRealTimers();
    document.body.innerHTML = '';
  });

  it('pone un marco sobre el elemento y lo saca solo pasado el tiempo', () => {
    vi.useFakeTimers();
    document.body.innerHTML = '<div id="x"></div>';

    resaltar(document.getElementById('x'), { duracion: 1000 });
    expect(document.querySelectorAll('.resaltado-marco')).toHaveLength(1);

    vi.advanceTimersByTime(1000);
    expect(document.querySelector('.resaltado-marco.saliendo')).not.toBeNull();
    vi.advanceTimersByTime(500);
    expect(document.querySelector('.resaltado-marco')).toBeNull();
  });

  it('no deja dos marcos sobre el mismo elemento', () => {
    document.body.innerHTML = '<div id="x"></div>';
    const nodo = document.getElementById('x');

    resaltar(nodo);
    resaltar(nodo);

    expect(document.querySelectorAll('.resaltado-marco')).toHaveLength(1);
  });

  it('sin elemento no hace nada', () => {
    resaltar(null);
    expect(document.querySelector('.resaltado-marco')).toBeNull();
  });
});

describe('destinoDeAviso', () => {
  it('prefiere el registro del aviso a cualquier tabla', () => {
    const destino = destinoDeAviso(
      aviso({
        objeto: { sistemaId: 's1', bloqueId: 'inv', filaId: 'f1' },
        acciones: [{ tipo: 'registrar', texto: 'Registrar', sistemaId: 's1', bloqueId: 'ent' }],
      }),
    );
    expect(destino).toEqual({ sistemaId: 's1', bloqueId: 'inv', filaId: 'f1' });
  });

  it('si no hay registro, usa el de la acción que lo tenga y después la tabla', () => {
    const conFila = aviso({
      acciones: [
        { tipo: 'abrir', texto: 'Ver', sistemaId: 's1', bloqueId: 'inv' },
        {
          tipo: 'aplicar',
          texto: 'Subir',
          sistemaId: 's1',
          bloqueId: 'inv',
          filaId: 'f2',
          valores: {},
          confirmacion: '',
        },
      ],
    });
    expect(destinoDeAviso(conFila)).toMatchObject({ bloqueId: 'inv', filaId: 'f2' });

    const soloTabla = aviso({
      acciones: [{ tipo: 'registrar', texto: 'Registrar', sistemaId: 's1', bloqueId: 'ent' }],
    });
    expect(destinoDeAviso(soloTabla)).toEqual({
      sistemaId: 's1',
      bloqueId: 'ent',
      filaId: undefined,
    });
  });

  it('un aviso de la zona no apunta a ningún objeto', () => {
    const destino = destinoDeAviso(
      aviso({ tipo: 'consumo_zona', acciones: [{ tipo: 'zona', texto: 'Ver en Mi zona' }] }),
    );
    expect(destino).toBeNull();
  });
});
