import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { GraficoSemanal } from '@/pages/Objetivos/autogestion/GraficoSemanal';
import { HistorialReajustes } from '@/pages/Objetivos/autogestion/HistorialReajustes';
import { MiniObjetivoCard } from '@/pages/Objetivos/autogestion/MiniObjetivoCard';
import { RecorridoDeLaSemana } from '@/pages/Objetivos/autogestion/RecorridoDeLaSemana';
import type { DecisionObjetivo, MiniMedido, SemanasObjetivo } from '@/types/objetivos';

type Semana = SemanasObjetivo['semanas'][number];
type Usuario = ReturnType<typeof userEvent.setup>;
type Progreso = Parameters<typeof GraficoSemanal>[0]['progreso'];

const mini: MiniMedido = {
  id: 'mini-1',
  modo: 'MEDIDO',
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

const semana = (cambios: Partial<Semana> = {}): Semana => ({
  semana: '2026-11-02',
  resultado: 170000,
  avance: 0.34,
  decisiones: { aplicadas: 0, descartadas: 0, aceptadas: 0, ignoradas: 0 },
  queFunciono: '',
  textoConIa: false,
  reajustes: [],
  ...cambios,
});

const PROGRESO: Progreso = {
  meta: 500000,
  hito: null,
  minis: [],
  semanas: [
    { semana: '2026-10-26', valor: 150000, estimado: true },
    { semana: '2026-11-02', valor: 170000, estimado: false },
    { semana: '2026-11-09', valor: -10000, estimado: false },
  ],
};

const REAJUSTES = [
  {
    tipo: 'REEMPLAZAR' as const,
    motivo: 'No avanza.',
    de: { titulo: 'Recortá gastos', meta: 1 },
    a: { titulo: 'Mové Fernet', meta: 6 },
  },
  {
    tipo: 'PROBAR_OTRO_CAMINO' as const,
    motivo: '',
    de: { titulo: 'Subí la yerba', meta: 1 },
    a: null,
  },
];

let user: Usuario;
let contenedor: HTMLElement;
let onDecidir: ReturnType<typeof vi.fn>;
let onRestaurar: ReturnType<typeof vi.fn>;

describe('MiniObjetivoCard', () => {
  it('muestra qué hacer, por qué y cuánto va', async () => {
    dadaLaTarjeta();

    await cuandoSeDespliegaElPorQue();

    entoncesLaTarjetaExplicaElPasoYCuantoVa();
  });

  it('antes de desplegar no muestra la justificación', () => {
    dadaLaTarjeta();

    entoncesLaJustificacionEstaOculta();
  });

  it('deja decidir «lo hago» y «ahora no», sin cambiar ni quitar', async () => {
    dadaLaTarjeta();

    await cuandoSeDecideLoHagoYDespuesLoDejaParaDespues();

    entoncesSeDecidioEnOrden(['ACEPTADO'], ['IGNORADO']);
    entoncesNoSePuedeCambiarNiQuitar();
  });

  it('si ya decidió esta semana lo dice en vez de volver a preguntar', () => {
    dadaLaTarjeta({ decidido: 'ACEPTADO' });

    entoncesDiceQueYaDecidioQueLoHace();
  });

  it('un mini logrado sigue con su diseño y dice «Meta alcanzada»', () => {
    dadaLaTarjeta({ mini: { ...mini, estado: 'LOGRADO', valorActual: 92000, avance: 1 } });

    entoncesLaTarjetaDiceMetaAlcanzada();
  });

  it('un mini logrado no pide decidir', () => {
    dadaLaTarjeta({ mini: { ...mini, estado: 'LOGRADO', valorActual: 92000, avance: 1 } });

    entoncesNoPideDecidir();
  });

  it('lo que dejó para después se puede restaurar para volver a decidir', async () => {
    dadaLaTarjeta({ decidido: 'IGNORADO' });

    await cuandoSeRestauraElPaso();

    entoncesSePidioRestaurar();
  });

  it('mientras guarda no deja tocar los botones', () => {
    dadaLaTarjeta({ ocupado: true });

    entoncesLosBotonesDeDecidirEstanDeshabilitados();
  });
});

describe('GraficoSemanal', () => {
  it('dibuja la curva con la meta', () => {
    dadoElGraficoConDecisiones();

    entoncesHayUnaCurvaConLaMeta();
  });

  it('marca cada decisión en su semana y los puntos estimados', () => {
    dadoElGraficoConDecisiones();

    entoncesHayDosMarcasYUnPuntoEstimado();
  });

  it('ofrece una tabla para lectores de pantalla con lo que decidió cada semana', () => {
    dadoElGraficoConDecisiones();

    entoncesLaTablaDiceLoQuePasoCadaSemana();
  });

  it('con una sola semana la pone al medio', () => {
    dadoElGraficoDeUnaSolaSemana();

    entoncesElUnicoPuntoQuedaAlMedio();
  });

  it('sin estimados no explica cómo se calculan', () => {
    dadoElGraficoDeUnaSolaSemana();

    entoncesNoExplicaLosEstimados();
  });

  it('sin semanas explica que todavía no hay nada para dibujar', () => {
    dadoElGraficoSinSemanas();

    entoncesDiceQueTodaviaNoHaySemanas();
  });
});

describe('HistorialReajustes', () => {
  it('cuenta qué se cambió y por qué', () => {
    dadoElHistorialConDosReajustes();

    entoncesElPrimerCambioDiceQueSeReemplazoYPorQue();
  });

  it('un cambio sin reemplazo muestra solo el anterior', () => {
    dadoElHistorialConDosReajustes();

    entoncesElSegundoCambioDiceQueSeProboOtroCamino();
  });

  it('sin cambios explica qué hace Sami los domingos', () => {
    dadoElHistorialSinCambios();

    entoncesExplicaQueTodaviaNoSeCambioNada();
  });
});

describe('RecorridoDeLaSemana', () => {
  it('une las semanas de la más nueva a la más vieja', () => {
    dadoElRecorridoConTresSemanas();

    entoncesHayTresSemanasYLaPrimeraEsEstaSemana();
  });

  it('cuenta lo que decidió la persona en cada semana', () => {
    dadoElRecorridoConTresSemanas();

    entoncesCuentaLoQueDecidioLaPersona();
  });

  it('cuenta lo que cambió Sami en cada semana', () => {
    dadoElRecorridoConTresSemanas();

    entoncesCuentaLoQueCambioSami();
  });

  it('una semana sin nada lo dice', () => {
    dadoElRecorridoConTresSemanas();

    entoncesLaUltimaSemanaNoTuvoNada();
  });

  it('sin nada que contar lo dice', () => {
    dadoUnRecorridoVacio();

    entoncesDiceQueTodaviaNoPasoNada();
  });
});

// --- dado ---

function dadaLaTarjeta(cambios: Partial<Parameters<typeof MiniObjetivoCard>[0]> = {}) {
  onDecidir = vi.fn();
  onRestaurar = vi.fn();
  user = userEvent.setup();
  render(
    <MiniObjetivoCard mini={mini} onDecidir={onDecidir} onRestaurar={onRestaurar} {...cambios} />,
  );
}

function dadoElGraficoConDecisiones() {
  contenedor = render(
    <GraficoSemanal
      progreso={PROGRESO}
      decisiones={[
        decision('a', 'AVISO_APLICADO', '2026-11-02'),
        decision('b', 'AVISO_DESCARTADO', '2026-11-02'),
        decision('c', 'PASO_IGNORADO', '2026-09-01'),
      ]}
    />,
  ).container;
}

function dadoElGraficoDeUnaSolaSemana() {
  const unaSemana = [{ semana: '2026-11-09', valor: 0, estimado: false }];
  contenedor = render(
    <GraficoSemanal progreso={{ ...PROGRESO, meta: 0, semanas: unaSemana }} decisiones={[]} />,
  ).container;
}

function dadoElGraficoSinSemanas() {
  render(<GraficoSemanal progreso={{ ...PROGRESO, semanas: [] }} decisiones={[]} />);
}

function dadoElHistorialConDosReajustes() {
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
}

function dadoElHistorialSinCambios() {
  render(<HistorialReajustes semanas={[semana()]} />);
}

function dadoElRecorridoConTresSemanas() {
  render(
    <RecorridoDeLaSemana
      estaSemana="2026-11-09"
      semanas={[
        semana({
          decisiones: { aplicadas: 1, descartadas: 0, aceptadas: 0, ignoradas: 0 },
          reajustes: REAJUSTES,
        }),
        semana({ semana: '2026-10-26' }),
      ]}
      decisiones={[
        { ...decision('a', 'AVISO_APLICADO', '2026-11-02'), efecto: 'MIDIENDO' },
        decision('b', 'PASO_ACEPTADO', '2026-11-09'),
      ]}
    />,
  );
}

function dadoUnRecorridoVacio() {
  render(<RecorridoDeLaSemana estaSemana="2026-11-09" semanas={[]} decisiones={[]} />);
}

// --- cuando ---

async function cuandoSeDespliegaElPorQue() {
  await user.click(screen.getByRole('button', { name: /Por qué de este paso/ }));
}

async function cuandoSeDecideLoHagoYDespuesLoDejaParaDespues() {
  await user.click(screen.getByRole('button', { name: 'Lo hago' }));
  await user.click(screen.getByRole('button', { name: 'Dejarlo para después' }));
}

async function cuandoSeRestauraElPaso() {
  await user.click(
    screen.getByRole('button', {
      name: 'Restaurar «Vendé $90.000 por semana» para volver a decidir',
    }),
  );
}

// --- entonces: tarjeta ---

const laTarjeta = () => screen.getByRole('article', { name: 'Vendé $90.000 por semana' });

function entoncesLaJustificacionEstaOculta() {
  expect(within(laTarjeta()).queryByText('Hoy vendés unos $78.000 por semana.')).toBeNull();
}

function entoncesLaTarjetaExplicaElPasoYCuantoVa() {
  const tarjeta = laTarjeta();
  expect(within(tarjeta).getByText('Hoy vendés unos $78.000 por semana.')).toBeVisible();
  expect(within(tarjeta).getByText('$84.000')).toBeVisible();
  expect(within(tarjeta).getByText('$90.000')).toBeVisible();
  expect(within(tarjeta).getByText('Te faltan $6.000 por semana')).toBeVisible();
}

function entoncesSeDecidioEnOrden(...llamadas: string[][]) {
  expect(onDecidir.mock.calls).toEqual(llamadas);
}

function entoncesNoSePuedeCambiarNiQuitar() {
  expect(screen.queryByRole('button', { name: /Cambiar|Quitar/ })).not.toBeInTheDocument();
}

function entoncesDiceQueYaDecidioQueLoHace() {
  expect(screen.getByRole('status')).toHaveTextContent('Esta semana dijiste que lo hacés');
  expect(screen.queryByRole('button', { name: 'Lo hago' })).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: /Restaurar/ })).not.toBeInTheDocument();
}

