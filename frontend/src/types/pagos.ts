import { z } from 'zod';

const fecha = z.string().datetime({ offset: true });
const enlaceMercadoPago = z.string().refine((valor) => {
  if (valor === '') return true;
  try {
    const url = new URL(valor);
    return (
      url.protocol === 'https:' &&
      url.hostname === 'www.mercadopago.com.ar' &&
      url.pathname === '/subscriptions/checkout' &&
      url.username === '' &&
      url.password === ''
    );
  } catch {
    return false;
  }
}, 'El enlace de Mercado Pago no es válido');

export const renovacionSchema = z.object({
  habilitada: z.boolean(),
  periodoDias: z.literal(30),
  contrato: z
    .object({
      id: z.string().uuid(),
      estado: z.enum(['PREPARANDO', 'PENDIENTE', 'AUTORIZADA', 'PAUSADA', 'CANCELADA']),
      importe: z.number().positive(),
      moneda: z.literal('ARS'),
      primerCobroEn: fecha,
      proximoCobroEn: fecha.nullable(),
      cancelacionSolicitada: z.boolean(),
      enlace: enlaceMercadoPago.nullable(),
    })
    .nullable(),
});

export type Renovacion = z.infer<typeof renovacionSchema>;
