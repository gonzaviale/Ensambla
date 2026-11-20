import { CATALOG } from './catalogo';
import { esUnaSeccionObligatoria } from './estructuraFija';
import type { Bloque, TipoBloque } from './tipos';

// Lo que Sami (el backend) sabe de los componentes del editor. El catálogo vive en `catalogo.ts`: de ahí se exporta
// a `backend/src/main/resources/ia/catalogo-componentes.json` con `npm run catalogo:sami`, y un test falla si el
// archivo quedó viejo. Así hay una sola fuente: si el editor suma un bloque o un campo, Sami lo puede usar.

export type ComponenteParaSami = {
  tipo: TipoBloque;
  nombre: string;
  singular: string;
  proposito: string;
  obligatorioEnElNegocio: boolean;
  campos: {
    clave: string;
    etiqueta: string;
    tipo: string;
    obligatorio: boolean;
    opciones?: string[];
    fija: boolean;
  }[];
};

export type CatalogoParaSami = { aviso: string; componentes: ComponenteParaSami[] };

export const AVISO_CATALOGO_SAMI =
  'Generado desde frontend/src/lib/sistema/catalogo.ts con `npm run catalogo:sami`. No editar a mano.';

export function catalogoParaSami(): CatalogoParaSami {
  return {
    aviso: AVISO_CATALOGO_SAMI,
    componentes: (Object.keys(CATALOG) as TipoBloque[]).map((tipo) => {
      const definicion = CATALOG[tipo];
      return {
        tipo,
        nombre: definicion.name,
        singular: definicion.singular,
        proposito: definicion.purpose,
        obligatorioEnElNegocio: esUnaSeccionObligatoria({ type: tipo } as Bloque),
        campos: definicion.fields.map((campo) => ({
          clave: campo.key,
          etiqueta: campo.label,
          tipo: campo.type,
          obligatorio: campo.required,
          ...(campo.options && { opciones: campo.options }),
          fija: campo.system === true,
        })),
      };
    }),
  };
}

/** El JSON tal como se guarda en el backend. */
export const catalogoParaSamiComoTexto = () => `${JSON.stringify(catalogoParaSami(), null, 2)}\n`;
