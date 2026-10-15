import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { pesos } from '@/lib/red/formato';
import { NOMBRE_FUNCION, ORDEN_PLANES, PLANES } from '@/lib/red/planes';
import { renderApp } from '@/test/renderApp';
import { verifyHeadingPresentAsync } from '@/test/testHelpers';

async function abrirLanding() {
  renderApp('/');
  await verifyHeadingPresentAsync('Tu sistema, un bloque a la vez.');
  return userEvent.setup();
}

// Los importes salen con espacio duro; el DOM de prueba los compara con un espacio común.
const plano = (texto: string) => texto.replace(/\s+/g, ' ');

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

  describe('la propuesta', () => {
    it('cuenta el dolor con el ejemplo del aceite', async () => {
      await abrirLanding();
      const seccion = screen.getByRole('region', {
        name: 'Vendés por debajo del costo y no te enterás.',
      });

      expect(within(seccion).getByRole('table')).toHaveTextContent('Aceite Natura 900 ml');
      expect(seccion).toHaveTextContent(plano(pesos(24800)));
    });

    it('muestra ocho avisos de ejemplo, cada uno con su acción de un toque', async () => {
      await abrirLanding();
      const seccion = screen.getByRole('region', {
        name: 'No buscás reportes: Ensambla te avisa.',
      });
      const avisos = within(seccion)
        .getAllByRole('listitem')
        .filter((li) => li.classList.contains('aviso'));

      expect(avisos).toHaveLength(8);
      avisos.forEach((aviso) => expect(aviso).toHaveTextContent('Un toque'));
      ['Precio en riesgo', 'Lista nueva', 'Stock por agotarse', 'Oferta que sirve'].forEach(
        (tipo) =>
          expect(
            within(seccion).getByRole('heading', { level: 3, name: tipo }),
          ).toBeInTheDocument(),
      );
    });

    it('explica la red con sus tres garantías', async () => {
      await abrirLanding();
      const seccion = screen.getByRole('region', {
        name: 'Cada comercio que se suma mejora la red.',
      });

      expect(seccion).toHaveTextContent('nunca se le muestra a otro comercio');
      expect(seccion).toHaveTextContent('5 comercios o más');
      expect(seccion).toHaveTextContent('nunca por esconder información');
    });
  });

  describe('demo «Llegó la lista de tu proveedor»', () => {
    const regionDemo = () =>
      screen.getByRole('region', { name: 'Llegó la lista de tu proveedor.' });

    it('muestra la línea tal como llega, sin cambios hasta que se confirma', async () => {
      await abrirLanding();
      const demo = within(regionDemo());

      expect(demo.getByText(plano('ACEITE NATURA 900 ML X12 — $ 26.520'))).toBeInTheDocument();
      expect(demo.getByRole('button', { name: 'Ensambla, revisá' })).toBeInTheDocument();
      expect(demo.queryByRole('button', { name: 'Confirmar' })).not.toBeInTheDocument();
    });

    it('revisa la línea y muestra costo, suba, precio sugerido y margen', async () => {
      const user = await abrirLanding();
      const demo = within(regionDemo());

      await user.click(demo.getByRole('button', { name: 'Ensambla, revisá' }));

      expect(regionDemo()).toHaveTextContent(plano(`Te sale cada una${pesos(2210)}`));
      expect(regionDemo()).toHaveTextContent('+16 %');
      expect(regionDemo()).toHaveTextContent('Precio en riesgo');
      expect(regionDemo()).toHaveTextContent(plano(`Precio sugerido${pesos(2770)}`));
      expect(regionDemo()).toHaveTextContent('Nada cambia sin tu confirmación.');
      expect(demo.getByRole('button', { name: 'Confirmar' })).toHaveFocus();
    });

    it('recalcula el precio sugerido con la regla que se elige', async () => {
      const user = await abrirLanding();
      const demo = within(regionDemo());
      await user.click(demo.getByRole('button', { name: 'Ensambla, revisá' }));

      const regla = demo.getByRole('button', { name: '30 %' });
      expect(demo.getByRole('button', { name: '25 %' })).toHaveAttribute('aria-pressed', 'true');
      await user.click(regla);

      expect(regla).toHaveAttribute('aria-pressed', 'true');
      expect(regionDemo()).toHaveTextContent(plano(`Precio sugerido${pesos(2880)}`));
    });

    it('confirma, pide cambiar la etiqueta y vuelve a empezar', async () => {
      const user = await abrirLanding();
      const demo = within(regionDemo());

      await user.click(demo.getByRole('button', { name: 'Ensambla, revisá' }));
      await user.click(demo.getByRole('button', { name: 'Confirmar' }));

      expect(regionDemo()).toHaveTextContent('Listo: cambiá la etiqueta');
      expect(regionDemo()).toHaveTextContent(plano(`Aceite Natura 900 ml: ${pesos(2770)}`));
      expect(demo.queryByRole('button', { name: 'Confirmar' })).not.toBeInTheDocument();

      await user.click(demo.getByRole('button', { name: 'Ver de nuevo' }));
      expect(demo.getByRole('button', { name: 'Ensambla, revisá' })).toHaveFocus();
    });

    it('se maneja sólo con el teclado', async () => {
      const user = await abrirLanding();
      const demo = within(regionDemo());

      demo.getByRole('button', { name: 'Ensambla, revisá' }).focus();
      await user.keyboard('{Enter}');
      expect(demo.getByRole('button', { name: 'Confirmar' })).toHaveFocus();

      await user.keyboard('{Enter}');
      expect(regionDemo()).toHaveTextContent('Listo: cambiá la etiqueta');
    });
  });

  describe('planes y precios', () => {
    const regionPlanes = () =>
      screen.getByRole('region', { name: 'Se paga solo con lo que te hace ganar.' });

    it('muestra los tres planes con el precio de PLANES y el más completo destacado', async () => {
      await abrirLanding();
      const planes = within(regionPlanes());

      ORDEN_PLANES.forEach((id) => {
        const plan = PLANES[id];
        const tarjeta = planes.getByRole('heading', { level: 3, name: plan.nombre }).closest('li');
        expect(tarjeta).not.toBeNull();
        expect(tarjeta).toHaveTextContent(plano(pesos(plan.precioMensual)));
        expect(tarjeta).toHaveTextContent(plan.bajada);
      });

      const destacada = planes
        .getByRole('heading', { level: 3, name: 'Comercio + Red' })
        .closest('li');
      expect(destacada).toHaveTextContent('El más completo');
      expect(destacada).toHaveTextContent(NOMBRE_FUNCION['comparar-proveedores']);
    });

    it('ofrece la prueba de 7 días, aclara que son precios ilustrativos y lleva a registro', async () => {
      await abrirLanding();
      const planes = within(regionPlanes());

      expect(planes.getByText('Probá 7 días gratis, sin tarjeta')).toBeInTheDocument();
      expect(regionPlanes()).toHaveTextContent('Precios ilustrativos');
      expect(planes.getAllByRole('link')).toHaveLength(1);
      expect(planes.getByRole('link', { name: /Empezar mi prueba/ })).toHaveAttribute(
        'href',
        '/registro',
      );
    });
  });

  it('la landing completa no tiene violaciones de accesibilidad (axe)', async () => {
    await abrirLanding();

    // El contraste no se puede medir en jsdom (sin estilos): se verifica a mano con los tokens.
    const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });

    expect(resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`)).toEqual([]);
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
