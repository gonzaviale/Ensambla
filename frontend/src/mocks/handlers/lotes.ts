import { delay, http, HttpResponse } from 'msw';
import { z } from 'zod';
import { tipoCampoSchema } from '@/types/campo';
import type { Lectura } from '@/types/lectura';
import { MAX_FOTOS_POR_LOTE, type Lote } from '@/types/lote';
import type { Bloque, Campo, Proyecto } from '@/types/proyecto';
import type { Registro } from '@/types/registro';
import { estado } from '../db/estado';
import { errorResponse } from '../errores';
import { api, exigirCsrf, exigirSesion, LATENCIA_BACKEND_MS } from './comun';

// Lotes creados por el mock: cuántas veces se consultó su lectura y si ya se confirmaron.
type LoteMock = { lote: Lote; consultas: number; confirmado: boolean };
const lotes = new Map<string, LoteMock>();

// Un PNG de 1×1 para que las fotos del mock se vean como imagen.
const PNG_UN_PIXEL = Uint8Array.from(
  atob('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQAAAABJRU5ErkJggg=='),
  (caracter) => caracter.charCodeAt(0),
);

const respuestaFoto = () => new HttpResponse(PNG_UN_PIXEL, { headers: { 'Content-Type': 'image/png' } });

const PROPUESTA_EJEMPLO: NonNullable<Lectura['propuesta']> = {
  tipoRegistro: 'VENTAS',
  nombre: 'Ventas',
  campos: [
    { clave: 'producto', nombre: 'Producto', tipo: 'TEXTO' },
    { clave: 'monto', nombre: 'Monto', tipo: 'NUMERO' },
  ],
  filas: [
    {
      pagina: 1,
      renglon: 1,
      datos: {
        producto: { valor: 'Pan', confianza: 0.98 },
        monto: { valor: '1200', confianza: 0.95 },
      },
    },
    {
      pagina: 1,
      renglon: 2,
      datos: {
        producto: { valor: 'Leche', confianza: 0.9 },
        monto: { valor: '850', confianza: 0.4 },
      },
    },
  ],
  advertencias: [],
};

const confirmarSchema = z.object({
  proyectoId: z.string().nullable(),
  nombreProyecto: z.string().nullable(),
  bloqueId: z.string().nullable(),
  nombreBloque: z.string().nullable(),
  campos: z.array(
    z.object({
      clave: z.string(),
      campoId: z.string().nullable(),
      nombre: z.string(),
      tipo: tipoCampoSchema,
      opciones: z.array(z.string()),
    }),
  ),
  filas: z.array(
    z.object({
      pagina: z.number().nullable(),
      valores: z.record(z.union([z.string(), z.number(), z.boolean()])),
    }),
  ),
});

type Confirmar = z.infer<typeof confirmarSchema>;

const pathDe = (request: Request) => new URL(request.url).pathname;

function proyectoDeLaSesion(id: string): Proyecto | undefined {
  const item = estado.proyectos.get(id);
  return item?.usuarioId === estado.sesionUsuarioId ? item.proyecto : undefined;
}

function bloqueDeLaSesion(id: string) {
  for (const { usuarioId, proyecto } of estado.proyectos.values()) {
    if (usuarioId !== estado.sesionUsuarioId) continue;
    const bloque = proyecto.bloques.find((item) => item.id === id);
    if (bloque) return { proyecto, bloque };
  }
  return undefined;
}

// Mismas reglas que el backend (Campo.errorDe): cada valor tiene que corresponder al tipo de su columna.
function errorDeValor(tipo: Campo['tipo'], opciones: string[], valor: string | number | boolean) {
  switch (tipo) {
    case 'NUMERO':
      return typeof valor === 'number' ? undefined : 'Tiene que ser un número';
    case 'SI_NO':
      return typeof valor === 'boolean' ? undefined : 'Tiene que ser sí o no';
    case 'FECHA':
      return typeof valor === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(valor)
        ? undefined
        : 'Tiene que ser una fecha válida';
    case 'OPCION':
      return typeof valor === 'string' && opciones.includes(valor)
        ? undefined
        : 'Elegí una de las opciones';
    case 'TEXTO':
      return typeof valor === 'string' ? undefined : 'Tiene que ser un texto';
    // Lo leído del cuaderno no elige filas de otra tabla.
    case 'RELACION':
      return 'Elegí una fila de la tabla relacionada';
    case 'FORMULA':
      return 'Este dato se calcula solo';
  }
}

