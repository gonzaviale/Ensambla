import { screen, within } from '@testing-library/react';
import type { UserEvent } from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { estado } from '@/mocks/db/estado';
import {
  LO_QUE_TE_DEBEN,
  PRODUCTO_MAS_VENDIDO,
  cuandoSeAbreAgregarMetrica,
  cuandoSeAbreLaSolapa,
  cuandoSeAgregaLaSugerida,
  cuandoSeEligeLaPestana,
  cuandoSeEligeLaTarjeta,
  cuandoSeEligeVerlaComo,
  cuandoSeEligeDelMenu,
  cuandoSeRenombra,
  dadoElInicioDelAlmacen,
  entoncesElMenuNoDejaMoverA,
  entoncesElPanelParaAgregarSeCerro,
  entoncesEstaAbiertaLaSolapa,
  entoncesLaSolapaMuestraElTitulo,
  entoncesLasSolapasSon,
  entoncesQuedoGuardadaComo,
  entoncesSeVeComo,
  entoncesSeGuardoConLaFuente,
} from '@/test/panelDeTuNegocio';
import { ESPERA, verifyAlertPresentAsync } from '@/test/testHelpers';

const cuandoSeAbreArmalaVos = (usuario: UserEvent) => cuandoSeEligeLaPestana(usuario, 'Armala vos');

// Guardar queda deshabilitado hasta que llega el cálculo de la vista previa en vivo.
const entoncesSeVeLaVistaPrevia = async () => {
  expect(await screen.findByLabelText('Así quedaría', {}, ESPERA)).toBeInTheDocument();
};

describe('Inicio: métricas en solapas de «Tu negocio»', () => {
  beforeEach(() => estado.metricas.clear());

  it('sin métricas guardadas muestra sólo la solapa de ventas', async () => {
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
    await entoncesElPanelParaAgregarSeCerro();
  });

  it('cada sugerida se ve sólo con su número, sin gráfico, para que la lista no se alargue', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();

    await cuandoSeAbreAgregarMetrica(usuario);

    const sugerida = (await screen.findByRole('heading', { name: 'Qué producto vendo más' }, ESPERA))
      .parentElement as HTMLElement;
    expect(within(sugerida).queryByRole('list')).not.toBeInTheDocument();
  });

  it('el panel para agregar se cierra con Escape sin cambiar la solapa abierta', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();
    await cuandoSeAbreAgregarMetrica(usuario);

    await usuario.keyboard('{Escape}');

    await entoncesElPanelParaAgregarSeCerro();
    await entoncesEstaAbiertaLaSolapa('Ventas');
  });

  it('dice de dónde sale el número en palabras simples', async () => {
    const { usuario } = await dadoElInicioDelAlmacen(LO_QUE_TE_DEBEN);
    await cuandoSeAbreLaSolapa(usuario, 'Lo que te deben');

    await usuario.click(await screen.findByRole('button', { name: '¿De dónde sale?' }, ESPERA));

    expect(screen.getByRole('dialog', { name: '¿De dónde sale?' })).toHaveTextContent(
      /Sale de tus ventas/,
    );
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

  it('arma una propia en cinco pasos con tarjetas, con la vista previa en vivo', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();
    await cuandoSeAbreArmalaVos(usuario);

    await cuandoSeEligeLaTarjeta(usuario, '¿Qué querés mirar?', 'Ventas');
    await cuandoSeEligeLaTarjeta(usuario, '¿Qué querés saber?', 'Total de ventas');
    await cuandoSeEligeLaTarjeta(usuario, '¿Cómo querés verla?', 'Por día de la semana');
    await cuandoSeEligeLaTarjeta(usuario, '¿De cuándo?', 'Todo lo cargado');
    await entoncesSeVeLaVistaPrevia();

    await usuario.type(screen.getByLabelText('Nombre'), 'Ventas por día');
    await usuario.click(screen.getByRole('button', { name: 'Guardar métrica' }));
    await entoncesEstaAbiertaLaSolapa('Ventas por día');
  });

  it('no deja guardar la propia hasta contestar todas las solapas y ponerle nombre', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();
    await cuandoSeAbreArmalaVos(usuario);
    await screen.findByRole('radiogroup', { name: '¿Qué querés mirar?' }, ESPERA);
    expect(screen.queryByRole('radio', { checked: true })).not.toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Paso 5: Nombre' })).toBeDisabled();

    await cuandoSeEligeLaTarjeta(usuario, '¿Qué querés mirar?', 'Ventas');
    await cuandoSeEligeLaTarjeta(usuario, '¿Qué querés saber?', 'Total de ventas');
    await cuandoSeEligeLaTarjeta(usuario, '¿Cómo querés verla?', 'Por producto');
    await cuandoSeEligeLaTarjeta(usuario, '¿De cuándo?', 'Esta semana');

    expect(screen.getByRole('button', { name: 'Guardar métrica' })).toBeDisabled();
    expect(screen.getByText('Para guardarla te falta: Nombre.')).toBeInTheDocument();
  });

  it('sólo ofrece armar de lo que el negocio cargó: sin gastos no ofrece gastos', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();
    await cuandoSeAbreArmalaVos(usuario);
    const queMirar = await screen.findByRole(
      'radiogroup',
      { name: '¿Qué querés mirar?' },
      ESPERA,
    );
    expect(
      within(queMirar)
        .getAllByRole('radio')
        .map((opcion) => opcion.textContent),
    ).toEqual(['Ventas', 'Productos en stock']);
  });

  it('avisa «Ya la tenés» en la sugerida que ya muestra el panel de ventas, sin botón para agregarla', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();
    await cuandoSeAbreAgregarMetrica(usuario);
    const sugerida = (await screen.findByRole('heading', { name: 'Cuánto vendí esta semana' }, ESPERA))
      .closest('li') as HTMLElement;
    expect(within(sugerida).getByRole('status')).toHaveTextContent('Ya la tenés en tu panel');
    expect(within(sugerida).queryByRole('button')).not.toBeInTheDocument();
  });

  it('no deja guardar lo que ya muestra el panel de ventas y dice por qué', async () => {
    const { usuario } = await dadoElInicioDelAlmacen();
    await cuandoSeAbreArmalaVos(usuario);
    await cuandoSeEligeLaTarjeta(usuario, '¿Qué querés mirar?', 'Ventas');
    await cuandoSeEligeLaTarjeta(usuario, '¿Qué querés saber?', 'Total de ventas');
    await cuandoSeEligeLaTarjeta(usuario, '¿Cómo querés verla?', 'Todo junto');
    await cuandoSeEligeLaTarjeta(usuario, '¿De cuándo?', 'Este mes');
    await entoncesSeVeLaVistaPrevia();
    await usuario.type(screen.getByLabelText('Nombre'), 'Total del mes');
    await usuario.click(screen.getByRole('button', { name: 'Guardar métrica' }));
    await verifyAlertPresentAsync('Esa ya la tenés en el panel');
  });
});
