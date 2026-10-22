import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { MINIMO_DE_COMERCIOS } from '@/lib/sistema/zona';
import { estado } from '@/mocks/db/estado';
import {
  DIRECCION_EN_ZONA_CHICA_DE_PRUEBA,
  DIRECCION_SIN_ZONA_DE_PRUEBA,
  DIRECCIONES_DE_PRUEBA,
} from '@/mocks/db/ubicacion';
import { zonaParaTexto } from '@/mocks/db/zona';
import { server } from '@/mocks/node';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupServerResponse,
  setupUsuarioConSesion,
  verifyPathAsync,
  verifyToastAsync,
} from '@/test/testHelpers';

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
    expect(screen.getByText(/DATOS DE VILLA CRESPO, SIN NOMBRES/)).toBeVisible();
    // 12 productos: el que aportan menos de tres comercios (Fernet) no llega.
    expect(document.querySelectorAll('.zona-tarjeta')).toHaveLength(12);
    expect(screen.queryByText(/Fernet/)).toBeNull();
    expect(screen.getByText(/Datos agregados y anónimos/)).toBeVisible();
    expect(screen.getByText(/nunca se muestra uno solo ni dónde queda/)).toBeVisible();
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

    expect(screen.getByText(/necesitás un sistema con un bloque de Inventario/)).toBeVisible();
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
      expect(screen.getByText('Escribí un barrio o una dirección.')).toBeVisible(),
    );
  });
});

describe('Otra zona por punto del mapa', () => {
  const abrirOtraZona = async () => {
    const abierta = await abrirZona();
    await abierta.user.click(screen.getByRole('tab', { name: /Otra zona/ }));
    return abierta;
  };

  const buscarYElegir = async (
    user: ReturnType<typeof userEvent.setup>,
    consulta: string,
    direccion: string,
  ) => {
    await user.type(screen.getByLabelText('Barrio o dirección'), consulta);
    await clickButton(user, 'Buscar dirección');
    await user.click(await screen.findByRole('radio', { name: direccion }, ESPERA));
  };

  it('sin lugar elegido no muestra el mapa y lo explica', async () => {
    await abrirOtraZona();

    expect(screen.queryByRole('region', { name: 'Mapa de la zona a analizar' })).toBeNull();
    expect(screen.getByText(/Buscá un lugar y elegilo para verlo acá/)).toBeVisible();
    expect(screen.getByText('Análisis simulado')).toBeVisible();
  });

  it('busca una dirección, la elige, ve el mapa y analiza la zona de ese punto', async () => {
    const { user } = await abrirOtraZona();
    let pedido: unknown = null;
    server.use(
      http.post('/api/zona/analisis', async ({ request }) => {
        pedido = await request.json();
        return HttpResponse.json({
          nombre: 'San Justo - UNLaM',
          comercios: 10,
          productos: [
            {
              nombre: 'Leche La Serenísima 1 L',
              comercios: 10,
              precio: { minimo: 1500, promedio: 1650, maximo: 1800 },
              ventas: { comercios: 10, unidadesPorSemana: 60 },
            },
          ],
        });
      }),
    );

    await buscarYElegir(
      user,
      'Florencio Varela 1903, San Justo',
      DIRECCIONES_DE_PRUEBA[0].direccion,
    );
    expect(screen.getByRole('region', { name: 'Mapa de la zona a analizar' })).toBeVisible();
    await clickButton(user, 'Analizar la zona');

    expect(
      await screen.findByRole(
        'heading',
        { level: 2, name: 'Precios en San Justo - UNLaM' },
        ESPERA,
      ),
    ).toBeVisible();
    expect(pedido).toEqual({
      latitud: DIRECCIONES_DE_PRUEBA[0].latitud,
      longitud: DIRECCIONES_DE_PRUEBA[0].longitud,
    });
    expect(document.querySelectorAll('.zona-tarjeta')).toHaveLength(1);
  });

  it('con los datos de demostración, la zona de San Justo llega ordenada por lo que más se vende', async () => {
    const { user } = await abrirOtraZona();

    await buscarYElegir(
      user,
      'Florencio Varela 1903, San Justo',
      DIRECCIONES_DE_PRUEBA[0].direccion,
    );
    await clickButton(user, 'Analizar la zona');

    await screen.findByRole('heading', { level: 2, name: 'Precios en San Justo - UNLaM' }, ESPERA);
    const nombres = [...document.querySelectorAll('.zona-tarjeta strong')].map(
      (nodo) => nodo.textContent,
    );
    expect(nombres[0]).toBe('Leche La Serenísima 1 L');
    // Fernet lo aportan menos de tres comercios: no llega.
    expect(nombres).not.toContain('Fernet Branca 750 ml');
  });

  it('una zona con menos de tres comercios no muestra productos', async () => {
    const { user } = await abrirOtraZona();

    await buscarYElegir(user, 'Ramos Mejía', DIRECCION_EN_ZONA_CHICA_DE_PRUEBA.direccion);
    await clickButton(user, 'Analizar la zona');

    expect(
      await screen.findByText(/Hay 2 comercios en esta zona\. Hacen falta al menos 3/, {}, ESPERA),
    ).toBeVisible();
    expect(document.querySelectorAll('.zona-tarjeta')).toHaveLength(0);
  });

  it('un punto fuera de toda zona avisa que todavía no hay datos', async () => {
    const { user } = await abrirOtraZona();

    await buscarYElegir(user, 'Palermo', DIRECCION_SIN_ZONA_DE_PRUEBA.direccion);
    await clickButton(user, 'Analizar la zona');

    expect(
      await screen.findByText('Todavía no tenemos datos de esa zona.', {}, ESPERA),
    ).toBeVisible();
    expect(document.querySelectorAll('.zona-tarjeta')).toHaveLength(0);
  });

  it('un error del servidor se muestra sin datos viejos', async () => {
    const { user } = await abrirOtraZona();
    server.use(http.post('/api/zona/analisis', () => HttpResponse.error()));

    await buscarYElegir(
      user,
      'Florencio Varela 1903, San Justo',
      DIRECCIONES_DE_PRUEBA[0].direccion,
    );
    await clickButton(user, 'Analizar la zona');

    expect(
      await screen.findByText('No pudimos analizar esa zona. Probá de nuevo.', {}, ESPERA),
    ).toBeVisible();
  });

  it('al elegir otra dirección descarta el análisis anterior', async () => {
    const { user } = await abrirOtraZona();
    await buscarYElegir(
      user,
      'Florencio Varela 1903, San Justo',
      DIRECCIONES_DE_PRUEBA[0].direccion,
    );
    await clickButton(user, 'Analizar la zona');
    await screen.findByRole('heading', { level: 2, name: 'Precios en San Justo - UNLaM' }, ESPERA);

    await user.click(screen.getByRole('radio', { name: DIRECCIONES_DE_PRUEBA[1].direccion }));

    expect(
      screen.queryByRole('heading', { level: 2, name: 'Precios en San Justo - UNLaM' }),
    ).toBeNull();
  });
});
