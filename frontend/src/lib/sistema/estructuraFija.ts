import { CATALOG } from './catalogo';
import type { Bloque, Campo, Sistema, TipoBloque } from './tipos';

// La estructura que Ensambla necesita para funcionar. La tabla completa y su porqué están en docs/estructura-fija.md;
// el backend la hace cumplir y esta pantalla evita intentar lo que el servidor rechazaría.
export const TIPOS_OBLIGATORIOS: readonly TipoBloque[] = ['inventory', 'sales', 'clients', 'expenses'];

const OPCIONES_QUE_NO_SE_QUITAN: Readonly<Record<string, readonly string[]>> = {
  saleMode: ['unidad', 'peso'],
  category: ['Mercadería'],
};

const NO_SE_PUEDE_TOCAR = 'Ensambla la necesita para funcionar.';

export const esUnaSeccionObligatoria = (bloque: Bloque) => TIPOS_OBLIGATORIOS.includes(bloque.type);

export const esColumnaFija = (campo: Campo) => campo.system === true;

export const tieneColumnasFijas = (bloque: Bloque) => bloque.fields.some(esColumnaFija);

export function sePuedeQuitarLaSeccion(sistema: Sistema, bloque: Bloque) {
  return !esUnaSeccionObligatoria(bloque) || hayOtraSeccionDelMismoTipo(sistema, bloque);
}

const hayOtraSeccionDelMismoTipo = (sistema: Sistema, bloque: Bloque) =>
  sistema.sections
    .flatMap((seccion) => seccion.blocks)
    .some((otro) => otro.id !== bloque.id && otro.type === bloque.type);

export const motivoParaNoQuitarLaSeccion = (bloque: Bloque) =>
  `No se puede quitar la sección «${bloque.name}»: Ensambla la necesita para funcionar.`;

export const motivoParaNoQuitarLaColumna = (campo: Campo) =>
  `No se puede quitar «${campo.label}»: ${NO_SE_PUEDE_TOCAR}`;

export const motivoParaNoCambiarLaColumna = (campo: Campo) =>
  `No se puede cambiar ni renombrar «${campo.label}»: ${NO_SE_PUEDE_TOCAR}`;

/** Lo que no se puede hacer con una columna fija al guardar sus cambios; nada si no es fija o el cambio es válido. */
export function problemaAlCambiarLaColumna(existente: Campo | undefined, cambio: Campo) {
  if (!existente || !esColumnaFija(existente)) return null;
  return cambiaLoQueNoDebe(existente, cambio) ? motivoParaNoCambiarLaColumna(existente) : null;
}

const cambiaLoQueNoDebe = (existente: Campo, cambio: Campo) =>
  existente.type !== cambio.type ||
  existente.label !== cambio.label ||
  (existente.required && !cambio.required) ||
  !conservaSusOpciones(existente, cambio);

const conservaSusOpciones = (existente: Campo, cambio: Campo) =>
  (OPCIONES_QUE_NO_SE_QUITAN[existente.key] ?? []).every((opcion) =>
    (cambio.options ?? []).includes(opcion),
  );

/** Las claves que una columna propia no puede usar en esta sección, porque son de sus columnas fijas. */
export const clavesReservadasDe = (bloque: Bloque) =>
  CATALOG[bloque.type].fields.filter(esColumnaFija).map((campo) => campo.key);
