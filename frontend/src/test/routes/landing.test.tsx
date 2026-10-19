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
    expect(screen.getByText('Empezá a ordenar las anotaciones de tu negocio.')).toBeVisible();
  });

  it('lleva al ingreso existente desde el cierre', async () => {
    const { router, user } = await abrirLanding();

    await user.click(screen.getByRole('link', { name: 'Ya tengo cuenta' }));
    await screen.findByRole('heading', { name: 'Ingresá a tu cuenta' }, ESPERA);
    expect(router.state.location.pathname).toBe('/login');
  });

  it('el ejemplo cambia de paso, comunica la revisión y permite volver al inicio', async () => {
    const { user } = await abrirLanding();
    const foto = screen.getByRole('button', { name: 'Sacás una foto' });
    const revisar = screen.getByRole('button', { name: 'Revisás los datos' });
    const guardar = screen.getByRole('button', { name: 'Guardás en tu cuaderno' });

    expect(foto).toHaveAttribute('aria-pressed', 'true');
    await user.click(revisar);
    expect(revisar).toHaveAttribute('aria-pressed', 'true');
    expect(foto).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByRole('status')).toHaveTextContent('Vos tenés la última palabra.');
    expect(screen.getByRole('table')).toHaveTextContent('Yerba 500 g');

    await user.click(guardar);
    expect(screen.getByRole('status')).toHaveTextContent('Listo para volver a consultarlo.');
    expect(screen.getByText('Ejemplo: anotaciones confirmadas en tu cuaderno.')).toBeVisible();

    await user.click(foto);
    expect(screen.getByRole('status')).toHaveTextContent('Tu punto de partida.');
    expect(foto).toHaveAttribute('aria-pressed', 'true');
    expect(localStorage.length).toBe(0);
  });

  it('permite recorrer el ejemplo con el teclado', async () => {
    const { user } = await abrirLanding();

    screen.getByRole('button', { name: 'Sacás una foto' }).focus();
    await user.keyboard('{Tab}{Enter}');
    expect(screen.getByRole('button', { name: 'Revisás los datos' })).toHaveFocus();
    expect(screen.getByRole('status')).toHaveTextContent('Vos tenés la última palabra.');
  });

  it('cada enlace interno tiene un destino y el ejemplo se alcanza desde la primera pantalla', async () => {
    await abrirLanding();

    expect(screen.getByRole('link', { name: /Ver un ejemplo/ })).toHaveAttribute(
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
    await abrirLanding();
    const seccion = screen.getByRole('region', { name: 'Tus anotaciones son tuyas.' });

    expect(within(seccion).getByText('Función prevista')).toBeVisible();
    expect(within(seccion).getByText('La descarga todavía no está disponible.')).toBeVisible();
    expect(within(seccion).queryByRole('button')).not.toBeInTheDocument();
    expect(within(seccion).queryByRole('link')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Publicar comentario' })).not.toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Voz' })).not.toBeInTheDocument();
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
