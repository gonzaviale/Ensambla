import { describe, it } from 'vitest';
import {
  almacenamientosDelLector,
  givenLectorConCamaraQueFalla,
  givenLectorConCamaraRecordada,
  whenSeEligeLaCamaraOcupada,
  thenSePuedeCorregirLaCamara,
  whenSeEligeLaCamaraDisponible,
  thenElLectorUsaLaCamaraDisponible,
  whenSeVuelveALaSeleccionAutomatica,
  thenLaCamaraAutomaticaFunciona,
} from '@/test/lectorConCamara';

describe('recuperación del lector al fallar una cámara', () => {
  it.each(['NotReadableError', 'NotFoundError'])(
    'permite elegir otra cámara después de que la seleccionada falle con %s',
    async (nombreError) => {
      const user = givenLectorConCamaraQueFalla(nombreError);
      await whenSeEligeLaCamaraOcupada(user);
      thenSePuedeCorregirLaCamara();
      await whenSeEligeLaCamaraDisponible(user);
      thenElLectorUsaLaCamaraDisponible();
    },
  );

  it.each(almacenamientosDelLector)(
    'permite volver a automático tras fallar la cámara recordada, con almacenamiento $almacenamiento',
    async ({ preparar, recordada }) => {
      const user = givenLectorConCamaraRecordada(preparar);
      await whenSeVuelveALaSeleccionAutomatica(user);
      thenLaCamaraAutomaticaFunciona(recordada);
    },
  );
});
