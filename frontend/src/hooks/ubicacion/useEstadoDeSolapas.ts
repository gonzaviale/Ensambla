import { useState } from 'react';
import type { PorSeccion, SeccionUbicacion } from '@/types/ubicacion';

const todasEn = <T>(valor: T): PorSeccion<T> => ({
  comercio: valor,
  ubicacion: valor,
  rubro: valor,
});

const conValor =
  <T>(seccion: SeccionUbicacion, valor: T) =>
  (actual: PorSeccion<T>): PorSeccion<T> => ({ ...actual, [seccion]: valor });

/** Por solapa: si tiene cambios sin guardar, si recién se guardó y qué error dejó el último intento. */
export function useEstadoDeSolapas() {
  const [sinGuardar, setSinGuardar] = useState(todasEn(false));
  const [guardados, setGuardados] = useState(todasEn(false));
  const [erroresGuardado, setErroresGuardado] = useState(todasEn(''));

  // Un cambio deja la solapa «sin guardar» y borra su aviso de guardado o de error.
  const cambio = (seccion: SeccionUbicacion) => {
    setSinGuardar(conValor(seccion, true));
    setGuardados(conValor(seccion, false));
    setErroresGuardado(conValor(seccion, ''));
  };

  const empezarAGuardar = (seccion: SeccionUbicacion) => setGuardados(conValor(seccion, false));

  const explicarError = (seccion: SeccionUbicacion, mensaje: string) =>
    setErroresGuardado(conValor(seccion, mensaje));

  const marcarGuardada = (seccion: SeccionUbicacion) => {
    setSinGuardar(conValor(seccion, false));
    setGuardados(conValor(seccion, true));
  };

  return {
    sinGuardar,
    guardados,
    erroresGuardado,
    cambio,
    empezarAGuardar,
    explicarError,
    marcarGuardada,
  };
}
