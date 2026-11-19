import type { LectorInventario } from '@/types/lectorInventario';

const accionesDeCamara = {
  activa: {
    normal: { texto: 'Pausar cámara', etiqueta: 'Pausar cámara' },
    error: { texto: 'Pausar cámara', etiqueta: 'Pausar cámara' },
  },
  inactiva: {
    normal: { texto: 'Escanear', etiqueta: 'Escanear producto' },
    error: { texto: 'Reintentar', etiqueta: 'Reintentar cámara' },
  },
};

export function presentacionDelLector(lector: LectorInventario) {
  const estado = lector.abierta ? 'activa' : 'inactiva';
  const atencion = lector.errorCamara ? 'error' : 'normal';
  const accion = accionesDeCamara[estado][atencion];
  const texto = lector.abierta && lector.iniciando ? 'Iniciando…' : accion.texto;
  const estadoListo = lector.abierta ? 'Cámara lista para leer' : 'Cámara del celular';
  const estadoVisible = lector.iniciando ? 'Iniciando cámara…' : estadoListo;
  return { ...accion, texto, estadoVisible };
}

export function guiaDelLector(lector: LectorInventario) {
  const guiaDeLectura = lector.sinLectura
    ? 'Alejá un poco el teléfono y evitá reflejos.'
    : 'Alineá todas las barras dentro del marco horizontal.';
  return lector.errorCamara
    ? 'La cámara necesita atención. Abrí Ajustes o escribí el código.'
    : guiaDeLectura;
}
