import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { MenuDePerfil } from '@/components/sistema/MenuDePerfil';

const abrir = async (props: Partial<Parameters<typeof MenuDePerfil>[0]> = {}) => {
  const alternativas = {
    nombre: 'Ana Pérez',
    enCuenta: false,
    cerrando: false,
    onConfiguracion: vi.fn(),
    onCerrarSesion: vi.fn(),
    ...props,
  };
  const user = userEvent.setup();
  render(<MenuDePerfil {...alternativas} />);
  await user.click(screen.getByRole('button', { name: 'Menú de tu cuenta' }));
  return { user, ...alternativas };
};

const configuracion = () => screen.getByRole('menuitem', { name: 'Configuración' });
const cerrarSesion = () => screen.getByRole('menuitem', { name: 'Cerrar sesión' });

describe('el menú del avatar', () => {
  it('al abrirse enfoca la primera opción y las flechas recorren las opciones dando la vuelta', async () => {
    const { user } = await abrir();
    expect(configuracion()).toHaveFocus();

    await user.keyboard('{ArrowDown}');
    expect(cerrarSesion()).toHaveFocus();

    await user.keyboard('{ArrowDown}');
    expect(configuracion()).toHaveFocus();

    await user.keyboard('{ArrowUp}');
    expect(cerrarSesion()).toHaveFocus();
  });

  it('Tab lo cierra', async () => {
    const { user } = await abrir();

    await user.tab();

    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('elegir una opción llama a su acción y cierra el menú', async () => {
    const { user, onCerrarSesion, onConfiguracion } = await abrir();

    await user.click(cerrarSesion());

    expect(onCerrarSesion).toHaveBeenCalledOnce();
    expect(onConfiguracion).not.toHaveBeenCalled();
    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('mientras se cierra la sesión «Cerrar sesión» no se puede volver a elegir', async () => {
    const { user, onCerrarSesion } = await abrir({ cerrando: true });

    expect(cerrarSesion()).toBeDisabled();
    await user.click(cerrarSesion());

    expect(onCerrarSesion).not.toHaveBeenCalled();
  });

  it('en «Tu cuenta» marca «Configuración» como la página actual, no el avatar', async () => {
    await abrir({ enCuenta: true });

    expect(configuracion()).toHaveAttribute('aria-current', 'page');
    expect(cerrarSesion()).not.toHaveAttribute('aria-current');
    expect(screen.getByRole('button', { name: 'Menú de tu cuenta' })).not.toHaveAttribute(
      'aria-current',
    );
  });
});
