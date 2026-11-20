import { expect } from 'vitest';
import type { Aviso } from '@/lib/sistema/alertas';
import { type DestinoDeAviso, destinoDeAviso } from '@/lib/sistema/destinoDeAviso';

let aviso: Aviso;
let destino: DestinoDeAviso | null;

const unAviso = (parcial: Partial<Aviso>): Aviso => ({
  id: 'a',
  tipo: 'stock',
  grupo: 'stock',
  prioridad: 2,
  sistemaId: 's1',
  sistema: 'Almacén',
  titulo: 'Aviso',
  detalle: '',
  porque: [],
  acciones: [],
  ...parcial,
});

export const dadoUnAvisoSobreUnRegistroYUnaAccionQueRegistraEnOtraTabla = () => {
  aviso = unAviso({
    objeto: { sistemaId: 's1', bloqueId: 'inv', filaId: 'f1' },
    acciones: [{ tipo: 'registrar', texto: 'Registrar', sistemaId: 's1', bloqueId: 'ent' }],
  });
};

export const dadoUnAvisoSinRegistroConUnaAccionQueAplicaSobreUnaFila = () => {
  aviso = unAviso({
    acciones: [
      { tipo: 'abrir', texto: 'Ver', sistemaId: 's1', bloqueId: 'inv' },
      {
        tipo: 'aplicar',
        texto: 'Subir',
        sistemaId: 's1',
        bloqueId: 'inv',
        filaId: 'f2',
        valores: {},
        confirmacion: '',
      },
    ],
  });
};

export const dadoUnAvisoSinRegistroConUnaAccionQueSoloApuntaAUnaTabla = () => {
  aviso = unAviso({
    acciones: [{ tipo: 'registrar', texto: 'Registrar', sistemaId: 's1', bloqueId: 'ent' }],
  });
};

export const dadoUnAvisoDeLaZona = () => {
  aviso = unAviso({
    tipo: 'consumo_zona',
    acciones: [{ tipo: 'zona', texto: 'Ver en Mi zona' }],
  });
};

export const cuandoSeBuscaElDestinoDelAviso = () => {
  destino = destinoDeAviso(aviso);
};

export const entoncesElDestinoEsElRegistroDelAviso = () => {
  expect(destino).toEqual({ sistemaId: 's1', bloqueId: 'inv', filaId: 'f1' });
};

export const entoncesElDestinoEsLaFilaDeLaAccion = () => {
  expect(destino).toMatchObject({ bloqueId: 'inv', filaId: 'f2' });
};

export const entoncesElDestinoEsSoloLaTabla = () => {
  expect(destino).toEqual({ sistemaId: 's1', bloqueId: 'ent', filaId: undefined });
};

export const entoncesNoApuntaANingunObjeto = () => {
  expect(destino).toBeNull();
};
