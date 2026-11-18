import type { QueryClient } from '@tanstack/react-query';
import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect } from 'vitest';
import { useSistemaStore } from '@/stores/sistemaStore';
import { ESPERA, clickButton } from '@/test/testHelpers';

const COCA_COLA = 'Coca-Cola 2,25 L';
const AVISO_DE_STOCK = `${COCA_COLA} está por debajo del mínimo`;
const CLAVE_DE_AVISOS_DEL_SERVIDOR = ['avisos', 'pendientes'];

export const entoncesNoHayAvisoDeSami = () => {
  expect(screen.queryByText('Sami vio un cambio')).toBeNull();
};

export const esperarQueSamiTomeSuLineaBase = async (cliente: QueryClient) => {
  await waitFor(
    () => expect(cliente.isFetching({ queryKey: CLAVE_DE_AVISOS_DEL_SERVIDOR })).toBe(0),
    ESPERA,
  );
  await waitFor(
    () => expect(cliente.getQueryState(CLAVE_DE_AVISOS_DEL_SERVIDOR)?.status).toBe('success'),
    ESPERA,
  );
};

export const cuandoBajaElStockDeLaGaseosa = () => {
  const { sistemas, actualizarSistema } = useSistemaStore.getState();
  act(() => {
    actualizarSistema(sistemas[0]?.id ?? '', (sistema) => {
      const gaseosa = sistema.sections[0]?.blocks[0]?.rows.find((fila) => fila.name === COCA_COLA);
      if (gaseosa) gaseosa.quantity = 3;
    });
  });
};

export const entoncesAparecePendienteElAviso = async () => {
  const encabezado = await screen.findByText('Sami vio un cambio', {}, ESPERA);
  const aviso = encabezado.closest<HTMLElement>('[role="status"]');
  expect(within(aviso as HTMLElement).getByText(AVISO_DE_STOCK)).toBeVisible();
};

export const cuandoSeAbreElAvisoDeSami = () => clickButton(userEvent.setup(), 'Ver el aviso');

export const entoncesElPanelResaltaElAvisoDeStock = async () => {
  expect(await screen.findByRole('region', { name: /Lo que encontré/ }, ESPERA)).toBeVisible();
  expect(document.querySelector('.aviso-fila.foco')).toHaveTextContent(AVISO_DE_STOCK);
};
