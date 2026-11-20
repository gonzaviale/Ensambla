import { normalize } from '../core';
import type { Bloque, Sistema } from '../tipos';
import type { ProductoDeZona, Zona } from '../zona';
import { aDiez, parecido, pesos } from './medidas';
import type { AvisoSinGrupo } from './tipos';

const PARTE_DE_LA_ZONA_QUE_LO_VENDE = 0.6;
const MAXIMO_DE_SUGERENCIAS = 2;

/** 6. Lo que se vende en la zona y todavía no está en el inventario. */
export function avisosDeConsumoDeZona(
  sistema: Sistema,
  inventarios: Bloque[],
  zona: Zona,
): AvisoSinGrupo[] {
  const [inventario] = inventarios;
  if (!inventario || zona.comercios <= 0) return [];
  const tuyos = inventarios.flatMap((bloque) => bloque.rows.map((fila) => String(fila.name ?? '')));
  return popularesQueNoTenes(zona, tuyos).map((producto) =>
    avisoDeConsumo(sistema, inventario, zona, producto),
  );
}

const popularesQueNoTenes = (zona: Zona, tuyos: string[]) =>
  zona.productos
    .filter(
      (producto) => producto.ventas.comercios / zona.comercios >= PARTE_DE_LA_ZONA_QUE_LO_VENDE,
    )
    .filter((producto) => !tuyos.some((nombre) => parecido(nombre, producto.nombre) > 0))
    .sort(
      (a, b) =>
        b.ventas.comercios - a.ventas.comercios ||
        b.ventas.unidadesPorSemana - a.ventas.unidadesPorSemana,
    )
    .slice(0, MAXIMO_DE_SUGERENCIAS);

function avisoDeConsumo(
  sistema: Sistema,
  inventario: Bloque,
  zona: Zona,
  producto: ProductoDeZona,
): AvisoSinGrupo {
  const precio = aDiez(producto.precio.promedio);
  return {
    sistemaId: sistema.id,
    sistema: sistema.name,
    id: `consumo_zona:${normalize(producto.nombre)}`,
    tipo: 'consumo_zona',
    prioridad: 1,
    titulo: `En tu zona se vende ${producto.nombre} y no lo tenés`,
    detalle: `${producto.ventas.comercios} de ${zona.comercios} comercios lo venden, a ${pesos(producto.precio.promedio)} en promedio. Cada uno vende unas ${producto.ventas.unidadesPorSemana} unidades por semana.`,
    porque: [
      'No encontramos nada parecido en tu stock. Es un dato agregado: nadie queda identificado.',
    ],
    acciones: [
      {
        tipo: 'crear',
        texto: 'Sumar a mi stock',
        sistemaId: sistema.id,
        bloqueId: inventario.id,
        valores: { name: producto.nombre, quantity: 0, price: precio, cost: '', minimum: 5 },
        confirmacion: `Sumamos ${producto.nombre} a tu stock, a ${pesos(precio)}.`,
      },
      { tipo: 'zona', texto: 'Ver en Mi zona', producto: producto.nombre },
    ],
  };
}
