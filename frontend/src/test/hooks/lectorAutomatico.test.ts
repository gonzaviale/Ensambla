import { act, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useLectorAutomatico } from '@/hooks/escaneo/useLectorAutomatico';
import {
  useConfirmacionLectura,
  type ConfirmacionLectura,
} from '@/hooks/escaneo/useConfirmacionLectura';

type Estado = { habilitado: boolean; abierta?: boolean; iniciando?: boolean };

let abrir: ReturnType<typeof vi.fn>;
let cerrar: ReturnType<typeof vi.fn>;
let visibilidad: ReturnType<typeof vi.spyOn>;
let lector: ReturnType<typeof renderHook<void, Estado>>;
let confirmacion: ReturnType<
  typeof renderHook<ConfirmacionLectura | null, { lectura: ConfirmacionLectura | null }>
>;

afterEach(() => {
  vi.restoreAllMocks();
  vi.useRealTimers();
});

describe('lector automático de caja', () => {
  it('abre al entrar', () => {
    dadoUnLectorHabilitado();

    entoncesSeAbrioLaCamara(1);
  });

  it('no reintenta los permisos en cada render', () => {
    dadoUnLectorHabilitado();

    cuandoSeVuelveARenderizar({ habilitado: true });

    entoncesSeAbrioLaCamara(1);
  });

  it('cierra al salir a un panel', () => {
    dadoUnLectorHabilitado();

    cuandoSeVuelveARenderizar({ habilitado: false });

    entoncesSeCerroLaCamara();
  });

  it('vuelve a abrir al regresar de un panel', () => {
    dadoUnLectorHabilitado();
    cuandoSeVuelveARenderizar({ habilitado: false });

    cuandoSeVuelveARenderizar({ habilitado: true });

    entoncesSeAbrioLaCamara(2);
  });

  it('libera la cámara al ocultar la app', () => {
    dadoUnLectorConLaAppVisible();

    cuandoSeCambiaLaVisibilidadA('hidden');

    entoncesSeCerroLaCamaraUnaVez();
  });

  it('vuelve a abrir la cámara al regresar a la app', () => {
    dadoUnLectorConLaAppVisible();
    cuandoSeCambiaLaVisibilidadA('hidden');

    cuandoSeCambiaLaVisibilidadA('visible');

    entoncesSeAbrioLaCamara(2);
  });

  it('no reinicia una cámara que ya está abierta al elegir otro dispositivo', () => {
    dadoUnLectorDeshabilitado();

    cuandoSeVuelveARenderizar({ habilitado: true, abierta: true });

    entoncesNoSeReinicioLaCamara();
  });

  it('retoma la cámara aunque se haya ocultado mientras todavía estaba iniciando', () => {
    dadoUnLectorDeshabilitadoQueEstabaIniciando();

    cuandoSeVuelveARenderizar({ habilitado: true, abierta: false, iniciando: true });

    entoncesSeAbrioLaCamara(1);
  });
});

describe('confirmación de lectura', () => {
  it('renueva el marco por 1,4 segundos sin que el temporizador anterior borre una lectura rápida', () => {
    dadaUnaLecturaReconocida();

    cuandoLlegaOtraLecturaMedioSegundoDespues();

    entoncesElMarcoSigueConLaNuevaLectura();
  });

  it('borra el marco cuando pasó el tiempo de la última lectura', () => {
    dadaUnaLecturaReconocida();
    cuandoLlegaOtraLecturaMedioSegundoDespues();

    cuandoPasan(1400);

    entoncesYaNoHayMarco();
  });
});

// --- dado ---

function useLectorDePrueba(estado: Estado) {
  return useLectorAutomatico(estado.habilitado, {
    abrir: () => abrir(),
    cerrar: () => cerrar(),
    abierta: estado.abierta ?? false,
    iniciando: estado.iniciando ?? false,
  });
}

function dadoUnLector(inicial: Estado) {
  abrir = vi.fn();
  cerrar = vi.fn();
  lector = renderHook((estado: Estado) => useLectorDePrueba(estado), { initialProps: inicial });
}

function dadoUnLectorHabilitado() {
  dadoUnLector({ habilitado: true });
}

function dadoUnLectorDeshabilitado() {
  dadoUnLector({ habilitado: false, abierta: false });
  cerrar.mockClear();
}

function dadoUnLectorDeshabilitadoQueEstabaIniciando() {
  dadoUnLector({ habilitado: false, abierta: false, iniciando: true });
}

function dadoUnLectorConLaAppVisible() {
  visibilidad = vi.spyOn(document, 'visibilityState', 'get');
  visibilidad.mockReturnValue('visible');
  dadoUnLector({ habilitado: true });
}

function dadaUnaLecturaReconocida() {
  vi.useFakeTimers();
  confirmacion = renderHook(
    ({ lectura }: { lectura: ConfirmacionLectura | null }) => useConfirmacionLectura(lectura),
    { initialProps: { lectura: { id: 1, tipo: 'reconocido' } as ConfirmacionLectura | null } },
  );
}

// --- cuando ---

function cuandoSeVuelveARenderizar(estado: Estado) {
  lector.rerender(estado);
}

function cuandoSeCambiaLaVisibilidadA(valor: 'visible' | 'hidden') {
  visibilidad.mockReturnValue(valor);
  act(() => document.dispatchEvent(new Event('visibilitychange')));
}

function cuandoLlegaOtraLecturaMedioSegundoDespues() {
  act(() => vi.advanceTimersByTime(500));
  confirmacion.rerender({ lectura: { id: 2, tipo: 'reconocido' } });
  act(() => vi.advanceTimersByTime(900));
}

function cuandoPasan(milisegundos: number) {
  act(() => vi.advanceTimersByTime(milisegundos - 900));
}

// --- entonces ---

function entoncesSeAbrioLaCamara(veces: number) {
  expect(abrir).toHaveBeenCalledTimes(veces);
}

function entoncesSeCerroLaCamara() {
  expect(cerrar).toHaveBeenCalled();
}

function entoncesSeCerroLaCamaraUnaVez() {
  expect(cerrar).toHaveBeenCalledOnce();
}

function entoncesNoSeReinicioLaCamara() {
  expect(abrir).not.toHaveBeenCalled();
  expect(cerrar).not.toHaveBeenCalled();
}

function entoncesElMarcoSigueConLaNuevaLectura() {
  expect(confirmacion.result.current?.id).toBe(2);
}

function entoncesYaNoHayMarco() {
  expect(confirmacion.result.current).toBeNull();
}
