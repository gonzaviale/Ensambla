import { beforeEach, describe, expect, it } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import {
  seleccionarBloqueElegido,
  seleccionarSeccionActiva,
  seleccionarSistemaActivo,
  useSistemaStore,
} from '@/stores/sistemaStore';

const store = () => useSistemaStore.getState();

function conUnSistema() {
  const sistema = makeTemplate('shop', {}, true);
  store().hidratar('usuario-1', [sistema]);
  store().abrirSistema(sistema.id, 'build');
  return sistema;
}

describe('store del taller', () => {
  beforeEach(() => store().reiniciar());

  it('carga los sistemas de la cuenta y no pisa lo editado si es la misma persona', () => {
    const sistema = conUnSistema();
    store().commit((s) => {
      s.name = 'Editado';
    });

    store().hidratar('usuario-1', [sistema]);

    expect(seleccionarSistemaActivo(store())?.name).toBe('Editado');
  });

  it('suma los sistemas que creó el servidor sin repetir ni pisar los que ya estaban', () => {
    const sistema = conUnSistema();
    const nuevo = { ...makeTemplate('business', {}, true), id: 'creado-por-sami' };

    store().incorporarSistemas([{ ...sistema, name: 'Viejo' }, nuevo]);

    expect(store().sistemas.map((item) => item.id)).toEqual([sistema.id, 'creado-por-sami']);
    expect(store().sistemas[0]?.name).not.toBe('Viejo');
    expect(store().pendientes).toEqual([]);
  });

  it('no vuelve a sumar un sistema que la persona acaba de eliminar', () => {
    const sistema = conUnSistema();
    store().eliminarSistema(sistema.id);

    store().incorporarSistemas([sistema]);

    expect(store().sistemas).toEqual([]);
  });

  it('trae lo que el servidor tiene de más en un sistema que la pestaña no cambió', () => {
    const sistema = conUnSistema();
    const delServidor = { ...sistema, name: 'Con filas de WhatsApp' };

    store().actualizarDesdeServidor([delServidor]);

    expect(store().sistemas[0]?.name).toBe('Con filas de WhatsApp');
    expect(store().pendientes).toEqual([]);
  });

  it('no pisa un sistema con cambios sin guardar al traer lo del servidor', () => {
    const sistema = conUnSistema();
    store().commit((s) => {
      s.name = 'Editado acá';
    });

    store().actualizarDesdeServidor([{ ...sistema, name: 'Del servidor' }]);

    expect(store().sistemas[0]?.name).toBe('Editado acá');
    expect(store().pendientes).toContain(sistema.id);
  });

  it('al traer lo del servidor suma los sistemas nuevos y respeta los que se eliminaron', () => {
    const sistema = conUnSistema();
    const nuevo = { ...makeTemplate('business', {}, true), id: 'creado-por-sami' };
    const eliminado = { ...makeTemplate('business', {}, true), id: 'eliminado' };
    store().agregarSistema(eliminado);
    store().eliminarSistema(eliminado.id);

    store().actualizarDesdeServidor([sistema, nuevo, eliminado]);

    expect(store().sistemas.map((item) => item.id)).toEqual([sistema.id, 'creado-por-sami']);
  });

  it('si el servidor no trae nada distinto no toca el estado', () => {
    const sistema = conUnSistema();
    const antes = store().sistemas;

    store().actualizarDesdeServidor([structuredClone(sistema)]);

    expect(store().sistemas).toBe(antes);
  });

  it('cambiar de cuenta reemplaza todo lo que había', () => {
    conUnSistema();

    store().hidratar('usuario-2', []);

    expect(store().sistemas).toEqual([]);
    expect(store().activeId).toBeNull();
    expect(store().usuarioId).toBe('usuario-2');
  });

  it('abrir un sistema elige su primera sección y limpia la selección y el historial', () => {
    const sistema = conUnSistema();
    store().seleccionar('algo');
    store().commit((s) => {
      s.name = 'x';
    });

    store().abrirSistema(sistema.id, 'use', true);

    expect(store()).toMatchObject({ modo: 'use', selectedId: null, historial: [], listo: true });
    expect(seleccionarSeccionActiva(store())?.id).toBe(sistema.sections[0]?.id);
  });

  it('un cambio se aplica sobre una copia, queda pendiente y se puede deshacer', () => {
    const sistema = conUnSistema();
    const original = store().sistemas[0];

    const resultado = store().commit((s) => {
      s.name = 'Mi almacén nuevo';
      return 42;
    });

    expect(resultado).toBe(42);
    expect(sistema.name).toBe('Mi almacén');
    expect(original?.name).toBe('Mi almacén');
    expect(seleccionarSistemaActivo(store())?.name).toBe('Mi almacén nuevo');
    expect(store().pendientes).toEqual([sistema.id]);
    expect(store().deshacer()).toBe(true);
    expect(seleccionarSistemaActivo(store())?.name).toBe('Mi almacén');
    expect(store().deshacer()).toBe(false);
  });

  it('el historial guarda como máximo 30 pasos', () => {
    conUnSistema();

    for (let i = 0; i < 40; i++) {
      store().commit((s) => {
        s.name = `Nombre ${i}`;
      });
    }

    expect(store().historial).toHaveLength(30);
  });

  it('quitar un sistema lo marca para borrar y no lo deja pendiente de guardar', () => {
    const sistema = conUnSistema();
    store().commit((s) => {
      s.name = 'x';
    });

    store().eliminarSistema(sistema.id);

    expect(store().sistemas).toEqual([]);
    expect(store().pendientes).toEqual([]);
    expect(store().eliminados).toEqual([sistema.id]);
    expect(store().activeId).toBeNull();
  });

  it('marcar guardados limpia solo lo enviado', () => {
    const a = makeTemplate('shop', {}, false);
    const b = makeTemplate('warehouse', {}, false);
    store().hidratar('usuario-1', []);
    store().agregarSistema(a);
    store().agregarSistema(b);

    store().marcarGuardados([a.id], []);

    expect(store().pendientes).toEqual([b.id]);
  });

  it('elige una sección y un bloque, y los lectores los encuentran', () => {
    const sistema = conUnSistema();
    const bloque = sistema.sections[0]?.blocks[0];
    if (!bloque) throw new Error('Falta un bloque');

    store().seleccionar(bloque.id);

    expect(seleccionarBloqueElegido(store())?.id).toBe(bloque.id);
    store().elegirSeccion(sistema.sections[0]?.id ?? '');
    expect(store().selectedId).toBeNull();
  });

  it('deshacer una sección que ya no existe vuelve a la primera', () => {
    const sistema = conUnSistema();
    store().commit((s) => {
      s.sections.push({ id: 'otra', name: 'Otra', blocks: [] });
    });
    store().elegirSeccion('otra');

    store().deshacer();

    expect(store().sectionId).toBe(sistema.sections[0]?.id);
  });

  it('los avisos tienen un número que cambia cada vez', () => {
    store().avisar('Uno');
    const primero = store().aviso?.id ?? 0;
    store().avisar('Uno');

    expect(store().aviso?.id).toBeGreaterThan(primero);
    store().cerrarAviso();
    expect(store().aviso).toBeNull();
  });
});
