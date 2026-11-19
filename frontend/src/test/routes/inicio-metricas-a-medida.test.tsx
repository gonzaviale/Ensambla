import { screen, within } from '@testing-library/react';
import type { UserEvent } from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { estado } from '@/mocks/db/estado';
import { entoncesApareceQueEntendio } from '@/test/loQueEntendi';
import {
  LO_QUE_TE_DEBEN,
  PRODUCTO_MAS_VENDIDO,
  cuandoSeAbreAgregarMetrica,
  cuandoSeAbreLaSolapa,
  cuandoSeAgregaLaSugerida,
  cuandoSeEligeVerlaComo,
  cuandoSeEligeDelMenu,
  cuandoSeRenombra,
  dadoElInicioDelAlmacen,
  entoncesElMenuNoDejaMoverA,
  entoncesEstaAbiertaLaSolapa,
  entoncesLaSolapaMuestra,
  entoncesLaSolapaMuestraElTitulo,
  entoncesLasSolapasSon,
  entoncesQuedoGuardadaComo,
  entoncesSeVeComo,
  entoncesSeGuardoConLaFuente,
} from '@/test/panelDeTuNegocio';

const cuandoSePideConPalabras = async (usuario: UserEvent, pedido: string) => {
  await cuandoSeAbreAgregarMetrica(usuario);
  await usuario.click(screen.getByRole('button', { name: 'Pedila con tus palabras' }));
  await usuario.type(screen.getByLabelText('¿Qué querés ver?'), pedido);
  await usuario.click(screen.getByRole('button', { name: 'Ver qué entendí' }));
};

const cuandoSeAbreArmarLaMia = async (usuario: UserEvent) => {
  await cuandoSeAbreAgregarMetrica(usuario);
  await usuario.click(screen.getByRole('button', { name: 'Armar la mía' }));
};

