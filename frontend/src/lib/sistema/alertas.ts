import { allBlocks, normalize } from './core';
import type { Bloque, Sistema, Valor } from './tipos';
import type { ProductoDeZona, Zona } from './zona';

// Los avisos de Sami: reglas transparentes sobre los datos del comercio y sobre los datos agregados y
// anónimos de la zona. Cada aviso dice por qué aparece y de dónde sale cada número; nada se inventa ni
// se envía. El motor es puro: la interfaz y las acciones viven en otro lado.

export type TipoDeAviso =
  'stock' | 'precio_zona' | 'estancado' | 'muy_vendido' | 'combo' | 'consumo_zona';

export type GrupoDeAviso = 'stock' | 'ventas' | 'zona';

export const GRUPOS: Record<GrupoDeAviso, string> = {
  stock: 'Tu stock',
  ventas: 'Precios y ventas',
  zona: 'Tu zona',
};

const GRUPO_DE: Record<TipoDeAviso, GrupoDeAviso> = {
  stock: 'stock',
  precio_zona: 'ventas',
  estancado: 'ventas',
  muy_vendido: 'ventas',
  combo: 'ventas',
  consumo_zona: 'zona',
};

export type AccionDeAviso =
  /** Cambia valores de un registro (un precio, por ejemplo). */
  | {
      tipo: 'aplicar';
      texto: string;
      sistemaId: string;
      bloqueId: string;
      filaId: string;
      valores: Record<string, Valor>;
      confirmacion: string;
    }
  /** Suma un registro nuevo a un bloque (un combo, un producto de la zona). */
  | {
      tipo: 'crear';
      texto: string;
      sistemaId: string;
      bloqueId: string;
      valores: Record<string, Valor>;
      confirmacion: string;
    }
  /** Abre el formulario de un registro nuevo (por ejemplo, una entrada de mercadería). */
  | { tipo: 'registrar'; texto: string; sistemaId: string; bloqueId: string }
  | { tipo: 'abrir'; texto: string; sistemaId: string; bloqueId: string; filaId?: string }
  /** Lleva a «Mi zona», filtrando por un producto. */
  | { tipo: 'zona'; texto: string; producto?: string }
  /** Le pide al servidor que ejecute la acción del aviso y lo cierre, todo en una sola operación. */
  | { tipo: 'servidor'; texto: string; avisoId: string; confirmacion: string };

export type Aviso = {
  /** Depende del estado de los datos: si lo que lo disparó cambia, el id cambia y el aviso desaparece. */
  id: string;
  tipo: TipoDeAviso;
  grupo: GrupoDeAviso;
  /** 3 urgente, 2 para hoy, 1 para mirar. */
  prioridad: 1 | 2 | 3;
  sistemaId: string;
  sistema: string;
  titulo: string;
  detalle: string;
  porque: string[];
  acciones: AccionDeAviso[];
  /** Id del aviso en el servidor; falta en los que Sami calcula en el navegador. */
  servidorId?: string;
};

const ORDEN: TipoDeAviso[] = [
  'stock',
  'estancado',
  'precio_zona',
  'muy_vendido',
  'combo',
  'consumo_zona',
];

const VACIAS = new Set([
  'de',
  'del',
  'la',
  'el',
  'los',
  'las',
  'con',
  'sin',
  'por',
  'para',
  'kg',
  'gr',
  'ml',
  'lts',
  'unidad',
  'unidades',
  'pack',
  'docena',
]);

export const palabras = (texto: string) =>
  normalize(texto)
    .replace(/[^a-z0-9 ]+/g, ' ')
    .split(/\s+/)
    .filter((palabra) => palabra.length >= 3 && !VACIAS.has(palabra) && !/^\d/.test(palabra));

/** Qué tan parecido es un producto tuyo a otro: comparten la primera palabra y suman por cada palabra común. */
export function parecido(tuyo: string, otro: string) {
  const a = palabras(tuyo);
  const b = new Set(palabras(otro));
  if (!a.length || !b.has(a[0] ?? '')) return 0;
  return a.filter((palabra) => b.has(palabra)).length / a.length;
}

