import { z } from 'zod';

// Lo que el backend de Sami (POST /api/sami/conversacion y /api/sami/proyectos) y las plantillas
// (GET /api/plantillas) devuelven. Ver docs/sami-asistente.md.

export const tipoDeColumnaSchema = z.enum(['TEXTO', 'NUMERO', 'FECHA', 'OPCION', 'SI_NO']);
export type TipoDeColumna = z.infer<typeof tipoDeColumnaSchema>;

export const columnaSugeridaSchema = z.object({
  nombre: z.string(),
  tipo: tipoDeColumnaSchema,
  opciones: z
    .array(z.string())
    .nullish()
    .transform((valor) => valor ?? []),
});
export type ColumnaSugerida = z.infer<typeof columnaSugeridaSchema>;

export const opcionDePreguntaSchema = z.object({ id: z.string(), texto: z.string() });

export const preguntaDeSamiSchema = z.object({
  id: z.string(),
  texto: z.string(),
  opciones: z.array(opcionDePreguntaSchema),
});
export type PreguntaDeSami = z.infer<typeof preguntaDeSamiSchema>;
export type OpcionDePregunta = z.infer<typeof opcionDePreguntaSchema>;

export const propuestaDeSamiSchema = z.object({
  plantillaId: z.string(),
  plantilla: z.string(),
  columnas: z.array(columnaSugeridaSchema),
  confianza: z.number(),
  /** ALGORITMO: diccionario y preguntas, sin costo. IA: la consultó Sami. */
  origen: z.enum(['ALGORITMO', 'IA']),
});
export type PropuestaDeSami = z.infer<typeof propuestaDeSamiSchema>;

export const respuestaDeConversacionSchema = z.object({
  tipo: z.enum(['PREGUNTA', 'PROPUESTA', 'RESPALDO_IA']),
  pregunta: preguntaDeSamiSchema.nullish(),
  propuesta: propuestaDeSamiSchema.nullish(),
  preguntasHechas: z.number(),
});
export type RespuestaDeConversacion = z.infer<typeof respuestaDeConversacionSchema>;

export type RespuestaElegida = { preguntaId: string; opcionId: string };
export type PedidoDeConversacion = { texto: string; respuestas: RespuestaElegida[] };

export type PedidoDeProyecto = {
  plantillaId: string;
  nombre?: string;
  columnas: { nombre: string; tipo: TipoDeColumna; opciones?: string[] }[];
};

const campoDePlantillaSchema = z.object({
  nombre: z.string(),
  tipo: z.string(),
  opciones: z.array(z.string()).nullish(),
});

export const plantillaDelServidorSchema = z.object({
  id: z.string(),
  nombre: z.string(),
  descripcion: z.string().nullish(),
  estructura: z.object({
    bloques: z.array(z.object({ nombre: z.string(), campos: z.array(campoDePlantillaSchema) })),
  }),
});
export type PlantillaDelServidor = z.infer<typeof plantillaDelServidorSchema>;

export const proyectoCreadoSchema = z.object({
  id: z.string(),
  nombre: z.string(),
  bloques: z.array(
    z.object({
      nombre: z.string(),
      campos: z.array(z.object({ nombre: z.string(), tipo: z.string() })),
    }),
  ),
});
export type ProyectoCreado = z.infer<typeof proyectoCreadoSchema>;
