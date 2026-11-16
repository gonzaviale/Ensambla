import { useState } from 'react';
import { guardarArchivo } from '@/lib/descargas';
import { contenidoDe, pedidoDeTextos } from '@/lib/carteles';
import type {
  ContenidoDeCartel,
  FormatoDeCartel,
  OpcionDeCartel,
  PromocionSugerida,
  VarianteDeCartel,
} from '@/types/carteles';
import { useArmarOpciones, useExportarCartel, useRedactarTextos } from './mutations';

export type CartelEnCurso = {
  promocion: PromocionSugerida;
  contenido: ContenidoDeCartel;
  textoPosteo: string;
  redactadoConIa: boolean;
  opciones: OpcionDeCartel[];
  variante: VarianteDeCartel;
};

// El error ya queda en el estado de cada pedido (`error`) y la pantalla lo muestra: no hace falta que suba.
const sinPropagarElError = (accion: () => Promise<void>) => accion().catch(() => undefined);

/** Arma un cartel paso a paso: elegir qué promocionar, ver las 3 opciones, ajustar los textos y bajarlo. */
export function useCrearCartel() {
  const [cartel, setCartel] = useState<CartelEnCurso | null>(null);
  const redactar = useRedactarTextos();
  const armar = useArmarOpciones();
  const exportar = useExportarCartel();

  const empezarCon = (promocion: PromocionSugerida, formato: FormatoDeCartel = 'A4') =>
    sinPropagarElError(async () => {
      const textos = await redactar.mutateAsync(pedidoDeTextos(promocion));
      const contenido = contenidoDe(promocion, textos, formato);
      const { opciones } = await armar.mutateAsync(contenido);
      setCartel({
        promocion,
        contenido,
        textoPosteo: textos.textoPosteo,
        redactadoConIa: textos.redactadoConIa,
        opciones,
        variante: opciones[0]?.variante ?? 'BLOQUE',
      });
    });

  const actualizarContenido = (cambios: Partial<ContenidoDeCartel>) =>
    sinPropagarElError(async () => {
      if (!cartel) return;
      const contenido = { ...cartel.contenido, ...cambios };
      const { opciones } = await armar.mutateAsync(contenido);
      setCartel({ ...cartel, contenido, opciones });
    });

  const elegirVariante = (variante: VarianteDeCartel) =>
    setCartel((actual) => (actual ? { ...actual, variante } : actual));

  const descargar = () =>
    sinPropagarElError(async () => {
      if (!cartel) return;
      guardarArchivo(await exportar.mutateAsync({ ...cartel.contenido, variante: cartel.variante }));
    });

  return {
    cartel,
    empezarCon,
    actualizarContenido,
    elegirVariante,
    descargar,
    redactando: redactar.isPending || armar.isPending,
    descargando: exportar.isPending,
    error: redactar.error ?? armar.error ?? exportar.error,
  };
}
