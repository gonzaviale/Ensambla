import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { HerramientasDelCuaderno } from '@/components/sistema/editor/HerramientasDelCuaderno';

const montar = () => {
  const acciones = {
    onDistribucion: vi.fn(),
    onBloques: vi.fn(),
    onSecciones: vi.fn(),
    onNuevaSeccion: vi.fn(),
    onFoto: vi.fn(),
    onResumen: vi.fn(),
  };
  render(<HerramientasDelCuaderno distribucion="mosaic" {...acciones} />);
  return { user: userEvent.setup(), ...acciones };
};

describe('HerramientasDelCuaderno', () => {
  it('son dos íconos y cada uno despliega lo suyo al tocarlo', async () => {
    const { user } = montar();
    const agregar = screen.getByRole('button', { name: 'Agregar y organizar' });
    const distribucion = screen.getByRole('button', { name: 'Distribución de los bloques' });

    expect(agregar).toHaveAttribute('aria-expanded', 'false');
    await user.click(agregar);
    expect(agregar).toHaveAttribute('aria-expanded', 'true');
    await user.click(distribucion);
    expect(agregar).toHaveAttribute('aria-expanded', 'false');
    expect(distribucion).toHaveAttribute('aria-expanded', 'true');
  });

  it('elegir una opción hace lo que dice y cierra el panel', async () => {
    const { user, onBloques, onResumen } = montar();
    const agregar = screen.getByRole('button', { name: 'Agregar y organizar' });

    await user.click(agregar);
    await user.click(screen.getByRole('button', { name: 'Bloques' }));
    expect(onBloques).toHaveBeenCalledOnce();
    expect(agregar).toHaveAttribute('aria-expanded', 'false');

    await user.click(agregar);
    await user.click(screen.getByRole('button', { name: /Resumen en PDF/ }));
    expect(onResumen).toHaveBeenCalledOnce();
  });

  it('los mosaicos se eligen desde el segundo ícono', async () => {
    const { user, onDistribucion } = montar();

    await user.click(screen.getByRole('button', { name: 'Distribución de los bloques' }));
    await user.click(screen.getByRole('button', { name: /2 columnas/ }));

    expect(onDistribucion).toHaveBeenCalledWith('two');
  });

  it('Escape y tocar afuera cierran el panel', async () => {
    const { user } = montar();
    const agregar = screen.getByRole('button', { name: 'Agregar y organizar' });

    await user.click(agregar);
    await user.keyboard('{Escape}');
    expect(agregar).toHaveAttribute('aria-expanded', 'false');

    await user.click(agregar);
    await user.click(document.body);
    expect(agregar).toHaveAttribute('aria-expanded', 'false');
  });
});
