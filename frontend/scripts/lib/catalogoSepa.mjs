import { createReadStream, createWriteStream } from 'node:fs';
import { mkdir, mkdtemp, rename, rm, unlink, writeFile } from 'node:fs/promises';
import { createHash, randomUUID } from 'node:crypto';
import { dirname, join, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Readable, Transform } from 'node:stream';
import { pipeline } from 'node:stream/promises';
import { createInterface } from 'node:readline';
import { parse } from 'csv-parse';
import yauzl from 'yauzl';

export const API_SEPA = 'https://datos.produccion.gob.ar/api/3/action/package_show?id=sepa-precios';
export const SALIDA_SEPA = resolve(
  dirname(fileURLToPath(import.meta.url)),
  '../../.local/catalogo-sepa.json',
);
const MAX_ZIP = 1_000_000_000;
const limpiar = (dato) => (typeof dato === 'string' ? dato.trim().replace(/\s+/g, ' ') : '');

function validarFecha(fecha) {
  if (
    !/^\d{4}-\d{2}-\d{2}$/.test(fecha ?? '') ||
    !Number.isFinite(Date.parse(`${fecha}T00:00:00Z`)) ||
    new Date(`${fecha}T00:00:00Z`).toISOString().slice(0, 10) !== fecha
  )
    throw new Error('Indicá una fecha válida: AAAA-MM-DD.');
}

function gtin(codigo) {
  if (!/^(?:\d{8}|\d{12}|\d{13}|\d{14})$/.test(codigo) || /^0+$/.test(codigo)) return null;
  let suma = 0;
  for (let i = codigo.length - 2, peso = 3; i >= 0; i--, peso = peso === 3 ? 1 : 3)
    suma += Number(codigo[i]) * peso;
  return (10 - (suma % 10)) % 10 === Number(codigo.at(-1)) ? codigo.padStart(14, '0') : null;
}

export function seleccionarRecursoSepa(respuesta) {
  if (respuesta?.success !== true || !Array.isArray(respuesta.result?.resources))
    throw new Error('El catálogo oficial SEPA no devolvió sus recursos.');
  const recursos = respuesta.result.resources
    .flatMap((recurso) => {
      try {
        const url = new URL(recurso.url);
        const fecha = recurso.last_modified || recurso.created;
        if (
          url.protocol !== 'https:' ||
          url.hostname !== 'datos.produccion.gob.ar' ||
          !url.pathname.endsWith('.zip') ||
          !Number.isFinite(Date.parse(fecha))
        )
          return [];
        return [{ url: url.href, fecha: fecha.slice(0, 10), modificado: Date.parse(fecha) }];
      } catch {
        return [];
      }
    })
    .sort((a, b) => b.modificado - a.modificado);
  if (!recursos[0]) throw new Error('No hay un ZIP SEPA oficial con fecha válida.');
  return recursos[0];
}

async function crearTemporal(salida) {
  await mkdir(dirname(salida), { recursive: true });
  return mkdtemp(join(dirname(salida), '.sepa-'));
}

async function limpiarTemporal(temporal, salida) {
  // Sólo se elimina la carpeta temporal propia, resuelta dentro del directorio del índice.
  const destino = resolve(temporal);
  if (
    !destino.startsWith(resolve(dirname(salida)) + sep) ||
    !destino.split(sep).at(-1).startsWith('.sepa-')
  )
    throw new Error('La carpeta temporal no pertenece al catálogo.');
  await rm(destino, { recursive: true, force: true });
}

function acumulador() {
  const productos = Object.create(null);
  const codigos = new Map();
  const conteos = { filas: 0, omitidos: 0, archivos: 0 };
  return {
    productos,
    conteos,
    agregar(codigo, nombre, marca = '', presentacion = '', esEan = true) {
      conteos.filas++;
      codigo = limpiar(codigo);
      nombre = limpiar(nombre);
      if (!esEan || !nombre) {
        conteos.omitidos++;
        return;
      }
      if (!codigos.has(codigo)) codigos.set(codigo, gtin(codigo));
      const identidad = codigos.get(codigo);
      if (!identidad) {
        conteos.omitidos++;
        return;
      }
      const anterior = productos[identidad];
      if (!anterior || nombre.length > anterior.nombre.length)
        productos[identidad] = {
          codigo,
          nombre,
          marca: limpiar(marca),
          presentacion: limpiar(presentacion),
        };
    },
  };
}

