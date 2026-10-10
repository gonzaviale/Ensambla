import { z } from 'zod';

export const usuarioSchema = z.object({
  id: z.string(),
  nombre: z.string(),
  email: z.string(),
});

const MAX_BYTES_CONTRASENIA = 72;

const emailSchema = z
  .string()
  .trim()
  .min(1, 'Ingresá tu email')
  .max(254, 'El email es demasiado largo')
  .email('Ingresá un email válido');

export const loginSchema = z.object({
  email: emailSchema,
  contrasenia: z.string().min(1, 'Ingresá tu contraseña').max(MAX_BYTES_CONTRASENIA),
});

export const registroSchema = z.object({
  nombre: z.string().trim().min(1, 'Ingresá tu nombre').max(100, 'Máximo 100 caracteres'),
  nombreNegocio: z
    .string()
    .trim()
    .min(1, 'Ingresá el nombre de tu negocio')
    .max(120, 'Máximo 120 caracteres'),
  email: emailSchema,
  contrasenia: z
    .string()
    .min(8, 'Usá al menos 8 caracteres')
    .max(MAX_BYTES_CONTRASENIA, 'Máximo 72 caracteres')
    .refine((valor) => new TextEncoder().encode(valor).length <= MAX_BYTES_CONTRASENIA, {
      message: 'La contraseña es demasiado larga',
    }),
});

export type Usuario = z.infer<typeof usuarioSchema>;
export type Login = z.infer<typeof loginSchema>;
export type Registro = z.infer<typeof registroSchema>;
