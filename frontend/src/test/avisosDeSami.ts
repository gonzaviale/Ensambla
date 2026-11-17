import type { QueryClient } from '@tanstack/react-query';
import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect } from 'vitest';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { estado } from '@/mocks/db/estado';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import { ESPERA, clickButton, setupUsuarioConSesion as setupUsuarioBase } from '@/test/testHelpers';

const COCA_COLA = 'Coca-Cola 2,25 L';
const AVISO_DE_STOCK = `${COCA_COLA} está por debajo del mínimo`;
const CLAVE_DE_AVISOS_DEL_SERVIDOR = ['avisos', 'pendientes'];

let clienteDeConsultas: QueryClient;

// Estos casos revisan todas las herramientas; los límites gratuitos tienen pruebas propias.
export const setupUsuarioConSesion = (...args: Parameters<typeof setupUsuarioBase>) => {
  const usuario = setupUsuarioBase(...args);
  estado.pruebasPremium.set(usuario.email, {
    inicioEn: new Date(Date.now() - 1000).toISOString(),
    finEn: new Date(Date.now() + 7 * 86400000).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });
  return usuario;
};

const negocioConfigurado = {
  nombre: 'Mi almacén',
  rubro: 'ALMACEN',
  comparteDatosZona: true,
  zona: null,
  ubicacion: {
    direccion: 'Calle 123',
    localidad: 'San Justo',
    latitud: -34.67,
    longitud: -58.56,
  },
};

export const abrirConDemo = async (ruta: 'usar' | 'construir' = 'usar', configurado = false) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  if (configurado) estado.negocios.set(usuario.email, negocioConfigurado);
  const sistema = crearAlmacenDeEjemplo(hoy());
  estado.sistemas.set(usuario.email, [sistema]);
  const router = renderApp(`/sistemas/${sistema.id}?modo=${ruta}`);
  await screen.findByRole('heading', { level: 1, name: /Almacén de ejemplo/ }, ESPERA);
  await waitFor(() => expect(useSistemaStore.getState().avisos.length).toBeGreaterThan(0), ESPERA);
  const cliente = router.options.context.queryClient as QueryClient;
  return { user: userEvent.setup(), sistema, usuario, cliente };
};

export const dadoUnAlmacenDeEjemploAbierto = async () => {
  clienteDeConsultas = (await abrirConDemo()).cliente;
};

export const dadoUnAlmacenDeEjemploAbiertoConSamiAtenta = async () => {
  await dadoUnAlmacenDeEjemploAbierto();
  await waitFor(() => expect(consultasDelServidorEnCurso()).toBe(0), ESPERA);
  await waitFor(() => expect(estadoDeLasConsultasDelServidor()).toBe('success'), ESPERA);
};

export const cuandoBajaElStockDeLaGaseosa = () => {
  act(() => bajarElStockDeLaGaseosaA(3));
};

export const cuandoSeAbreElAvisoPendiente = async () => {
  await screen.findByRole('button', { name: 'Ver el aviso' }, ESPERA);
  await clickButton(userEvent.setup(), 'Ver el aviso');
};

export const entoncesSamiTodaviaNoAvisaNada = () => {
  expect(screen.queryByText('Sami vio un cambio')).toBeNull();
};

export const entoncesAparecePendienteElAvisoDeStockDeLaGaseosa = async () => {
  const encabezado = await screen.findByText('Sami vio un cambio', {}, ESPERA);
  const aviso = encabezado.closest<HTMLElement>('[role="status"]');
  expect(within(aviso as HTMLElement).getByText(AVISO_DE_STOCK)).toBeVisible();
};

export const entoncesElPanelResaltaElAvisoDeStockDeLaGaseosa = async () => {
  expect(await screen.findByRole('region', { name: /Lo que encontré/ }, ESPERA)).toBeVisible();
  expect(document.querySelector('.alerta.foco')).toHaveTextContent(AVISO_DE_STOCK);
};

const consultasDelServidorEnCurso = () =>
  clienteDeConsultas.isFetching({ queryKey: CLAVE_DE_AVISOS_DEL_SERVIDOR });

const estadoDeLasConsultasDelServidor = () =>
  clienteDeConsultas.getQueryState(CLAVE_DE_AVISOS_DEL_SERVIDOR)?.status;

const bajarElStockDeLaGaseosaA = (cantidad: number) => {
  const { sistemas, actualizarSistema } = useSistemaStore.getState();
  actualizarSistema(sistemas[0]?.id ?? '', (sistema) => {
    const gaseosa = sistema.sections[0]?.blocks[0]?.rows.find((fila) => fila.name === COCA_COLA);
    if (gaseosa) gaseosa.quantity = cantidad;
  });
};
