import { screen, waitFor, within } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { expect } from 'vitest';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { estado, type MetricaGuardada } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';
import type { DefinicionDeMetrica } from '@/types/metricas';

// El panel «Tu negocio» en solapas, como lo usa la persona: abrir una solapa, elegir del menú, agregar una métrica.

const ESPERA = { timeout: 3000 };

const receta = (nombre: string, cambios: Partial<DefinicionDeMetrica>): DefinicionDeMetrica => ({
  nombre,
  fuente: 'VENTAS',
  calculo: 'SUMA',
  agrupar: 'NINGUNA',
  periodo: 'TODO',
  soloFiado: false,
  ...cambios,
});

export const LO_QUE_TE_DEBEN = receta('Lo que te deben', { agrupar: 'CLIENTE', soloFiado: true });
export const PRODUCTO_MAS_VENDIDO = receta('Producto más vendido', {
  calculo: 'UNIDADES',
  agrupar: 'PRODUCTO',
  periodo: 'MES',
});

/** El inicio de Ana con su almacén de demo cargado y, si se dan, estas métricas ya guardadas en ese orden. */
export async function dadoElInicioDelAlmacen(...guardadas: DefinicionDeMetrica[]) {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  const almacen = crearAlmacenCargado(hoy());
  estado.sistemas.set(usuario.email, [almacen]);
  estado.metricas.set(almacen.id, guardadas.map(comoGuardada));
  renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  await screen.findByRole('tab', { name: 'Ventas' }, ESPERA);
  return { usuario: userEvent.setup(), almacen };
}

const comoGuardada = (definicion: DefinicionDeMetrica, lugar: number): MetricaGuardada => ({
  id: `m-${lugar}`,
  definicion,
  autoria: 'PROPIA',
});

const solapa = (nombre: string) => screen.findByRole('tab', { name: nombre }, ESPERA);

export const cuandoSeAbreLaSolapa = async (usuario: UserEvent, nombre: string) =>
  usuario.click(await solapa(nombre));

export const cuandoSeAbreAgregarMetrica = (usuario: UserEvent) =>
  cuandoSeAbreLaSolapa(usuario, 'Agregar métrica');

export async function cuandoSeEligeDelMenu(usuario: UserEvent, metrica: string, opcion: string) {
  await usuario.click(await screen.findByRole('button', { name: `Opciones de ${metrica}` }));
  await usuario.click(screen.getByRole('menuitem', { name: opcion }));
}

export async function cuandoSeRenombra(usuario: UserEvent, actual: string, nuevo: string) {
  await cuandoSeAbreLaSolapa(usuario, actual);
  await cuandoSeEligeDelMenu(usuario, actual, 'Cambiar el nombre');
  const campo = screen.getByLabelText('Nombre de la métrica');
  await usuario.clear(campo);
  await usuario.type(campo, nuevo);
  await usuario.click(screen.getByRole('button', { name: 'Guardar nombre' }));
}

export async function cuandoSeAgregaLaSugerida(usuario: UserEvent, nombre: string) {
  await cuandoSeAbreAgregarMetrica(usuario);
  await usuario.click(await screen.findByRole('button', { name: `Agregar ${nombre}` }, ESPERA));
}

const nombresDeLasSolapas = () =>
  screen.getAllByRole('tab').map((unaSolapa) => unaSolapa.textContent?.trim());

export const entoncesLasSolapasSon = (nombres: string[]) =>
  waitFor(
    () => expect(nombresDeLasSolapas()).toEqual(['Ventas', ...nombres, 'Agregar métrica']),
    ESPERA,
  );

export const entoncesEstaAbiertaLaSolapa = (nombre: string) =>
  waitFor(
    () => expect(screen.getByRole('tab', { selected: true })).toHaveTextContent(nombre),
    ESPERA,
  );

const cuerpo = () => within(screen.getByRole('tabpanel'));

export const entoncesLaSolapaMuestra = async (texto: string | RegExp) =>
  expect(await cuerpo().findByText(texto, undefined, ESPERA)).toBeInTheDocument();

export const entoncesLaSolapaMuestraElTitulo = async (nombre: string) =>
  expect(await cuerpo().findByRole('heading', { name: nombre }, ESPERA)).toBeInTheDocument();

export async function entoncesElMenuNoDejaMoverA(metrica: string, opcion: string) {
  await userEvent.click(await screen.findByRole('button', { name: `Opciones de ${metrica}` }));
  expect(screen.getByRole('menuitem', { name: opcion })).toBeDisabled();
}

export const entoncesSeGuardoConLaFuente = (
  almacenId: string,
  fuente: DefinicionDeMetrica['fuente'],
) => expect(estado.metricas.get(almacenId)?.[0]?.definicion.fuente).toBe(fuente);
