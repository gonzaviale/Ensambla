import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';

const abrirInicio = async () => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const almacen = crearAlmacenCargado(hoy());
  estado.sistemas.set(usuario.email, [almacen]);
  renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  await screen.findByRole('heading', { name: 'Tus métricas' });
  return { usuario: userEvent.setup(), almacen };
};

describe('Inicio: métricas a medida', () => {
  beforeEach(() => estado.metricas.clear());

  it('sin métricas guardadas invita a agregar la primera', async () => {
    await abrirInicio();
    expect(await screen.findByText(/Todavía no elegiste ninguna/)).toBeInTheDocument();
  });

  it('agrega una sugerida, la muestra con su número y se puede quitar', async () => {
    const { usuario } = await abrirInicio();
    await usuario.click(screen.getByRole('button', { name: /Agregar métrica/ }));
    await usuario.click(await screen.findByRole('button', { name: 'Agregar Cuánto vendí esta semana' }));

    const lista = await screen.findByRole('list', {}, { timeout: 3000 });
    const tarjeta = within(lista).getByRole('heading', { name: 'Cuánto vendí esta semana' }).closest('li');
    expect(tarjeta).not.toBeNull();
    await usuario.click(within(tarjeta as HTMLElement).getByRole('button', { name: '¿De dónde sale?' }));
    expect(within(tarjeta as HTMLElement).getByText(/Sale de la tabla de ventas/)).toBeInTheDocument();

    await usuario.click(within(tarjeta as HTMLElement).getByRole('button', { name: 'Quitar Cuánto vendí esta semana' }));
    await waitFor(() => expect(screen.getByText(/Todavía no elegiste ninguna/)).toBeInTheDocument());
  });

  it('arma una propia, la mira antes de guardar y avisa si falta el nombre', async () => {
    const { usuario } = await abrirInicio();
    await usuario.click(screen.getByRole('button', { name: /Agregar métrica/ }));
    await usuario.click(screen.getByRole('button', { name: 'Armar la mía' }));

    await usuario.click(screen.getByRole('button', { name: 'Guardar métrica' }));
    expect(screen.getByText('Ponele un nombre.')).toBeInTheDocument();

    await usuario.selectOptions(screen.getByLabelText('¿Cómo la querés repartir?'), 'DIA_DE_LA_SEMANA');
    await usuario.selectOptions(screen.getByLabelText('¿De cuándo?'), 'TODO');
    await usuario.click(screen.getByRole('button', { name: 'Ver cómo queda' }));
    expect(await screen.findByLabelText('Así quedaría')).toBeInTheDocument();

    await usuario.type(screen.getByLabelText('Nombre'), 'Ventas por día');
    await usuario.click(screen.getByRole('button', { name: 'Guardar métrica' }));
    expect(await screen.findByRole('heading', { name: 'Ventas por día' })).toBeInTheDocument();
  });

  it('si no se puede calcular dice por qué y no muestra un cero', async () => {
    const { usuario } = await abrirInicio();
    await usuario.click(screen.getByRole('button', { name: /Agregar métrica/ }));
    await usuario.click(screen.getByRole('button', { name: 'Armar la mía' }));
    await usuario.selectOptions(screen.getByLabelText('¿Qué querés mirar?'), 'GASTOS');
    await usuario.click(screen.getByRole('button', { name: 'Ver cómo queda' }));
    const vista = await screen.findByLabelText('Así quedaría');
    expect(within(vista).getByRole('status')).toHaveTextContent(/gastos/);
  });

  it('muestra el error del servidor al guardar una métrica repetida', async () => {
    const { usuario } = await abrirInicio();
    const agregar = async () => {
      await usuario.click(await screen.findByRole('button', { name: /Agregar métrica/ }));
      await usuario.click(screen.getByRole('button', { name: 'Armar la mía' }));
      await usuario.type(screen.getByLabelText('Nombre'), 'Total del mes');
      await usuario.click(screen.getByRole('button', { name: 'Guardar métrica' }));
    };
    await agregar();
    await screen.findByRole('heading', { name: 'Total del mes' });
    await agregar();
    expect(await screen.findByRole('alert')).toHaveTextContent('Ya tenés una métrica igual a esa.');
  });
});
