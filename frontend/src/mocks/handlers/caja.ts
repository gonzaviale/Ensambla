import { http, HttpResponse, type RequestHandler, type JsonBodyType } from 'msw';
import { cuenta, api, pathDe } from './comun';
import { errorResponse } from '../errores';
import { ApiError } from '@/api/apiError';
import { cuentasDeCaja } from '../db/cuentasCaja';
import type {
  CierreCaja,
  CobroDeudaCaja,
  CobroQr,
  ConsultaCaja,
  SolicitudCobroQr,
  SolicitudCierreCaja,
  SolicitudCobro,
  SolicitudPagoDeudaCaja,
  VentaCaja,
} from '@/types/caja';

type Respuesta<T> = T | Promise<T>;
export type EscenarioCajaMock = {
  ventas: () => VentaCaja[];
  cobros: () => CobroDeudaCaja[];
  cobrar?: (solicitud: SolicitudCobro) => Respuesta<VentaCaja>;
  registrarPago?: (solicitud: SolicitudPagoDeudaCaja) => Respuesta<CobroDeudaCaja>;
  consultar?: (url: URL) => Respuesta<ConsultaCaja>;
  cerrar?: (solicitud: SolicitudCierreCaja) => Respuesta<CierreCaja>;
  obtenerCierre?: (id: string) => Respuesta<CierreCaja>;
  generarCobroQr?: (solicitud: SolicitudCobroQr) => Respuesta<CobroQr>;
  cancelarCobroQr?: (id: string) => Respuesta<CobroQr>;
};

export function crearHandlersCaja(escenario: EscenarioCajaMock): RequestHandler[] {
  const handlers: RequestHandler[] = [
    http.get(api('/caja/ventas'), ({ request }) =>
      atender(request, false, () => cuentasDelEscenario(escenario)),
    ),
  ];
  agregarVenta(handlers, escenario);
  agregarCobro(handlers, escenario);
  agregarConsulta(handlers, escenario);
  agregarCierre(handlers, escenario);
  agregarDetalleCierre(handlers, escenario);
  agregarCobroQr(handlers, escenario);
  return handlers;
}
function cuentasDelEscenario(escenario: EscenarioCajaMock) {
  const ventas = escenario.ventas(),
    cobros = escenario.cobros();
  return { ventas, cobros, cuentas: cuentasDeCaja(ventas, cobros) };
}
async function atender<T extends JsonBodyType>(
  request: Request,
  escritura: boolean,
  responder: () => Respuesta<T>,
) {
  const { error } = cuenta(request, escritura);
  if (error) return error;
  try {
    return HttpResponse.json(await responder());
  } catch (causa) {
    if (causa instanceof ApiError)
      return errorResponse(causa.status, causa.message, pathDe(request));
    throw causa;
  }
}
function agregarVenta(handlers: RequestHandler[], escenario: EscenarioCajaMock) {
  const cobrar = escenario.cobrar;
  if (!cobrar) return;
  handlers.push(
    http.post(api('/caja/ventas'), ({ request }) =>
      atender(request, true, async () => cobrar((await request.json()) as SolicitudCobro)),
    ),
  );
}
function agregarCobro(handlers: RequestHandler[], escenario: EscenarioCajaMock) {
  const registrar = escenario.registrarPago;
  if (!registrar) return;
  handlers.push(
    http.post(api('/caja/cobros'), ({ request }) =>
      atender(request, true, async () =>
        registrar((await request.json()) as SolicitudPagoDeudaCaja),
      ),
    ),
  );
}
function agregarConsulta(handlers: RequestHandler[], escenario: EscenarioCajaMock) {
  const consultar = escenario.consultar;
  if (!consultar) return;
  handlers.push(
    http.get(api('/caja/resumen'), ({ request }) =>
      atender(request, false, () => consultar(new URL(request.url))),
    ),
  );
}
function agregarCierre(handlers: RequestHandler[], escenario: EscenarioCajaMock) {
  const cerrar = escenario.cerrar;
  if (!cerrar) return;
  handlers.push(
    http.post(api('/caja/cierres'), ({ request }) =>
      atender(request, true, async () => cerrar((await request.json()) as SolicitudCierreCaja)),
    ),
  );
}
function agregarDetalleCierre(handlers: RequestHandler[], escenario: EscenarioCajaMock) {
  const obtener = escenario.obtenerCierre;
  if (!obtener) return;
  handlers.push(
    http.get(api('/caja/cierres/:id'), ({ request, params }) =>
      atender(request, false, () => obtener(String(params.id))),
    ),
  );
}
function agregarCobroQr(handlers: RequestHandler[], escenario: EscenarioCajaMock) {
  const { generarCobroQr, cancelarCobroQr } = escenario;
  if (generarCobroQr)
    handlers.push(
      http.post(api('/caja/cobros-qr'), ({ request }) =>
        atender(request, true, async () => generarCobroQr((await request.json()) as SolicitudCobroQr)),
      ),
    );
  if (cancelarCobroQr)
    handlers.push(
      http.post(api('/caja/cobros-qr/:id/cancelar'), ({ request, params }) =>
        atender(request, true, () => cancelarCobroQr(String(params.id))),
      ),
    );
}
