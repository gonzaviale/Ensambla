import { screen, waitFor, within } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { estado } from '@/mocks/db/estado';
import { entoncesApareceQueEntendio } from '@/test/loQueEntendi';
import { renderApp } from '@/test/renderApp';
import type { DefinicionDeMetrica } from '@/types/metricas';
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

const VENTAS_DE_LA_SEMANA: DefinicionDeMetrica = {
  nombre: 'Ventas de la semana',
  fuente: 'VENTAS',
  calculo: 'SUMA',
  agrupar: 'NINGUNA',
  periodo: 'SEMANA',
  soloFiado: false,
};
const GASTOS_DEL_MES: DefinicionDeMetrica = {
  nombre: 'Gastos del mes',
  fuente: 'GASTOS',
  calculo: 'SUMA',
  agrupar: 'NINGUNA',
  periodo: 'MES',
  soloFiado: false,
};

const dadoUnPanelConVentasYGastos = async () => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const almacen = crearAlmacenCargado(hoy());
  estado.sistemas.set(usuario.email, [almacen]);
  estado.metricas.set(almacen.id, [
    { id: 'm-ventas', definicion: VENTAS_DE_LA_SEMANA, autoria: 'SUGERIDA' },
    { id: 'm-gastos', definicion: GASTOS_DEL_MES, autoria: 'PROPIA' },
  ]);
  renderApp('/inicio');
  await screen.findByRole('heading', { name: 'Gastos del mes' }, { timeout: 3000 });
  return { usuario: userEvent.setup(), almacen };
};

const cuandoSeRenombra = async (usuario: UserEvent, actual: string, nuevo: string) => {
  await usuario.click(screen.getByRole('button', { name: `Renombrar ${actual}` }));
  const campo = screen.getByLabelText('Nombre de la métrica');
  await usuario.clear(campo);
  await usuario.type(campo, nuevo);
  await usuario.click(screen.getByRole('button', { name: 'Guardar nombre' }));
};

const cuandoSePideConPalabras = async (usuario: UserEvent, pedido: string) => {
  await usuario.click(screen.getByRole('button', { name: /Agregar métrica/ }));
  await usuario.click(screen.getByRole('button', { name: 'Pedila con tus palabras' }));
  await usuario.type(screen.getByLabelText('¿Qué querés ver?'), pedido);
  await usuario.click(screen.getByRole('button', { name: 'Ver qué entendí' }));
};

