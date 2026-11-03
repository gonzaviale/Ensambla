import { screen, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { ESPERA, verifyHeadingPresentAsync } from '@/test/testHelpers';

async function abrirLanding() {
  const router = renderApp('/');
  await verifyHeadingPresentAsync('Tu cuaderno de siempre, con tus cuentas más claras');
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
    expect(screen.getByText('Empezá a armar el cuaderno de tu negocio.')).toBeVisible();
  });

  it('lleva al ingreso existente desde el cierre', async () => {
    const { router, user } = await abrirLanding();

    await user.click(screen.getByRole('link', { name: 'Ya tengo cuenta' }));
    await screen.findByRole('heading', { name: 'Ingresá a tu cuenta' }, ESPERA);
    expect(router.state.location.pathname).toBe('/login');
  });

  it('permite corregir un importe y encontrar el resultado confirmado', async () => {
    const { user } = await abrirLanding();
    await user.click(screen.getByRole('button', { name: 'Revisás los datos' }));
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
    expect(
      within(document.getElementById('como-funciona') as HTMLElement).getByRole('status'),
    ).toHaveTextContent('No hay coincidencias');
    await user.clear(buscar);
    await user.type(buscar, 'luci');
    expect(lista).toHaveTextContent('Lucía Pérez');
  });

  it('rechaza un importe vacío o no positivo sin pasar a la confirmación', async () => {
    const { user } = await abrirLanding();
    await user.click(screen.getByRole('button', { name: 'Revisás los datos' }));
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
    expect(screen.getByRole('button', { name: 'Revisás los datos' })).toHaveFocus();
    expect(screen.getByLabelText('Importe del fiado')).toBeVisible();
  });

  it('Sami organiza cada categoría y vuelve a la anotación al cambiar de ejemplo', async () => {
    const { user } = await abrirLanding();
    const seccion = within(document.getElementById('a-tu-manera') as HTMLElement);
    const ejemplos = [
      ['Ventas', 'Total de la venta', '$ 7.000'],
      ['Gastos', 'Medio de pago', 'Transferencia'],
      ['Fiados', 'Importe pendiente', '$ 3.200'],
      ['Productos', 'Cantidad disponible', '12 unidades'],
    ];

    for (const [categoria, campo, valor] of ejemplos) {
      await user.click(seccion.getByRole('button', { name: categoria }));
      const panel = within(seccion.getByRole('region', { name: `Ejemplo de ${categoria}` }));
      expect(seccion.getByRole('button', { name: categoria })).toHaveAttribute(
        'aria-pressed',
        'true',
      );
      expect(seccion.getAllByRole('button', { pressed: true })).toHaveLength(1);
      expect(panel.getByText('Tu anotación')).toBeVisible();
      expect(panel.queryByRole('definition')).not.toBeInTheDocument();
      await user.click(panel.getByRole('button', { name: 'Ver cómo se organiza' }));
      expect(panel.getByText(campo as string)).toBeVisible();
      expect(panel.getByText(valor as string)).toBeVisible();
      expect(panel.getAllByRole('definition')).toHaveLength(3);
      expect(panel.getByRole('status')).toHaveTextContent('organizado para revisar');
    }
    expect(localStorage.length).toBe(0);
  });

  it('mantiene un solo Sami y una pose diferente por categoría incluso al volver a elegirlas', async () => {
    const { user } = await abrirLanding();
    const seccion = within(document.getElementById('a-tu-manera') as HTMLElement);
    const poses = new Set<string>();
    for (const categoria of ['Ventas', 'Gastos', 'Fiados', 'Productos', 'Ventas']) {
      await user.click(seccion.getByRole('button', { name: categoria }));
      await waitFor(() =>
        expect(document.querySelector('.ens-sami-character')).toHaveAttribute(
          'data-cambiando',
          'false',
        ),
      );
      expect(seccion.getAllByRole('img', { name: 'Sami, el pulpito de Ensambla' })).toHaveLength(1);
      const imagen = seccion.getByRole('img', { name: 'Sami, el pulpito de Ensambla' });
      const pose = imagen.getAttribute('viewBox') as string;
      poses.add(pose);
      await user.click(seccion.getByRole('button', { name: 'Ver cómo se organiza' }));
      expect(imagen).toHaveAttribute('viewBox', pose);
    }
    expect(poses.size).toBe(4);
  });

  it('permite elegir una categoría con el teclado y regresar al apunte original', async () => {
    const { user } = await abrirLanding();
    const seccion = within(document.getElementById('a-tu-manera') as HTMLElement);
    seccion.getByRole('button', { name: 'Ventas' }).focus();
    await user.keyboard('{Tab}{Enter}');
    expect(seccion.getByRole('button', { name: 'Gastos' })).toHaveFocus();
    expect(seccion.getByRole('button', { name: 'Gastos' })).toHaveAttribute('aria-pressed', 'true');
    const panel = within(seccion.getByRole('region', { name: 'Ejemplo de Gastos' }));
    await user.click(panel.getByRole('button', { name: 'Ver cómo se organiza' }));
    const volver = panel.getByRole('button', { name: 'Volver a la anotación' });
    volver.focus();
    await user.keyboard('{Enter}');
    expect(panel.getByText(/Pagué \$ 18.500 al proveedor/)).toBeVisible();
    expect(panel.queryByRole('definition')).not.toBeInTheDocument();
    expect(panel.getByRole('button', { name: 'Ver cómo se organiza' })).toHaveFocus();
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
