import type { TipoCampo } from '../tipos';
import type { ComportamientoDeCampo } from './ComportamientoDeCampo';
import { ComportamientoDeFecha } from './ComportamientoDeFecha';
import {
  ComportamientoDeEmail,
  ComportamientoDeHora,
  ComportamientoDeTexto,
  ComportamientoDeTextoLargo,
} from './ComportamientoDeTexto';
import {
  ComportamientoDeMoneda,
  ComportamientoDeSiNo,
  ComportamientoNumerico,
} from './ComportamientoNumerico';
import { ComportamientoDeRelacion, ComportamientoDeSeleccion } from './ComportamientosDeVinculo';

const texto = new ComportamientoDeTexto();
const numerico = new ComportamientoNumerico();

const REGISTRO: Record<TipoCampo, ComportamientoDeCampo> = {
  text: texto,
  textarea: new ComportamientoDeTextoLargo(),
  tel: texto,
  number: numerico,
  currency: new ComportamientoDeMoneda(),
  date: new ComportamientoDeFecha(),
  time: new ComportamientoDeHora(),
  email: new ComportamientoDeEmail(),
  select: new ComportamientoDeSeleccion(),
  checkbox: new ComportamientoDeSiNo(),
  relation: new ComportamientoDeRelacion(),
};

export const comportamientoDe = (tipo: TipoCampo): ComportamientoDeCampo => REGISTRO[tipo];

export { ComportamientoDeCampo } from './ComportamientoDeCampo';
