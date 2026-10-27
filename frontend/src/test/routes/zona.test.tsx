import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { MINIMO_DE_COMERCIOS } from '@/lib/sistema/zona';
import { estado } from '@/mocks/db/estado';
import { zonaParaTexto } from '@/mocks/db/zona';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupServerResponse,
  setupUsuarioConSesion as setupUsuarioBase,
  verifyPathAsync,
  verifyToastAsync,
} from '@/test/testHelpers';

// Estos casos revisan todas las herramientas; los límites gratuitos tienen pruebas propias.
const setupUsuarioConSesion = (...args: Parameters<typeof setupUsuarioBase>) => {
  const usuario = setupUsuarioBase(...args);
  estado.pruebasPremium.set(usuario.email, {
    inicioEn: new Date(Date.now() - 1000).toISOString(),
    finEn: new Date(Date.now() + 7 * 86400000).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });
  return usuario;
};

const abrirZona = async (conDemo = false) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  if (conDemo) estado.sistemas.set(usuario.email, [crearAlmacenDeEjemplo(hoy())]);
  const router = renderApp('/zona');
  await screen.findByRole('heading', { level: 1, name: /Mirá cuánto se cobra en tu zona/ }, ESPERA);
  return { user: userEvent.setup(), router };
};

const tarjetaDe = (nombre: string) => {
  const nodo = screen.getByText(nombre, { selector: 'strong' }).closest('article');
  if (!nodo) throw new Error(`Falta la tarjeta de ${nombre}`);
  return nodo;
};

describe('Mi zona', () => {
  it('muestra los precios agregados de la zona, sin mapa ni comercios', async () => {
    await abrirZona();

    expect(
      screen.getByRole('heading', { level: 2, name: 'Precios en Villa Crespo' }),
    ).toBeVisible();
    expect(document.querySelector('.leaflet-container, .mapa-lienzo, [class*="map"]')).toBeNull();
    expect(screen.getByText(/DATOS DE VILLA CRESPO/)).toBeVisible();
    // 12 productos: el que aportan menos de tres comercios (Fernet) no llega.
    expect(document.querySelectorAll('.zona-tarjeta')).toHaveLength(12);
    expect(screen.queryByText(/Fernet/)).toBeNull();
    expect(screen.getByText(/Datos agregados y anónimos/)).toBeVisible();
    expect(screen.getByText(/Nunca se muestra un comercio solo ni dónde queda/)).toBeVisible();
  });

  it('el rango de cada producto es accesible y no nombra comercios', async () => {
    await abrirZona();

    const yerba = tarjetaDe('Yerba Playadito 1 kg');
    expect(
      within(yerba).getByRole('img', { name: /De \$ 3\.500 a \$ 4\.300, promedio \$ 3\.900/ }),
    ).toBeVisible();
    expect(within(yerba).getByText('11 comercios aportan este precio')).toBeVisible();
  });

  it('compara tus productos con la zona y cuenta cuántos están más baratos, en línea o más caros', async () => {
    await abrirZona(true);

    expect(
      within(tarjetaDe('Fideos Matarazzo 500 g')).getByText(/por debajo del promedio de tu zona/),
    ).toBeVisible();
    expect(
      within(tarjetaDe('Azúcar Ledesma 1 kg')).getByText(/por encima del promedio de tu zona/),
    ).toBeVisible();
    expect(within(tarjetaDe('Coca-Cola 2,25 L')).getByText(/en línea con tu zona/)).toBeVisible();
    // Lo que tenés va primero para comparar de entrada.
    expect(document.querySelector('.zona-tarjeta')?.classList.contains('mio')).toBe(true);
    const resumen = document.querySelector('.zona-ref');
    expect(resumen).toHaveTextContent('8 productos comparados');
    expect(resumen?.querySelectorAll('.zona-ref-nums strong')[0]).toHaveTextContent('1');
    expect(resumen?.querySelectorAll('.zona-ref-nums strong')[2]).toHaveTextContent('1');
  });

  it('busca un producto', async () => {
    const { user } = await abrirZona();

    await user.type(screen.getByLabelText('Buscar un producto'), 'yerba');

    expect(document.querySelectorAll('.zona-tarjeta')).toHaveLength(1);
    await user.clear(screen.getByLabelText('Buscar un producto'));
    await user.type(screen.getByLabelText('Buscar un producto'), 'caviar');
    expect(screen.getByText(/No hay datos de ese producto/)).toBeVisible();
  });

  it('con menos de tres comercios dice cuántos hay y no muestra datos', async () => {
    setupUsuarioConSesion();
    setupServerResponse('get', '/zona/agregados', 200, {
      nombre: 'Chacra Chica',
      comercios: 2,
      productos: [
        {
          nombre: 'Yerba Playadito 1 kg',
          comercios: 2,
          precio: { minimo: 3500, promedio: 3900, maximo: 4300 },
          ventas: { comercios: 2, unidadesPorSemana: 10 },
        },
      ],
    });
    renderApp('/zona');

    expect(
      await screen.findByText(
        /Hay 2 comercios en esta zona\. Hacen falta al menos 3 para mostrar datos/,
        {},
        ESPERA,
      ),
    ).toBeVisible();
    expect(document.querySelectorAll('.zona-tarjeta')).toHaveLength(0);
    expect(screen.queryByText(/Yerba/)).toBeNull();
  });

  it('desde el inicio se llega por el cartel y por el menú', async () => {
    setupUsuarioConSesion();
    const router = renderApp('/inicio');
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /Ver mi zona/ }, ESPERA));
    await verifyPathAsync(router, '/zona');

    await clickButton(user, 'Inicio');
    await clickButton(user, 'Mi zona');
    await verifyPathAsync(router, '/zona');
  });
});

