import { act, renderHook } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { claveDemoProveedores, useProveedoresDemo } from '@/hooks/proveedores/useProveedoresDemo';

describe('almacenamiento y archivos de la demo de proveedores', () => {
  it('recupera un pedido guardado al volver a abrir la cuenta y no lo traslada a otra', () => {
    const ana = renderHook(() => useProveedoresDemo('ana'));
    const producto = ana.result.current.datos.productos[0];
    if (!producto) throw new Error('Falta semilla');
    act(() => ana.result.current.cambiarCantidad(producto.id, 123));
    ana.unmount();
    const retorno = renderHook(() => useProveedoresDemo('ana'));
    expect(
      retorno.result.current.lineasPedido.find((l) => l.productoId === producto.id)?.cantidad,
    ).toBe(123);
    const otra = renderHook(() => useProveedoresDemo('otra'));
    expect(otra.result.current.lineasPedido.some((l) => l.cantidad === 123)).toBe(false);
    expect(localStorage.getItem(claveDemoProveedores('otra'))).not.toBe(
      localStorage.getItem(claveDemoProveedores('ana')),
    );
  });

  it('previsualiza el archivo local, guarda sólo su nombre y libera el adjunto al confirmar', () => {
    const crearOriginal = Object.getOwnPropertyDescriptor(URL, 'createObjectURL');
    const liberarOriginal = Object.getOwnPropertyDescriptor(URL, 'revokeObjectURL');
    const crear = vi.fn(() => 'blob:comprobante-local');
    const liberar = vi.fn();
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: crear });
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: liberar });
    const demo = renderHook(() => useProveedoresDemo('archivo'));
    try {
      const archivo = new File(['contenido-local-que-no-se-debe-persistir'], 'ticket.png', {
        type: 'image/png',
      });
      act(() => demo.result.current.seleccionarArchivo(archivo));
      expect(crear).toHaveBeenCalledWith(archivo);
      expect(demo.result.current.vistaArchivo).toBe('blob:comprobante-local');
      act(() => demo.result.current.simularLectura());
      expect(demo.result.current.revision?.archivo).toBe('ticket.png');
      act(() => demo.result.current.setRevisado(true));
      act(() => demo.result.current.guardarCompra());
      const guardado = localStorage.getItem(claveDemoProveedores('archivo')) ?? '';
      expect(guardado).toContain('ticket.png');
      expect(guardado).not.toContain('contenido-local-que-no-se-debe-persistir');
      expect(demo.result.current.archivo).toBeNull();
      expect(liberar).toHaveBeenCalledWith('blob:comprobante-local');
    } finally {
      demo.unmount();
      if (crearOriginal) Object.defineProperty(URL, 'createObjectURL', crearOriginal);
      else Reflect.deleteProperty(URL, 'createObjectURL');
      if (liberarOriginal) Object.defineProperty(URL, 'revokeObjectURL', liberarOriginal);
      else Reflect.deleteProperty(URL, 'revokeObjectURL');
    }
  });

  it('rechaza formatos ajenos, archivos vacíos y archivos por encima del límite', () => {
    const { result } = renderHook(() => useProveedoresDemo('invalidos'));
    act(() =>
      result.current.seleccionarArchivo(
        new File(['texto'], 'ticket.exe', { type: 'application/octet-stream' }),
      ),
    );
    expect(result.current.error).toMatch(/PDF o una imagen/);
    act(() =>
      result.current.seleccionarArchivo(new File([], 'vacio.pdf', { type: 'application/pdf' })),
    );
    expect(result.current.error).toMatch(/tener contenido/);
    act(() =>
      result.current.seleccionarArchivo(
        new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'grande.pdf', { type: 'application/pdf' }),
      ),
    );
    expect(result.current.error).toMatch(/10 MB/);
    expect(result.current.archivo).toBeNull();
    expect(result.current.revision).toBeNull();
  });
});
