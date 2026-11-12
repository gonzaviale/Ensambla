import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { GraficoSemanal } from '@/pages/Objetivos/autogestion/GraficoSemanal';
import { HistorialReajustes } from '@/pages/Objetivos/autogestion/HistorialReajustes';
import { MiniObjetivoCard } from '@/pages/Objetivos/autogestion/MiniObjetivoCard';
import { RecorridoDeLaSemana } from '@/pages/Objetivos/autogestion/RecorridoDeLaSemana';
import type { DecisionObjetivo, MiniObjetivo, SemanasObjetivo } from '@/types/objetivos';

const mini: MiniObjetivo = {
  id: 'mini-1',
  posicion: 1,
  tipo: 'VENTAS_SEMANALES',
  sentido: 'SUBE',
  estado: 'EN_CURSO',
  titulo: 'Vendé $90.000 por semana',
  justificacion: 'Hoy vendés unos $78.000 por semana.',
  referencia: null,
  registroId: null,
  valorInicial: 78000,
  valorActual: 84000,
  meta: 90000,
  avance: 0.5,
  aporteEstimado: 12000,
  semanaInicio: '2026-11-02',
  textoConIa: false,
};

const decision = (
  id: string,
  tipo: DecisionObjetivo['tipo'],
  semanaInicio: string,
): DecisionObjetivo => ({
  id,
  tipo,
  miniObjetivoId: 'mini-1',
  avisoId: null,
  registroId: null,
  descripcion: `Decisión ${id}`,
  tomadaEn: `${semanaInicio}T15:00:00Z`,
  semanaInicio,
  efecto: 'SIN_MEDICION',
});

describe('MiniObjetivoCard', () => {
  it('muestra qué hacer, por qué, cuánto va y deja decidir, cambiar o quitar', async () => {
    const onDecidir = vi.fn();
    const onCambiar = vi.fn();
    const onQuitar = vi.fn();
    const user = userEvent.setup();
    render(
      <MiniObjetivoCard
        mini={mini}
        onDecidir={onDecidir}
        onCambiar={onCambiar}
        onQuitar={onQuitar}
      />,
    );

    const tarjeta = screen.getByRole('article', { name: 'Vendé $90.000 por semana' });
    expect(within(tarjeta).getByText('Hoy vendés unos $78.000 por semana.')).toBeVisible();
    expect(within(tarjeta).getByText('$84.000')).toBeVisible();
    expect(within(tarjeta).getByText('$90.000')).toBeVisible();
    expect(within(tarjeta).getByText('Te faltan $6.000 por semana')).toBeVisible();
    expect(within(tarjeta).getByRole('progressbar')).toHaveAttribute('aria-valuenow', '50');

    await user.click(screen.getByRole('button', { name: 'Lo hago' }));
    await user.click(screen.getByRole('button', { name: 'Ahora no' }));
    await user.click(
      screen.getByRole('button', { name: 'Cambiar «Vendé $90.000 por semana» por otro' }),
    );
    await user.click(screen.getByRole('button', { name: 'Quitar «Vendé $90.000 por semana»' }));

    expect(onDecidir.mock.calls).toEqual([['ACEPTADO'], ['IGNORADO']]);
    expect(onCambiar).toHaveBeenCalledOnce();
    expect(onQuitar).toHaveBeenCalledOnce();
  });

  it('si ya decidió esta semana lo dice en vez de volver a preguntar', () => {
    const { rerender } = render(
      <MiniObjetivoCard
        mini={mini}
        decidido="ACEPTADO"
        onDecidir={vi.fn()}
        onCambiar={vi.fn()}
        onQuitar={vi.fn()}
      />,
    );
    expect(screen.getByRole('status')).toHaveTextContent('Esta semana dijiste que lo hacés');
    expect(screen.queryByRole('button', { name: 'Lo hago' })).not.toBeInTheDocument();

    rerender(
      <MiniObjetivoCard
        mini={mini}
        decidido="IGNORADO"
        onDecidir={vi.fn()}
        onCambiar={vi.fn()}
        onQuitar={vi.fn()}
      />,
    );
    expect(screen.getByRole('status')).toHaveTextContent('Lo dejaste para después');
  });

  it('mientras guarda no deja tocar los botones', () => {
    render(
      <MiniObjetivoCard
        mini={mini}
        ocupado
        onDecidir={vi.fn()}
        onCambiar={vi.fn()}
        onQuitar={vi.fn()}
      />,
    );

    screen.getAllByRole('button').forEach((boton) => expect(boton).toBeDisabled());
  });
});

