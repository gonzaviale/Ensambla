import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { fileURLToPath } from 'node:url';
import { setTimeout as esperar } from 'node:timers/promises';

const url = new URL(process.env.ENSAMBLA_URL || 'http://localhost:8088');
const ejecutar = promisify(execFile);
const codigo = '7790895000997';
const resultados = [];

function sesion() {
  const cookies = new Map();
  async function pedir(ruta, { metodo = 'GET', cuerpo, csrf = true } = {}) {
    const headers = { Origin: url.origin };
    if (cuerpo !== undefined) headers['Content-Type'] = 'application/json';
    if (metodo !== 'GET' && csrf) {
      const token = await pedir('/api/auth/csrf');
      assert.equal(token.estado, 200, 'No se pudo obtener CSRF');
      headers[token.datos.headerName] = token.datos.token;
    }
    headers.Cookie = [...cookies].map(([nombre, valor]) => `${nombre}=${valor}`).join('; ');
    const respuesta = await fetch(new URL(ruta, url), {
      method: metodo,
      headers,
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
      signal: AbortSignal.timeout(15_000),
      redirect: 'manual',
    });
    for (const cookie of respuesta.headers.getSetCookie()) {
      // Respetar Secure también en esta prueba: una cookie inválida para HTTP no acredita un login.
      if (/;\s*Secure\b/i.test(cookie) && url.protocol !== 'https:') continue;
      const [par] = cookie.split(';');
      const corte = par.indexOf('=');
      const nombre = par.slice(0, corte);
      const valor = par.slice(corte + 1);
      if (!valor || /;\s*Max-Age=0\b/i.test(cookie)) cookies.delete(nombre);
      else cookies.set(nombre, valor);
    }
    const texto = await respuesta.text();
    const datos = respuesta.headers.get('Content-Type')?.includes('application/json') && texto
      ? JSON.parse(texto) : texto;
    return { estado: respuesta.status, datos };
  }
  return { pedir };
}

async function exigir(cliente, ruta, estado, opciones) {
  const respuesta = await cliente.pedir(ruta, opciones);
  assert.equal(respuesta.estado, estado,
    `${opciones?.metodo || 'GET'} ${ruta}: se esperaba ${estado}; respuesta ${JSON.stringify(respuesta.datos)}`);
  return respuesta.datos;
}

async function crearCuenta() {
  const cliente = sesion();
  const cuenta = { email: `integracion-${randomUUID()}@example.test`, contrasenia: `Ensambla9!${randomUUID()}` };
  await exigir(cliente, '/api/auth/register', 201, {
    metodo: 'POST', cuerpo: { ...cuenta, nombre: 'Prueba de integración', nombreNegocio: 'Integración automatizada' },
  });
  await exigir(cliente, '/api/auth/login', 200, { metodo: 'POST', cuerpo: cuenta });
  await exigir(cliente, '/api/auth/me', 200);
  return { ...cliente, cuenta };
}

function inventario(nombre, filas) {
  return {
    id: randomUUID(), type: 'inventory', name: nombre, description: '', color: 'orange', width: 'half', content: '',
    fields: [
      { key: 'name', label: 'Producto', type: 'text', required: true },
      { key: 'barcode', label: 'Código', type: 'text', required: false },
      { key: 'quantity', label: 'Cantidad', type: 'number', required: true, min: 0 },
      { key: 'price', label: 'Precio', type: 'currency', required: false, min: 0 },
    ],
    rows: filas,
  };
}

async function comprobarInventario(cliente, id) {
  const sistemas = await exigir(cliente, '/api/sistemas', 200);
  const sistema = sistemas.find((item) => item.id === id);
  assert.ok(sistema, 'El sistema no volvió desde PostgreSQL');
  const bloques = sistema.sections.flatMap((seccion) => seccion.blocks);
  const destino = bloques.find((bloque) => bloque.name === 'Escaneados');
  assert.equal(destino.rows.length, 2, 'Se duplicaron o perdieron productos');
  assert.equal(destino.rows[0].barcode, `0${codigo}`, 'Se perdieron ceros iniciales');
  assert.equal(destino.rows[0].quantity, 14, 'La cantidad persistida no coincide');
  assert.equal(destino.rows[1].barcode, '0000002674407');
  assert.equal(bloques.find((bloque) => bloque.name === 'Sin cambios').rows[0].quantity, 5);
  return sistema;
}

