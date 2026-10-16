import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { describe, expect, it } from 'vitest';
import { pesos } from '@/lib/formato';
import { ORDEN_PLANES, PLANES } from '@/lib/planes/planes';
import { renderApp } from '@/test/renderApp';
import { verifyHeadingPresentAsync } from '@/test/testHelpers';

async function abrirLanding() {
  renderApp('/');
  await verifyHeadingPresentAsync('Contá tu negocio. Ensambla lo arma.');
  return userEvent.setup();
}

describe('landing', () => {
  it('tiene un único h1 y lleva a probar, a registro y a login', async () => {
    await abrirLanding();

    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    expect(screen.getAllByRole('link', { name: /Probá ahora/ })[0]).toHaveAttribute(
      'href',
      '/probar',
    );
    expect(screen.getAllByRole('link', { name: 'Crear mi sistema gratis' })[0]).toHaveAttribute(
      'href',
      '/registro',
    );
    expect(screen.getAllByRole('link', { name: 'Ya tengo cuenta' })[0]).toHaveAttribute(
      'href',
      '/login',
    );
  });

  it('cuenta para quién es: no sólo el almacén', async () => {
    await abrirLanding();
    const seccion = screen.getByRole('region', {
      name: 'No sólo el almacén. Tu negocio también.',
    });

    expect(within(seccion).getByText('Comercio de barrio')).toBeInTheDocument();
    expect(within(seccion).getByText('Gastronomía y viandas')).toBeInTheDocument();
    expect(within(seccion).getByText('Tu negocio, sea cual sea')).toBeInTheDocument();
  });

  describe('demo «Se ensambla solo»', () => {
    it('arma bloques con su motivo y muestra la respuesta que desbloquean', async () => {
      const user = await abrirLanding();
      const demo = screen.getByRole('region', { name: 'Se ensambla solo.' });
      const campo = within(demo).getByRole('textbox', {
        name: 'Contá tu negocio con tus palabras',
      });

      await user.clear(campo);
      expect(within(demo).getByText('Escribí tu negocio para empezar')).toBeInTheDocument();

      await user.type(campo, 'Tengo un almacén y a veces fío');

      expect(within(demo).getByText('Lo leí como: Comercio de barrio')).toBeInTheDocument();
      expect(within(demo).getByText('Fiados')).toBeInTheDocument();
      expect(within(demo).getByText(/La sumé porque mencionaste «fio»/)).toBeInTheDocument();
      expect(within(demo).getByText('¿Quién te debe?')).toBeInTheDocument();
      expect(within(demo).getByRole('status')).toHaveTextContent('4 bloques');
    });

    it('los ejemplos completan la frase y cambian el sistema', async () => {
      const user = await abrirLanding();
      const demo = screen.getByRole('region', { name: 'Se ensambla solo.' });
      const ejemplos = within(demo).getByRole('group', { name: 'Ejemplos para probar' });

      await user.click(
        within(ejemplos).getByRole('button', { name: /Hago tortas y anoto lo que gasto/ }),
      );

      expect(
        within(demo).getByRole('textbox', { name: 'Contá tu negocio con tus palabras' }),
      ).toHaveValue('Hago tortas y anoto lo que gasto en ingredientes');
      expect(within(demo).getByText('Lo leí como: Gastronomía y viandas')).toBeInTheDocument();
      expect(within(demo).getByText('Insumos')).toBeInTheDocument();
      expect(within(demo).getByText('¿Cuánto te cuesta cada producto?')).toBeInTheDocument();
    });
  });

  it('muestra cuatro respuestas de ejemplo, rotuladas', async () => {
    await abrirLanding();
    const seccion = screen.getByRole('region', { name: 'Cada bloque desbloquea una respuesta.' });

    expect(within(seccion).getAllByText('Número de ejemplo')).toHaveLength(4);
    expect(within(seccion).getByText('¿Cuánto te quedó este mes?')).toBeInTheDocument();
    expect(within(seccion).getByText('¿Quién te debe?')).toBeInTheDocument();
  });

  it('compara con otras herramientas en una tabla con título', async () => {
    await abrirLanding();
    const tabla = screen.getByRole('table', {
      name: 'Qué hacés vos y qué te queda con cada herramienta',
    });

    expect(within(tabla).getByRole('rowheader', { name: 'Treinta, Kyte' })).toBeInTheDocument();
    expect(
      within(tabla).getByRole('rowheader', { name: 'Excel, Google Sheets' }),
    ).toBeInTheDocument();
    expect(within(tabla).getByRole('rowheader', { name: 'Airtable, Notion' })).toBeInTheDocument();
    expect(within(tabla).getByRole('rowheader', { name: 'Ensambla' })).toBeInTheDocument();
    expect(tabla.querySelector('caption')).not.toBeNull();
  });

  it('muestra los tres planes con sus precios y una sola invitación', async () => {
    await abrirLanding();
    const seccion = screen.getByRole('region', {
      name: 'Empezá gratis. Crecé cuando lo necesites.',
    });

    for (const id of ORDEN_PLANES) {
      const { nombre, precioMensual } = PLANES[id];
      const tarjeta = within(seccion)
        .getByRole('heading', { level: 3, name: nombre })
        .closest('li');
      expect(tarjeta).not.toBeNull();
      // Intl usa espacios no separables: se comparan con espacios comunes.
      const texto = (tarjeta?.textContent ?? '').replace(/\s/g, ' ');
      expect(texto).toContain(pesos(precioMensual).replace(/\s/g, ' '));
    }
    expect(within(seccion).getByText(/Precios ilustrativos/)).toBeInTheDocument();
    expect(within(seccion).getByText('Probá 7 días gratis')).toBeInTheDocument();
    const invitaciones = within(seccion).getAllByRole('link');
    expect(invitaciones).toHaveLength(1);
    expect(invitaciones[0]).toHaveAttribute('href', '/registro');
  });

  it('las anclas del menú existen en la página', async () => {
    await abrirLanding();

    for (const id of ['se-arma', 'desbloquea', 'diferencia', 'planes']) {
      expect(document.getElementById(id)).not.toBeNull();
    }
  });

  it('no tiene violaciones de accesibilidad detectables (axe)', async () => {
    await abrirLanding();

    // El contraste no se puede medir en jsdom (sin estilos): se cuida con los tokens.
    const resultado = await axe(document.body, { rules: { 'color-contrast': { enabled: false } } });
    expect(resultado.violations.map((v) => `${v.id}: ${v.nodes[0]?.html ?? ''}`)).toEqual([]);
  });
});