export const pesos = (valor: number) =>
  `$ ${Math.round(Number(valor) || 0).toLocaleString('es-AR')}`;
const porcentaje = (valor: number) => `${Math.round(valor * 100)}%`;
const aDiez = (valor: number) => Math.round(valor / 10) * 10;
const numero = (valor: Valor) => Number(valor ?? 0) || 0;

const DIA_MS = 86_400_000;
const diasEntre = (hoy: string, fecha: string) =>
  Math.floor((Date.parse(`${hoy}T12:00:00`) - Date.parse(`${fecha}T12:00:00`)) / DIA_MS);

const fechaCorta = (iso: string) => {
  const [, mes, dia] = iso.split('-');
  return `${Number(dia)}/${Number(mes)}`;
};

/** El producto de la zona que es el mismo que el tuyo (mismo nombre, salvo el tamaño o la marca extra). */
export const productoDeZona = (nombre: string, zona: Zona): ProductoDeZona | undefined =>
  zona.productos.find((producto) => parecido(nombre, producto.nombre) === 1);

type Rotacion = { u30: number; ultima?: string };

// Lo que se vendió de cada producto en los últimos 30 días, y cuándo fue la última venta.
function rotaciones(sistema: Sistema, hoy: string) {
  const porProducto = new Map<string, Rotacion>();
  let hayVentas = false;
  for (const bloque of allBlocks(sistema).filter((item) => item.type === 'sales')) {
    for (const venta of bloque.rows) {
      const id = String(venta.product ?? '');
      if (!id || !venta.date) continue;
      hayVentas = true;
      const fecha = String(venta.date);
      const dias = diasEntre(hoy, fecha);
      const actual = porProducto.get(id) ?? { u30: 0 };
      if (dias >= 0 && dias < 30) actual.u30 += numero(venta.quantity);
      if (!actual.ultima || fecha > actual.ultima) actual.ultima = fecha;
      porProducto.set(id, actual);
    }
  }
  return { porProducto, hayVentas };
}

const mediana = (valores: number[]) => {
  if (!valores.length) return 0;
  const ordenados = [...valores].sort((a, b) => a - b);
  const medio = Math.floor(ordenados.length / 2);
  return ordenados.length % 2
    ? (ordenados[medio] ?? 0)
    : ((ordenados[medio - 1] ?? 0) + (ordenados[medio] ?? 0)) / 2;
};

type Entrada = { sistemas?: Sistema[]; zona?: Zona; hoy: string; grupos?: Record<string, boolean> };

