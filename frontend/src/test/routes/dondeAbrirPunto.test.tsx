import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { FILTROS_INICIALES } from '@/lib/dondeAbrir';
import { pedidoDondeAbrir } from '@/hooks/dondeAbrir/queries';
import { adaptarDondeAbrir } from '@/lib/dondeAbrirRemoto';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';

// El mapa real (Leaflet) no recibe toques en jsdom: este doble solo reenvía el lugar tocado.
vi.mock('@/pages/DondeAbrir/MapaDeZonas', () => ({
  MapaDeZonas: ({
    onElegirPunto,
  }: {
    onElegirPunto?: (latitud: number, longitud: number) => void;
  }) => (
    <div>
      <button type="button" onClick={() => onElegirPunto?.(-34.6203, -58.4495)}>
        Tocar un lugar del mapa
      </button>
      <button type="button" onClick={() => onElegirPunto?.(-31.4, -64.2)}>
        Tocar un lugar lejano
      </button>
    </div>
  ),
}));

const abrirResultado = async () => {
  const usuario = setupUsuarioConSesion();
  estado.negocios.set(usuario.email, {
    nombre: 'Almacén de barrio',
    rubro: 'ALMACEN',
    ubicacion: {
      direccion: 'Av. Corrientes 5000',
      localidad: 'Villa Crespo',
      latitud: -34.5996,
      longitud: -58.4403,
    },
    comparteDatosZona: true,
    zona: 'Villa Crespo',
  });
  renderApp('/zona/donde-abrir');
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /Sí, es correcto/ }, ESPERA));
  for (const respuesta of [
    /Cerca de mi local actual/,
    /Un equilibrio/,
    /Movimiento residencial/,
    /^Mediano/,
  ]) {
    await user.click(await screen.findByRole('button', { name: respuesta }));
  }
  await screen.findByRole('list', { name: /Zonas recomendadas/ });
  return user;
};

describe('dónde abrir: medir un lugar del mapa', () => {
  it('el pedido lleva el lugar tocado y, sin él, no manda nada', () => {
    const sinLugar = pedidoDondeAbrir(FILTROS_INICIALES, {});
    const conLugar = pedidoDondeAbrir(FILTROS_INICIALES, {}, { latitud: -34.62, longitud: -58.45 });

    expect(sinLugar).not.toHaveProperty('punto');
    expect(conLugar.punto).toEqual({ latitud: -34.62, longitud: -58.45 });
  });

  it('la respuesta dice cuál es el lugar medido, o ninguno si no se pidió', () => {
    const base = {
      negocioActual: {
        nombre: 'Negocio',
        rubro: 'ALMACEN',
        direccion: null,
        latitud: null,
        longitud: null,
        zonaId: null,
      },
      evaluadas: 0,
      ranking: [],
      descartadas: [],
      avisos: [],
    };

    expect(adaptarDondeAbrir(base, FILTROS_INICIALES).puntoElegidoId).toBeNull();
    expect(
      adaptarDondeAbrir(
        { ...base, puntoElegidoId: '00000000-0000-4000-8000-0000000000ff' },
        FILTROS_INICIALES,
      ).puntoElegidoId,
    ).toBe('00000000-0000-4000-8000-0000000000ff');
  });

  it('al tocar el mapa mide ese lugar, lo suma al ranking y lo deja elegido', async () => {
    const user = await abrirResultado();
    expect(screen.getByText(/Tocá cualquier lugar del mapa/)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Tocar un lugar del mapa' }));

    const lista = await screen.findByRole('list', { name: /Zonas recomendadas/ });
    await waitFor(() =>
      expect(within(lista).getByRole('button', { name: /Punto elegido · cerca de/ })).toBeVisible(),
    );
    expect(await screen.findByText(/Modo demo: el lugar que elegiste se simula/)).toBeVisible();
    expect(
      await screen.findByRole('button', { name: 'Quitar el lugar' }, ESPERA),
    ).toBeInTheDocument();
  });

  it('la leyenda explica los puntos del mapa y sólo muestra las capas prendidas', async () => {
    const user = await abrirResultado();
    const puntos = () => screen.getByRole('list', { name: 'Qué significa cada punto del mapa' });

    expect(within(puntos()).getByText('Comercios similares')).toBeVisible();
    expect(within(puntos()).getByText('Escuelas')).toBeVisible();
    expect(within(puntos()).queryByText('Oficinas y bancos')).toBeNull();
    expect(screen.queryByText('Lugar que mediste')).toBeNull();

    await user.click(screen.getByText('Qué mostrar en el mapa'));
    await user.click(screen.getByRole('checkbox', { name: /Oficinas y bancos/ }));
    expect(within(puntos()).getByText('Oficinas y bancos')).toBeVisible();

    await user.click(screen.getByRole('button', { name: 'Tocar un lugar del mapa' }));
    expect(await screen.findByText('Lugar que mediste', undefined, ESPERA)).toBeVisible();
  });

  it('quitar el lugar vuelve al ranking de siempre', async () => {
    const user = await abrirResultado();
    await user.click(screen.getByRole('button', { name: 'Tocar un lugar del mapa' }));
    await user.click(await screen.findByRole('button', { name: 'Quitar el lugar' }, ESPERA));

    await waitFor(() =>
      expect(screen.queryByRole('button', { name: /Punto elegido · cerca de/ })).toBeNull(),
    );
    expect(screen.getByText(/Tocá cualquier lugar del mapa/)).toBeInTheDocument();
  });

  it('un lugar fuera de CABA y La Matanza avisa que no se pudo medir y deja quitarlo', async () => {
    const user = await abrirResultado();

    await user.click(screen.getByRole('button', { name: 'Tocar un lugar lejano' }));

    expect(
      await screen.findByText(/No pudimos medir ese lugar/, undefined, ESPERA),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Quitar el lugar' }));
    await waitFor(() => expect(screen.queryByText(/No pudimos medir ese lugar/)).toBeNull());
  });
});
