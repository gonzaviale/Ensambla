import { describe, it } from 'vitest';
import {
  almacenamientosDelLector,
  dadoLectorConCamaraQueFalla,
  dadoLectorConCamaraRecordada,
  cuandoSeEligeLaCamaraOcupada,
  entoncesSePuedeCorregirLaCamara,
  cuandoSeEligeLaCamaraDisponible,
  entoncesElLectorUsaLaCamaraDisponible,
  cuandoSeVuelveALaSeleccionAutomatica,
  entoncesLaCamaraAutomaticaFunciona,
} from '@/test/lectorConCamara';

describe('recuperación del lector al fallar una cámara', () => {
  it.each(['NotReadableError', 'NotFoundError'])(
    'permite elegir otra cámara después de que la seleccionada falle con %s',
    async (nombreError) => {
      const user = dadoLectorConCamaraQueFalla(nombreError);
      await cuandoSeEligeLaCamaraOcupada(user);
      entoncesSePuedeCorregirLaCamara();
      await cuandoSeEligeLaCamaraDisponible(user);
      entoncesElLectorUsaLaCamaraDisponible();
    },
  );

  it.each(almacenamientosDelLector)(
    'permite volver a automático tras fallar la cámara recordada, con almacenamiento $almacenamiento',
    async ({ preparar, recordada }) => {
      const user = dadoLectorConCamaraRecordada(preparar);
      await cuandoSeVuelveALaSeleccionAutomatica(user);
      entoncesLaCamaraAutomaticaFunciona(recordada);
    },
  );
});
