import { http, HttpResponse } from 'msw';
import {
  codigoVerificacionSchema,
  telefonoWhatsappSchema,
  type VinculoWhatsapp,
} from '@/types/whatsapp';
import { estado, type CuentaWhatsapp } from '../db/estado';
import { errorResponse } from '../errores';
import { api, cuenta, pathDe } from './comun';

/** El código que el mock da por bueno, en lugar de mandarlo por WhatsApp. */
export const CODIGO_DE_PRUEBA = '123456';
/** Un número que ya es de otra cuenta. */
export const TELEFONO_OCUPADO = '5491100000000';
const NUMERO_DE_SAMI = '5491155550000';
const MAX_INTENTOS = 5;
const ESPERA_MS = 60_000;
const VIGENCIA_MS = 10 * 60_000;

const cuentaWhatsapp = (email: string): CuentaWhatsapp =>
  estado.whatsapp.get(email) ?? { vinculo: null, verificacion: null };

// Igual que el backend: sólo dígitos y, en los celulares de Argentina, el 9 después del 54.
const normalizar = (ingresado: string) => {
  const digitos = ingresado.replace(/\D/g, '');
  return /^54(?!9)\d{10}$/.test(digitos) ? `549${digitos.slice(2)}` : digitos;
};

export const handlersWhatsapp = [
  http.get(api('/whatsapp/vinculacion'), ({ request }) => {
    const { usuario, error } = cuenta(request);
    if (error) return error;
    const { vinculo } = cuentaWhatsapp(usuario.email);
    return vinculo
      ? HttpResponse.json(vinculo)
      : errorResponse(404, 'No hay un número de WhatsApp vinculado', pathDe(request));
  }),
  http.post(api('/whatsapp/verificacion'), async ({ request }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    const cuerpo = (await request.json()) as { telefono?: string };
    const valido = telefonoWhatsappSchema.safeParse(cuerpo.telefono);
    if (!valido.success)
      return errorResponse(400, 'Revisá el número', pathDe(request), {
        telefono: valido.error.issues[0]?.message ?? 'Número inválido',
      });
    const telefono = normalizar(valido.data);
    if (telefono === TELEFONO_OCUPADO)
      return errorResponse(409, 'Ese número de WhatsApp ya está vinculado a otra cuenta', pathDe(request));
    const actual = cuentaWhatsapp(usuario.email);
    if (actual.verificacion && Date.now() < new Date(actual.verificacion.reenviarDesde).getTime())
      return errorResponse(400, 'Esperá un momento antes de pedir otro código', pathDe(request));
    const verificacion = {
      telefono,
      expiraEn: new Date(Date.now() + VIGENCIA_MS).toISOString(),
      reenviarDesde: new Date(Date.now() + ESPERA_MS).toISOString(),
      intentos: 0,
    };
    estado.whatsapp.set(usuario.email, { ...actual, verificacion });
    estado.alCambiar?.();
    return HttpResponse.json({
      telefono,
      expiraEn: verificacion.expiraEn,
      reenviarDesde: verificacion.reenviarDesde,
    });
  }),
  http.post(api('/whatsapp/verificacion/confirmar'), async ({ request }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    const cuerpo = (await request.json()) as { codigo?: string };
    if (!codigoVerificacionSchema.safeParse(cuerpo.codigo).success)
      return errorResponse(400, 'Revisá el código', pathDe(request), { codigo: 'El código tiene 6 dígitos' });
    const actual = cuentaWhatsapp(usuario.email);
    const verificacion = actual.verificacion;
    if (!verificacion)
      return errorResponse(404, 'Primero pedí un código de verificación', pathDe(request));
    if (verificacion.intentos >= MAX_INTENTOS)
      return errorResponse(400, 'Hiciste demasiados intentos. Pedí un código nuevo', pathDe(request));
    if (cuerpo.codigo?.trim() !== CODIGO_DE_PRUEBA) {
      verificacion.intentos += 1;
      return errorResponse(
        400,
        `El código no es correcto. Te quedan ${MAX_INTENTOS - verificacion.intentos} intentos`,
        pathDe(request),
      );
    }
    const vinculo: VinculoWhatsapp = {
      telefono: verificacion.telefono,
      vinculadoEn: new Date().toISOString(),
      numeroBot: NUMERO_DE_SAMI,
    };
    estado.whatsapp.set(usuario.email, { vinculo, verificacion: null });
    estado.alCambiar?.();
    return HttpResponse.json(vinculo);
  }),
  http.post(api('/whatsapp/vinculacion/codigo'), ({ request }) => {
    const { error } = cuenta(request, true);
    if (error) return error;
    return HttpResponse.json({
      codigo: 'ABC234',
      expiraEn: new Date(Date.now() + VIGENCIA_MS).toISOString(),
      numeroBot: NUMERO_DE_SAMI,
    });
  }),
  http.delete(api('/whatsapp/vinculacion'), ({ request }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    if (!cuentaWhatsapp(usuario.email).vinculo)
      return errorResponse(404, 'No hay un número de WhatsApp vinculado', pathDe(request));
    estado.whatsapp.set(usuario.email, { vinculo: null, verificacion: null });
    estado.alCambiar?.();
    return new HttpResponse(null, { status: 204 });
  }),
];
