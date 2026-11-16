import { mkdtemp, readFile, readdir, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve, sep } from 'node:path';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import yazl from 'yazl';
import { descargarSepa, importarSepa, seleccionarRecursoSepa } from './lib/catalogoSepa.mjs';

let directorio;
let salida;
beforeEach(async () => {
  directorio = await mkdtemp(join(tmpdir(), 'ensambla-sepa-test-'));
  salida = join(directorio, 'catalogo.json');
});
afterEach(async () => {
  if (!resolve(directorio).startsWith(resolve(tmpdir()) + sep))
    throw new Error('Temporal inválido');
  await rm(directorio, { recursive: true, force: true });
});
async function zipDe(entradas) {
  const zip = new yazl.ZipFile();
  for (const [nombre, contenido] of entradas) zip.addBuffer(Buffer.from(contenido), nombre);
  zip.end();
  const partes = [];
  for await (const parte of zip.outputStream) partes.push(parte);
  return Buffer.concat(partes);
}
const cabecera =
  'id_producto|productos_ean|productos_descripcion|productos_marca|productos_cantidad_presentacion|productos_unidad_medida_presentacion|productos_precio_lista';
const csv = `${cabecera}\r\r\n77903754|1|"Alfajor con chocolate | Guaymallén"|Guaymallén|48.00|gr|999999\r\r\n0000077903754|1|Alfajor|Guaymallén|48|gr|999999\r\r\n77903755|1|Código inválido||1|unidad|0\r\r\n3017620422003|0|Código interno||1|unidad|0\r\r\n`;
const recurso = {
  url: 'https://datos.produccion.gob.ar/dataset/sepa/download/sepa_martes.zip',
  last_modified: '2026-11-10T16:00:00',
};
const metadata = { success: true, result: { resources: [recurso] } };

describe('descarga e importación de SEPA', () => {
  it('selecciona el ZIP oficial más reciente, sin depender del orden ni aceptar otros hosts', () => {
    expect(
      seleccionarRecursoSepa({
        success: true,
        result: {
          resources: [
            { ...recurso, last_modified: '2026-11-08T16:00:00' },
            {
              ...recurso,
              url: 'https://otro.example/catalogo.zip',
              last_modified: '2026-11-11T16:00:00',
            },
            recurso,
          ],
        },
      }).fecha,
    ).toBe('2026-11-10');
    expect(() => seleccionarRecursoSepa({ success: false })).toThrow(/no devolvió/);
  });
  it('descarga el ZIP con CSV anidados, deduplica GTIN y no importa precios de supermercados', async () => {
    const interior = await zipDe([['productos.csv', csv]]);
    const archivo = await zipDe([
      ['2026-11-10/comercio.zip', interior],
      ['2026-11-10/vacio.zip', ''],
    ]);
    const consulta = vi
      .fn()
      .mockResolvedValueOnce(new Response(JSON.stringify(metadata)))
      .mockResolvedValueOnce(
        new Response(archivo, { headers: { 'content-length': String(archivo.length) } }),
      );
    const resultado = await descargarSepa({ salida, fetchImpl: consulta, log: () => undefined });
    expect(resultado).toMatchObject({ cantidad: 1, fecha: '2026-11-10', archivos: 1, omitidos: 2 });
    const indice = JSON.parse(await readFile(salida, 'utf8'));
    expect(indice.sha256).toMatch(/^[a-f0-9]{64}$/);
    expect(indice.productos['00000077903754']).toEqual({
      codigo: '77903754',
      nombre: 'Alfajor con chocolate | Guaymallén',
      marca: 'Guaymallén',
      presentacion: '48 g',
    });
    expect(await readdir(directorio)).toEqual(['catalogo.json']);
    expect(await readFile(salida, 'utf8')).not.toContain('999999');
  });
  it('mantiene el índice anterior si falla HTTP, el ZIP es inválido o queda incompleto', async () => {
    await writeFile(salida, 'indice anterior');
    for (const respuesta of [
      new Response('', { status: 503 }),
      new Response('no es ZIP'),
      new Response('ZIP incompleto', { headers: { 'content-length': '1000' } }),
    ]) {
      const consulta = vi
        .fn()
        .mockResolvedValueOnce(new Response(JSON.stringify(metadata)))
        .mockResolvedValueOnce(respuesta);
      await expect(
        descargarSepa({ salida, fetchImpl: consulta, log: () => undefined }),
      ).rejects.toThrow();
      expect(await readFile(salida, 'utf8')).toBe('indice anterior');
      expect(await readdir(directorio)).toEqual(['catalogo.json']);
    }
  });
  it('no reemplaza el índice si un CSV posterior es ilegible, aunque otro ya se procesó', async () => {
    await writeFile(salida, 'indice anterior');
    const archivo = join(directorio, 'fuente.zip');
    await writeFile(
      archivo,
      await zipDe([
        ['primero/productos.csv', csv],
        ['segundo/productos.csv', 'otra|cabecera\n1|2'],
      ]),
    );
    await expect(
      importarSepa({ archivo, salida, fecha: '2026-11-10', log: () => undefined }),
    ).rejects.toThrow(/columnas/);
    expect(await readFile(salida, 'utf8')).toBe('indice anterior');
  });
  it('respeta CSV en Windows-1252 y conserva los ceros iniciales', async () => {
    const archivo = join(directorio, 'fuente.zip');
    await writeFile(
      archivo,
      await zipDe([
        [
          'productos.csv',
          Buffer.from(`${cabecera}\n0000077903754|1|Guaymallén|Guaymallén|48|gr|0`, 'latin1'),
        ],
      ]),
    );
    await importarSepa({ archivo, salida, fecha: '2026-11-10', log: () => undefined });
    const indice = JSON.parse(await readFile(salida, 'utf8'));
    expect(indice.productos['00000077903754']).toMatchObject({
      codigo: '0000077903754',
      nombre: 'Guaymallén',
    });
  });
  it('mantiene el importador JSONL y rechaza archivos sin productos válidos', async () => {
    const archivo = join(directorio, 'productos.jsonl');
    await writeFile(
      archivo,
      JSON.stringify({
        codigo: '77903754',
        descripciones: ['Alfajor', 'Alfajor Guaymallén de chocolate'],
        marcas: ['Guaymallén'],
      }),
    );
    await importarSepa({ archivo, salida, fecha: '2026-11-10', log: () => undefined });
    expect(JSON.parse(await readFile(salida, 'utf8')).productos['00000077903754'].nombre).toBe(
      'Alfajor Guaymallén de chocolate',
    );
    const previo = await readFile(salida, 'utf8');
    await writeFile(archivo, JSON.stringify({ codigo: '77903755', descripciones: ['Inválido'] }));
    await expect(
      importarSepa({ archivo, salida, fecha: '2026-11-10', log: () => undefined }),
    ).rejects.toThrow(/No hay productos válidos/);
    expect(await readFile(salida, 'utf8')).toBe(previo);
  });
});