function validarFilas(datos: Confirmar, destino: Bloque | undefined, fotos: Lote['fotos']) {
  const errores: Record<string, string> = {};
  datos.filas.forEach((fila, indice) => {
    for (const campo of datos.campos) {
      const valor = fila.valores[campo.clave];
      if (valor === undefined) continue;
      const existente = destino?.campos.find((item) => item.id === campo.campoId);
      const error = errorDeValor(
        existente?.tipo ?? campo.tipo,
        existente?.opciones ?? campo.opciones,
        valor,
      );
      if (error) errores[`filas[${indice}].${campo.clave}`] = error;
    }
    if (fila.pagina !== null && !fotos[fila.pagina - 1]) {
      errores[`filas[${indice}].pagina`] = 'Esta página no está en el cuaderno';
    }
  });
  return errores;
}

function crearCampos(datos: Confirmar, bloque: Bloque) {
  return new Map(
    datos.campos.map((campo) => {
      const existente = bloque.campos.find((item) => item.id === campo.campoId);
      if (existente) return [campo.clave, existente] as const;
      const nuevo: Campo = {
        id: crypto.randomUUID(),
        nombre: campo.nombre,
        tipo: campo.tipo,
        obligatorio: false,
        valorInicial: null,
        opciones: campo.opciones,
        bloqueRelacionadoId: null,
        formula: null,
        posicion: bloque.campos.length,
      };
      bloque.campos.push(nuevo);
      return [campo.clave, nuevo] as const;
    }),
  );
}

function destinoDeLaConfirmacion(datos: Confirmar) {
  if (datos.bloqueId) return bloqueDeLaSesion(datos.bloqueId);
  const ahora = new Date().toISOString();
  const proyecto = datos.proyectoId
    ? proyectoDeLaSesion(datos.proyectoId)
    : ({
        id: crypto.randomUUID(),
        nombre: datos.nombreProyecto ?? 'Proyecto',
        descripcion: null,
        plantillaId: null,
        creadoEn: ahora,
        actualizadoEn: ahora,
        bloques: [],
      } satisfies Proyecto);
  if (!proyecto) return undefined;
  const bloque: Bloque = {
    id: crypto.randomUUID(),
    nombre: datos.nombreBloque ?? 'Hoja',
    posicion: proyecto.bloques.length,
    campos: [],
  };
  return { proyecto, bloque, esNuevo: !datos.proyectoId };
}

