/* ==========================================================================
   Ensambla · Landing page
   ========================================================================== */

// Dirección de la app (frontend/). Cambiala por la URL de producción al publicar.
const APP_URL = 'http://localhost:5173';
const RUTAS_APP = { registro: '/registro', login: '/login' };

const CLAVE_TEMA = 'ensambla-tema';
const CLAVE_COMENTARIOS = 'ensambla-comentarios';

document.documentElement.classList.add('js');

// Lectura y escritura seguras: el almacenamiento puede no estar disponible (modo privado, etc.)
const almacen = {
  leer(clave, porDefecto) {
    try {
      const valor = localStorage.getItem(clave);
      return valor === null ? porDefecto : JSON.parse(valor);
    } catch {
      return porDefecto;
    }
  },
  guardar(clave, valor) {
    try { localStorage.setItem(clave, JSON.stringify(valor)); } catch { /* sin almacenamiento */ }
  },
};

/* ---------- Enlaces a la app ---------- */
document.querySelectorAll('[data-app]').forEach((enlace) => {
  const ruta = RUTAS_APP[enlace.dataset.app];
  if (ruta) enlace.href = APP_URL + ruta;
});

/* ---------- Modo claro / oscuro (claro por defecto) ---------- */
const botonTema = document.getElementById('tema');
const metaTema = document.querySelector('meta[name="theme-color"]');

function aplicarTema(tema) {
  document.documentElement.dataset.theme = tema;
  const oscuro = tema === 'dark';
  botonTema.setAttribute('aria-pressed', String(oscuro));
  botonTema.setAttribute('aria-label', oscuro ? 'Activar modo claro' : 'Activar modo oscuro');
  metaTema.setAttribute('content', oscuro ? '#07090C' : '#FFF8F1');
}

