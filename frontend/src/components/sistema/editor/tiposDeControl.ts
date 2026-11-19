import type { Sistema, Campo, Valor } from '@/lib/sistema/tipos';

export type ControlProps = {
  sistema: Sistema | undefined;
  campo: Campo;
  valor: Valor;
  id: string;
  name?: string;
  /** Con `onCambio` el control es controlado; sin él, el formulario lee los valores al enviarse. */
  onCambio?: (valor: string | boolean) => void;
  etiqueta?: string;
  invalido?: boolean;
};
