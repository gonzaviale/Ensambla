import { screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { setupServerError, ESPERA } from '@/test/testHelpers';
import {
  givenLaPantallaDeCarteles,
  givenUnCartelArmado,
  thenLaVistaPreviaDiceElTitular,
  thenSeGuardoLaOpcion,
  thenSeSugiereElAceite,
  thenSeVenLasTresOpciones,
  whenSeArmaElCartelDelAceite,
  whenSeCambiaElTitular,
  whenSeElijeLaOpcion,
} from '@/test/carteles/ayudasDeCarteles';

const guardarArchivo = vi.hoisted(() => vi.fn());
vi.mock('@/lib/descargas', () => ({ guardarArchivo }));

describe('Carteles y promociones', () => {
  beforeEach(() => guardarArchivo.mockClear());

  it('sugiere qué promocionar con los números calculados por el servidor', async () => {
    await givenLaPantallaDeCarteles();

    await thenSeSugiereElAceite();
  });

  it('muestra tres opciones distintas al elegir qué promocionar', async () => {
    const { user } = await givenLaPantallaDeCarteles();

    await whenSeArmaElCartelDelAceite(user);

    thenSeVenLasTresOpciones();
  });

  it('actualiza la vista previa cuando se edita el titular', async () => {
    const { user } = await givenUnCartelArmado();

    await whenSeCambiaElTitular(user, 'Aceite en oferta');

    await waitFor(() => thenLaVistaPreviaDiceElTitular('Aceite en oferta'), ESPERA);
  });

  it('deja elegir una de las opciones', async () => {
    const { user } = await givenUnCartelArmado();

    await whenSeElijeLaOpcion(user, /Opción 2/);

    thenSeGuardoLaOpcion(/Opción 2/);
  });

  it('descarga el cartel elegido con el nombre que manda el servidor', async () => {
    const { user } = await givenUnCartelArmado();

    await user.click(screen.getByRole('button', { name: 'Descargar cartel' }));

    await waitFor(() => expect(guardarArchivo).toHaveBeenCalledTimes(1), ESPERA);
    expect(guardarArchivo.mock.calls[0]?.[0]).toMatchObject({ nombre: 'cartel-promo-precio-a4.pdf' });
  });

  it('ofrece compartir por WhatsApp con el texto del posteo', async () => {
    await givenUnCartelArmado();

    const enlace = screen.getByRole('link', { name: 'Compartir por WhatsApp' });

    expect(enlace.getAttribute('href')).toContain('https://wa.me/?text=');
    expect(decodeURIComponent(enlace.getAttribute('href') ?? '')).toContain('Pasá por Don Rufino');
  });

  it('avisa el error cuando el servidor no puede armar las opciones', async () => {
    const { user } = await givenLaPantallaDeCarteles();
    setupServerError('post', '/carteles/textos', 403, 'Usaste los carteles con IA de este mes');

    await user.click(await screen.findByRole('button', { name: /Armar cartel de/ }, ESPERA));

    expect(await screen.findByRole('alert', undefined, ESPERA)).toHaveTextContent(
      'Usaste los carteles con IA de este mes',
    );
  });
});