// El script del <head> ya aplicó el tema guardado; acá sólo sincronizamos el botón.
aplicarTema(document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light');

botonTema.addEventListener('click', () => {
  const nuevo = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
  aplicarTema(nuevo);
  try { localStorage.setItem(CLAVE_TEMA, nuevo); } catch { /* sin almacenamiento */ }
});

/* ---------- Encabezado y menú móvil ---------- */
const encabezado = document.getElementById('encabezado');
const botonMenu = document.getElementById('menu');
const nav = document.getElementById('nav');

const marcarScroll = () => encabezado.classList.toggle('con-sombra', window.scrollY > 8);
marcarScroll();
window.addEventListener('scroll', marcarScroll, { passive: true });

function cerrarMenu() {
  nav.classList.remove('abierto');
  botonMenu.setAttribute('aria-expanded', 'false');
  botonMenu.setAttribute('aria-label', 'Abrir menú');
}

botonMenu.addEventListener('click', () => {
  const abierto = nav.classList.toggle('abierto');
  botonMenu.setAttribute('aria-expanded', String(abierto));
  botonMenu.setAttribute('aria-label', abierto ? 'Cerrar menú' : 'Abrir menú');
});
nav.addEventListener('click', (e) => { if (e.target.closest('a')) cerrarMenu(); });
document.addEventListener('keydown', (e) => { if (e.key === 'Escape') cerrarMenu(); });
document.addEventListener('click', (e) => {
  if (nav.classList.contains('abierto') && !nav.contains(e.target) && !botonMenu.contains(e.target)) cerrarMenu();
});

/* ---------- Revelado al hacer scroll ---------- */
function observarRevelado(elementos) {
  if (!('IntersectionObserver' in window)) {
    elementos.forEach((el) => el.classList.add('visible'));
    return;
  }
  const observador = new IntersectionObserver((entradas) => {
    entradas.forEach((entrada) => {
      if (entrada.isIntersecting) {
        entrada.target.classList.add('visible');
        observador.unobserve(entrada.target);
      }
    });
  }, { threshold: 0.15, rootMargin: '0px 0px -40px 0px' });
  elementos.forEach((el) => observador.observe(el));
}
observarRevelado(document.querySelectorAll('.revelar'));

/* ---------- Armador de bloques ---------- */
const tablero = document.getElementById('tablero');
const contador = document.getElementById('contador');
const piezas = document.querySelectorAll('.pieza');

function actualizarContador() {
  const cantidad = tablero.querySelectorAll('.bloque:not(.saliendo)').length;
  contador.textContent = cantidad === 1 ? '1 bloque' : `${cantidad} bloques`;
  tablero.classList.toggle('con-bloques', cantidad > 0);
}

function crearBloque({ bloque, color, icono }) {
  const el = document.createElement('div');
  el.className = 'bloque';
  el.dataset.color = color;
  el.dataset.bloque = bloque;
  el.innerHTML = `
    <div class="bloque__cabeza"><span></span><span class="bloque__icono" aria-hidden="true"></span></div>
    <div class="bloque__campos" aria-hidden="true"><i></i><i></i><i></i></div>`;
  el.querySelector('.bloque__cabeza span').textContent = bloque;
  el.querySelector('.bloque__icono').textContent = icono;
  return el;
}

function quitarBloque(el) {
  el.classList.add('saliendo');
  actualizarContador();
  el.addEventListener('animationend', () => el.remove(), { once: true });
  // Por si la animación está desactivada (movimiento reducido)
  setTimeout(() => el.remove(), 400);
}

piezas.forEach((pieza) => {
  pieza.setAttribute('aria-pressed', 'false');
  pieza.addEventListener('click', () => {
    const activa = pieza.getAttribute('aria-pressed') === 'true';
    if (activa) {
      const existente = tablero.querySelector(`.bloque[data-bloque="${pieza.dataset.bloque}"]:not(.saliendo)`);
      if (existente) quitarBloque(existente);
    } else {
      tablero.appendChild(crearBloque(pieza.dataset));
    }
    pieza.setAttribute('aria-pressed', String(!activa));
    actualizarContador();
  });
});

document.getElementById('reiniciar').addEventListener('click', () => {
  tablero.querySelectorAll('.bloque:not(.saliendo)').forEach(quitarBloque);
  piezas.forEach((p) => p.setAttribute('aria-pressed', 'false'));
  actualizarContador();
});

// Arranca con un par de bloques cuando la sección entra en pantalla, para mostrar la idea
const seccionBloques = document.getElementById('bloques');
if ('IntersectionObserver' in window) {
  const demo = new IntersectionObserver(([entrada]) => {
    if (!entrada.isIntersecting) return;
    demo.disconnect();
    if (tablero.querySelector('.bloque')) return;
    ['Inventario', 'Pedidos', 'Clientes'].forEach((nombre, i) => {
      setTimeout(() => {
        const pieza = document.querySelector(`.pieza[data-bloque="${nombre}"]`);
        if (pieza && pieza.getAttribute('aria-pressed') === 'false') pieza.click();
      }, 350 + i * 280);
    });
  }, { threshold: 0.4 });
  demo.observe(seccionBloques);
}

/* ---------- Comentarios ---------- */
const formulario = document.getElementById('formulario');
const lista = document.getElementById('lista-comentarios');
const mensaje = document.getElementById('mensaje');
const COLORES_AVATAR = ['naranja', 'coral', 'arena', 'ambar', 'pizarra'];

function crearComentario({ nombre, negocio, puntaje, texto }, nuevo = false) {
  const articulo = document.createElement('article');
  articulo.className = 'comentario revelar visible' + (nuevo ? ' nuevo' : '');

  const bloques = document.createElement('div');
  bloques.className = 'puntaje';
  bloques.setAttribute('aria-label', `Puntaje: ${puntaje} de 5`);
  for (let i = 1; i <= 5; i++) {
    const b = document.createElement('i');
    if (i > puntaje) b.className = 'off';
    bloques.appendChild(b);
  }

  const cita = document.createElement('blockquote');
  cita.textContent = `“${texto}”`;

  const pie = document.createElement('footer');
  const avatar = document.createElement('span');
  const color = COLORES_AVATAR[(nombre.charCodeAt(0) || 0) % COLORES_AVATAR.length];
  avatar.className = `avatar avatar--${color}`;
  avatar.setAttribute('aria-hidden', 'true');
  avatar.textContent = nombre.charAt(0).toUpperCase();

  const datos = document.createElement('div');
  const b = document.createElement('b');
  b.textContent = nombre;
  const small = document.createElement('small');
  small.textContent = negocio || 'Comunidad Ensambla';
  datos.append(b, small);
  pie.append(avatar, datos);

  articulo.append(bloques, cita, pie);
  return articulo;
}

// Los comentarios se guardan en este navegador hasta que exista un endpoint en el backend.
const leidos = almacen.leer(CLAVE_COMENTARIOS, []);
const guardados = (Array.isArray(leidos) ? leidos : [])
  .filter((c) => c && typeof c.nombre === 'string' && c.nombre && typeof c.texto === 'string');
guardados.forEach((c) => lista.prepend(crearComentario(c)));

function marcarError(campo, hayError) {
  campo.setAttribute('aria-invalid', String(hayError));
}

formulario.addEventListener('submit', (e) => {
  e.preventDefault();
  const datos = new FormData(formulario);
  const nombre = String(datos.get('nombre') || '').trim();
  const negocio = String(datos.get('negocio') || '').trim();
  const texto = String(datos.get('comentario') || '').trim();
  const puntaje = Number(datos.get('puntaje')) || 5;

  marcarError(formulario.nombre, !nombre);
  marcarError(formulario.comentario, texto.length < 5);

  if (!nombre || texto.length < 5) {
    mensaje.className = 'formulario__mensaje error';
    mensaje.textContent = !nombre ? 'Contanos tu nombre.' : 'El comentario necesita al menos 5 caracteres.';
    (!nombre ? formulario.nombre : formulario.comentario).focus();
    return;
  }

  const comentario = { nombre, negocio, puntaje, texto };
  lista.prepend(crearComentario(comentario, true));
  guardados.push(comentario);
  almacen.guardar(CLAVE_COMENTARIOS, guardados.slice(-12));

  formulario.reset();
  mensaje.className = 'formulario__mensaje ok';
  mensaje.textContent = '¡Gracias! Tu comentario ya es parte del sistema.';
  lista.scrollIntoView({ behavior: 'smooth', block: 'start' });
});

formulario.addEventListener('input', (e) => {
  if (e.target.getAttribute('aria-invalid') === 'true') marcarError(e.target, false);
});

/* ---------- Año del pie ---------- */
document.getElementById('anio').textContent = new Date().getFullYear();
