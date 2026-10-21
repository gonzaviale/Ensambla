import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { ESPERA, verifyHeadingPresentAsync } from '@/test/testHelpers';

async function abrirLanding() {
  const router = renderApp('/');
  await verifyHeadingPresentAsync('Tu cuaderno de siempre. Tus cuentas, más claras.');
  return { router, user: userEvent.setup() };
}

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe('landing del cuaderno digital', () => {
  it('presenta un solo h1 y sus llamadas principales abren el registro real', async () => {
    const { router, user } = await abrirLanding();

    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    const enlaces = screen.getAllByRole('link', { name: /Empezar mi cuaderno/ });
    for (const enlace of enlaces) expect(enlace).toHaveAttribute('href', '/registro');

    await user.click(enlaces[0] as HTMLElement);
    await screen.findByRole('heading', { name: 'Creá tu cuenta' }, ESPERA);
    expect(router.state.location.pathname).toBe('/registro');
    expect(screen.getByText('Empezá a armar el sistema de tu negocio.')).toBeVisible();
  });

  it('lleva al ingreso existente desde el cierre', async () => {
    const { router, user } = await abrirLanding();

    await user.click(screen.getByRole('link', { name: 'Ya tengo cuenta' }));
    await screen.findByRole('heading', { name: 'Ingresá a tu cuenta' }, ESPERA);
    expect(router.state.location.pathname).toBe('/login');
  });

  it('permite corregir un importe y encontrar el resultado confirmado', async () => {
    const { user } = await abrirLanding();
    await user.click(screen.getByRole('button', { name: 'Revisás lo leído' }));
    const importe = screen.getByLabelText('Importe del fiado');
    await user.clear(importe);
    await user.type(importe, '4500.5');
    await user.click(screen.getByRole('button', { name: /Confirmar y buscar/ }));
    const resultados = screen.getByRole('list', { name: 'Fiados encontrados' });
    expect(within(resultados).getByText('Ana López')).toBeVisible();
    expect(resultados).toHaveTextContent(/4\.500,5/);
    expect(within(resultados).getAllByRole('listitem')).toHaveLength(1);
    expect(screen.getByRole('button', { name: 'Lo encontrás' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(localStorage.length).toBe(0);
  });

  it('la búsqueda encuentra por nombre sin depender de mayúsculas o tildes y resuelve el vacío', async () => {
    const { user } = await abrirLanding();
    await user.click(screen.getByRole('button', { name: 'Lo encontrás' }));
    const buscar = screen.getByRole('searchbox', { name: 'Buscá un nombre en este ejemplo' });
    const lista = screen.getByRole('list', { name: 'Fiados encontrados' });
    await user.clear(buscar);
    expect(within(lista).getAllByRole('listitem')).toHaveLength(3);
    await user.type(buscar, 'LOPEZ');
    expect(lista).toHaveTextContent('Ana López');
    expect(within(lista).getAllByRole('listitem')).toHaveLength(1);
    await user.clear(buscar);
    await user.type(buscar, 'nadie');
    expect(within(lista).queryByRole('listitem')).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('No hay coincidencias');
    await user.clear(buscar);
    await user.type(buscar, 'luci');
    expect(lista).toHaveTextContent('Lucía Pérez');
  });

  it('rechaza un importe vacío o no positivo sin pasar a la confirmación', async () => {
    const { user } = await abrirLanding();
    await user.click(screen.getByRole('button', { name: 'Revisás lo leído' }));
    const importe = screen.getByLabelText('Importe del fiado');
    await user.clear(importe);
    await user.type(importe, '0');
    await user.click(screen.getByRole('button', { name: /Confirmar y buscar/ }));
    expect(screen.getByRole('alert')).toHaveTextContent('Ingresá un importe mayor a cero.');
    expect(importe).toHaveFocus();
    expect(importe).toHaveAttribute('aria-invalid', 'true');
    expect(screen.queryByRole('searchbox')).not.toBeInTheDocument();
  });

  it('permite recorrer los pasos del ejemplo con el teclado', async () => {
    const { user } = await abrirLanding();
    screen.getByRole('button', { name: 'Lo tenés en papel' }).focus();
    await user.keyboard('{Tab}{Enter}');
    expect(screen.getByRole('button', { name: 'Revisás lo leído' })).toHaveFocus();
    expect(screen.getByLabelText('Importe del fiado')).toBeVisible();
  });

  it('cada enlace interno tiene un destino y el ejemplo se alcanza desde la primera pantalla', async () => {
    await abrirLanding();

    expect(screen.getByRole('link', { name: /Ver un ejemplo/ })).toHaveAttribute(
      'href',
      '#como-funciona',
    );
    const inicio = document.getElementById('inicio') as HTMLElement;
    expect(within(inicio).getByRole('link', { name: /Ana López/ })).toHaveAttribute(
      'href',
      '#como-funciona',
    );
    for (const enlace of document.querySelectorAll<HTMLAnchorElement>('a[href^="#"]')) {
      expect(
        document.getElementById(decodeURIComponent(enlace.hash.slice(1))),
        enlace.hash,
      ).not.toBeNull();
    }
  });

  it('identifica la descarga como pendiente sin ofrecer una acción falsa', async () => {
    const { user } = await abrirLanding();
    await user.click(screen.getByText('¿Puedo descargar una copia de mis datos?'));
    const seccion = document.getElementById('tus-datos') as HTMLElement;

    expect(within(seccion).getByText(/Todavía no/)).toBeVisible();
    expect(within(seccion).queryByRole('button')).not.toBeInTheDocument();
    expect(within(seccion).queryByRole('link')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Publicar comentario' })).not.toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Voz' })).not.toBeInTheDocument();
  });

  it('guía el ejemplo con acciones de avance y lleva el foco al campo del nuevo paso', async () => {
    const { user } = await abrirLanding();
    await user.click(screen.getByRole('button', { name: 'Comenzar el ejemplo' }));
    expect(screen.getByLabelText('Importe del fiado')).toHaveFocus();
    expect(screen.getByText('Paso 2 de 3')).toBeVisible();
    await user.click(screen.getByRole('button', { name: /Confirmar y buscar/ }));
    expect(screen.getByRole('searchbox')).toHaveFocus();
    expect(screen.getByText('Paso 3 de 3')).toBeVisible();
  });

  it('abre y cierra las preguntas sin salir de la página', async () => {
    const { user, router } = await abrirLanding();
    const pregunta = screen.getByText('¿Tengo que dejar de usar mi cuaderno?');
    const detalle = pregunta.closest('details');

    expect(detalle).not.toHaveAttribute('open');
    await user.click(pregunta);
    expect(detalle).toHaveAttribute('open');
    expect(screen.getByText(/Podés seguir anotando como hasta ahora/)).toBeVisible();
    await user.click(pregunta);
    expect(detalle).not.toHaveAttribute('open');
    expect(router.state.location.pathname).toBe('/');
  });

  it('abre el menú de secciones y lo cierra con Escape', async () => {
    const { user } = await abrirLanding();
    const boton = screen.getByRole('button', { name: 'Abrir menú' });

    await user.click(boton);
    expect(screen.getByRole('navigation', { name: 'Menú de secciones' })).toBeVisible();
    await user.keyboard('{Escape}');
    expect(boton).toHaveAttribute('aria-expanded', 'false');
    expect(boton).toHaveFocus();
  });

  it('el botón de volver arriba y el logo conservan su navegación local', async () => {
    const irA = vi.fn();
    vi.stubGlobal('scrollTo', irA);
    const { user } = await abrirLanding();
    irA.mockClear();

    await user.click(screen.getByRole('link', { name: 'Ensambla, ir al inicio' }));
    expect(irA).toHaveBeenCalledWith(expect.objectContaining({ top: 0 }));
    irA.mockClear();

    await user.click(screen.getByRole('button', { name: 'Volver arriba', hidden: true }));
    expect(irA).toHaveBeenCalledWith(expect.objectContaining({ top: 0 }));
  });

  it('no tiene problemas de accesibilidad detectables sin layout', async () => {
    await abrirLanding();
    const resultado = await axe(document.body, {
      rules: { 'color-contrast': { enabled: false } },
    });
    expect(resultado.violations.map((v) => v.id)).toEqual([]);
  });
});
