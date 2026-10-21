import { act, cleanup, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderApp } from '@/test/renderApp';
import {
  clickButton,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyToastAsync,
} from '@/test/testHelpers';

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

const ultimoReconocimiento = () => {
  if (!ReconocimientoFalso.ultimo) throw new Error('Nadie empezó a escuchar');
  return ReconocimientoFalso.ultimo;
};

const abrirTaller = async () => {
  setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  const user = userEvent.setup();
  await clickButton(user, /Crear con Sami/);
  await verifyHeadingPresentAsync('Lo armamos juntos.');
  return user;
};

const campo = () => screen.getByLabelText('Contame un poco de tu negocio') as HTMLTextAreaElement;
const microfono = () => screen.getByRole('button', { name: /Dictar con la voz|Dejar de dictar/ });

describe('el dictado por voz del taller de Sami', () => {
  beforeEach(() => {
    ReconocimientoFalso.ultimo = null;
    (window as unknown as Record<string, unknown>).SpeechRecognition = ReconocimientoFalso;
  });

  afterEach(() => {
    delete (window as unknown as Record<string, unknown>).SpeechRecognition;
  });

  it('empieza a escuchar en español rioplatense y el botón queda apretado', async () => {
    const user = await abrirTaller();

    await user.click(microfono());

    const reconocimiento = ultimoReconocimiento();
    expect(reconocimiento.start).toHaveBeenCalledOnce();
    expect(reconocimiento.lang).toBe('es-AR');
    expect(reconocimiento.interimResults).toBe(true);
    expect(microfono()).toHaveAttribute('aria-pressed', 'true');
    expect(microfono()).toHaveAccessibleName('Dejar de dictar');
  });

  it('lo dictado llega al campo igual que si se hubiera escrito, para revisarlo antes de enviar', async () => {
    const user = await abrirTaller();
    await user.type(campo(), 'Tengo un kiosco.');
    await user.click(microfono());

    act(() => ultimoReconocimiento().escuchar('vendo golosinas', ' y cigarrillos'));

    expect(campo()).toHaveValue('Tengo un kiosco. vendo golosinas y cigarrillos');
    expect(screen.queryByText(/Pensalo conmigo/)).toBeVisible();
  });

  it('al volver a tocar el botón deja de escuchar y lo dictado se conserva', async () => {
    const user = await abrirTaller();
    await user.click(microfono());
    act(() => ultimoReconocimiento().escuchar('vendo velas'));

    await user.click(microfono());

    expect(ultimoReconocimiento().stop).toHaveBeenCalledOnce();
    expect(microfono()).toHaveAttribute('aria-pressed', 'false');
    expect(campo()).toHaveValue('vendo velas');
  });

  it('si el navegador no tiene permiso, explica cómo habilitar el micrófono y se puede seguir escribiendo', async () => {
    const user = await abrirTaller();
    await user.click(microfono());

    act(() => {
      ultimoReconocimiento().onerror?.({ error: 'not-allowed' });
      ultimoReconocimiento().onend?.();
    });

    await verifyToastAsync(/permití el micrófono/);
    expect(microfono()).toHaveAttribute('aria-pressed', 'false');
    await user.type(campo(), 'Vendo velas');
    expect(campo()).toHaveValue('Vendo velas');
  });

  it('si no se escuchó nada, avisa sin molestar con códigos', async () => {
    const user = await abrirTaller();
    await user.click(microfono());

    act(() => ultimoReconocimiento().onerror?.({ error: 'no-speech' }));

    await verifyToastAsync(/No te escuché/);
  });

  it('cuando la persona corta el dictado no aparece ningún error', async () => {
    const user = await abrirTaller();
    await user.click(microfono());

    act(() => ultimoReconocimiento().onerror?.({ error: 'aborted' }));

    expect(document.getElementById('toast')).toBeEmptyDOMElement();
  });

  it('si start falla, avisa y el botón no queda apretado', async () => {
    const user = await abrirTaller();
    (window as unknown as Record<string, unknown>).SpeechRecognition = class extends (
      ReconocimientoFalso
    ) {
      override start = vi.fn(() => {
        throw new Error('InvalidStateError');
      });
    };

    await user.click(microfono());

    await verifyToastAsync(/No pudimos dictar ahora/);
    expect(microfono()).toHaveAttribute('aria-pressed', 'false');
  });

  it('al enviar el mensaje corta el micrófono', async () => {
    const user = await abrirTaller();
    await user.click(microfono());
    act(() => ultimoReconocimiento().escuchar('Tengo un almacén y vendo productos'));

    await clickButton(user, /Pensalo conmigo/);

    expect(ultimoReconocimiento().stop).toHaveBeenCalled();
  });

  it('al salir de la pantalla corta el micrófono', async () => {
    const user = await abrirTaller();
    await user.click(microfono());
    const reconocimiento = ultimoReconocimiento();

    cleanup();

    expect(reconocimiento.abort).toHaveBeenCalled();
  });
});

describe('el dictado en un navegador sin reconocimiento de voz', () => {
  it('el botón explica que no se puede y la persona sigue escribiendo', async () => {
    const user = await abrirTaller();

    await user.click(microfono());

    await verifyToastAsync(/Tu navegador no permite dictar/);
    await user.type(campo(), 'Vendo velas');
    expect(campo()).toHaveValue('Vendo velas');
  });
});