async function* decodificar(stream) {
  let decoder;
  for await (const parte of stream) {
    if (!decoder) {
      let encoding = 'utf-8';
      if (parte[0] === 0xff && parte[1] === 0xfe) encoding = 'utf-16le';
      else if (parte[0] === 0xfe && parte[1] === 0xff) encoding = 'utf-16be';
      else {
        try {
          new TextDecoder('utf-8', { fatal: true }).decode(parte, { stream: true });
        } catch {
          encoding = 'windows-1252';
        }
      }
      decoder = new TextDecoder(encoding);
    }
    yield decoder.decode(parte, { stream: true });
  }
  if (decoder) yield decoder.decode();
}

async function leerProductosCsv(stream, datos) {
  let columnas;
  let errorDeColumnas;
  await pipeline(
    Readable.from(decodificar(stream)),
    parse({
      delimiter: '|',
      record_delimiter: ['\r\r\n', '\r\n', '\n', '\r'],
      bom: true,
      skip_empty_lines: true,
      relax_quotes: true,
      relax_column_count: true,
      max_record_size: 2_000_000,
    }),
    async (filas) => {
      for await (const fila of filas) {
        if (!columnas) {
          columnas = Object.fromEntries(fila.map((clave, i) => [clave.trim().toLowerCase(), i]));
          if (
            ['id_producto', 'productos_ean', 'productos_descripcion'].some(
              (clave) => columnas[clave] === undefined,
            )
          ) {
            errorDeColumnas = new Error(
              'El CSV SEPA no tiene las columnas de productos esperadas.',
            );
            throw errorDeColumnas;
          }
          continue;
        }
        const valor = (clave) => fila[columnas[clave]] ?? '';
        const cantidad = Number(valor('productos_cantidad_presentacion').replace(',', '.'));
        const unidad = limpiar(valor('productos_unidad_medida_presentacion'));
        const unidades = { gr: 'g', cm3: 'ml' };
        const presentacion =
          cantidad > 0 && unidad ? `${cantidad} ${unidades[unidad] ?? unidad}` : '';
        // productos_ean es un indicador (1 = EAN); el código real es id_producto.
        datos.agregar(
          valor('id_producto'),
          valor('productos_descripcion'),
          valor('productos_marca'),
          presentacion,
          valor('productos_ean').trim() === '1',
        );
      }
    },
  ).catch((error) => {
    throw errorDeColumnas ?? error;
  });
  datos.conteos.archivos++;
}

async function leerZip(archivo, temporal, datos, log, profundidad = 0) {
  const zip = await yauzl.openPromise(archivo);
  try {
    for await (const entrada of zip.eachEntry()) {
      if (!entrada.uncompressedSize) continue;
      if (/(^|\/)productos\.csv$/i.test(entrada.fileName)) {
        await leerProductosCsv(await zip.openReadStreamPromise(entrada), datos);
        log(
          `SEPA: ${datos.conteos.archivos} archivos, ${Object.keys(datos.productos).length} códigos.`,
        );
      } else if (/\.zip$/i.test(entrada.fileName)) {
        if (profundidad >= 2 || entrada.uncompressedSize > MAX_ZIP)
          throw new Error('El ZIP SEPA supera el tamaño o anidamiento admitido.');
        // Los nombres del ZIP nunca se usan como rutas de salida.
        const anidado = join(temporal, `${randomUUID()}.zip`);
        try {
          await pipeline(
            await zip.openReadStreamPromise(entrada),
            createWriteStream(anidado, { flags: 'wx' }),
          );
          await leerZip(anidado, temporal, datos, log, profundidad + 1);
        } finally {
          await unlink(anidado).catch(() => undefined);
        }
      }
    }
  } finally {
    zip.close();
  }
}

