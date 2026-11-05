import {
  sugerenciasDePromocionSchema,
  textoDePromocionSchema,
  type PedidoDeTexto,
} from '@/types/promociones';
import { http } from './client';

// Lo que conviene promocionar según lo que vende el sistema, y los canales donde compartirlo.
export const consultarPromociones = (sistemaId: string, signal?: AbortSignal) =>
  http.get(
    `/proyectos/${encodeURIComponent(sistemaId)}/promociones`,
    sugerenciasDePromocionSchema,
    signal,
  );

// La promoción escrita con el tono y el largo del canal elegido.
export const redactarPromocion = ({ canal, promocion }: PedidoDeTexto) =>
  http.post('/promociones/texto', { canal, ...promocion }, textoDePromocionSchema);
