export type ResultadoSonoroEscaneo = 'reconocido' | 'atencion';

export const TONOS_ESCANEO = {
  reconocido: [
    { frecuencia: 1046, inicio: 0, duracion: 0.07 },
    { frecuencia: 1568, inicio: 0.08, duracion: 0.085 },
  ],
  atencion: [
    { frecuencia: 660, inicio: 0, duracion: 0.09 },
    { frecuencia: 440, inicio: 0.14, duracion: 0.11 },
  ],
} as const;

export const VOLUMEN_ESCANEO = 0.16;
export const ATAQUE_ESCANEO = 0.006;
export const SALIDA_ESCANEO = 0.018;
