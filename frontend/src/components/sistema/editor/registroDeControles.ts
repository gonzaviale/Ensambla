import type { ComponentType } from 'react';
import type { TipoCampo } from '@/lib/sistema/tipos';
import {
  ControlDeCasilla,
  ControlDeCorreo,
  ControlDeFecha,
  ControlDeHora,
  ControlDeSeleccion,
  ControlDeTelefono,
  ControlDeTexto,
  ControlDeTextoLargo,
  ControlDeVinculo,
  ControlNumerico,
} from './ControlesDeEntrada';
import type { ControlProps } from './tiposDeControl';

// El control de cada tipo de campo: agregar un tipo sin su control no compila.
const CONTROLES: Record<TipoCampo, ComponentType<ControlProps>> = {
  text: ControlDeTexto,
  textarea: ControlDeTextoLargo,
  number: ControlNumerico,
  currency: ControlNumerico,
  date: ControlDeFecha,
  time: ControlDeHora,
  email: ControlDeCorreo,
  tel: ControlDeTelefono,
  select: ControlDeSeleccion,
  checkbox: ControlDeCasilla,
  relation: ControlDeVinculo,
};

export const controlDe = (tipo: TipoCampo): ComponentType<ControlProps> => CONTROLES[tipo];