async function construirIndice({ archivo, fecha, salida, origen, sha256, temporal, log }) {
  validarFecha(fecha);
  const datos = acumulador();
  if (/\.zip$/i.test(archivo)) await leerZip(archivo, temporal, datos, log);
  else if (/\.jsonl$/i.test(archivo)) {
    for await (const linea of createInterface({
      input: createReadStream(archivo, 'utf8'),
      crlfDelay: Infinity,
    })) {
      if (!linea.trim()) continue;
      const p = JSON.parse(linea);
      const nombres = Array.isArray(p.descripciones)
        ? p.descripciones.filter((n) => typeof n === 'string')
        : [];
      const marcas = Array.isArray(p.marcas) ? p.marcas.filter((m) => typeof m === 'string') : [];
      datos.agregar(p.codigo, nombres.sort((a, b) => b.length - a.length)[0], marcas.join(', '));
    }
    datos.conteos.archivos++;
  } else throw new Error('El archivo debe ser un ZIP oficial SEPA o un JSONL de productos.');
  const cantidad = Object.keys(datos.productos).length;
  if (!cantidad) throw new Error('No hay productos válidos. Se conserva el índice anterior.');
  const indice = {
    fecha,
    fuente: 'SEPA / Secretaría de Comercio',
    licencia: 'CC BY 4.0',
    url: 'https://datos.produccion.gob.ar/dataset/sepa-precios',
    ...(origen ? { origen } : {}),
    ...(sha256 ? { sha256 } : {}),
    productos: datos.productos,
  };
  const pendiente = join(temporal, 'indice.json');
  await writeFile(pendiente, JSON.stringify(indice));
  // El índice previo sólo se reemplaza cuando se terminó de validar todo el archivo.
  await rename(pendiente, salida);
  return { cantidad, fecha, ...datos.conteos, ...(sha256 ? { sha256 } : {}) };
}

export async function importarSepa({ archivo, fecha, salida = SALIDA_SEPA, log = console.log }) {
  salida = resolve(salida);
  validarFecha(fecha);
  const temporal = await crearTemporal(salida);
  try {
    return await construirIndice({ archivo: resolve(archivo), fecha, salida, temporal, log });
  } finally {
    await limpiarTemporal(temporal, salida);
  }
}

export async function descargarSepa({
  salida = SALIDA_SEPA,
  fetchImpl = fetch,
  log = console.log,
} = {}) {
  const respuesta = await fetchImpl(API_SEPA, { signal: AbortSignal.timeout(30_000) });
  if (!respuesta.ok)
    throw new Error(`El catálogo oficial no respondió (HTTP ${respuesta.status}).`);
  const recurso = seleccionarRecursoSepa(await respuesta.json());
  salida = resolve(salida);
  const temporal = await crearTemporal(salida);
  try {
    log(`Descargando SEPA del ${recurso.fecha} desde la fuente oficial…`);
    const res = await fetchImpl(recurso.url, { signal: AbortSignal.timeout(300_000) });
    if (!res.ok || !res.body) throw new Error(`La descarga SEPA falló (HTTP ${res.status}).`);
    const archivo = join(temporal, 'descarga.zip');
    const hash = createHash('sha256');
    let bytes = 0;
    let aviso = 0;
    await pipeline(
      Readable.fromWeb(res.body),
      new Transform({
        transform(parte, _encoding, cb) {
          bytes += parte.length;
          if (bytes > MAX_ZIP) return cb(new Error('La descarga SEPA supera 1 GB.'));
          hash.update(parte);
          if (bytes - aviso >= 64 * 1024 * 1024) {
            aviso = bytes;
            log(`Descargados ${Math.round(bytes / 1024 / 1024)} MB…`);
          }
          cb(null, parte);
        },
      }),
      createWriteStream(archivo, { flags: 'wx' }),
    );
    const esperado = Number(res.headers.get('content-length'));
    if (esperado && !res.headers.get('content-encoding') && bytes !== esperado)
      throw new Error('La descarga SEPA quedó incompleta. Se conserva el índice anterior.');
    return await construirIndice({
      archivo,
      fecha: recurso.fecha,
      salida,
      temporal,
      origen: recurso.url,
      sha256: hash.digest('hex'),
      log,
    });
  } finally {
    await limpiarTemporal(temporal, salida);
  }
}
