import { http, HttpResponse } from 'msw';
import { api, cuenta } from './comun';

// Sólo para el modo completo de pruebas/demo. La aplicación normal consulta el backend.
export const handlersCatalogos = [
  http.get(api('/catalogos/:fuente/:codigo'), ({ request }) => {
    const { error } = cuenta(request);
    return error ?? HttpResponse.json({ producto: null });
  }),
];
