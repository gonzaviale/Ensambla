import { act, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { objetivoDeEjemplo } from '@/lib/objetivos';
import { useObjetivosStore } from '@/stores/objetivosStore';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

const conObjetivoDeEjemplo = () =>
  act(() =>
    useObjetivosStore.setState({ objetivos: [objetivoDeEjemplo()], ejemploSembrado: true }),
  );

describe('objetivos', () => {
  beforeEach(() => {
    act(() => useObjetivosStore.setState({ objetivos: [], ejemploSembrado: false }));
  });

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

  it('sin objetivos propone definir uno y lo muestra con su avance', async () => {
    setupUsuarioConSesion();
    renderApp('/objetivos');
    const user = userEvent.setup();

    await screen.findByRole('heading', { name: 'Definí tu primer objetivo' }, ESPERA);
    expect(screen.getByRole('button', { name: 'Definir objetivo' })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: /Quiero ganar \$400\.000/ }));
    await user.click(screen.getByRole('button', { name: 'Definir objetivo' }));

    expect(await screen.findByRole('heading', { name: 'Cómo van tus objetivos' })).toBeVisible();
    expect(screen.getByText(/Meta mensual · \$400\.000/)).toBeVisible();
    expect(screen.getByRole('progressbar', { name: /Avance hacia \$400\.000/ })).toHaveAttribute(
      'aria-valuenow',
      '45',
    );
    expect(screen.getByText(/Todavía no hay semanas para comparar/)).toBeVisible();
  });

  it('muestra cómo cambió el resultado y la recomendación en la tarjeta', async () => {
    setupUsuarioConSesion();
    conObjetivoDeEjemplo();
    renderApp('/objetivos');
    const user = userEvent.setup();

    await screen.findByRole('heading', { name: 'Cómo van tus objetivos' }, ESPERA);
    expect(screen.getByText(/Subió \$60\.000 en las últimas 5 semanas/)).toBeVisible();
    expect(screen.queryByText('Camino combinado')).not.toBeInTheDocument();

    const recomendacion = screen.getByRole('button', { name: /Recomendación de Sami/ });
    expect(recomendacion).toHaveAttribute('aria-expanded', 'false');
    await user.click(recomendacion);

    expect(recomendacion).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('heading', { name: 'Recomendación para tu objetivo' })).toBeVisible();
    expect(screen.getByText('Camino combinado')).toBeVisible();
    expect(screen.getByText('Recomendado por equilibrio')).toBeVisible();
    expect(screen.queryByText('Aviso económico')).not.toBeInTheDocument();
  });

  it('lleva al plan de acción del objetivo, con su seguimiento', async () => {
    setupUsuarioConSesion();
    conObjetivoDeEjemplo();
    const router = renderApp('/objetivos');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Ver plan de acción' }, ESPERA));

    expect(router.state.location.pathname).toBe('/objetivos/objetivo-ejemplo');
    expect(await screen.findByRole('heading', { name: 'Seguimiento' })).toBeVisible();
    expect(screen.getByText('Revisar los precios con margen bajo')).toBeVisible();
    expect(screen.getByText('Reducir gastos extraordinarios')).toBeVisible();
    expect(screen.getByText(/Tu resultado subió \$60\.000/)).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Volver a objetivos' }));
    expect(router.state.location.pathname).toBe('/objetivos');
  });

  it('un objetivo que no existe invita a volver', async () => {
    setupUsuarioConSesion();
    renderApp('/objetivos/no-existe');

    expect(
      await screen.findByRole('heading', { name: 'No encontramos ese objetivo' }, ESPERA),
    ).toBeVisible();
    expect(screen.getByRole('button', { name: 'Ir a mis objetivos' })).toBeVisible();
  });

  it('abre el escenario de Sami Academy con el objetivo elegido', async () => {
    setupUsuarioConSesion();
    conObjetivoDeEjemplo();
    const router = renderApp('/objetivos');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Ver cómo funcionaría' }, ESPERA));

    expect(router.state.location.pathname).toBe('/sami-academy');
    expect(router.state.location.search).toMatchObject({
      leccion: 'escenario',
      objetivo: 'objetivo-ejemplo',
    });
    expect(await screen.findByRole('heading', { name: 'Probá un escenario' })).toBeVisible();
  });

  it('pide confirmación antes de quitar un objetivo', async () => {
    setupUsuarioConSesion();
    conObjetivoDeEjemplo();
    renderApp('/objetivos');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /Quitar objetivo/ }, ESPERA));
    const dialogo = await screen.findByRole('dialog');
    expect(within(dialogo).getByText('¿Quitar este objetivo?')).toBeVisible();
    await user.click(within(dialogo).getByRole('button', { name: 'Quitar objetivo' }));

    expect(await screen.findByRole('heading', { name: 'Definí tu primer objetivo' })).toBeVisible();
    expect(useObjetivosStore.getState().objetivos).toHaveLength(0);
  });

  it('deja la pestaña de Mi zona y desde ahí usa el plan del objetivo', async () => {
    setupUsuarioConSesion();
    conObjetivoDeEjemplo();
    const router = renderApp('/objetivos');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('tab', { name: 'Cruce con Mi zona' }, ESPERA));
    expect(
      screen.getByRole('heading', { name: 'Compará una decisión con tu contexto' }),
    ).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Usar en mi plan' }));
    expect(router.state.location.pathname).toBe('/objetivos/objetivo-ejemplo');
  });
});
