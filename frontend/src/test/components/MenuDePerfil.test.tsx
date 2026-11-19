import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { MenuDePerfil } from '@/components/sistema/MenuDePerfil';

type Propiedades = Parameters<typeof MenuDePerfil>[0];
type Usuario = ReturnType<typeof userEvent.setup>;

let user: Usuario;
let onConfiguracion: ReturnType<typeof vi.fn>;
let onCerrarSesion: ReturnType<typeof vi.fn>;

describe('el menú del avatar', () => {
  it('al abrirse enfoca la primera opción', async () => {
    await dadoUnMenuAbierto();

    entoncesElFocoEstaEn(configuracion());
  });

  it('las flechas recorren las opciones', async () => {
    await dadoUnMenuAbierto();

    await cuandoSePresiona('{ArrowDown}');

    entoncesElFocoEstaEn(cerrarSesion());
  });

  it('las flechas dan la vuelta al final de las opciones', async () => {
    await dadoUnMenuAbierto();

    await cuandoSePresiona('{ArrowDown}{ArrowDown}');

    entoncesElFocoEstaEn(configuracion());
  });

  it('la flecha de arriba da la vuelta al principio de las opciones', async () => {
    await dadoUnMenuAbierto();

    await cuandoSePresiona('{ArrowUp}');

    entoncesElFocoEstaEn(cerrarSesion());
  });

  it('Tab lo cierra', async () => {
    await dadoUnMenuAbierto();

    await cuandoSePresiona('{Tab}');

    entoncesElMenuEstaCerrado();
  });

  it('elegir una opción llama a su acción', async () => {
    await dadoUnMenuAbierto();

    await cuandoSeElige(cerrarSesion());

    entoncesSoloSeLlamoACerrarSesion();
  });

  it('elegir una opción cierra el menú y deja el foco en el avatar', async () => {
    await dadoUnMenuAbierto();

    await cuandoSeElige(cerrarSesion());

    entoncesElMenuEstaCerradoYElFocoVuelveAlAvatar();
  });

  it('mientras se cierra la sesión «Cerrar sesión» no se puede volver a elegir', async () => {
    await dadoUnMenuAbierto({ cerrando: true });

    await cuandoSeElige(cerrarSesion());

    entoncesNoSeLlamoACerrarSesion();
  });

  it('en «Tu cuenta» marca «Configuración» como la página actual', async () => {
    await dadoUnMenuAbierto({ enCuenta: true });

    entoncesConfiguracionEsLaPaginaActual();
  });

  it('en «Tu cuenta» no marca el avatar como la página actual', async () => {
    await dadoUnMenuAbierto({ enCuenta: true });

    entoncesElAvatarNoEsLaPaginaActual();
  });
});

const avatar = () => screen.getByRole('button', { name: 'Menú de tu cuenta' });
const configuracion = () => screen.getByRole('menuitem', { name: 'Configuración' });
const cerrarSesion = () => screen.getByRole('menuitem', { name: 'Cerrar sesión' });

async function dadoUnMenuAbierto(cambios: Partial<Propiedades> = {}) {
  onConfiguracion = vi.fn();
  onCerrarSesion = vi.fn();
  user = userEvent.setup();
  render(
    <MenuDePerfil
      nombre="Ana Pérez"
      enCuenta={false}
      cerrando={false}
      onConfiguracion={onConfiguracion}
      onCerrarSesion={onCerrarSesion}
      {...cambios}
    />,
  );
  await user.click(avatar());
}

async function cuandoSePresiona(teclas: string) {
  await user.keyboard(teclas);
}

async function cuandoSeElige(opcion: HTMLElement) {
  await user.click(opcion);
}

function entoncesElFocoEstaEn(elemento: HTMLElement) {
  expect(elemento).toHaveFocus();
}

function entoncesElMenuEstaCerrado() {
  expect(screen.queryByRole('menu')).toBeNull();
}

function entoncesSoloSeLlamoACerrarSesion() {
  expect(onCerrarSesion).toHaveBeenCalledOnce();
  expect(onConfiguracion).not.toHaveBeenCalled();
}

function entoncesElMenuEstaCerradoYElFocoVuelveAlAvatar() {
  entoncesElMenuEstaCerrado();
  expect(avatar()).toHaveFocus();
}

function entoncesNoSeLlamoACerrarSesion() {
  expect(cerrarSesion()).toBeDisabled();
  expect(onCerrarSesion).not.toHaveBeenCalled();
}

function entoncesConfiguracionEsLaPaginaActual() {
  expect(configuracion()).toHaveAttribute('aria-current', 'page');
  expect(cerrarSesion()).not.toHaveAttribute('aria-current');
}

function entoncesElAvatarNoEsLaPaginaActual() {
  expect(avatar()).not.toHaveAttribute('aria-current');
}
