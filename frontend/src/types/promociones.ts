import { z } from 'zod';

// Lo que conviene promocionar según lo que vende el sistema, y los lugares donde se puede compartir.
export const productoPromocionadoSchema = z.object({
  nombre: z.string(),
  precio: z.number(),
});

export const promocionSchema = z.object({
  /** De qué idea salió: «estrella», «liquidar» o «lista» (la que arma el navegador). */
  clave: z.string(),
  titulo: z.string(),
  gancho: z.string(),
  productos: z.array(productoPromocionadoSchema),
});

export const canalSchema = z.object({ clave: z.string(), nombre: z.string() });

export const sugerenciasDePromocionSchema = z.object({
  promociones: z.array(promocionSchema),
  canales: z.array(canalSchema),
});

export const textoDePromocionSchema = z.object({ texto: z.string() });

export type ProductoPromocionado = z.infer<typeof productoPromocionadoSchema>;
export type Promocion = z.infer<typeof promocionSchema>;
export type Canal = z.infer<typeof canalSchema>;
export type SugerenciasDePromocion = z.infer<typeof sugerenciasDePromocionSchema>;

/** Lo que se le manda al servidor para escribir una promoción en un canal. */
export type PedidoDeTexto = { canal: string; promocion: Promocion };
