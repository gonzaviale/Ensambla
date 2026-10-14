import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { verifyHeadingPresentAsync } from '@/test/testHelpers';

async function abrirLanding() {
  renderApp('/');
  await verifyHeadingPresentAsync('Tu sistema, un bloque a la vez.');
  return userEvent.setup();
}

describe('landing', () => {
  it('tiene un único h1 y lleva a registro y a login', async () => {
    await abrirLanding();

    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    expect(screen.getByRole('link', { name: /Crear mi sistema gratis/ })).toHaveAttribute(
      'href',
      '/registro',
    );
    expect(screen.getByRole('link', { name: 'Ya tengo cuenta' })).toHaveAttribute('href', '/login');
  });

  describe('armador de bloques', () => {
    it('suma y quita bloques y cuenta los activos', async () => {
      const user = await abrirLanding();
      const grupo = screen.getByRole('group', { name: 'Bloques disponibles' });
      const inventario = within(grupo).getByRole('button', { name: 'Inventario' });

      expect(screen.getByText('0 bloques')).toBeInTheDocument();
      expect(inventario).toHaveAttribute('aria-pressed', 'false');

      await user.click(inventario);
      expect(screen.getByText('1 bloque')).toBeInTheDocument();
      expect(inventario).toHaveAttribute('aria-pressed', 'true');

      await user.click(within(grupo).getByRole('button', { name: 'Ventas' }));
      expect(screen.getByText('2 bloques')).toBeInTheDocument();

      await user.click(inventario);
      expect(screen.getByText('1 bloque')).toBeInTheDocument();
      expect(inventario).toHaveAttribute('aria-pressed', 'false');
    });

    it('vuelve a empezar', async () => {
      const user = await abrirLanding();
      const grupo = screen.getByRole('group', { name: 'Bloques disponibles' });

      await user.click(within(grupo).getByRole('button', { name: 'Pedidos' }));
      await user.click(within(grupo).getByRole('button', { name: 'Clientes' }));
      await user.click(screen.getByRole('button', { name: 'Empezar de nuevo' }));

      expect(screen.getByText('0 bloques')).toBeInTheDocument();
      expect(within(grupo).getByRole('button', { name: 'Pedidos' })).toHaveAttribute(
        'aria-pressed',
        'false',
      );
    });
  });

  describe('comentarios', () => {
    it('pide el nombre y un comentario de al menos 5 caracteres', async () => {
      const user = await abrirLanding();

      await user.click(screen.getByRole('button', { name: 'Publicar comentario' }));
      expect(screen.getByRole('status')).toHaveTextContent('Contanos tu nombre.');
      expect(screen.getByRole('textbox', { name: 'Nombre' })).toHaveAttribute(
        'aria-invalid',
        'true',
      );

      await user.type(screen.getByRole('textbox', { name: 'Nombre' }), 'Ana');
      await user.type(screen.getByRole('textbox', { name: 'Comentario' }), 'Ok');
      await user.click(screen.getByRole('button', { name: 'Publicar comentario' }));
      expect(screen.getByRole('status')).toHaveTextContent('al menos 5 caracteres');
    });

    it('publica el comentario, lo muestra primero y lo guarda en el navegador', async () => {
      const user = await abrirLanding();

      await user.type(screen.getByRole('textbox', { name: 'Nombre' }), 'Ana');
      await user.type(screen.getByRole('textbox', { name: /Negocio/ }), 'Panadería Ana');
      await user.type(screen.getByRole('textbox', { name: 'Comentario' }), 'Muy simple de usar');
      await user.click(screen.getByRole('radio', { name: '4 bloques' }));
      await user.click(screen.getByRole('button', { name: 'Publicar comentario' }));

      expect(screen.getByRole('status')).toHaveTextContent('Tu comentario ya es parte del sistema');
      const tarjetas = screen.getAllByRole('article').filter((a) => a.classList.contains('comentario'));
      expect(tarjetas[0]).toHaveTextContent('Muy simple de usar');
      expect(within(tarjetas[0] as HTMLElement).getByRole('img')).toHaveAccessibleName(
        'Puntaje: 4 de 5',
      );
      expect(screen.getByRole('textbox', { name: 'Nombre' })).toHaveValue('');

      const guardados = JSON.parse(localStorage.getItem('ensambla-comentarios') ?? '[]') as unknown[];
      expect(guardados).toEqual([
        { nombre: 'Ana', negocio: 'Panadería Ana', puntaje: 4, texto: 'Muy simple de usar' },
      ]);
    });

    it('recupera los comentarios guardados', async () => {
      localStorage.setItem(
        'ensambla-comentarios',
        JSON.stringify([{ nombre: 'Beto', negocio: '', puntaje: 5, texto: 'Me ordenó el día' }]),
      );
      await abrirLanding();

      expect(screen.getByText('“Me ordenó el día”')).toBeInTheDocument();
      expect(screen.getByText('Comunidad Ensambla')).toBeInTheDocument();
    });
  });

  describe('menú del encabezado', () => {
    it('abre y cierra el menú de secciones', async () => {
      const user = await abrirLanding();
      const boton = screen.getByRole('button', { name: 'Abrir menú' });

      expect(boton).toHaveAttribute('aria-expanded', 'false');
      await user.click(boton);
      expect(screen.getByRole('button', { name: 'Cerrar menú' })).toHaveAttribute(
        'aria-expanded',
        'true',
      );
      expect(screen.getByRole('navigation', { name: 'Menú de secciones' })).toBeVisible();

      await user.keyboard('{Escape}');
      expect(screen.getByRole('button', { name: 'Abrir menú' })).toHaveAttribute(
        'aria-expanded',
        'false',
      );
    });
  });
});
