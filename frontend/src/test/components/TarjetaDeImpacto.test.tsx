import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { TarjetaDeImpacto } from '@/components/sistema/avisos/TarjetaDeImpacto';
import { server } from '@/mocks/node';
import type { ImpactoAvisos } from '@/types/avisos';
import { ESPERA } from '@/test/testHelpers';

const medido = (
  cambios: Partial<ImpactoAvisos['medidos'][number]> = {},
): ImpactoAvisos['medidos'][number] => ({
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

const renderTarjeta = (impacto: Partial<ImpactoAvisos>) => {
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
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <TarjetaDeImpacto />
    </QueryClientProvider>,
  );
};

describe('la tarjeta de impacto de los avisos', () => {
  it('muestra cuánto cambió lo que se vende por semana y el detalle de cada aviso medido', async () => {
    renderTarjeta({ ingresoSemanalDelta: 770, medidos: [medido()] });

    expect(
      await screen.findByText(/vendés .*770.* más por semana que antes/, undefined, ESPERA),
    ).toBeInTheDocument();
    expect(screen.getByText('Mermelada cara')).toBeInTheDocument();
    expect(
      screen.getByText(/0,4 → 1,5 unidades por día, comparando 14 días antes y después/),
    ).toBeInTheDocument();
    expect(screen.getByText(/otras cosas también cambian las ventas/)).toBeInTheDocument();
  });

  it('si se vende menos lo dice sin presentarlo como una ganancia', async () => {
    renderTarjeta({ ingresoSemanalDelta: -300, medidos: [medido({ ingresoSemanalDelta: -300 })] });

    expect(
      await screen.findByText(/vendés .*300.* menos por semana que antes/, undefined, ESPERA),
    ).toBeInTheDocument();
  });

  it('si no cambió, lo dice', async () => {
    renderTarjeta({ ingresoSemanalDelta: 0, medidos: [medido({ ingresoSemanalDelta: 0 })] });

    expect(await screen.findByText(/están igual que antes/, undefined, ESPERA)).toBeInTheDocument();
  });

  it('sin avisos medidos no muestra nada', async () => {
    const { container } = renderTarjeta({ medidos: [] });

    await new Promise((resolver) => setTimeout(resolver, 150));
    expect(container).toBeEmptyDOMElement();
  });
});
