import type { RefObject } from 'react';
import type { CamaraDelLector } from './escaneo';

export type PanelLector = 'codigo' | 'ajustes' | null;

export type PropsLectorCodigos = Omit<LectorInventario, 'panel' | 'onPanel' | 'mensaje'> & {
  continua?: boolean;
  compacto?: boolean;
};

export type LectorInventario = {
  codigo: string;
  buscando: boolean;
  abierta: boolean;
  iniciando: boolean;
  sinLectura: boolean;
  sesion: number;
  camaras: CamaraDelLector[];
  camaraId: string;
  camaraActiva: string;
  camaraActualId: string;
  errorCamara: string;
  video: RefObject<HTMLVideoElement | null>;
  entradaCodigo: RefObject<HTMLInputElement | null>;
  onCodigo: (codigo: string) => void;
  onBuscar: () => void;
  onAbrir: () => void;
  onCerrar: () => void;
  onReiniciar: () => void;
  onCamara: (id: string) => void;
  panel: PanelLector;
  onPanel: (panel: PanelLector) => void;
  mensaje?: string;
};