export function analizar({ sistemas = [], zona, hoy, grupos = {} }: Entrada): Aviso[] {
  const datosDeZona: Zona = zona ?? { nombre: '', comercios: 0, productos: [] };
  const salida: Aviso[] = [];
  const vistos = new Set<string>();
  const sumar = (aviso: Omit<Aviso, 'grupo'>) => {
    const grupo = GRUPO_DE[aviso.tipo];
    if (vistos.has(aviso.id) || grupos[grupo] === false) return;
    vistos.add(aviso.id);
    salida.push({ ...aviso, grupo });
  };

  for (const sistema of sistemas) {
    const bloques = allBlocks(sistema);
    const base = { sistemaId: sistema.id, sistema: sistema.name };
    const entradas = bloques.find((bloque) => bloque.type === 'entries');
    const { porProducto, hayVentas } = rotaciones(sistema, hoy);
    const inventarios = bloques.filter((bloque) => bloque.type === 'inventory');

    for (const inventario of inventarios) {
      const abrir = {
        tipo: 'abrir' as const,
        texto: 'Ver inventario',
        sistemaId: sistema.id,
        bloqueId: inventario.id,
      };
      for (const fila of inventario.rows) {
        const nombre = String(fila.name ?? '').trim();
        if (!nombre) continue;
        const cantidad = numero(fila.quantity);
        const minimo = numero(fila.minimum);
        const venta = numero(fila.price);
        const costo = numero(fila.cost);
        const rotacion = porProducto.get(fila.id) ?? { u30: 0 };
        // Qué tanto se vende «normalmente» entre los demás productos: se compara contra ellos, no consigo mismo.
        const tipica = mediana(
          [...porProducto.entries()]
            .filter(([productoId]) => productoId !== fila.id)
            .map(([, otra]) => otra.u30)
            .filter((u) => u > 0),
        );
        const delaZona = productoDeZona(nombre, datosDeZona);
        const referencia = delaZona
          ? `En tu zona, ${delaZona.comercios} comercios lo venden entre ${pesos(delaZona.precio.minimo)} y ${pesos(delaZona.precio.maximo)} (promedio ${pesos(delaZona.precio.promedio)}). Es un dato agregado: no se sabe quién cobra qué.`
          : '';
        const aplicarPrecio = (texto: string, precio: number) => ({
          tipo: 'aplicar' as const,
          texto,
          sistemaId: sistema.id,
          bloqueId: inventario.id,
          filaId: fila.id,
          valores: { price: precio },
          confirmacion: `${nombre} ahora está a ${pesos(precio)}.`,
        });

        // 1. Stock bajo o agotado.
        if (cantidad <= 0 || (minimo > 0 && cantidad <= minimo)) {
          const diario = rotacion.u30 / 30;
          const pedir =
            diario > 0
              ? Math.max(Math.ceil(diario * 14) - cantidad, 1)
              : Math.max((minimo > 0 ? minimo * 2 : 10) - cantidad, 1);
          const porque: string[] = [];
          if (rotacion.u30 > 0) {
            const alcanza = diario > 0 ? cantidad / diario : 0;
            porque.push(
              `En los últimos 30 días vendiste ${rotacion.u30} unidades (${diario.toFixed(1).replace('.', ',')} por día): ${cantidad <= 0 ? 'ya no te queda stock' : `te alcanza para ${alcanza < 1 ? 'menos de 1 día' : `${Math.floor(alcanza)} días`}`}.`,
            );
            porque.push(`Para cubrir las próximas 2 semanas, pedí ${pedir}.`);
          } else {
            porque.push(
              minimo > 0
                ? `Definiste un mínimo de ${minimo} y quedan ${cantidad}.`
                : 'No tiene un mínimo definido: te avisamos porque se agotó.',
            );
            porque.push(`Para no quedarte corto, pedí ${pedir}.`);
          }
          if (delaZona) {
            porque.push(
              `En tu zona lo venden ${delaZona.ventas.comercios} comercios, a unos ${pesos(delaZona.precio.promedio)} en promedio.`,
            );
          }
          sumar({
            ...base,
            id: `stock:${fila.id}:${cantidad}`,
            tipo: 'stock',
            prioridad: cantidad <= 0 ? 3 : 2,
            titulo:
              cantidad <= 0 ? `Te quedaste sin ${nombre}` : `${nombre} está por debajo del mínimo`,
            detalle: `${cantidad <= 0 ? 'No te quedan unidades' : `Te quedan ${cantidad}`}${minimo ? ` y tu mínimo es ${minimo}` : ''}. Para no quedarte corto, pedí ${pedir}.`,
            porque,
            acciones: [
              entradas
                ? {
                    tipo: 'registrar',
                    texto: 'Registrar entrada',
                    sistemaId: sistema.id,
                    bloqueId: entradas.id,
                  }
                : abrir,
            ],
          });
        }

        if (!delaZona || venta <= 0) continue;
        const promedio = delaZona.precio.promedio;
        const margenTexto = (sugerido: number) =>
          costo > 0
            ? `Hoy ganás ${pesos(venta - costo)} por unidad (${porcentaje((venta - costo) / venta)}); a ${pesos(sugerido)} ganarías ${pesos(sugerido - costo)}.`
            : `A ${pesos(sugerido)} ganarías ${pesos(sugerido - venta)} más por unidad.`;

        // 2. Mucha rotación con un precio por debajo del promedio de la zona.
        const muyVendido =
          rotacion.u30 >= 15 && rotacion.u30 >= tipica * 1.5 && venta < promedio * 0.95;
        if (muyVendido) {
          const sugerido = Math.max(aDiez(promedio * 0.98), aDiez(venta) + 10);
          sumar({
            ...base,
            id: `muy_vendido:${fila.id}:${venta}`,
            tipo: 'muy_vendido',
            prioridad: 1,
            titulo: `${nombre} se vende mucho y lo tenés por debajo de tu zona`,
            detalle: `Vendiste ${rotacion.u30} en 30 días a ${pesos(venta)}; el promedio de tu zona es ${pesos(promedio)}.`,
            porque: [
              referencia,
              `Con las mismas ventas, subirlo a ${pesos(sugerido)} te deja ${pesos((sugerido - venta) * rotacion.u30)} más por mes.`,
            ],
            acciones: [aplicarPrecio(`Subir a ${pesos(sugerido)}`, sugerido), abrir],
          });
        }

        // 3. Caro para la zona y sin ventas en 30 días.
        const estancado =
          hayVentas && cantidad > 0 && rotacion.u30 === 0 && venta > promedio * 1.05;
        if (estancado) {
          const sugerido = aDiez(promedio);
          sumar({
            ...base,
            id: `estancado:${fila.id}:${venta}`,
            tipo: 'estancado',
            prioridad: 2,
            titulo: `${nombre} está caro y no se mueve`,
            detalle: `Lo tenés a ${pesos(venta)}, ${porcentaje(venta / promedio - 1)} más que el promedio de tu zona, y no lo vendiste en 30 días.`,
            porque: [
              referencia,
              rotacion.ultima
                ? `La última venta que anotaste fue el ${fechaCorta(rotacion.ultima)}. Tenés ${cantidad} en stock.`
                : `Todavía no anotaste ventas de este producto. Tenés ${cantidad} en stock.`,
              `Rebajarlo a ${pesos(sugerido)} lo deja en el promedio de tu zona.`,
            ],
            acciones: [aplicarPrecio(`Rebajar a ${pesos(sugerido)}`, sugerido), abrir],
          });
        }

        // 4. Más barato que la zona: puede estar perdiendo margen. Más caro: bajarlo vende más.
        if (!muyVendido && venta < promedio * 0.92) {
          const sugerido = Math.max(
            aDiez(Math.min(promedio * 0.97, delaZona.precio.maximo)),
            aDiez(venta) + 10,
          );
          const alCosto = costo > 0 && venta <= costo * 1.1;
          sumar({
            ...base,
            id: `precio_zona:${fila.id}:${venta}`,
            tipo: 'precio_zona',
            prioridad: alCosto ? 2 : 1,
            titulo: alCosto
              ? `Vendés ${nombre} casi al costo`
              : `${nombre} está más barato que en tu zona`,
            detalle: `Lo tenés a ${pesos(venta)} y el promedio de tu zona es ${pesos(promedio)}. Puede que estés perdiendo margen.`,
            porque: [referencia, margenTexto(sugerido)],
            acciones: [aplicarPrecio(`Subir a ${pesos(sugerido)}`, sugerido), abrir],
          });
        } else if (!estancado && venta > promedio * 1.12) {
          const sugerido = aDiez(promedio * 1.03);
          sumar({
            ...base,
            id: `precio_zona:${fila.id}:${venta}`,
            tipo: 'precio_zona',
            prioridad: 1,
            titulo: `${nombre} está más caro que en tu zona`,
            detalle: `Lo tenés a ${pesos(venta)} y el promedio de tu zona es ${pesos(promedio)}. Bajarlo puede ayudarte a vender más.`,
            porque: [
              referencia,
              hayVentas
                ? `En los últimos 30 días vendiste ${rotacion.u30}.`
                : 'Todavía no hay ventas anotadas para comparar.',
              `A ${pesos(sugerido)} quedás apenas por encima del promedio.`,
            ],
            acciones: [aplicarPrecio(`Bajar a ${pesos(sugerido)}`, sugerido), abrir],
          });
        }
      }

      // 5. Dos productos de poca salida que se venden juntos: un combo.
      avisosDeCombo(sistema, inventario, porProducto, sumar);
    }

    // 6. Lo que se vende en la zona y todavía no está en el inventario.
    if (inventarios[0] && datosDeZona.comercios > 0) {
      const tuyos = inventarios.flatMap((inventario) =>
        inventario.rows.map((fila) => String(fila.name ?? '')),
      );
      const populares = datosDeZona.productos
        .filter((producto) => producto.ventas.comercios / datosDeZona.comercios >= 0.6)
        .filter((producto) => !tuyos.some((nombre) => parecido(nombre, producto.nombre) > 0))
        .sort(
          (a, b) =>
            b.ventas.comercios - a.ventas.comercios ||
            b.ventas.unidadesPorSemana - a.ventas.unidadesPorSemana,
        )
        .slice(0, 2);
      for (const producto of populares) {
        const precio = aDiez(producto.precio.promedio);
        sumar({
          ...base,
          id: `consumo_zona:${normalize(producto.nombre)}`,
          tipo: 'consumo_zona',
          prioridad: 1,
          titulo: `En tu zona se vende ${producto.nombre} y no lo tenés`,
          detalle: `${producto.ventas.comercios} de ${datosDeZona.comercios} comercios lo venden, a ${pesos(producto.precio.promedio)} en promedio. Cada uno vende unas ${producto.ventas.unidadesPorSemana} unidades por semana.`,
          porque: [
            'Es un dato agregado de varios comercios de tu zona: nadie queda identificado.',
            'Lo cruzamos con tu inventario y no encontramos nada parecido.',
          ],
          acciones: [
            {
              tipo: 'crear',
              texto: 'Sumar a mi inventario',
              sistemaId: sistema.id,
              bloqueId: inventarios[0].id,
              valores: { name: producto.nombre, quantity: 0, price: precio, cost: '', minimum: 5 },
              confirmacion: `Sumamos ${producto.nombre} a tu inventario, a ${pesos(precio)}.`,
            },
            { tipo: 'zona', texto: 'Ver en Mi zona', producto: producto.nombre },
          ],
        });
      }
    }
  }

  return salida.sort(
    (a, b) => b.prioridad - a.prioridad || ORDEN.indexOf(a.tipo) - ORDEN.indexOf(b.tipo),
  );
}