describe('Inicio: métricas en solapas de «Tu negocio»', () => {
  beforeEach(() => estado.metricas.clear());

  it('sin métricas guardadas muestra la solapa de ventas y la de agregar', async () => {
    await dadoElInicioDelAlmacen();

    await entoncesLasSolapasSon([]);
  });

  it('arranca abierta en las ventas', async () => {
    await dadoElInicioDelAlmacen(LO_QUE_TE_DEBEN);

    await entoncesEstaAbiertaLaSolapa('Ventas');
  });

  it('cada métrica guardada es una solapa más, en su orden', async () => {
    await dadoElInicioDelAlmacen(LO_QUE_TE_DEBEN, PRODUCTO_MAS_VENDIDO);

    await entoncesLasSolapasSon(['Lo que te deben', 'Producto más vendido']);
  });

  it('al abrir la solapa de una métrica muestra su número en el mismo panel', async () => {
    const { usuario } = await dadoElInicioDelAlmacen(LO_QUE_TE_DEBEN);

    await cuandoSeAbreLaSolapa(usuario, 'Lo que te deben');

    await entoncesLaSolapaMuestraElTitulo('Lo que te deben');
  });

  it('al agregar una sugerida aparece su solapa y queda abierta', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();

    await cuandoSeAgregaLaSugerida(usuario, 'Qué producto vendo más');

    await entoncesEstaAbiertaLaSolapa('Qué producto vendo más');
  });

  it('dice de dónde sale el número en palabras simples', async () => {
    const { usuario } = await dadoElInicioDelAlmacen(LO_QUE_TE_DEBEN);
    await cuandoSeAbreLaSolapa(usuario, 'Lo que te deben');

    await usuario.click(await screen.findByRole('button', { name: '¿De dónde sale?' }));

    await entoncesLaSolapaMuestra(/Sale de tus ventas/);
  });

  it('quitar una métrica desde el menú saca su solapa y vuelve a las ventas', async () => {
    const { usuario } = await dadoElInicioDelAlmacen(LO_QUE_TE_DEBEN);
    await cuandoSeAbreLaSolapa(usuario, 'Lo que te deben');

    await cuandoSeEligeDelMenu(usuario, 'Lo que te deben', 'Quitar del panel');

    await entoncesLasSolapasSon([]);
  });

  it('renombra una métrica desde el menú sin cambiar lo que calcula', async () => {
    const { usuario, almacen } = await dadoElInicioDelAlmacen(LO_QUE_TE_DEBEN);

    await cuandoSeRenombra(usuario, 'Lo que te deben', 'Fiado');

    await entoncesLasSolapasSon(['Fiado']);
    entoncesSeGuardoConLaFuente(almacen.id, 'VENTAS');
  });

  it('no deja guardar un nombre vacío', async () => {
    const { usuario } = await dadoElInicioDelAlmacen(LO_QUE_TE_DEBEN);

    await cuandoSeRenombra(usuario, 'Lo que te deben', '   ');

    expect(screen.getByRole('alert')).toHaveTextContent('Ponele un nombre.');
  });

  it('mueve una métrica a la derecha y cambia el orden de las solapas', async () => {
    const { usuario } = await dadoElInicioDelAlmacen(LO_QUE_TE_DEBEN, PRODUCTO_MAS_VENDIDO);
    await cuandoSeAbreLaSolapa(usuario, 'Lo que te deben');

    await cuandoSeEligeDelMenu(usuario, 'Lo que te deben', 'Mover a la derecha');

    await entoncesLasSolapasSon(['Producto más vendido', 'Lo que te deben']);
  });

  it('no deja mover a la izquierda la primera métrica', async () => {
    const { usuario } = await dadoElInicioDelAlmacen(LO_QUE_TE_DEBEN, PRODUCTO_MAS_VENDIDO);
    await cuandoSeAbreLaSolapa(usuario, 'Lo que te deben');

    await entoncesElMenuNoDejaMoverA('Lo que te deben', 'Mover a la izquierda');
  });

  it('no deja mover a la derecha la última métrica', async () => {
    const { usuario } = await dadoElInicioDelAlmacen(LO_QUE_TE_DEBEN, PRODUCTO_MAS_VENDIDO);
    await cuandoSeAbreLaSolapa(usuario, 'Producto más vendido');

    await entoncesElMenuNoDejaMoverA('Producto más vendido', 'Mover a la derecha');
  });

  it('una métrica repartida arranca como barras', async () => {
    const { usuario } = await dadoElInicioDelAlmacen(LO_QUE_TE_DEBEN);
    await cuandoSeAbreLaSolapa(usuario, 'Lo que te deben');

    await entoncesSeVeComo('Barras');
  });

  it('elige ver una métrica como torta y queda guardado', async () => {
    const { usuario, almacen } = await dadoElInicioDelAlmacen(LO_QUE_TE_DEBEN);
    await cuandoSeAbreLaSolapa(usuario, 'Lo que te deben');

    await cuandoSeEligeVerlaComo(usuario, 'Torta');

    await entoncesQuedoGuardadaComo(almacen.id, 'DONA');
  });

  it('arma una propia, la mira antes de guardar y avisa si falta el nombre', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();
    await cuandoSeAbreArmarLaMia(usuario);

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
    await entoncesEstaAbiertaLaSolapa('Ventas por día');
  });

  it('sólo ofrece armar de lo que el negocio cargó: sin gastos no ofrece gastos', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();
    await cuandoSeAbreArmarLaMia(usuario);
    const queMirar = await screen.findByLabelText('¿Qué querés mirar?');
    expect(
      within(queMirar)
        .getAllByRole('option')
        .map((opcion) => opcion.textContent),
    ).toEqual(['Ventas', 'Productos en stock']);
  });

  it('marca «Ya la tenés» la sugerida que ya muestra el panel de ventas y no deja agregarla', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();
    await cuandoSeAbreAgregarMetrica(usuario);
    expect(
      await screen.findByRole('button', { name: 'Ya tenés Cuánto vendí esta semana' }),
    ).toBeDisabled();
  });

  it('pide una métrica con sus palabras, la mira y la guarda como una solapa', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();
    await cuandoSePideConPalabras(usuario, '¿Qué producto vendo más este mes?');
    const vista = await screen.findByLabelText('Así quedaría');
    expect(within(vista).getByText(/Por producto/)).toBeInTheDocument();
    await usuario.click(within(vista).getByRole('button', { name: 'Guardar métrica' }));
    await entoncesEstaAbiertaLaSolapa('Qué producto vendo más este mes');
  });

  it('al pedir con sus palabras muestra qué entendió, qué tomó por defecto y qué no entendió', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();

    await cuandoSePideConPalabras(usuario, 'cuanto vendi el finde');

    await entoncesApareceQueEntendio('Período: este mes (no dijiste cuándo)', '«finde»');
  });

  it('si no entiende el pedido dice qué falta y da ejemplos', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();
    await cuandoSePideConPalabras(usuario, 'hola, ¿cómo andás?');
    const aviso = await screen.findByRole('alert');
    expect(aviso).toHaveTextContent('Decime si es de tus ventas, tus gastos o tu stock');
    expect(within(aviso).getByText('«Cuánto vendí esta semana»')).toBeInTheDocument();
  });

  it('descarta lo que entendió sin guardar nada', async () => {
    const { usuario, almacen } = await dadoElInicioDelAlmacen();
    await cuandoSePideConPalabras(usuario, 'cuánto gasté en total');
    const vista = await screen.findByLabelText('Así quedaría');
    await usuario.click(within(vista).getByRole('button', { name: 'Descartar' }));
    expect(screen.queryByLabelText('Así quedaría')).not.toBeInTheDocument();
    expect(estado.metricas.get(almacen.id) ?? []).toEqual([]);
  });

  it('no deja guardar lo que ya muestra el panel de ventas y dice por qué', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();
    await cuandoSeAbreArmarLaMia(usuario);
    await usuario.type(await screen.findByLabelText('Nombre'), 'Total del mes');
    await usuario.click(screen.getByRole('button', { name: 'Guardar métrica' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Esa ya la tenés en el panel');
  });
});
