import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { createElement } from 'react';
import { expect } from 'vitest';
import { TarjetaDeImpacto } from '@/components/sistema/avisos/TarjetaDeImpacto';
import { server } from '@/mocks/node';
import type { ImpactoAvisos } from '@/types/avisos';
import { ESPERA } from '@/test/testHelpers';

type AvisoMedido = ImpactoAvisos['medidos'][number];

let consultas: QueryClient;
let tarjeta: HTMLElement;

const avisoMedido = (cambios: Partial<AvisoMedido> = {}): AvisoMedido => ({
  avisoId: 'aviso-1',
  tipo: 'ESTANCADO',
  titulo: 'Mermelada cara',
  aplicadoEn: '2026-10-01T15:00:00Z',
  ventanaDias: 14,
  unidadesPorDiaAntes: 0.4,
  unidadesPorDiaDespues: 1.5,
  ingresoPorDiaAntes: 40,
  ingresoPorDiaDespues: 150,
  ingresoSemanalDelta: 770,
  ...cambios,
});

const dadoUnImpactoDe = (impacto: Partial<ImpactoAvisos>) => {
  server.use(
    http.get('/api/avisos/impacto', () =>
      HttpResponse.json({
        aplicados: 1,
        descartados: 0,
        ingresoSemanalDelta: 0,
        medidos: [],
        ...impacto,
      }),
    ),
  );
};

export const dadoUnAvisoMedidoQueCambioElIngresoSemanalEn = (delta: number) =>
  dadoUnImpactoDe({ ingresoSemanalDelta: delta, medidos: [avisoMedido({ ingresoSemanalDelta: delta })] });

export const dadoUnImpactoSinAvisosMedidos = () => dadoUnImpactoDe({ medidos: [] });

export const cuandoSeMuestraLaTarjetaDeImpacto = () => {
  consultas = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  tarjeta = render(
    createElement(QueryClientProvider, { client: consultas }, createElement(TarjetaDeImpacto)),
  ).container;
};

export const entoncesDiceCuantoMasSeVendePorSemana = async () => {
  expect(
    await screen.findByText(/vendés .*770.* más por semana que antes/, undefined, ESPERA),
  ).toBeInTheDocument();
};

export const entoncesDiceCuantoMenosSeVendePorSemana = async () => {
  expect(
    await screen.findByText(/vendés .*300.* menos por semana que antes/, undefined, ESPERA),
  ).toBeInTheDocument();
};

export const entoncesDiceQueLasVentasEstanIgualQueAntes = async () => {
  expect(await screen.findByText(/están igual que antes/, undefined, ESPERA)).toBeInTheDocument();
};

export const entoncesMuestraElDetalleDelAvisoMedido = () => {
  expect(screen.getByText('Mermelada cara')).toBeInTheDocument();
  expect(
    screen.getByText(/0,4 → 1,5 unidades por día, comparando 14 días antes y después/),
  ).toBeInTheDocument();
  expect(screen.getByText(/otras cosas también cambian las ventas/)).toBeInTheDocument();
};

export const entoncesNoMuestraNada = async () => {
  await waitFor(() => {
    expect(consultas.isFetching()).toBe(0);
    expect(consultas.getQueryCache().getAll()[0]?.state.status).toBe('success');
  }, ESPERA);
  expect(tarjeta).toBeEmptyDOMElement();
};
