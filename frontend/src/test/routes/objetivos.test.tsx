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
    expect(screen.getByText(/ganás por mes · te faltan \$400\.000/)).toBeVisible();
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
    expect(router.state.location.pathname).toBe('/objetivos');
  });

  it('sin ventas no muestra números de ejemplo: ni cómo viene el negocio ni semanas inventadas', async () => {
    setupObjetivoDeEjemplo(setupUsuarioConSesion().email);
    renderApp('/objetivos');

    await screen.findByRole('heading', { name: 'Cómo van tus objetivos' }, ESPERA);
    expect(screen.getByText(/Todavía no hay semanas para comparar/)).toBeVisible();
    expect(screen.getByText(/ganás por mes · te faltan \$500\.000/)).toBeVisible();
    expect(
      screen.queryByRole('heading', { name: 'Cómo viene tu negocio' }),
    ).not.toBeInTheDocument();
  });

  it('con ventas muestra cómo viene el negocio y el avance de las semanas reales', async () => {
    setupObjetivoDeEjemplo(setupUsuarioConSesion().email);
    setupServerResponse('get', '/objetivos/negocio', 200, {
      hayVentas: true,
      vendiste: 400_000,
      mercaderia: 250_000,
      gastos: 30_000,
      queda: 120_000,
      teDeben: 15_000,
      semanas: [
        { semana: 'Hace 2 sem.', queda: 80_000 },
        { semana: 'Hace 1 sem.', queda: 100_000 },
        { semana: 'Hoy', queda: 120_000 },
      ],
    });
    renderApp('/objetivos');

    expect(
      await screen.findByRole('heading', { name: 'Cómo viene tu negocio' }, ESPERA),
    ).toBeVisible();
    expect(
      await screen.findByText(/Subió \$40\.000 en las últimas 2 semanas/, {}, ESPERA),
    ).toBeVisible();
    expect(screen.getByText(/ganás por mes · te faltan \$380\.000/)).toBeVisible();
    expect(screen.getByText(/Te deben \(fiado\)/)).toBeVisible();
  });

  it('la tarjeta no repite qué hacer: lo muestra el seguimiento de la semana', async () => {
    setupObjetivoDeEjemplo(setupUsuarioConSesion().email);
    const router = renderApp('/objetivos');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /Ver mi semana/ }, ESPERA));

    expect(screen.queryByRole('button', { name: /Qué podés hacer/ })).not.toBeInTheDocument();
    expect(screen.queryByText('Lo voy a hacer')).not.toBeInTheDocument();
    await waitFor(() => expect(router.state.location.pathname).toMatch(/^\/objetivos\/.+/));
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