function avisosDeCombo(
  sistema: Sistema,
  inventario: Bloque,
  porProducto: Map<string, Rotacion>,
  sumar: (aviso: Omit<Aviso, 'grupo'>) => void,
) {
  const canastas = new Map<string, Set<string>>();
  for (const bloque of allBlocks(sistema).filter((item) => item.type === 'sales')) {
    for (const venta of bloque.rows) {
      // Una canasta es lo que un mismo cliente se llevó el mismo día.
      if (!venta.customer || !venta.date || !venta.product) continue;
      const clave = `${String(venta.date)}|${String(venta.customer)}`;
      const productos = canastas.get(clave) ?? new Set<string>();
      productos.add(String(venta.product));
      canastas.set(clave, productos);
    }
  }
  const juntas = new Map<string, number>();
  for (const productos of canastas.values()) {
    const lista = [...productos].filter((id) => (porProducto.get(id)?.u30 ?? 0) <= 8).sort();
    for (let i = 0; i < lista.length; i++) {
      for (let j = i + 1; j < lista.length; j++) {
        const clave = `${lista[i]}|${lista[j]}`;
        juntas.set(clave, (juntas.get(clave) ?? 0) + 1);
      }
    }
  }
  for (const [clave, veces] of juntas) {
    if (veces < 3) continue;
    const [idA, idB] = clave.split('|');
    const encontrados = inventario.rows.filter((fila) => fila.id === idA || fila.id === idB);
    // El orden lo da el inventario (no el id), así el aviso se lee siempre igual.
    const [a, b] = encontrados;
    if (!a || !b) continue;
    const nombreA = String(a.name);
    const nombreB = String(b.name);
    const yaExiste = inventario.rows.some(
      (fila) =>
        /combo/.test(normalize(fila.name)) &&
        parecido(nombreA, String(fila.name)) > 0 &&
        normalize(fila.name).includes(normalize(palabras(nombreB)[0] ?? '')),
    );
    if (yaExiste) continue;
    const precio = aDiez((numero(a.price) + numero(b.price)) * 0.9);
    const costo = numero(a.cost) + numero(b.cost);
    // Lo que se puede armar con lo que hay; si naciera en cero, el aviso de stock diría que "te quedaste sin" el combo.
    const armables = Math.max(0, Math.min(numero(a.quantity), numero(b.quantity)));
    sumar({
      id: `combo:${a.id}:${b.id}`,
      tipo: 'combo',
      prioridad: 1,
      sistemaId: sistema.id,
      sistema: sistema.name,
      titulo: `${nombreA} y ${nombreB} se venden juntos`,
      detalle: `Los dos salen poco por separado, pero ${veces} veces se los llevó el mismo cliente el mismo día.`,
      porque: [
        `Por separado suman ${pesos(numero(a.price) + numero(b.price))}; como combo, ${pesos(precio)} (10% menos) puede ayudar a que roten.`,
        'El combo se crea como un producto nuevo del inventario, con las unidades que podés armar hoy con tu stock.',
      ],
      acciones: [
        {
          tipo: 'crear',
          texto: 'Crear el combo',
          sistemaId: sistema.id,
          bloqueId: inventario.id,
          valores: {
            name: `Combo ${nombreA} + ${nombreB}`,
            quantity: armables,
            price: precio,
            cost: costo || '',
            minimum: '',
          },
          confirmacion: `Creamos el combo a ${pesos(precio)}.`,
        },
      ],
    });
  }
}