function entoncesLaTarjetaDiceMetaAlcanzada() {
  expect(screen.getByRole('article')).toHaveClass('mini-objetivo');
  expect(screen.getByText('Meta alcanzada')).toBeVisible();
  expect(screen.getByRole('status')).toHaveTextContent('Meta alcanzada: el domingo Sami la sube');
}

function entoncesNoPideDecidir() {
  expect(screen.queryByRole('button', { name: /Lo hago|Dejarlo para después/ })).toBeNull();
}

function entoncesSePidioRestaurar() {
  expect(screen.getByRole('status')).toHaveTextContent('Lo dejaste para después');
  expect(onRestaurar).toHaveBeenCalledOnce();
}

function entoncesLosBotonesDeDecidirEstanDeshabilitados() {
  // «Por qué de este paso» sólo despliega texto: sigue disponible mientras guarda.
  screen
    .getAllByRole('button', { name: /^(?!Por qué de este paso)/ })
    .forEach((boton) => expect(boton).toBeDisabled());
}

// --- entonces: gráfico ---

function entoncesHayUnaCurvaConLaMeta() {
  expect(screen.getByRole('img', { name: /meta de \$500\.000/ })).toBeInTheDocument();
}

function entoncesHayDosMarcasYUnPuntoEstimado() {
  expect(contenedor.querySelectorAll('.grafico-semanal-marca')).toHaveLength(2);
  expect(contenedor.querySelectorAll('.grafico-semanal-punto.estimado')).toHaveLength(1);
}

