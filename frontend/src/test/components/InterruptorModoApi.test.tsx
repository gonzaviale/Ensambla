import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type * as ConfigEnv from '@/config/env';
import { InterruptorModoApi } from '@/components/Navbar/InterruptorModoApi';
import { modoApiElegido, tomarEntrarComoDemo } from '@/lib/modoApi';

const modo = vi.hoisted(() => ({ mocks: 'completo' as 'completo' | 'ninguno' }));

vi.mock('@/config/env', async (importOriginal) => {
  const original = await importOriginal<typeof ConfigEnv>();
  return {
    ...original,
    env: {
      ...original.env,
      get mocks() {
        return modo.mocks;
      },
    },
  };
});

afterEach(() => {
  localStorage.clear();
  modo.mocks = 'completo';
});

describe('interruptor de modo de datos', () => {
  it('con el backend real, pasa a datos simulados y entra con la demo', async () => {
    modo.mocks = 'ninguno';
    const recargar = vi.fn();
    render(<InterruptorModoApi recargar={recargar} />);

    const boton = screen.getByRole('button', { name: 'Datos reales. Pasar a datos simulados' });
    expect(boton).toHaveTextContent('real');
    await userEvent.setup().click(boton);

    expect(modoApiElegido()).toBe('mock');
    expect(tomarEntrarComoDemo()).toBe(true);
    expect(recargar).toHaveBeenCalledTimes(1);
  });

  it('con datos simulados, vuelve al backend real', async () => {
    const recargar = vi.fn();
    render(<InterruptorModoApi recargar={recargar} />);

    const boton = screen.getByRole('button', { name: 'Datos simulados. Pasar al backend real' });
    expect(boton).toHaveTextContent('mock');
    await userEvent.setup().click(boton);

    expect(modoApiElegido()).toBe('real');
    expect(recargar).toHaveBeenCalledTimes(1);
  });

  it('al volver al real da de baja sólo el service worker de los mocks', async () => {
    const delMock = { active: { scriptURL: 'http://localhost/mockServiceWorker.js' }, unregister: vi.fn() };
    const otro = { active: { scriptURL: 'http://localhost/otro.js' }, unregister: vi.fn() };
    vi.stubGlobal('navigator', {
      ...navigator,
      serviceWorker: { getRegistrations: vi.fn().mockResolvedValue([delMock, otro]) },
    });
    const recargar = vi.fn();
    render(<InterruptorModoApi recargar={recargar} />);

    await userEvent.setup().click(screen.getByRole('button', { name: /Pasar al backend real/ }));

    expect(delMock.unregister).toHaveBeenCalledTimes(1);
    expect(otro.unregister).not.toHaveBeenCalled();
    expect(recargar).toHaveBeenCalledTimes(1);
    vi.unstubAllGlobals();
  });
});
