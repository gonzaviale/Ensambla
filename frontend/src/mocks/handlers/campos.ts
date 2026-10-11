import { delay, http, HttpResponse } from 'msw';
import { TIPOS_CAMPO, type Campo as CampoResponse, type GuardarCampo } from '@/types/campo';
import type { Campo } from '@/types/proyecto';
import { esValorInicialValido, mismoNombre, opcionRepetida } from '@/lib/campos';
import { estado } from '../db/estado';
import {
  bloqueDeLaSesion,
  fechasDe,
  registrarAlta,
  registrarCambio,
  siguientePosicion,
} from '../db/estructura';
import { errorResponse } from '../errores';
import { api, exigirCsrf, exigirSesion, LATENCIA_BACKEND_MS } from './comun';

// Contrato de T-018 (campos de un bloque) para el modo completo: los campos viven en la estructura del
// proyecto simulado (`proyecto.bloques[].campos`), con sesión y CSRF como el resto de la API simulada.

const MAX_NOMBRE = 120;

const pathDe = (request: Request) => new URL(request.url).pathname;

/** Dónde están guardados los campos de un bloque. */
type Contenedor = {
  bloqueId: string;
  campos: () => Campo[];
  reemplazar: (campos: Campo[]) => void;
  /** Fecha que se informa para campos sembrados sin fecha propia (los de una plantilla). */
  fechaBase: string;
};

type Almacen = {
  privado: (request: Request, escritura?: boolean) => Response | null;
  bloque: (bloqueId: string) => Contenedor | undefined;
  campo: (campoId: string) => { contenedor: Contenedor; campo: Campo } | undefined;
};

const almacenCompleto: Almacen = {
  privado: (request, escritura = false) =>
    (escritura && exigirCsrf(request)) || exigirSesion(request),
  bloque(bloqueId) {
    const ubicado = bloqueDeLaSesion(bloqueId);
    if (!ubicado) return undefined;
    const { proyecto, bloque } = ubicado;
    return {
      bloqueId,
      campos: () => bloque.campos,
      reemplazar: (campos) => {
        bloque.campos = campos;
      },
      fechaBase: proyecto.creadoEn,
    };
  },
  campo(campoId) {
    for (const { usuarioId, proyecto } of estado.proyectos.values()) {
      if (usuarioId !== estado.sesionUsuarioId) continue;
      for (const bloque of proyecto.bloques) {
        const campo = bloque.campos.find((item) => item.id === campoId);
        const contenedor = campo && this.bloque(bloque.id);
        if (campo && contenedor) return { contenedor, campo };
      }
    }
    return undefined;
  },
};

const aRespuesta = (contenedor: Contenedor, campo: Campo): CampoResponse => ({
  ...campo,
  bloqueId: contenedor.bloqueId,
  ...fechasDe(campo.id, contenedor.fechaBase),
});

const esTexto = (valor: unknown): valor is string => typeof valor === 'string';

// Mismas reglas que el backend: 400 con todos los `errors` juntos y, si el cuerpo es válido,
// 409 cuando el nombre ya lo usa otro campo del bloque (sin distinguir mayúsculas).
async function leerCampo(
  request: Request,
  otros: Campo[],
  propioId?: string,
): Promise<{ error: Response } | { datos: GuardarCampo }> {
  const cuerpo: unknown = await request.json().catch(() => null);
  if (!cuerpo || typeof cuerpo !== 'object') {
    return { error: errorResponse(400, 'El cuerpo JSON no es válido', pathDe(request)) };
  }
  const { nombre, tipo, obligatorio, valorInicial, opciones } = cuerpo as Record<string, unknown>;
  const errores: Record<string, string> = {};

  const texto = esTexto(nombre) ? nombre.trim() : '';
  if (!texto) errores.nombre = 'El nombre es obligatorio';
  else if (texto.length > MAX_NOMBRE)
    errores.nombre = `El nombre puede tener hasta ${MAX_NOMBRE} caracteres`;

  const tipoValido = TIPOS_CAMPO.find((item) => item === tipo);
  if (!tipoValido) errores.tipo = 'El tipo de campo no es válido';

  let listaOpciones: string[] = [];
  if (tipoValido === 'OPCION') {
    const recibidas = Array.isArray(opciones) ? opciones : [];
    listaOpciones = recibidas.map((opcion) => (esTexto(opcion) ? opcion.trim() : ''));
    if (listaOpciones.length === 0) errores.opciones = 'Un campo de opción necesita opciones';
    else if (listaOpciones.some((opcion) => !opcion))
      errores.opciones = 'Las opciones no pueden estar vacías';
    else if (opcionRepetida(listaOpciones)) errores.opciones = 'Las opciones no pueden repetirse';
  }

  const inicial = esTexto(valorInicial) && valorInicial.trim() !== '' ? valorInicial : null;
  if (
    inicial !== null &&
    tipoValido &&
    !errores.opciones &&
    !esValorInicialValido(
      tipoValido,
      tipoValido === 'TEXTO' ? inicial : inicial.trim(),
      listaOpciones,
    )
  ) {
    errores.valorInicial = 'El valor inicial no es válido para el tipo de campo';
  }

  if (Object.keys(errores).length > 0 || !tipoValido) {
    return {
      error: errorResponse(
        400,
        'Error de validación en los datos enviados',
        pathDe(request),
        errores,
      ),
    };
  }
  if (otros.some((campo) => campo.id !== propioId && mismoNombre(campo.nombre, texto))) {
    return {
      error: errorResponse(
        409,
        `Ya hay un campo llamado "${texto}" en este bloque`,
        pathDe(request),
      ),
    };
  }
  return {
    datos: {
      nombre: texto,
      tipo: tipoValido,
      obligatorio: obligatorio === true,
      valorInicial: inicial === null || tipoValido === 'TEXTO' ? inicial : inicial.trim(),
      opciones: listaOpciones,
    },
  };
}