function entoncesLaTablaDiceLoQuePasoCadaSemana() {
  const filas = within(screen.getByRole('table')).getAllByRole('row');
  expect(filas[1]).toHaveTextContent('$150.000 (calculado con tus secciones)');
  expect(filas[1]).toHaveTextContent('Ninguna');
  expect(filas[2]).toHaveTextContent('Aplicaste: Decisión a; Descartaste: Decisión b');
}

function entoncesElUnicoPuntoQuedaAlMedio() {
  expect(contenedor.querySelector('.grafico-semanal-punto')).toHaveAttribute('cx', '338');
}

function entoncesNoExplicaLosEstimados() {
  expect(screen.queryByText('Calculado con tus secciones')).not.toBeInTheDocument();
}

function entoncesDiceQueTodaviaNoHaySemanas() {
  expect(screen.getByText(/Todavía no hay semanas para dibujar/)).toBeVisible();
}

// --- entonces: historial ---

const losCambios = () => screen.getAllByRole('listitem');

function entoncesElPrimerCambioDiceQueSeReemplazoYPorQue() {
  const [primero] = losCambios();
  expect(losCambios()).toHaveLength(2);
  expect(primero).toHaveTextContent('Lo cambiamos');
  expect(primero).toHaveTextContent('Domingo 8 nov');
  expect(primero).toHaveTextContent('Recortá gastos');
  expect(within(primero as HTMLElement).getByText('Mové 6 Fernet')).toBeVisible();
  expect(primero).toHaveTextContent('Hace 2 semanas que no avanza.');
}

