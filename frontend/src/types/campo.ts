import { z } from 'zod';

// Contrato de T-018: GET/POST /api/bloques/{id}/campos, PATCH/DELETE /api/campos/{id}.
export const TIPOS_CAMPO = ['TEXTO', 'NUMERO', 'FECHA', 'OPCION', 'SI_NO'] as const;

export const tipoCampoSchema = z.enum(TIPOS_CAMPO);

export const campoSchema = z.object({
  id: z.string().uuid(),
  bloqueId: z.string().uuid(),
  nombre: z.string(),
  tipo: tipoCampoSchema,
  obligatorio: z.boolean(),
  // Viaja como texto: NUMERO "12.5", FECHA "aaaa-mm-dd", SI_NO "true"/"false", OPCION una de `opciones`.
  valorInicial: z.string().nullable(),
  opciones: z.array(z.string()),
  posicion: z.number(),
  creadoEn: z.string(),
  actualizadoEn: z.string(),
});

export const MAX_NOMBRE_CAMPO = 120;

export type TipoCampo = z.infer<typeof tipoCampoSchema>;
export type Campo = z.infer<typeof campoSchema>;

/** Cuerpo de POST /bloques/{id}/campos y de PATCH /campos/{id} (siempre completo). */
export type GuardarCampo = {
  nombre: string;
  tipo: TipoCampo;
  obligatorio: boolean;
  valorInicial: string | null;
  opciones: string[];
};