function crearHandlers(almacen: Almacen) {
  return [
    http.get(api('/bloques/:bloqueId/campos'), async ({ request, params }) => {
      await delay(LATENCIA_BACKEND_MS);
      const error = almacen.privado(request);
      if (error) return error;
      const contenedor = almacen.bloque(String(params.bloqueId));
      if (!contenedor) return errorResponse(404, 'El bloque no existe', pathDe(request));
      return HttpResponse.json(
        [...contenedor.campos()]
          .sort((a, b) => a.posicion - b.posicion)
          .map((campo) => aRespuesta(contenedor, campo)),
      );
    }),

    http.post(api('/bloques/:bloqueId/campos'), async ({ request, params }) => {
      await delay(LATENCIA_BACKEND_MS);
      const error = almacen.privado(request, true);
      if (error) return error;
      const contenedor = almacen.bloque(String(params.bloqueId));
      if (!contenedor) return errorResponse(404, 'El bloque no existe', pathDe(request));
      const leido = await leerCampo(request, contenedor.campos());
      if ('error' in leido) return leido.error;

      const campos = contenedor.campos();
      const campo: Campo = {
        id: crypto.randomUUID(),
        ...leido.datos,
        posicion: siguientePosicion(campos),
      };
      contenedor.reemplazar([...campos, campo]);
      registrarAlta(campo.id);
      return HttpResponse.json(aRespuesta(contenedor, campo), {
        status: 201,
        headers: { Location: `/api/campos/${campo.id}` },
      });
    }),

    http.patch(api('/campos/:campoId'), async ({ request, params }) => {
      await delay(LATENCIA_BACKEND_MS);
      const error = almacen.privado(request, true);
      if (error) return error;
      const ubicado = almacen.campo(String(params.campoId));
      if (!ubicado) return errorResponse(404, 'El campo no existe', pathDe(request));
      const { contenedor, campo } = ubicado;
      const leido = await leerCampo(request, contenedor.campos(), campo.id);
      if ('error' in leido) return leido.error;

      const editado: Campo = { ...campo, ...leido.datos };
      contenedor.reemplazar(
        contenedor.campos().map((item) => (item.id === campo.id ? editado : item)),
      );
      registrarCambio(campo.id, contenedor.fechaBase);
      return HttpResponse.json(aRespuesta(contenedor, editado));
    }),

    http.delete(api('/campos/:campoId'), async ({ request, params }) => {
      await delay(LATENCIA_BACKEND_MS);
      const error = almacen.privado(request, true);
      if (error) return error;
      const ubicado = almacen.campo(String(params.campoId));
      if (!ubicado) return errorResponse(404, 'El campo no existe', pathDe(request));
      const { contenedor, campo } = ubicado;

      contenedor.reemplazar(contenedor.campos().filter((item) => item.id !== campo.id));
      estado.fechas.delete(campo.id);
      // Los registros dejan de tener ese dato.
      for (const registro of estado.registros.values()) {
        if (registro.bloqueId !== contenedor.bloqueId) continue;
        registro.valores = Object.fromEntries(
          Object.entries(registro.valores).filter(([campoId]) => campoId !== campo.id),
        );
      }
      return new HttpResponse(null, { status: 204 });
    }),
  ];
}

export const handlersCampos = crearHandlers(almacenCompleto);
