import { act, fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { objetivoDeEjemplo } from '@/lib/objetivos';
import { useObjetivosStore } from '@/stores/objetivosStore';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

describe('sami academy', () => {
  beforeEach(() => {
    act(() => useObjetivosStore.setState({ objetivos: [], ejemploSembrado: false }));
  });

  it('se abre desde la navegación y empieza por la primera lección', async () => {
    setupUsuarioConSesion();
    const router = renderApp('/inicio');
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: 'Sami Academy' }, ESPERA));

    expect(
      await screen.findByRole('heading', { name: 'Cómo se forma tu resultado' }),
    ).toBeVisible();
    expect(router.state.location.pathname).toBe('/sami-academy');
    expect(screen.getByText('= Resultado operativo estimado')).toBeVisible();
  });

  it('una lección que no existe cae en la primera', async () => {
    setupUsuarioConSesion();
    renderApp('/sami-academy?leccion=inventada');

    expect(
      await screen.findByRole('heading', { name: 'Cómo se forma tu resultado' }, ESPERA),
    ).toBeVisible();
  });

  it('recorre las lecciones desde el índice', async () => {
    setupUsuarioConSesion();
    const router = renderApp('/sami-academy');
    const user = userEvent.setup();

    await screen.findByRole('heading', { name: 'Cómo se forma tu resultado' }, ESPERA);
    await user.click(screen.getByRole('button', { name: /Tus métricas, una por una/ }));
    expect(await screen.findByRole('heading', { name: 'Punto de equilibrio' })).toBeVisible();

    await user.click(screen.getByRole('button', { name: /Cómo recomienda Sami/ }));
    expect(await screen.findByRole('heading', { name: 'Qué nunca hace' })).toBeVisible();
    expect(screen.getByRole('button', { name: /Cómo recomienda Sami/ })).toHaveAttribute(
      'aria-current',
      'page',
    );

    await user.click(screen.getByRole('button', { name: /Sugerencias para usar Ensambla/ }));
    expect(await screen.findByText('Cargá el costo de tus productos')).toBeVisible();
    expect(router.state.location.search).toMatchObject({ leccion: 'consejos' });
  });

  it('prueba un escenario con el objetivo y el resultado sigue a los controles', async () => {
    setupUsuarioConSesion();
    act(() =>
      useObjetivosStore.setState({ objetivos: [objetivoDeEjemplo()], ejemploSembrado: true }),
    );
    renderApp('/sami-academy?leccion=escenario&objetivo=objetivo-ejemplo');

    await screen.findByRole('heading', { name: 'Probá un escenario' }, ESPERA);
    expect(screen.getByText('Resultado proyectado').nextElementSibling).toHaveTextContent(
      '$500.000',
    );
    expect(screen.getByText('Meta alcanzada')).toBeVisible();

    fireEvent.change(screen.getByRole('slider', { name: 'Gastar menos' }), {
      target: { value: '0' },
    });

    expect(screen.getByText('Resultado proyectado').nextElementSibling).toHaveTextContent(
      '$388.000',
    );
    expect(screen.getByText('Te faltaría')).toBeVisible();
  });

  it('sin objetivos prueba con un ejemplo e invita a definir el propio', async () => {
    setupUsuarioConSesion();
    const router = renderApp('/sami-academy?leccion=escenario');
    const user = userEvent.setup();

    await screen.findByRole('heading', { name: 'Probá un escenario' }, ESPERA);
    expect(screen.getByText(/Todavía no definiste objetivos/)).toBeVisible();
    expect(
      screen.queryByRole('button', { name: 'Ver el plan de este objetivo' }),
    ).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Definir mi objetivo' }));
    expect(router.state.location.pathname).toBe('/objetivos');
  });
});