function entoncesElSegundoCambioDiceQueSeProboOtroCamino() {
  const segundo = losCambios()[1];
  expect(segundo).toHaveTextContent('Probamos otro camino');
  expect(within(segundo as HTMLElement).queryByLabelText('por')).not.toBeInTheDocument();
}

function entoncesExplicaQueTodaviaNoSeCambioNada() {
  expect(screen.getByText(/Todavía no cambiamos ningún mini-objetivo/)).toBeVisible();
}

// --- entonces: recorrido ---

const lasSemanas = () =>
  within(screen.getByRole('list', { name: 'Lo que pasó cada semana' }))
    .getAllByRole('listitem')
    .filter((item) => item.classList.contains('recorrido-semana'));

function entoncesHayTresSemanasYLaPrimeraEsEstaSemana() {
  expect(lasSemanas()).toHaveLength(3);
  expect(lasSemanas()[0]).toHaveTextContent('Esta semana');
}

function entoncesCuentaLoQueDecidioLaPersona() {
  expect(lasSemanas()[0]).toHaveTextContent('Dijiste «lo hago»: Decisión b');
  expect(lasSemanas()[1]).toHaveTextContent('Semana del 2 novCerró en $170.000');
  expect(lasSemanas()[1]).toHaveTextContent('Sami todavía lo está midiendo.');
}

function entoncesCuentaLoQueCambioSami() {
  expect(lasSemanas()[1]).toHaveTextContent('Sami cambió «Recortá gastos» por «Mové Fernet»');
  expect(lasSemanas()[1]).toHaveTextContent('Sami cambió «Subí la yerba»Domingo 8 nov');
}

function entoncesLaUltimaSemanaNoTuvoNada() {
  expect(lasSemanas()[2]).toHaveTextContent('Sin decisiones ni cambios.');
}

function entoncesDiceQueTodaviaNoPasoNada() {
  expect(screen.getByText(/Todavía no pasó nada/)).toBeVisible();
}
