import { afterEach, describe, expect, it, vi } from 'vitest';
import { guardarArchivo } from '@/lib/descargas';

describe('guardarArchivo', () => {
  afterEach(() => vi.restoreAllMocks());

  it('descarga el archivo con el nombre que mandó el servidor y libera la dirección', () => {
    const crear = vi.fn(() => 'blob:resumen');
    const liberar = vi.fn();
    vi.stubGlobal('URL', { createObjectURL: crear, revokeObjectURL: liberar });
    const clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      expect(this.download).toBe('resumen-almacen-20261019.pdf');
      expect(this.href).toBe('blob:resumen');
      expect(document.body.contains(this)).toBe(true);
    });
    const archivo = new Blob(['%PDF-1.4'], { type: 'application/pdf' });

    guardarArchivo({ blob: archivo, nombre: 'resumen-almacen-20261019.pdf' });

    expect(crear).toHaveBeenCalledWith(archivo);
    expect(clic).toHaveBeenCalledTimes(1);
    expect(liberar).toHaveBeenCalledWith('blob:resumen');
    expect(document.querySelector('a[download]')).toBeNull();
    vi.unstubAllGlobals();
  });
});
