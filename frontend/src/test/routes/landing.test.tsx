import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { verifyHeadingPresentAsync } from '@/test/testHelpers';

async function abrirLanding() {
  renderApp('/');
  await verifyHeadingPresentAsync('¿Todavía anotás todo en el cuaderno?');
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

  describe('el problema', () => {
    it('muestra el eslogan y el problema antes que el funcionamiento', async () => {
      await abrirLanding();

      const hero = document.getElementById('inicio') as HTMLElement;
      expect(within(hero).getByText('Tu sistema, un bloque a la vez.')).toBeInTheDocument();
      const problema = document.getElementById('el-problema') as HTMLElement;
      expect(
        within(problema).getByRole('heading', { level: 2, name: /Tu negocio anda/ }),
      ).toBeInTheDocument();
      expect(within(problema).getAllByRole('heading', { level: 3 })).toHaveLength(5);
      expect(within(problema).getByText(/A fin de mes no sé cuánto gané/)).toBeInTheDocument();
    });

    it('ordena las secciones: el problema va antes que cómo te ayuda', async () => {
      await abrirLanding();

      const orden = ['el-problema', 'sami', 'como-funciona', 'monitoreo', 'entrada', 'comentarios'];
      const posiciones = orden.map((id) => {
        const seccion = document.getElementById(id);
        expect(seccion, id).not.toBeNull();
        return Array.from(document.querySelectorAll('section')).indexOf(seccion as HTMLElement);
      });
      expect(posiciones).toEqual([...posiciones].sort((x, y) => x - y));
    });

    it('explica cómo te ayuda en tres pasos cortos', async () => {
      await abrirLanding();
      const seccion = document.getElementById('como-funciona') as HTMLElement;

      expect(within(seccion).getAllByRole('listitem')).toHaveLength(3);
      expect(within(seccion).getByText('Contale tu negocio')).toBeInTheDocument();
    });
  });

  describe('entrada de datos', () => {
    it('muestra la foto por defecto y cambia a voz y texto', async () => {
      const user = await abrirLanding();
      const foto = screen.getByRole('tab', { name: 'Foto' });

      expect(foto).toHaveAttribute('aria-selected', 'true');
      expect(screen.getByRole('tabpanel')).toHaveTextContent('Revisá este dato');

      await user.click(screen.getByRole('tab', { name: 'Voz' }));
      expect(screen.getByRole('tab', { name: 'Voz' })).toHaveAttribute('aria-selected', 'true');
      expect(foto).toHaveAttribute('aria-selected', 'false');
      expect(screen.getByRole('tabpanel')).toHaveTextContent('Vendí 3 yerbas');

      await user.click(screen.getByRole('tab', { name: 'Texto' }));
      expect(screen.getByRole('tabpanel')).toHaveTextContent('Llegaron 24 gaseosas');
    });

    it('se mueve entre pestañas con el teclado', async () => {
      const user = await abrirLanding();

      screen.getByRole('tab', { name: 'Foto' }).focus();
      await user.keyboard('{ArrowRight}');
      expect(screen.getByRole('tab', { name: 'Voz' })).toHaveFocus();
      expect(screen.getByRole('tab', { name: 'Voz' })).toHaveAttribute('aria-selected', 'true');

      await user.keyboard('{End}');
      expect(screen.getByRole('tab', { name: 'Texto' })).toHaveFocus();

      await user.keyboard('{ArrowRight}');
      expect(screen.getByRole('tab', { name: 'Foto' })).toHaveFocus();
    });
  });

  describe('Sami y los avisos', () => {
    it('tienen su sección con un título', async () => {
      await abrirLanding();

      for (const id of ['sami', 'monitoreo']) {
        const seccion = document.getElementById(id) as HTMLElement;
        expect(seccion, id).not.toBeNull();
        expect(within(seccion).getByRole('heading', { level: 2 })).toBeInTheDocument();
      }
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
      const tarjetas = screen
        .getAllByRole('article')
        .filter((a) => a.classList.contains('comentario'));
      expect(tarjetas[0]).toHaveTextContent('Muy simple de usar');
      expect(within(tarjetas[0] as HTMLElement).getByRole('img')).toHaveAccessibleName(
        'Puntaje: 4 de 5',
      );
      expect(screen.getByRole('textbox', { name: 'Nombre' })).toHaveValue('');

      const guardados = JSON.parse(
        localStorage.getItem('ensambla-comentarios') ?? '[]',
      ) as unknown[];
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
