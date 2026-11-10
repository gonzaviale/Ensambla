import { readFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { gunzipSync } from 'node:zlib';
import type { Plugin, ProxyOptions } from 'vite';
import { z } from 'zod';
import { codigoGtin, identidadGtin } from '../src/lib/escaneo/codigo';

const indiceSchema = z.object({
  fecha: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
  productos: z.record(
    z.string().regex(/^\d{14}$/),
    z.object({
      codigo: z.string().regex(/^(?:\d{8}|\d{12}|\d{13}|\d{14})$/),
      nombre: z.string().trim().min(1),
      marca: z.string(),
      presentacion: z.string(),
    }),
  ),
});
type Indice = z.infer<typeof indiceSchema>;

export const proxyOpenFoodFacts: ProxyOptions = {
  target: 'https://world.openfoodfacts.org',
  changeOrigin: true,
  secure: true,
  followRedirects: true,
  timeout: 12_000,
  proxyTimeout: 10_000,
  headers: { 'User-Agent': 'Ensambla-Escaneo/0.1 (https://github.com/gonzaviale/Ensambla)' },
  rewrite: (ruta) =>
    `/api/v3/product/${ruta.split('/').at(-1)}?product_type=all&lc=es&fields=code,product_name,product_name_es,brands,quantity,image_front_url`,
};

/** Adaptadores de desarrollo local. Los catálogos y sus licencias permanecen separados. */
export function catalogosProductos(): Plugin {
  let indice: Indice | undefined;
  const consultas: number[] = [];
  return {
    name: 'catalogos-de-productos-locales',
    configureServer(servidor) {
      const local = resolve(servidor.config.root, '.local/catalogo-sepa.json');
      const archivo =
        process.env.SEPA_CATALOGO_PATH ||
        (existsSync(local) ? local : resolve(servidor.config.root, 'datos/catalogos/sepa.json.gz'));
      if (existsSync(archivo)) {
        try {
          const contenido = readFileSync(archivo);
          const json = archivo.endsWith('.gz')
            ? gunzipSync(contenido, { maxOutputLength: 50_000_000 }).toString('utf8')
            : contenido.toString('utf8');
          indice = indiceSchema.parse(JSON.parse(json));
        } catch {
          servidor.config.logger.warn('El índice SEPA no es legible. Volvé a importarlo.');
        }
      }
      servidor.middlewares.use((req, res, next) => {
        if (!req.url?.startsWith('/catalogos/')) return next();
        const responder = (estado: number, datos: unknown) => {
          res.statusCode = estado;
          res.setHeader('Content-Type', 'application/json; charset=utf-8');
          res.end(JSON.stringify(datos));
        };
        if (req.method !== 'GET') return responder(405, { mensaje: 'Sólo lectura' });
        const ruta = /^\/catalogos\/(sepa|openfoodfacts)\/(\d+)$/.exec(req.url);
        if (!ruta) return responder(400, { mensaje: 'Consulta inválida' });
        let codigo: string;
        try {
          codigo = codigoGtin(ruta[2] ?? '');
        } catch {
          return responder(400, { mensaje: 'Código inválido' });
        }
        if (ruta[1] === 'sepa') {
          if (!indice) return responder(503, { mensaje: 'Catálogo SEPA todavía no importado' });
          return responder(200, {
            producto: indice.productos[identidadGtin(codigo)] ?? null,
            fecha: indice.fecha,
          });
        }
        const ahora = Date.now();
        while (consultas[0] !== undefined && consultas[0] <= ahora - 60_000) consultas.shift();
        if (consultas.length >= 12) {
          res.setHeader('Retry-After', '60');
          return responder(429, {
            mensaje: 'Límite de consultas. Podés completar el producto a mano.',
          });
        }
        consultas.push(ahora);
        // Sanitizar antes de crear el pedido. Con redirecciones, proxyReq puede llegar
        // después de enviar headers y removeHeader provoca ERR_HTTP_HEADERS_SENT.
        delete req.headers.cookie;
        delete req.headers.authorization;
        delete req.headers.origin;
        delete req.headers.referer;
        next();
      });
    },
  };
}
