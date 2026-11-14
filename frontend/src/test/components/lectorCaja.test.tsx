import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { LectorCaja } from '@/components/caja/LectorCaja';

describe('lector de caja compacto', () => {
  it('conserva el video al mostrar ayuda durante una lectura continua', () => {
    const video = createRef<HTMLVideoElement>();
    const propiedades = {
      codigo: '',
      buscando: false,
      abierta: true,
      iniciando: false,
      sinLectura: false,
      sesion: 1,
      camaras: [],
      camaraId: '',
      camaraActiva: '',
      camaraActualId: '',
      errorCamara: '',
      video,
      entradaCodigo: createRef<HTMLInputElement>(),
      onCodigo: vi.fn(),
      onBuscar: vi.fn(),
      onAbrir: vi.fn(),
      onCerrar: vi.fn(),
      onReiniciar: vi.fn(),
      onCamara: vi.fn(),
      revisandoProducto: false,
      errorCodigo: '',
      mensaje: '',
      habilitado: true,
      lectura: null,
    };
    const vista = render(<LectorCaja {...propiedades} />);
    const reproduccion = screen.getByLabelText('Vista previa del lector');
    expect(video.current).toBe(reproduccion);
    vista.rerender(<LectorCaja {...propiedades} sinLectura mensaje="Coca-Cola agregada" />);
    expect(screen.getByLabelText('Vista previa del lector')).toBe(reproduccion);
    expect(screen.getByRole('status')).toHaveTextContent('Alejá un poco el teléfono');
    expect(screen.getByText('Lector activo')).toBeVisible();
    vista.rerender(<LectorCaja {...propiedades} lectura={{ id: 1, tipo: 'reconocido' }} />);
    expect(screen.getByLabelText('Vista previa del lector')).toBe(reproduccion);
    vista.rerender(<LectorCaja {...propiedades} lectura={{ id: 2, tipo: 'reconocido' }} />);
    expect(screen.getByLabelText('Vista previa del lector')).toBe(reproduccion);
  });
});
