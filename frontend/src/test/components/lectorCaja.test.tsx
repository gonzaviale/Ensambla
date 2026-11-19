import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { LectorCaja } from '@/components/caja/LectorCaja';

type Propiedades = Parameters<typeof LectorCaja>[0];
type Vista = ReturnType<typeof render>;

const video = createRef<HTMLVideoElement>();

const PROPIEDADES: Propiedades = {
  codigo: '',
  buscando: false,
  abierta: true,
  iniciando: false,
  sinLectura: false,
  sesion: 1,
  camaras: [],
  camaraId: '',
  camaraActiva: '',
  camaraActualId: '',
  errorCamara: '',
  video,
  entradaCodigo: createRef<HTMLInputElement>(),
  onCodigo: vi.fn(),
  onBuscar: vi.fn(),
  onAbrir: vi.fn(),
  onCerrar: vi.fn(),
  onReiniciar: vi.fn(),
  onCamara: vi.fn(),
  revisandoProducto: false,
  errorCodigo: '',
  mensaje: '',
  habilitado: true,
  lectura: null,
};

let vista: Vista;
let reproduccion: HTMLElement;

describe('lector de caja compacto', () => {
  it('conserva el video al mostrar ayuda durante una lectura continua', () => {
    dadoUnLectorAbierto();

    cuandoSeMuestraLaAyudaDeSinLectura();

    entoncesSeConservaElMismoVideo();
  });

  it('pide alejar el teléfono cuando no hay lectura', () => {
    dadoUnLectorAbierto();

    cuandoSeMuestraLaAyudaDeSinLectura();

    entoncesSePideAlejarElTelefono();
  });

  it('conserva el video al reconocer lecturas seguidas', () => {
    dadoUnLectorAbierto();

    cuandoSeReconocenDosLecturas();

    entoncesSeConservaElMismoVideo();
  });
});

const vistaPrevia = () => screen.getByLabelText('Vista previa del lector');

function dadoUnLectorAbierto() {
  vista = render(<LectorCaja {...PROPIEDADES} />);
  reproduccion = vistaPrevia();
}

function cuandoSeMuestraLaAyudaDeSinLectura() {
  vista.rerender(<LectorCaja {...PROPIEDADES} sinLectura mensaje="Coca-Cola agregada" />);
}

function cuandoSeReconocenDosLecturas() {
  vista.rerender(<LectorCaja {...PROPIEDADES} lectura={{ id: 1, tipo: 'reconocido' }} />);
  vista.rerender(<LectorCaja {...PROPIEDADES} lectura={{ id: 2, tipo: 'reconocido' }} />);
}

function entoncesSeConservaElMismoVideo() {
  expect(video.current).toBe(reproduccion);
  expect(vistaPrevia()).toBe(reproduccion);
}

function entoncesSePideAlejarElTelefono() {
  expect(screen.getByRole('status')).toHaveTextContent('Alejá un poco el teléfono');
  expect(screen.getByText('Lector activo')).toBeVisible();
}