async function principal() {
  const anonimo = sesion();
  const pagina = await exigir(anonimo, '/', 200);
  assert.match(pagina, /<html/i);
  assert.match(pagina, /\/assets\/[^"']+\.js/);
  assert.doesNotMatch(pagina, /\/@vite\/client/, 'La prueba debe usar el build estático');
  await exigir(anonimo, `/api/catalogos/sepa/${codigo}`, 401);
  resultados.push('frontend estático y catálogo protegido por sesión');

  const propio = await crearCuenta();
  const ajeno = await crearCuenta();
  resultados.push('registro, login y cookie de sesión reales');
  const sepa = await exigir(propio, `/api/catalogos/sepa/${codigo}`, 200);
  assert.ok(sepa.producto?.nombre, 'No se cargó el índice SEPA empaquetado');
  assert.equal(sepa.producto.codigo.padStart(14, '0'), codigo.padStart(14, '0'));
  await exigir(propio, '/api/catalogos/sepa/7790895000998', 400);
  await exigir(propio, `/api/catalogos/desconocida/${codigo}`, 400);
  resultados.push('SEPA empaquetado, código e identidad validados');

  const off = await propio.pedir(`/api/catalogos/openfoodfacts/${codigo}`);
  if (process.argv.includes('--exigir-off')) {
    assert.equal(off.estado, 200, 'Open Food Facts no respondió');
    assert.ok(off.datos.producto?.nombre, 'Open Food Facts no devolvió descripción');
    assert.match(off.datos.producto.imagen || '', /^https:\/\/images\.open(food|beauty|products)facts\.org\//);
    const imagen = await fetch(off.datos.producto.imagen, { method: 'HEAD', signal: AbortSignal.timeout(10_000) });
    assert.ok(imagen.ok, `La imagen devolvió HTTP ${imagen.status}`);
    assert.match(imagen.headers.get('Content-Type') || '', /^image\//);
  } else {
    assert.ok([200, 503].includes(off.estado), `Estado inesperado de Open Food Facts: ${off.estado}`);
  }
  resultados.push(off.estado === 200 && off.datos.producto ? 'Open Food Facts real disponible' : 'Open Food Facts ausente o temporalmente indisponible; SEPA operativo');

  const id = randomUUID();
  const ruta = `/api/sistemas/${id}`;
  const ahora = new Date().toISOString();
  const cuerpo = {
    id, name: 'Verificación de inventario', createdAt: ahora, updatedAt: ahora,
    template: 'shop', layout: 'mosaic', options: { movements: true, expenses: false }, demoAlmacen: false,
    sections: [{ id: randomUUID(), name: 'Inventarios', blocks: [
      inventario('Sin cambios', [{ id: randomUUID(), name: 'Control', barcode: '77903754', quantity: 5, price: 10 }]),
      inventario('Escaneados', [
        { id: randomUUID(), name: sepa.producto.nombre, barcode: `0${codigo}`, quantity: 14, price: 1200 },
        { id: randomUUID(), name: 'Con ceros iniciales', barcode: '0000002674407', quantity: 0, price: 0 },
      ]),
    ] }],
  };
  try {
    await exigir(propio, ruta, 403, { metodo: 'PUT', cuerpo, csrf: false });
    await exigir(propio, ruta, 200, { metodo: 'PUT', cuerpo });
    const guardado = await comprobarInventario(propio, id);
    await exigir(propio, ruta, 200, { metodo: 'PUT', cuerpo: guardado });
    await comprobarInventario(propio, id);
    resultados.push('CSRF, guardado y recarga de 14 unidades en el inventario elegido, sin duplicar');

    const duplicado = structuredClone(guardado);
    const destino = duplicado.sections.flatMap((seccion) => seccion.blocks).find((bloque) => bloque.name === 'Escaneados');
    destino.rows.push({ ...destino.rows[0], id: randomUUID(), barcode: codigo });
    await exigir(propio, ruta, 409, { metodo: 'PUT', cuerpo: duplicado });
    for (const invalido of ['7790895000998', 7790895000997]) {
      const copia = structuredClone(guardado);
      copia.sections.flatMap((seccion) => seccion.blocks).find((bloque) => bloque.name === 'Escaneados').rows[0].barcode = invalido;
      await exigir(propio, ruta, 400, { metodo: 'PUT', cuerpo: copia });
    }
    await comprobarInventario(propio, id);
    resultados.push('duplicados equivalentes e inválidos rechazados sin cambios parciales');

    const otros = await exigir(ajeno, '/api/sistemas', 200);
    assert.ok(!otros.some((item) => item.id === id));
    await exigir(ajeno, ruta, 404, { metodo: 'PUT', cuerpo: guardado });
    resultados.push('aislamiento entre negocios y recurso ajeno 404');

    if (process.argv.includes('--reiniciar-backend')) {
      const raiz = fileURLToPath(new URL('../', import.meta.url));
      await ejecutar('docker', ['compose', '-f', fileURLToPath(new URL('../compose.yaml', import.meta.url)), 'restart', 'backend'],
        { cwd: raiz, timeout: 120_000, windowsHide: true });
      const limite = Date.now() + 120_000;
      let listo = false;
      while (Date.now() < limite) {
        try { listo = (await propio.pedir('/api/auth/me')).estado === 200; } catch { }
        if (listo) break;
        await esperar(2000);
      }
      assert.ok(listo, 'No se recuperó la sesión después de reiniciar el backend');
      await comprobarInventario(propio, id);
      resultados.push('sesión e inventario conservados tras reiniciar el contenedor del backend');
    }

    await exigir(propio, '/api/auth/logout', 204, { metodo: 'POST' });
    await exigir(propio, '/api/auth/me', 401);
    await exigir(propio, '/api/auth/login', 200, { metodo: 'POST', cuerpo: propio.cuenta });
    await comprobarInventario(propio, id);
    resultados.push('logout y nuevo login conservan los datos');
    console.log(JSON.stringify({ url: url.origin, resultado: 'correcto', comprobaciones: resultados }, null, 2));
  } finally {
    // Sólo se elimina el sistema creado por esta ejecución. Las cuentas de prueba quedan identificadas.
    await propio.pedir(ruta, { metodo: 'DELETE' }).catch(() => undefined);
    await propio.pedir('/api/auth/logout', { metodo: 'POST' }).catch(() => undefined);
    await ajeno.pedir('/api/auth/logout', { metodo: 'POST' }).catch(() => undefined);
  }
}

principal().catch((error) => { console.error(error.message); process.exitCode = 1; });
