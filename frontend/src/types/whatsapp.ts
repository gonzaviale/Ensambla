import { z } from 'zod';

/** El número ya verificado y vinculado; `numeroBot` es el de Sami (vacío si todavía no se configuró). */
export const vinculoWhatsappSchema = z.object({
  telefono: z.string(),
  vinculadoEn: z.string(),
  numeroBot: z.string(),
});
export type VinculoWhatsapp = z.infer<typeof vinculoWhatsappSchema>;

/** El código ya salió por WhatsApp hacia ese número; nunca vuelve en la respuesta. */
export const verificacionWhatsappSchema = z.object({
  telefono: z.string(),
  expiraEn: z.string(),
  reenviarDesde: z.string(),
});
export type VerificacionWhatsapp = z.infer<typeof verificacionWhatsappSchema>;

/** El código de un solo uso para vincular mandando «VINCULAR <código>» desde el teléfono. */
export const codigoVinculacionSchema = z.object({
  codigo: z.string(),
  expiraEn: z.string(),
  numeroBot: z.string(),
});
export type CodigoVinculacion = z.infer<typeof codigoVinculacionSchema>;

export const telefonoWhatsappSchema = z
  .string()
  .trim()
  .regex(/^[+0-9 ()-]{8,25}$/, 'Escribí el número con código de país, por ejemplo +54 9 11 2233 4455');

export const codigoVerificacionSchema = z
  .string()
  .trim()
  .regex(/^[0-9]{6}$/, 'El código tiene 6 dígitos');
