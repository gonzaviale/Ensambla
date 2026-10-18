import { FetchInterceptor } from '@mswjs/interceptors/fetch';
import type { RequestHandler } from 'msw';

// Respaldo para navegadores que no dejan registrar el service worker de MSW (paneles integrados, modo
// privado, políticas de empresa): los mismos handlers atienden los fetch sin pasar por el worker.
export function interceptarFetch(handlers: RequestHandler[]) {
  const interceptor = new FetchInterceptor();
  interceptor.on('request', async ({ request, requestId, controller }) => {
    for (const handler of handlers) {
      const resultado = await handler.run({ request: request.clone(), requestId });
      if (resultado?.response) {
        controller.respondWith(resultado.response);
        return;
      }
    }
  });
  interceptor.apply();
}
