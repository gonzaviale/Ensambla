import { PASOS_RECORRIDO, TOTAL_PASOS, type PasoRecorrido } from '@/lib/recorrido/pasos';
import { useRecorridoStore } from '@/stores/recorrido';

// Lo único que la UI importa del recorrido guiado.
export function useRecorrido() {
  const visto = useRecorridoStore((s) => s.visto);
  const activo = useRecorridoStore((s) => s.activo);
  const paso = useRecorridoStore((s) => s.paso);
  const iniciar = useRecorridoStore((s) => s.iniciar);
  const siguiente = useRecorridoStore((s) => s.siguiente);
  const anterior = useRecorridoStore((s) => s.anterior);
  const cerrar = useRecorridoStore((s) => s.cerrar);
  const descartarOferta = useRecorridoStore((s) => s.descartarOferta);

  const actual: PasoRecorrido | undefined = PASOS_RECORRIDO[paso];

  return {
    visto,
    activo,
    paso,
    total: TOTAL_PASOS,
    actual,
    esUltimo: paso >= TOTAL_PASOS - 1,
    iniciar,
    siguiente,
    anterior,
    cerrar,
    descartarOferta,
  };
}
