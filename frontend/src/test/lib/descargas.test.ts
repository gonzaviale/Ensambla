import { afterEach, describe, expect, it, vi } from 'vitest';
import { guardarArchivo } from '@/lib/descargas';

type Descarga = { nombre: string; direccion: string; estabaEnElDocumento: boolean };

const archivo = new Blob(['%PDF-1.4'], { type: 'application/pdf' });
let crearDireccion = vi.fn();
let liberarDireccion = vi.fn();
let clic: ReturnType<typeof vi.spyOn>;
let descarga: Descarga;

describe('guardarArchivo', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('descarga el archivo con el nombre que mandó el servidor', () => {
    dadoUnNavegadorQueRecuerdaLaDescarga();

    cuandoSeGuardaElResumen();

    entoncesSeHizoClicEnUnEnlaceConElNombreDelServidor();
  });

  it('libera la dirección temporal y no deja el enlace en la página', () => {
    dadoUnNavegadorQueRecuerdaLaDescarga();

    cuandoSeGuardaElResumen();

    entoncesSeLiberaLaDireccionYNoQuedaElEnlace();
  });
});

function dadoUnNavegadorQueRecuerdaLaDescarga() {
  crearDireccion = vi.fn(() => 'blob:resumen');
  liberarDireccion = vi.fn();
  vi.stubGlobal('URL', { createObjectURL: crearDireccion, revokeObjectURL: liberarDireccion });
  clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
    this: HTMLAnchorElement,
  ) {
    descarga = {
      nombre: this.download,
      direccion: this.href,
      estabaEnElDocumento: document.body.contains(this),
    };
  });
}

function cuandoSeGuardaElResumen() {
  guardarArchivo({ blob: archivo, nombre: 'resumen-almacen-20261019.pdf' });
}

function entoncesSeHizoClicEnUnEnlaceConElNombreDelServidor() {
  expect(crearDireccion).toHaveBeenCalledWith(archivo);
  expect(clic).toHaveBeenCalledTimes(1);
  expect(descarga).toEqual({
    nombre: 'resumen-almacen-20261019.pdf',
    direccion: 'blob:resumen',
    estabaEnElDocumento: true,
  });
}

function entoncesSeLiberaLaDireccionYNoQuedaElEnlace() {
  expect(liberarDireccion).toHaveBeenCalledWith('blob:resumen');
  expect(document.querySelector('a[download]')).toBeNull();
}