// Simula POST /api/lotes (T-090): valida la cantidad y crea un lote pendiente. No guarda los archivos.
export const handlersLotes = [
  http.post(api('/lotes'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const error = exigirCsrf(request) ?? exigirSesion(request);
    if (error) return error;
    const fotos = (await request.formData()).getAll('fotos');
    if (fotos.length === 0) {
      return errorResponse(400, 'Falta el parámetro: fotos', '/api/lotes');
    }
    if (fotos.length > MAX_FOTOS_POR_LOTE) {
      return errorResponse(400, 'Podés subir hasta 10 fotos por lote', '/api/lotes');
    }
    const lote: Lote = {
      id: crypto.randomUUID(),
      estado: 'PENDIENTE',
      creadoEn: new Date().toISOString(),
      fotos: fotos.map((foto, posicion) => ({
        id: crypto.randomUUID(),
        posicion,
        tipoContenido: foto instanceof Blob ? foto.type : 'image/jpeg',
        tamanioBytes: foto instanceof Blob ? foto.size : 0,
      })),
    };
    lotes.set(lote.id, { lote, consultas: 0, confirmado: false });
    return HttpResponse.json(lote, {
      status: 201,
      headers: { Location: `/api/lotes/${lote.id}` },
    });
  }),

  http.get(api('/lotes/:id'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const error = exigirSesion(request);
    if (error) return error;
    const item = lotes.get(String(params.id));
    if (!item) return errorResponse(404, 'Lote no encontrado', pathDe(request));
    return HttpResponse.json(item.lote);
  }),

  http.get(api('/lotes/:id/fotos/:fotoId'), ({ request, params }) => {
    const error = exigirSesion(request);
    if (error) return error;
    const item = lotes.get(String(params.id));
    if (!item?.lote.fotos.some((foto) => foto.id === params.fotoId)) {
      return errorResponse(404, 'Foto no encontrada', pathDe(request));
    }
    return respuestaFoto();
  }),

  // Simula GET /api/lotes/{id}/lectura: la primera consulta responde PROCESANDO y las siguientes LISTO.
  http.get(api('/lotes/:id/lectura'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const error = exigirSesion(request);
    if (error) return error;
    const loteId = String(params.id);
    const item = lotes.get(loteId);
    if (!item) {
      return errorResponse(404, 'No se encontró el lote', `/api/lotes/${loteId}/lectura`);
    }
    item.consultas += 1;
    const lectura: Lectura = item.confirmado
      ? { loteId, estado: 'CONFIRMADO', intentos: 1 }
      : item.consultas === 1
        ? { loteId, estado: 'PROCESANDO', intentos: 1 }
        : { loteId, estado: 'LISTO', intentos: 1, propuesta: PROPUESTA_EJEMPLO };
    return HttpResponse.json(lectura);
  }),

  http.post(api('/lotes/:id/reintentar'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const error = exigirCsrf(request) ?? exigirSesion(request);
    if (error) return error;
    const item = lotes.get(String(params.id));
    if (item) item.consultas = 0;
    return new HttpResponse(null, { status: 202 });
  }),

  // Simula POST /api/lotes/{id}/confirmar (T-103): todo o nada, con cada registro vinculado a su foto.
  http.post(api('/lotes/:id/confirmar'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const error = exigirCsrf(request) ?? exigirSesion(request);
    if (error) return error;
    const path = pathDe(request);
    const item = lotes.get(String(params.id));
    if (!item) return errorResponse(404, 'Lote no encontrado', path);
    if (item.confirmado) return errorResponse(400, 'Este cuaderno ya se guardó', path);
    const cuerpo = confirmarSchema.safeParse(await request.json());
    if (!cuerpo.success) return errorResponse(400, 'Revisá los datos enviados', path);
    const datos = cuerpo.data;

    const destino = destinoDeLaConfirmacion(datos);
    if (!destino) return errorResponse(404, 'No se encontró el destino', path);
    const errores = validarFilas(datos, 'esNuevo' in destino ? undefined : destino.bloque, item.lote.fotos);
    if (Object.keys(errores).length > 0) {
      return errorResponse(400, 'Revisá los datos marcados', path, errores);
    }

    const { proyecto, bloque } = destino;
    if ('esNuevo' in destino) {
      proyecto.bloques.push(bloque);
      if (destino.esNuevo) {
        estado.proyectos.set(proyecto.id, {
          usuarioId: String(estado.sesionUsuarioId),
          proyecto,
        });
      }
    }
    const campos = crearCampos(datos, bloque);
    const ahora = new Date().toISOString();
    for (const fila of datos.filas) {
      const valores: Registro['valores'] = {};
      for (const [clave, campo] of campos) {
        const valor = fila.valores[clave];
        if (valor !== undefined) valores[campo.id] = valor;
      }
      const registro: Registro = {
        id: crypto.randomUUID(),
        bloqueId: bloque.id,
        valores,
        fotoOrigenId: fila.pagina === null ? null : (item.lote.fotos[fila.pagina - 1]?.id ?? null),
        creadoEn: ahora,
        actualizadoEn: ahora,
      };
      estado.registros.set(registro.id, registro);
    }
    item.confirmado = true;
    return HttpResponse.json(
      { proyectoId: proyecto.id, bloqueId: bloque.id, registros: datos.filas.length },
      { status: 201 },
    );
  }),

  http.get(api('/registros/:id/foto-origen'), ({ request, params }) => {
    const error = exigirSesion(request);
    if (error) return error;
    const registro = estado.registros.get(String(params.id));
    if (!registro?.fotoOrigenId || !bloqueDeLaSesion(registro.bloqueId)) {
      return errorResponse(404, 'El registro no tiene foto de origen', pathDe(request));
    }
    return respuestaFoto();
  }),
];