describe('GraficoSemanal', () => {
  const progreso = {
    meta: 500000,
    hito: null,
    minis: [],
    semanas: [
      { semana: '2026-10-26', valor: 150000, estimado: true },
      { semana: '2026-11-02', valor: 170000, estimado: false },
      { semana: '2026-11-09', valor: -10000, estimado: false },
    ],
  };

  it('dibuja la curva y marca cada decisión en su semana, con una tabla para lectores de pantalla', () => {
    const { container } = render(
      <GraficoSemanal
        progreso={progreso}
        decisiones={[
          decision('a', 'AVISO_APLICADO', '2026-11-02'),
          decision('b', 'AVISO_DESCARTADO', '2026-11-02'),
          decision('c', 'PASO_IGNORADO', '2026-09-01'),
        ]}
      />,
    );

    expect(screen.getByRole('img', { name: /meta de \$500\.000/ })).toBeInTheDocument();
    expect(container.querySelectorAll('.grafico-semanal-marca')).toHaveLength(2);
    expect(container.querySelectorAll('.grafico-semanal-punto.estimado')).toHaveLength(1);
    const filas = within(screen.getByRole('table')).getAllByRole('row');
    expect(filas[1]).toHaveTextContent('$150.000 (calculado con tus cuadernos)');
    expect(filas[1]).toHaveTextContent('Ninguna');
    expect(filas[2]).toHaveTextContent('Aplicaste: Decisión a; Descartaste: Decisión b');
    expect(screen.queryByText('Calculado con tus cuadernos')).not.toBeInTheDocument();
  });

  it('con una sola semana la pone al medio y sin estimados no los explica', () => {
    const { container } = render(
      <GraficoSemanal
        progreso={{
          ...progreso,
          meta: 0,
          semanas: [{ semana: '2026-11-09', valor: 0, estimado: false }],
        }}
        decisiones={[]}
      />,
    );

    expect(container.querySelector('.grafico-semanal-punto')).toHaveAttribute('cx', '338');
    expect(screen.queryByText('Calculado con tus cuadernos')).not.toBeInTheDocument();
  });

  it('sin semanas explica que todavía no hay nada para dibujar', () => {
    render(<GraficoSemanal progreso={{ ...progreso, semanas: [] }} decisiones={[]} />);

    expect(screen.getByText(/Todavía no hay semanas para dibujar/)).toBeVisible();
  });
});

