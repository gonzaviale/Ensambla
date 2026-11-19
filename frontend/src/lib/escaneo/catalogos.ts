import type { ProductoDeCatalogo, ProductoSugerido, ResultadoCatalogos } from '@/types/escaneo';

const limpiar = (texto: string) => texto.trim().replace(/\s+/g, ' ');
const normalizar = (texto: string) =>
  limpiar(texto)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '');
const nombreUtil = (nombre: string) =>
  /\p{L}/u.test(nombre) &&
  !/^(producto|product|desconocido|unknown|sin nombre|n\/?a)$/i.test(nombre);

function descripcion(producto: ProductoDeCatalogo) {
  let texto = limpiar(producto.nombre);
  if (!nombreUtil(texto)) return '';
  // Una marca puede contener varias etiquetas separadas por coma; no repetimos las presentes.
  for (const parte of [...producto.marca.split(','), producto.presentacion]) {
    const dato = limpiar(parte);
    if (dato && !normalizar(texto).includes(normalizar(dato))) texto += ` · ${dato}`;
  }
  return texto.slice(0, 200);
}

/** El nombre más informativo gana; la foto puede provenir de otra fuente del mismo GTIN. */
export function sugerirProducto(resultado: ResultadoCatalogos): ProductoSugerido {
  const encontrados = resultado.fuentes.filter(
    (fuente) => fuente.estado === 'encontrado' && fuente.producto,
  );
  const opciones = encontrados
    .flatMap((fuente) => {
      if (!fuente.producto) return [];
      const nombre = descripcion(fuente.producto);
      const palabras = new Set(normalizarPalabras(fuente.producto.nombre));
      return nombre
        ? [
            {
              nombre,
              fuente: fuente.fuente,
              puntos: palabras.size * 10 + Math.min(nombre.length, 200) / 10,
            },
          ]
        : [];
    })
    .sort((a, b) => b.puntos - a.puntos);
  const mejor = opciones[0];
  const foto =
    encontrados.find((fuente) => fuente.fuente === mejor?.fuente && fuente.producto?.imagen) ??
    encontrados.find((fuente) => fuente.producto?.imagen);
  return {
    nombre: mejor?.nombre ?? '',
    fuenteDescripcion: mejor?.fuente,
    imagen: foto?.producto?.imagen,
    fuenteImagen: foto?.fuente,
  };
}

function normalizarPalabras(texto: string) {
  return (
    limpiar(texto)
      .toLocaleLowerCase('es')
      .match(/[\p{L}\p{N}]+/gu) ?? []
  );
}
