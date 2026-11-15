import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupObjetivoDeEjemplo, setupUsuarioConSesion } from '@/test/testHelpers';

const EL_MES = () => new Date().toISOString().slice(0, 7);

const conObjetivo = (usadas: number) => {
  const usuario = setupUsuarioConSesion();
  setupObjetivoDeEjemplo(usuario.email);
  if (usadas > 0) estado.objetivosConIa.set(`${usuario.email}|${EL_MES()}`, usadas);
  return usuario;
};

describe('la cuota de objetivos con IA', () => {
  it('Objetivos muestra cuántos objetivos con IA quedan en el mes', async () => {
    conObjetivo(1);
    renderApp('/objetivos');

    expect(
      await screen.findByText('Te quedan 4 de 5 objetivos con IA este mes', {}, ESPERA),
    ).toBeVisible();
  });

  it('un objetivo monetario del formulario clásico no gasta cuota', async () => {
    const usuario = conObjetivo(0);
    renderApp('/objetivos');
    const user = userEvent.setup();

    await screen.findByText('Te quedan 5 de 5 objetivos con IA este mes', {}, ESPERA);
    await user.click(screen.getByRole('button', { name: /Definir otro objetivo/ }));
    await user.click(
      await screen.findByRole('button', { name: 'Quiero poner una meta en pesos por mes' }),
    );
    await user.click(screen.getByRole('button', { name: /Quiero ganar \$400\.000/ }));
    await user.click(screen.getByRole('button', { name: 'Definir objetivo' }));

    await waitFor(() => expect(screen.getAllByRole('article')).toHaveLength(2), ESPERA);
    expect(estado.objetivosConIa.has(`${usuario.email}|${EL_MES()}`)).toBe(false);
    expect(screen.getByText('Te quedan 5 de 5 objetivos con IA este mes')).toBeVisible();
  });

  it('al agotarse explica qué hacer y lleva a Planes', async () => {
    conObjetivo(5);
    const router = renderApp('/objetivos');
    const user = userEvent.setup();

    const aviso = within(
      await screen.findByRole('note', { name: 'Objetivos con IA de este mes' }, ESPERA),
    );
    expect(aviso.getByText('Usaste los 5 objetivos con IA de este mes')).toBeVisible();
    expect(await aviso.findByText(/Con Premium tenés 20 por mes/)).toBeVisible();
    await user.click(await aviso.findByRole('button', { name: 'Ver planes' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/planes'));
  });

  it('con Premium no ofrece cambiar de plan: dice que se renuevan el mes que viene', async () => {
    const usuario = conObjetivo(20);
    estado.pruebasPremium.set(usuario.email, {
      inicioEn: new Date(Date.now() - 86_400_000).toISOString(),
      finEn: new Date(Date.now() + 5 * 86_400_000).toISOString(),
      paginasIaUtilizadas: 0,
      limitePaginasIa: 10,
    });
    renderApp('/objetivos');

    const aviso = within(
      await screen.findByRole('note', { name: 'Objetivos con IA de este mes' }, ESPERA),
    );
    expect(aviso.getByText('Usaste los 20 objetivos con IA de este mes')).toBeVisible();
    expect(await aviso.findByText(/Se renuevan el mes que viene/)).toBeVisible();
    expect(aviso.queryByRole('button', { name: 'Ver planes' })).not.toBeInTheDocument();
  });

  it('Planes dice cuántos objetivos con IA incluye cada plan', async () => {
    setupUsuarioConSesion();
    renderApp('/planes');

    const gratuito = within(await screen.findByRole('article', { name: 'Plan Gratuito' }, ESPERA));
    const premium = within(screen.getByRole('article', { name: 'Plan Premium' }));
    expect(gratuito.getByText('Objetivos con IA por mes').nextElementSibling).toHaveTextContent(
      '5',
    );
    expect(premium.getByText('Objetivos con IA por mes').nextElementSibling).toHaveTextContent(
      '20',
    );
  });
});