describe('HistorialReajustes', () => {
  const semana = (
    cambios: Partial<SemanasObjetivo['semanas'][number]>,
  ): SemanasObjetivo['semanas'][number] => ({
    semana: '2026-11-02',
    resultado: 170000,
    avance: 0.34,
    decisiones: { aplicadas: 0, descartadas: 0, aceptadas: 0, ignoradas: 0 },
    queFunciono: '',
    textoConIa: false,
    reajustes: [],
    ...cambios,
  });

  it('cuenta qué se cambió por qué, y uno sin reemplazo solo con el anterior', () => {
    render(
      <HistorialReajustes
        semanas={[
          semana({
            reajustes: [
              {
                tipo: 'REEMPLAZAR',
                motivo: 'Hace 2 semanas que no avanza.',
                de: { titulo: 'Recortá gastos', meta: 65000 },
                a: { titulo: 'Mové 6 Fernet', meta: 6 },
              },
              {
                tipo: 'PROBAR_OTRO_CAMINO',
                motivo: 'No hubo otro.',
                de: { titulo: 'Subí la yerba', meta: 1 },
                a: null,
              },
            ],
          }),
          semana({ semana: '2026-10-26' }),
        ]}
      />,
    );

    const cambios = screen.getAllByRole('listitem');
    expect(cambios).toHaveLength(2);
    expect(cambios[0]).toHaveTextContent('Lo cambiamos');
    expect(cambios[0]).toHaveTextContent('Domingo 8 nov');
    expect(cambios[0]).toHaveTextContent('Recortá gastos');
    expect(within(cambios[0] as HTMLElement).getByText('Mové 6 Fernet')).toBeVisible();
    expect(cambios[0]).toHaveTextContent('Hace 2 semanas que no avanza.');
    expect(cambios[1]).toHaveTextContent('Probamos otro camino');
    expect(within(cambios[1] as HTMLElement).queryByLabelText('por')).not.toBeInTheDocument();
  });

  it('sin cambios explica qué hace Sami los domingos', () => {
    render(<HistorialReajustes semanas={[semana({})]} />);

    expect(screen.getByText(/Todavía no cambiamos ningún mini-objetivo/)).toBeVisible();
  });
});

describe('RecorridoDeLaSemana', () => {
  const cierre: SemanasObjetivo['semanas'][number] = {
    semana: '2026-11-02',
    resultado: 170000,
    avance: 0.34,
    decisiones: { aplicadas: 1, descartadas: 0, aceptadas: 0, ignoradas: 0 },
    queFunciono: '',
    textoConIa: false,
    reajustes: [
      {
        tipo: 'REEMPLAZAR',
        motivo: 'No avanza.',
        de: { titulo: 'Recortá gastos', meta: 1 },
        a: { titulo: 'Mové Fernet', meta: 6 },
      },
      { tipo: 'PROBAR_OTRO_CAMINO', motivo: '', de: { titulo: 'Subí la yerba', meta: 1 }, a: null },
    ],
  };

  it('une las semanas de la más nueva a la más vieja con lo que decidió la persona y lo que cambió Sami', () => {
    render(
      <RecorridoDeLaSemana
        estaSemana="2026-11-09"
        semanas={[cierre, { ...cierre, semana: '2026-10-26', reajustes: [] }]}
        decisiones={[
          { ...decision('a', 'AVISO_APLICADO', '2026-11-02'), efecto: 'MIDIENDO' },
          decision('b', 'PASO_ACEPTADO', '2026-11-09'),
        ]}
      />,
    );

    const semanas = within(screen.getByRole('list', { name: 'Lo que pasó cada semana' }))
      .getAllByRole('listitem')
      .filter((item) => item.classList.contains('recorrido-semana'));
    expect(semanas).toHaveLength(3);
    expect(semanas[0]).toHaveTextContent('Esta semana');
    expect(semanas[0]).toHaveTextContent('Dijiste «lo hago»: Decisión b');
    expect(semanas[1]).toHaveTextContent('Semana del 2 novCerró en $170.000');
    expect(semanas[1]).toHaveTextContent('Sami todavía lo está midiendo.');
    expect(semanas[1]).toHaveTextContent('Sami cambió «Recortá gastos» por «Mové Fernet»');
    expect(semanas[1]).toHaveTextContent('Sami cambió «Subí la yerba»Domingo 8 nov');
    expect(semanas[2]).toHaveTextContent('Sin decisiones ni cambios.');
  });

  it('sin nada que contar lo dice', () => {
    render(<RecorridoDeLaSemana estaSemana="2026-11-09" semanas={[]} decisiones={[]} />);

    expect(screen.getByText(/Todavía no pasó nada/)).toBeVisible();
  });
});
