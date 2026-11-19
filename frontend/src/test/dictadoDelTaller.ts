import { act, cleanup, screen } from '@testing-library/react';
import { afterEach, beforeEach, expect, vi } from 'vitest';
import { campoDeLaConversacion, dadoQueSeAbreElTaller, laPersonaDelTaller } from '@/test/taller';
import { avisoBreve, clickButton, verifyToastAsync } from '@/test/testHelpers';

type VentanaConVoz = Record<string, unknown>;

// Un reconocimiento de voz de mentira: la prueba decide qué "escucha" y cuándo termina.
class ReconocimientoFalso {
  static ultimo: ReconocimientoFalso | null = null;
  lang = '';
  continuous = false;
  interimResults = false;
  onresult: ((evento: { results: { transcript: string }[][] }) => void) | null = null;
  onerror: ((evento: { error: string }) => void) | null = null;
  onend: (() => void) | null = null;
  start = vi.fn();
  stop = vi.fn(() => this.onend?.());
  abort = vi.fn();

  constructor() {
    ReconocimientoFalso.ultimo = this;
  }

  escuchar(...frases: string[]) {
    this.onresult?.({ results: frases.map((transcript) => [{ transcript }]) });
  }
}

const ventana = () => window as unknown as VentanaConVoz;

const reconocimiento = () => {
  if (!ReconocimientoFalso.ultimo) throw new Error('Nadie empezó a escuchar');
  return ReconocimientoFalso.ultimo;
};

const microfono = () => screen.getByRole('button', { name: /Dictar con la voz|Dejar de dictar/ });

export const conUnNavegadorConReconocimientoDeVoz = () => {
  beforeEach(() => {
    ReconocimientoFalso.ultimo = null;
    ventana().SpeechRecognition = ReconocimientoFalso;
  });
  afterEach(() => {
    delete ventana().SpeechRecognition;
  });
};

/* dado */

export const dadoQueSeAbreElTallerParaDictar = dadoQueSeAbreElTaller;

export const dadoQueSeEscribioEnElCampo = (texto: string) =>
  laPersonaDelTaller().type(campoDeLaConversacion(), texto);

export const dadoQueSeEmpezoADictar = () => laPersonaDelTaller().click(microfono());

export const dadoQueSeDicto = (...frases: string[]) => {
  act(() => reconocimiento().escuchar(...frases));
};

export const dadoQueElNavegadorFallaAlEmpezarAEscuchar = () => {
  ventana().SpeechRecognition = class extends ReconocimientoFalso {
    override start = vi.fn(() => {
      throw new Error('InvalidStateError');
    });
  };
};

/* cuando */

export const cuandoSeTocaElMicrofono = () => laPersonaDelTaller().click(microfono());

export const cuandoSeDictan = dadoQueSeDicto;

export const cuandoElNavegadorNiegaElPermiso = () => {
  act(() => {
    reconocimiento().onerror?.({ error: 'not-allowed' });
    reconocimiento().onend?.();
  });
};

export const cuandoElNavegadorNoEscuchaNada = () => {
  act(() => reconocimiento().onerror?.({ error: 'no-speech' }));
};

export const cuandoLaPersonaCortaElDictado = () => {
  act(() => reconocimiento().onerror?.({ error: 'aborted' }));
};

export const cuandoSeEnviaElMensaje = () => clickButton(laPersonaDelTaller(), /Pensalo conmigo/);

export const cuandoSeSaleDeLaPantalla = () => {
  cleanup();
};

/* entonces */

export const entoncesEmpiezaAEscucharEnEspanolRioplatense = () => {
  expect(reconocimiento().start).toHaveBeenCalledOnce();
  expect(reconocimiento().lang).toBe('es-AR');
  expect(reconocimiento().interimResults).toBe(true);
};

export const entoncesElBotonQuedaApretado = () => {
  expect(microfono()).toHaveAttribute('aria-pressed', 'true');
  expect(microfono()).toHaveAccessibleName('Dejar de dictar');
};

export const entoncesElBotonNoQuedaApretado = () => {
  expect(microfono()).toHaveAttribute('aria-pressed', 'false');
};

export const entoncesLoDictadoLlegaAlCampoParaRevisarlo = () => {
  expect(campoDeLaConversacion()).toHaveValue('Tengo un kiosco. vendo golosinas y cigarrillos');
  expect(screen.queryByText(/Pensalo conmigo/)).toBeVisible();
};

export const entoncesDejaDeEscucharYConservaLoDictado = () => {
  expect(reconocimiento().stop).toHaveBeenCalledOnce();
  expect(campoDeLaConversacion()).toHaveValue('vendo velas');
};

export const entoncesExplicaComoHabilitarElMicrofono = () => verifyToastAsync(/permití el micrófono/);

export const entoncesAvisaQueNoSeEscuchoNada = () => verifyToastAsync(/No te escuché/);

export const entoncesNoApareceNingunError = () => {
  expect(avisoBreve()).toBeEmptyDOMElement();
};

export const entoncesAvisaQueNoSePudoDictar = () => verifyToastAsync(/No pudimos dictar ahora/);

export const entoncesExplicaQueElNavegadorNoPermiteDictar = () =>
  verifyToastAsync(/Tu navegador no permite dictar/);

export const entoncesSePuedeSeguirEscribiendo = async () => {
  await laPersonaDelTaller().type(campoDeLaConversacion(), 'Vendo velas');
  expect(campoDeLaConversacion()).toHaveValue('Vendo velas');
};

export const entoncesSeCortaElMicrofono = () => {
  expect(reconocimiento().stop).toHaveBeenCalled();
};

export const entoncesSeAbortaElReconocimiento = () => {
  expect(reconocimiento().abort).toHaveBeenCalled();
};
