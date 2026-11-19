import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { HerramientasDelSistema } from '@/components/sistema/editor/HerramientasDelSistema';

type Usuario = ReturnType<typeof userEvent.setup>;

let user: Usuario;
const crearAcciones = () => ({
  onDistribucion: vi.fn(),
  onBloques: vi.fn(),
  onSecciones: vi.fn(),
  onNuevaSeccion: vi.fn(),
  onFoto: vi.fn(),
  onResumen: vi.fn(),
});

let acciones: ReturnType<typeof crearAcciones>;

describe('HerramientasDelSistema', () => {
  it('son dos íconos y cada uno despliega lo suyo al tocarlo', async () => {
    dadasLasHerramientas();

    await cuandoSeToca(agregar());

    entoncesElPanelEstaAbierto(agregar());
  });

  it('abrir uno cierra el otro', async () => {
    dadasLasHerramientas();
    await cuandoSeToca(agregar());

    await cuandoSeToca(distribucion());

    entoncesElPanelEstaCerrado(agregar());
    entoncesElPanelEstaAbierto(distribucion());
  });

  it('elegir una opción hace lo que dice', async () => {
    dadasLasHerramientas();
    await cuandoSeToca(agregar());

    await cuandoSeToca(screen.getByRole('button', { name: 'Bloques' }));

    entoncesSeLlamo('onBloques');
  });

  it('elegir una opción cierra el panel', async () => {
    dadasLasHerramientas();
    await cuandoSeToca(agregar());

    await cuandoSeToca(screen.getByRole('button', { name: 'Bloques' }));

    entoncesElPanelEstaCerrado(agregar());
  });

  it('el resumen en PDF también hace lo que dice', async () => {
    dadasLasHerramientas();
    await cuandoSeToca(agregar());

    await cuandoSeToca(screen.getByRole('button', { name: /Resumen en PDF/ }));

    entoncesSeLlamo('onResumen');
  });

  it('los mosaicos se eligen desde el segundo ícono', async () => {
    dadasLasHerramientas();
    await cuandoSeToca(distribucion());

    await cuandoSeToca(screen.getByRole('button', { name: /2 columnas/ }));

    entoncesSeEligioLaDistribucion('two');
  });

  it('Escape cierra el panel', async () => {
    dadasLasHerramientas();
    await cuandoSeToca(agregar());

    await cuandoSePresiona('{Escape}');

    entoncesElPanelEstaCerrado(agregar());
  });

  it('tocar afuera cierra el panel', async () => {
    dadasLasHerramientas();
    await cuandoSeToca(agregar());

    await cuandoSeToca(document.body);

    entoncesElPanelEstaCerrado(agregar());
  });
});

const agregar = () => screen.getByRole('button', { name: 'Agregar y organizar' });
const distribucion = () => screen.getByRole('button', { name: 'Distribución de los bloques' });

function dadasLasHerramientas() {
  acciones = crearAcciones();
  render(<HerramientasDelSistema distribucion="mosaic" {...acciones} />);
  user = userEvent.setup();
}

async function cuandoSeToca(elemento: HTMLElement) {
  await user.click(elemento);
}

async function cuandoSePresiona(tecla: string) {
  await user.keyboard(tecla);
}

function entoncesElPanelEstaAbierto(boton: HTMLElement) {
  expect(boton).toHaveAttribute('aria-expanded', 'true');
}

function entoncesElPanelEstaCerrado(boton: HTMLElement) {
  expect(boton).toHaveAttribute('aria-expanded', 'false');
}

function entoncesSeLlamo(accion: 'onBloques' | 'onResumen') {
  expect(acciones[accion]).toHaveBeenCalledOnce();
}

function entoncesSeEligioLaDistribucion(distribucionElegida: string) {
  expect(acciones.onDistribucion).toHaveBeenCalledWith(distribucionElegida);
}
