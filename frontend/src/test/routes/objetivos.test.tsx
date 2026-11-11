import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { objetivosDe } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupObjetivoDeEjemplo,
  setupServerError,
  setupServerResponse,
  setupUsuarioConSesion,
} from '@/test/testHelpers';

describe('objetivos', () => {
  it('se abre desde la navegación principal y ya no hay un laboratorio', async () => {
    setupUsuarioConSesion();
    const router = renderApp('/inicio');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Objetivos' }, ESPERA));
    await screen.findByRole('heading', { name: 'Definí tu primer objetivo' }, ESPERA);

    expect(router.state.location.pathname).toBe('/objetivos');
    expect(screen.getByRole('button', { name: 'Objetivos' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    expect(screen.queryByRole('button', { name: 'Laboratorio' })).not.toBeInTheDocument();
    expect(screen.queryByText(/laboratorio/i)).not.toBeInTheDocument();
  });

  it('sin objetivos propone definir uno y lo muestra con lo que falta, sin porcentajes', async () => {
    setupUsuarioConSesion();
    renderApp('/objetivos');
    const user = userEvent.setup();

    await screen.findByRole('heading', { name: 'Definí tu primer objetivo' }, ESPERA);
    expect(screen.getByRole('button', { name: 'Definir objetivo' })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: /Quiero ganar \$400\.000/ }));
    await user.click(screen.getByRole('button', { name: 'Definir objetivo' }));

    expect(await screen.findByRole('heading', { name: 'Cómo van tus objetivos' })).toBeVisible();
    expect(screen.getByText(/ganás por mes · te faltan \$220\.000/)).toBeVisible();
    expect(screen.getByText(/Todavía no hay semanas para comparar/)).toBeVisible();
    expect(screen.queryByText(/%/)).not.toBeInTheDocument();
  });

  it('todo está en una pantalla: sin pestañas ni saltos a otras páginas', async () => {
    setupObjetivoDeEjemplo(setupUsuarioConSesion().email);
    const router = renderApp('/objetivos');

    await screen.findByRole('heading', { name: 'Cómo van tus objetivos' }, ESPERA);
    expect(screen.queryByRole('tab')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ver cómo funcionaría' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ver plan de acción' })).not.toBeInTheDocument();
    expect(screen.getByText(/Subió \$60\.000 en las últimas 5 semanas/)).toBeVisible();
    expect(router.state.location.pathname).toBe('/objetivos');
  });

  it('despliega qué hacer en el lugar, y sin productos cargados lo explica', async () => {
    setupObjetivoDeEjemplo(setupUsuarioConSesion().email);
    renderApp('/objetivos');
    const user = userEvent.setup();

    const boton = await screen.findByRole('button', { name: /Qué podés hacer/ }, ESPERA);
    expect(boton).toHaveAttribute('aria-expanded', 'true');
    expect(
      await screen.findByText(/Cargá tus productos y sus costos en un cuaderno/, {}, ESPERA),
    ).toBeVisible();

    await user.click(boton);
    expect(boton).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByText(/Cargá tus productos/)).not.toBeInTheDocument();
  });

  it('muestra los productos elegidos por el servidor y conserva el slider y la comparación de zona', async () => {
    setupObjetivoDeEjemplo(setupUsuarioConSesion().email);
    setupServerResponse('get', '/objetivos/pasos', 200, [
      {
        tipo: 'SUBIR_PRECIO',
        nombre: 'Yerba',
        precio: 3600,
        costo: 2700,
        stock: 4,
        vendidas30Dias: 60,
        precioZona: { minimo: 3500, promedio: 3900, maximo: 4300 },
      },
    ]);
    renderApp('/objetivos');

    expect(await screen.findByText(/Subile .* a Yerba/, {}, ESPERA)).toBeVisible();
    expect(screen.getByRole('slider', { name: /Subile/ })).toBeVisible();
    expect(screen.getByText(/En tu zona lo cobran entre/)).toBeVisible();
  });

  it('conserva el objetivo y permite reintentar cuando no puede calcular los pasos', async () => {
    setupObjetivoDeEjemplo(setupUsuarioConSesion().email);
    setupServerError('get', '/objetivos/pasos', 503, 'No pudimos calcular los pasos.');
    renderApp('/objetivos');
    const user = userEvent.setup();

    expect(
      await screen.findByRole('alert', { name: 'No pudimos calcular tus pasos' }, ESPERA),
    ).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Cómo van tus objetivos' })).toBeVisible();
    setupServerResponse('get', '/objetivos/pasos', 200, [
      {
        tipo: 'PONER_A_LA_VISTA',
        nombre: 'Coca-Cola',
        precio: 2700,
        costo: 1950,
        stock: 18,
        vendidas30Dias: 5,
        precioZona: null,
      },
    ]);

    await user.click(screen.getByRole('button', { name: 'Reintentar recomendaciones' }));

    expect(await screen.findByText('Poné Coca-Cola a la vista', {}, ESPERA)).toBeVisible();
    expect(
      screen.queryByRole('alert', { name: 'No pudimos calcular tus pasos' }),
    ).not.toBeInTheDocument();
  });

  it('un objetivo que no existe invita a volver', async () => {
    setupUsuarioConSesion();
    renderApp('/objetivos/no-existe');

    expect(
      await screen.findByRole('heading', { name: 'No encontramos ese objetivo' }, ESPERA),
    ).toBeVisible();
    expect(screen.getByRole('button', { name: 'Ir a mis objetivos' })).toBeVisible();
  });

  it('pide confirmación antes de quitar un objetivo', async () => {
    const usuario = setupUsuarioConSesion();
    setupObjetivoDeEjemplo(usuario.email);
    renderApp('/objetivos');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /Quitar objetivo/ }, ESPERA));
    const dialogo = await screen.findByRole('dialog');
    expect(within(dialogo).getByText('¿Quitar este objetivo?')).toBeVisible();
    await user.click(within(dialogo).getByRole('button', { name: 'Quitar objetivo' }));

    expect(await screen.findByRole('heading', { name: 'Definí tu primer objetivo' })).toBeVisible();
    await waitFor(() => expect(objetivosDe(usuario.email)).toHaveLength(0));
  });

  it('un objetivo definido queda guardado en el servidor de la cuenta, con su texto y su meta', async () => {
    const usuario = setupUsuarioConSesion();
    renderApp('/objetivos');
    const user = userEvent.setup();

    await screen.findByRole('heading', { name: 'Definí tu primer objetivo' }, ESPERA);
    await user.click(screen.getByRole('button', { name: /Quiero ganar \$400\.000/ }));
    await user.click(screen.getByRole('button', { name: 'Definir objetivo' }));

    await screen.findByRole('heading', { name: 'Cómo van tus objetivos' }, ESPERA);
    expect(objetivosDe(usuario.email)).toMatchObject([
      { texto: expect.stringContaining('$400.000') as string, meta: 400_000 },
    ]);
  });

  it('un objetivo guardado se ve al volver a abrir la pantalla', async () => {
    setupObjetivoDeEjemplo(setupUsuarioConSesion().email);
    renderApp('/objetivos');

    await screen.findByRole('heading', { name: 'Cómo van tus objetivos' }, ESPERA);
    expect(screen.getByText(/sin subir demasiado los precios/)).toBeVisible();
  });

  it('no muestra los objetivos de otra cuenta', async () => {
    setupObjetivoDeEjemplo('otra@example.com');
    setupUsuarioConSesion();
    renderApp('/objetivos');

    expect(
      await screen.findByRole('heading', { name: 'Definí tu primer objetivo' }, ESPERA),
    ).toBeVisible();
  });

  it('avisa si el servidor rechaza el objetivo, y no lo da por definido', async () => {
    setupUsuarioConSesion();
    setupServerError('post', '/objetivos', 400, 'Revisá los datos del objetivo.', {
      texto: 'El texto es obligatorio',
    });
    renderApp('/objetivos');
    const user = userEvent.setup();

    await user.click(
      await screen.findByRole('button', { name: /Quiero ganar \$400\.000/ }, ESPERA),
    );
    await user.click(screen.getByRole('button', { name: 'Definir objetivo' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('El texto es obligatorio');
    expect(
      screen.queryByRole('heading', { name: 'Cómo van tus objetivos' }),
    ).not.toBeInTheDocument();
  });
});
