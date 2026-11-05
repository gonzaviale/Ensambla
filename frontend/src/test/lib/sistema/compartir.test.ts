import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  compartir,
  enlaceDeWhatsapp,
  formaDeCompartirPara,
  type ParaCompartir,
} from '@/lib/sistema/compartir';

const guardarArchivo = vi.hoisted(() => vi.fn());
vi.mock('@/lib/descargas', () => ({ guardarArchivo }));

const PEDIDO: ParaCompartir = {
  archivo: { blob: new Blob(['%PDF'], { type: 'application/pdf' }), nombre: 'pedido-molinos-20261019.pdf' },
  texto: 'Hola, necesito reponer lo siguiente:\n- Yerba: 20',
  telefono: '11 5555-0101',
  titulo: 'Pedido de reposición',
};

const ARCHIVO = new File(['%PDF'], 'pedido.pdf', { type: 'application/pdf' });

const darCompartirAlNavegador = (canShare: boolean, share = vi.fn().mockResolvedValue(undefined)) => {
  Object.defineProperty(navigator, 'canShare', { value: () => canShare, configurable: true });
  Object.defineProperty(navigator, 'share', { value: share, configurable: true });
  return share;
};

const quitarCompartirDelNavegador = () => {
  Reflect.deleteProperty(navigator, 'canShare');
  Reflect.deleteProperty(navigator, 'share');
};

describe('compartir un archivo', () => {
  beforeEach(() => guardarArchivo.mockClear());
  afterEach(() => {
    quitarCompartirDelNavegador();
    vi.restoreAllMocks();
  });

  describe('en el celular, con la hoja de compartir', () => {
    it('comparte el PDF con el mensaje', async () => {
      const share = darCompartirAlNavegador(true);

      await compartir(PEDIDO);

      expect(share).toHaveBeenCalledTimes(1);
      const datos = share.mock.calls[0]?.[0] as ShareData;
      expect(datos.text).toBe(PEDIDO.texto);
      expect(datos.title).toBe('Pedido de reposición');
      expect(datos.files?.[0]?.name).toBe('pedido-molinos-20261019.pdf');
      expect(guardarArchivo).not.toHaveBeenCalled();
    });

    it('si la persona cancela no se muestra ningún error', async () => {
      darCompartirAlNavegador(true, vi.fn().mockRejectedValue(new DOMException('cancelado', 'AbortError')));

      await expect(compartir(PEDIDO)).resolves.toBeUndefined();
    });

    it('cualquier otro error de la hoja de compartir sí se informa', async () => {
      darCompartirAlNavegador(true, vi.fn().mockRejectedValue(new Error('falló')));

      await expect(compartir(PEDIDO)).rejects.toThrow('falló');
    });
  });

  describe('en la computadora, sin hoja de compartir', () => {
    it('baja el PDF y abre WhatsApp Web con el número y el mensaje', async () => {
      const abrir = vi.spyOn(window, 'open').mockReturnValue(null);

      await compartir(PEDIDO);

      expect(guardarArchivo).toHaveBeenCalledWith(PEDIDO.archivo);
      expect(abrir).toHaveBeenCalledWith(
        `https://wa.me/1155550101?text=${encodeURIComponent(PEDIDO.texto)}`,
        '_blank',
        'noopener',
      );
    });

    it('si el navegador no puede compartir ese archivo usa el mismo camino', async () => {
      darCompartirAlNavegador(false);
      const abrir = vi.spyOn(window, 'open').mockReturnValue(null);

      await compartir(PEDIDO);

      expect(guardarArchivo).toHaveBeenCalledTimes(1);
      expect(abrir).toHaveBeenCalledTimes(1);
    });
  });

  describe('formaDeEnvioPara', () => {
    it('elige la hoja de compartir solo cuando el navegador la permite', () => {
      darCompartirAlNavegador(true);
      const delCelular = formaDeCompartirPara(ARCHIVO);
      darCompartirAlNavegador(false);
      const delRespaldo = formaDeCompartirPara(ARCHIVO);

      expect(delCelular).not.toBe(delRespaldo);
      expect(delRespaldo.disponible(ARCHIVO)).toBe(true);
    });
  });
});

describe('enlaceDeWhatsapp', () => {
  it('deja solo los dígitos del número y codifica el mensaje', () => {
    expect(enlaceDeWhatsapp('+54 9 11 5555-0101', 'Hola, 2 cajas')).toBe(
      'https://wa.me/5491155550101?text=Hola%2C%202%20cajas',
    );
  });

  it('sin número se elige a quién mandárselo', () => {
    expect(enlaceDeWhatsapp('', 'Hola')).toBe('https://wa.me/?text=Hola');
  });
});
