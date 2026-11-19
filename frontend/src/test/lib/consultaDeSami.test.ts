import { describe, expect, it } from 'vitest';
import {
  diaSiguiente,
  esHoraDePreguntar,
  estaPospuesta,
  posponerHastaManana,
} from '@/lib/consultaDeSami';

const LUNES = '2026-11-16';
const MARTES = '2026-11-17';
const MIERCOLES = '2026-11-18';
const DOMINGO = '2026-11-22';
const LUNES_SIGUIENTE = '2026-11-23';

let pregunta: boolean;
let dia: string;

describe('cuándo pregunta Sami', () => {
  it.each([LUNES, MARTES])('no pregunta por lo de esta semana el %s', (hoy) => {
    cuandoSeConsultaSiEsHoraDePreguntarDeLaSemanaDelLunes(hoy);

    entoncesNoPregunta();
  });

  it.each([MIERCOLES, DOMINGO])('pregunta por lo de esta semana desde el miércoles: %s', (hoy) => {
    cuandoSeConsultaSiEsHoraDePreguntarDeLaSemanaDelLunes(hoy);

    entoncesPregunta();
  });

  it('pregunta siempre por lo que quedó de una semana anterior', () => {
    cuandoSeConsultaSiEsHoraDePreguntarDeLaSemanaDelLunes(LUNES_SIGUIENTE);

    entoncesPregunta();
  });

  it('el día siguiente cruza el fin de mes', () => {
    cuandoSePideElDiaSiguienteDe('2026-11-30');

    entoncesElDiaEs('2026-12-01');
  });
});

describe('dejar una pregunta para después', () => {
  it('queda en pausa el resto del día', () => {
    dadoUnaPreguntaPospuestaElMiercoles('mini-1');

    cuandoSeConsultaSiEstaPospuesta('mini-1', MIERCOLES);

    entoncesLaPreguntaEstaPospuesta();
  });

  it('vuelve al día siguiente', () => {
    dadoUnaPreguntaPospuestaElMiercoles('mini-1');

    cuandoSeConsultaSiEstaPospuesta('mini-1', diaSiguiente(MIERCOLES));

    entoncesLaPreguntaNoEstaPospuesta();
  });

  it('no afecta a las otras preguntas', () => {
    dadoUnaPreguntaPospuestaElMiercoles('mini-1');

    cuandoSeConsultaSiEstaPospuesta('mini-2', MIERCOLES);

    entoncesLaPreguntaNoEstaPospuesta();
  });
});

function dadoUnaPreguntaPospuestaElMiercoles(id: string) {
  posponerHastaManana(id, MIERCOLES);
}

function cuandoSeConsultaSiEsHoraDePreguntarDeLaSemanaDelLunes(hoy: string) {
  pregunta = esHoraDePreguntar(LUNES, hoy);
}

function cuandoSePideElDiaSiguienteDe(fecha: string) {
  dia = diaSiguiente(fecha);
}

function cuandoSeConsultaSiEstaPospuesta(id: string, hoy: string) {
  pregunta = estaPospuesta(id, hoy);
}

function entoncesNoPregunta() {
  expect(pregunta).toBe(false);
}

function entoncesPregunta() {
  expect(pregunta).toBe(true);
}

function entoncesElDiaEs(esperado: string) {
  expect(dia).toBe(esperado);
}

function entoncesLaPreguntaEstaPospuesta() {
  expect(pregunta).toBe(true);
}

function entoncesLaPreguntaNoEstaPospuesta() {
  expect(pregunta).toBe(false);
}
