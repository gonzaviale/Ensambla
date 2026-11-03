import { delay, http, HttpResponse } from 'msw';
import { planesDe } from '../db/planes';
import { sistemasDe } from '../db/estado';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';

// Un PDF mínimo: el modo de demostración sólo necesita algo que descargar con el nombre y el tipo correctos.
const PDF_DE_EJEMPLO = new TextEncoder().encode('%PDF-1.4\n%%EOF\n');

const sinTildes = (texto: string) =>
  texto
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');

// El resumen del negocio en PDF: una función Premium del sistema de la persona.
export const handlersReportes = [
  http.get(api('/proyectos/:sistemaId/resumen/pdf'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    const sistema = sistemasDe(sesion.usuario.email).find((otro) => otro.id === params.sistemaId);
    if (!sistema) return errorResponse(404, 'Proyecto no encontrado', pathDe(request));
    if (planesDe(sesion.usuario.email).suscripcion.codigoPlanActual !== 'PREMIUM') {
      return errorResponse(403, 'Esta función es parte del plan Premium', pathDe(request));
    }
    const fecha = new Date().toISOString().slice(0, 10).replaceAll('-', '');
    return new HttpResponse(PDF_DE_EJEMPLO, {
      headers: {
        'Content-Type': 'application/pdf',
        'Content-Disposition': `attachment; filename="resumen-${sinTildes(sistema.name) || 'sistema'}-${fecha}.pdf"`,
      },
    });
  }),
];