/* ---------- Preguntas a Sami ---------- */

export type Respuesta = {
  texto: string;
  avisos: Aviso[];
  /** Los precios de la zona que contestan la pregunta (agregados, nunca de un comercio puntual). */
  precios?: ProductoDeZona[];
  consulta?: string;
};

const PREGUNTA = new Set([
  'donde',
  'compro',
  'comprar',
  'compra',
  'mas',
  'barata',
  'barato',
  'baratos',
  'baratas',
  'precio',
  'precios',
  'conviene',
  'quien',
  'vende',
  'venden',
  'tiene',
  'tienen',
  'consigo',
  'cuesta',
  'sale',
  'cual',
  'que',
  'esta',
  'hay',
  'cerca',
  'mejor',
  'busco',
  'necesito',
  'cuanto',
  'cobro',
  'cobrar',
  'zona',
  'tengo',
  'estoy',
  'vendo',
  'vendes',
  'como',
  'cobran',
  'cobra',
  'cobras',
  'cuestan',
  'valen',
  'vale',
  'salen',
]);

export function responder(
  pregunta: string,
  { zona, avisos = [] }: { zona?: Zona; avisos?: Aviso[] },
): Respuesta {
  const texto = normalize(pregunta);
  const de = (...tipos: TipoDeAviso[]) => avisos.filter((aviso) => tipos.includes(aviso.tipo));
  const lista = (items: Aviso[], si: (n: number) => string, no: string): Respuesta => ({
    texto: items.length ? si(items.length) : no,
    avisos: items,
  });
  if (!texto) {
    return {
      texto: 'Escribime una pregunta corta, por ejemplo «¿qué tengo que reponer?».',
      avisos: [],
    };
  }
  if (/precio|cobro|cobrar|cuesta|cuanto|barat|car[oa]\b|conviene|vendo/.test(texto)) {
    const termino = palabras(texto).filter((palabra) => !PREGUNTA.has(palabra));
    if (termino.length) {
      const encontrados = (zona?.productos ?? [])
        .filter((producto) => {
          const propias = palabras(producto.nombre).join(' ');
          return termino.every((palabra) => propias.includes(palabra));
        })
        .sort((a, b) => b.ventas.comercios - a.ventas.comercios);
      const consulta = termino.join(' ');
      if (!encontrados.length) {
        return {
          texto: `No tengo datos de «${consulta}» en tu zona: hace falta que lo vendan al menos 3 comercios para poder mostrarlo.`,
          avisos: [],
          precios: [],
          consulta,
        };
      }
      const primero = encontrados[0];
      return {
        texto: `En tu zona, «${consulta}» va de ${pesos(primero?.precio.minimo ?? 0)} a ${pesos(primero?.precio.maximo ?? 0)}, con un promedio de ${pesos(primero?.precio.promedio ?? 0)} (${primero?.comercios} comercios).`,
        avisos: [],
        precios: encontrados.slice(0, 4),
        consulta,
      };
    }
    return lista(
      de('precio_zona', 'muy_vendido', 'estancado'),
      (n) =>
        `Encontré ${n === 1 ? '1 producto' : `${n} productos`} para revisar el precio frente a tu zona.`,
      'Tus precios están alineados con los de tu zona. No veo nada para ajustar.',
    );
  }
  if (/margen|gano|ganancia/.test(texto)) {
    return lista(
      de('precio_zona', 'muy_vendido'),
      (n) =>
        `Hay ${n === 1 ? '1 producto' : `${n} productos`} donde podés ganar más sin pasarte del precio de tu zona.`,
      'No veo productos con poco margen frente a lo que cobran en tu zona.',
    );
  }
  if (/repon|falta|stock|queda|agot|pedir/.test(texto)) {
    return lista(
      de('stock'),
      (n) => `Tenés ${n === 1 ? '1 producto' : `${n} productos`} para reponer.`,
      'Tu stock está por encima de los mínimos. No hace falta reponer nada por ahora.',
    );
  }
  if (/no se vende|estanc|parado|sin ventas|no sale|no rota/.test(texto)) {
    return lista(
      de('estancado'),
      (n) =>
        `${n === 1 ? 'Un producto' : `${n} productos`} no se mueve y está más caro que en tu zona.`,
      'No veo productos parados por precio.',
    );
  }
  if (/combo|junto|pack|promo/.test(texto)) {
    return lista(
      de('combo'),
      (n) => `Encontré ${n === 1 ? '1 combo posible' : `${n} combos posibles`}.`,
      'Todavía no veo productos que se vendan juntos con la frecuencia suficiente.',
    );
  }
  if (/vende|venden|cerca|zona|consumo|popular|nuevo|oferta/.test(texto)) {
    return lista(
      de('consumo_zona'),
      (n) =>
        `Hay ${n === 1 ? '1 producto' : `${n} productos`} que se venden bien cerca y vos no tenés.`,
      'No veo productos muy vendidos en tu zona que te falten.',
    );
  }
  if (/todo|resumen|como (estoy|voy|viene)|novedad|que hay|urgente/.test(texto)) {
    return lista(
      avisos.slice(0, 4),
      () => `Lo más importante ahora (${avisos.length} avisos en total):`,
      'Todo en orden por ahora. Sigo mirando.',
    );
  }
  return {
    texto:
      'Todavía no sé responder eso. Probá con «¿qué tengo que reponer?», «¿qué se vende cerca que no tengo?», «¿cuánto cobran la yerba en mi zona?» o «¿cómo vienen mis márgenes?».',
    avisos: [],
  };
}