describe('Qué se vende cerca', () => {
  it('ordena por cuántos comercios lo venden y deja sumar lo que falta a tu inventario', async () => {
    const { user } = await abrirZona(true);
    await user.click(screen.getByRole('tab', { name: /Qué se vende cerca/ }));

    const nombres = [...document.querySelectorAll('.zona-tarjeta strong')].map(
      (nodo) => nodo.textContent,
    );
    expect(nombres[0]).toBe('Leche La Serenísima 1 L');
    const leche = tarjetaDe('Leche La Serenísima 1 L');
    expect(within(leche).getByText(/Lo venden 12 de 12 comercios/)).toBeVisible();
    expect(
      within(leche).getByRole('img', { name: '100% de los comercios de la zona lo venden' }),
    ).toBeVisible();
    expect(within(tarjetaDe('Yerba Playadito 1 kg')).getByText('Ya lo tenés')).toBeVisible();

    await user.click(within(leche).getByRole('button', { name: /Sumar a mis productos/ }));

    await verifyToastAsync('Sumamos Leche La Serenísima 1 L a tu inventario, a $ 1.650.');
    const inventario = useSistemaStore.getState().sistemas[0]?.sections[0]?.blocks[0];
    expect(inventario?.rows.find((fila) => fila.name === 'Leche La Serenísima 1 L')).toMatchObject({
      quantity: 0,
      price: 1650,
    });
    expect(within(tarjetaDe('Leche La Serenísima 1 L')).getByText('Ya lo tenés')).toBeVisible();
  });

  it('sin un inventario avisa que hace falta uno y no deja sumar', async () => {
    const { user } = await abrirZona();
    await user.click(screen.getByRole('tab', { name: /Qué se vende cerca/ }));

    expect(screen.getByText(/necesitás un cuaderno con un bloque de Inventario/)).toBeVisible();
    expect(
      within(tarjetaDe('Leche La Serenísima 1 L')).getByRole('button', {
        name: /Sumar a mis productos/,
      }),
    ).toBeDisabled();
  });
});

describe('Otra zona', () => {
  const lugarConComercios = (suficientes: boolean) =>
    Array.from({ length: 200 }, (_, indice) => `Barrio ${indice}`).find(
      (lugar) => zonaParaTexto(lugar).comercios >= MINIMO_DE_COMERCIOS === suficientes,
    ) as string;

  it('analiza un barrio y muestra sus precios agregados', async () => {
    const { user } = await abrirZona(true);
    await user.click(screen.getByRole('tab', { name: /Otra zona/ }));
    const lugar = lugarConComercios(true);

    await user.type(screen.getByLabelText('Barrio o dirección'), lugar);
    await clickButton(user, 'Analizar la zona');

    expect(
      await screen.findByRole('heading', { level: 2, name: `Precios en ${lugar}` }, ESPERA),
    ).toBeVisible();
    expect(document.querySelectorAll('.zona-tarjeta').length).toBeGreaterThan(0);
    expect(
      screen.getByText(/productos comparados|Todavía no hay productos para comparar/),
    ).toBeVisible();
  });

  it('una zona con pocos comercios no muestra ningún dato', async () => {
    const { user } = await abrirZona();
    await user.click(screen.getByRole('tab', { name: /Otra zona/ }));
    const lugar = lugarConComercios(false);

    await user.type(screen.getByLabelText('Barrio o dirección'), lugar);
    await clickButton(user, 'Analizar la zona');

    expect(
      await screen.findByText(/Hacen falta al menos 3 para mostrar datos/, {}, ESPERA),
    ).toBeVisible();
    expect(document.querySelectorAll('.zona-tarjeta')).toHaveLength(0);
  });

  it('pide escribir algo antes de analizar', async () => {
    const { user } = await abrirZona();
    await user.click(screen.getByRole('tab', { name: /Otra zona/ }));

    await clickButton(user, 'Analizar la zona');

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('Escribí un barrio o una dirección.'),
    );
  });
});