const entoncesElPanelMuestraEnOrden = async (nombres: string[]) => {
  await waitFor(() =>
    expect(
      within(screen.getByRole('list', { name: 'Métricas del panel' }))
        .getAllByRole('heading', { level: 3 })
        .map((titulo) => titulo.textContent),
    ).toEqual(nombres),
  );
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
    await usuario.click(
      await screen.findByRole('button', { name: 'Agregar Qué producto vendo más' }),
    );

    const lista = await screen.findByRole(
      'list',
      { name: 'Métricas del panel' },
      { timeout: 3000 },
    );
    const tarjeta = within(lista)
      .getByRole('heading', { name: 'Qué producto vendo más' })
      .closest('li');
    expect(tarjeta).not.toBeNull();
    await usuario.click(
      within(tarjeta as HTMLElement).getByRole('button', { name: '¿De dónde sale?' }),
    );
    expect(
      within(tarjeta as HTMLElement).getByText(/Sale de la tabla de ventas/),
    ).toBeInTheDocument();

    await usuario.click(
      within(tarjeta as HTMLElement).getByRole('button', {
        name: 'Quitar Qué producto vendo más',
      }),
    );
    await waitFor(() =>
      expect(screen.getByText(/Todavía no elegiste ninguna/)).toBeInTheDocument(),
    );
  });

  it('arma una propia, la mira antes de guardar y avisa si falta el nombre', async () => {
    const { usuario } = await abrirInicio();
    await usuario.click(screen.getByRole('button', { name: /Agregar métrica/ }));
    await usuario.click(screen.getByRole('button', { name: 'Armar la mía' }));

    await usuario.click(await screen.findByRole('button', { name: 'Guardar métrica' }));
    expect(screen.getByText('Ponele un nombre.')).toBeInTheDocument();

    await usuario.selectOptions(
      screen.getByLabelText('¿Cómo la querés repartir?'),
      'DIA_DE_LA_SEMANA',
    );
    await usuario.selectOptions(screen.getByLabelText('¿De cuándo?'), 'TODO');
    await usuario.click(screen.getByRole('button', { name: 'Ver cómo queda' }));
    expect(await screen.findByLabelText('Así quedaría')).toBeInTheDocument();

    await usuario.type(screen.getByLabelText('Nombre'), 'Ventas por día');
    await usuario.click(screen.getByRole('button', { name: 'Guardar métrica' }));
    expect(await screen.findByRole('heading', { name: 'Ventas por día' })).toBeInTheDocument();
  });

  it('sólo ofrece armar de lo que el negocio cargó: sin gastos no ofrece gastos', async () => {
    const { usuario } = await abrirInicio();
    await usuario.click(screen.getByRole('button', { name: /Agregar métrica/ }));
    await usuario.click(screen.getByRole('button', { name: 'Armar la mía' }));
    const queMirar = await screen.findByLabelText('¿Qué querés mirar?');
    expect(
      within(queMirar)
        .getAllByRole('option')
        .map((opcion) => opcion.textContent),
    ).toEqual(['Ventas', 'Productos en stock']);
  });

  it('marca «Ya la tenés» la sugerida que ya muestra el panel de ventas y no deja agregarla', async () => {
    const { usuario } = await abrirInicio();
    await usuario.click(screen.getByRole('button', { name: /Agregar métrica/ }));
    expect(
      await screen.findByRole('button', { name: 'Ya tenés Cuánto vendí esta semana' }),
    ).toBeDisabled();
  });

  it('renombra una métrica y muestra el nombre nuevo sin cambiar lo que calcula', async () => {
    const { usuario, almacen } = await dadoUnPanelConVentasYGastos();
    await cuandoSeRenombra(usuario, 'Ventas de la semana', 'Lo que vendí');
    await entoncesElPanelMuestraEnOrden(['Lo que vendí', 'Gastos del mes']);
    expect(estado.metricas.get(almacen.id)?.[0]?.definicion.fuente).toBe('VENTAS');
  });

  it('no deja guardar un nombre vacío', async () => {
    const { usuario } = await dadoUnPanelConVentasYGastos();
    await cuandoSeRenombra(usuario, 'Ventas de la semana', '   ');
    expect(screen.getByRole('alert')).toHaveTextContent('Ponele un nombre.');
  });

  it('baja la primera y sube la última cambiando el orden del panel', async () => {
    const { usuario } = await dadoUnPanelConVentasYGastos();
    await usuario.click(screen.getByRole('button', { name: 'Bajar Ventas de la semana' }));
    await entoncesElPanelMuestraEnOrden(['Gastos del mes', 'Ventas de la semana']);
    await usuario.click(screen.getByRole('button', { name: 'Subir Ventas de la semana' }));
    await entoncesElPanelMuestraEnOrden(['Ventas de la semana', 'Gastos del mes']);
  });

  it('no deja subir la primera ni bajar la última', async () => {
    await dadoUnPanelConVentasYGastos();
    expect(screen.getByRole('button', { name: 'Subir Ventas de la semana' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Bajar Gastos del mes' })).toBeDisabled();
  });

  it('pide una métrica con sus palabras, la mira y la guarda', async () => {
    const { usuario } = await abrirInicio();
    await cuandoSePideConPalabras(usuario, '¿Qué producto vendo más este mes?');
    const vista = await screen.findByLabelText('Así quedaría');
    expect(
      within(vista).getByRole('heading', { name: 'Qué producto vendo más este mes' }),
    ).toBeInTheDocument();
    expect(within(vista).getByText(/Por producto/)).toBeInTheDocument();
    await usuario.click(within(vista).getByRole('button', { name: 'Guardar métrica' }));
    await entoncesElPanelMuestraEnOrden(['Qué producto vendo más este mes']);
  });

  it('al pedir con sus palabras muestra qué entendió, qué tomó por defecto y qué no entendió', async () => {
    const { usuario } = await abrirInicio();

    await cuandoSePideConPalabras(usuario, 'cuanto vendi el finde');

    await entoncesApareceQueEntendio('Período: este mes (no dijiste cuándo)', '«finde»');
  });

  it('si no entiende el pedido dice qué falta y da ejemplos', async () => {
    const { usuario } = await abrirInicio();
    await cuandoSePideConPalabras(usuario, 'hola, ¿cómo andás?');
    const aviso = await screen.findByRole('alert');
    expect(aviso).toHaveTextContent('Decime si es de tus ventas, tus gastos o tu stock');
    expect(within(aviso).getByText('«Cuánto vendí esta semana»')).toBeInTheDocument();
  });

  it('descarta lo que entendió sin guardar nada', async () => {
    const { usuario, almacen } = await abrirInicio();
    await cuandoSePideConPalabras(usuario, 'cuánto gasté en total');
    const vista = await screen.findByLabelText('Así quedaría');
    await usuario.click(within(vista).getByRole('button', { name: 'Descartar' }));
    expect(screen.queryByLabelText('Así quedaría')).not.toBeInTheDocument();
    expect(estado.metricas.get(almacen.id) ?? []).toEqual([]);
  });

  it('no deja guardar lo que ya muestra el panel de ventas y dice por qué', async () => {
    const { usuario } = await abrirInicio();
    await usuario.click(await screen.findByRole('button', { name: /Agregar métrica/ }));
    await usuario.click(screen.getByRole('button', { name: 'Armar la mía' }));
    await usuario.type(await screen.findByLabelText('Nombre'), 'Total del mes');
    await usuario.click(screen.getByRole('button', { name: 'Guardar métrica' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Esa ya la tenés en el panel');
  });
});
