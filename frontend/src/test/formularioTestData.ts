import type { Campo } from '@/types/proyecto';

export const camposFormulario: Campo[] = [
  {
    id: '00000000-0000-4000-8000-000000000001',
    nombre: 'Nombre',
    tipo: 'TEXTO',
    obligatorio: true,
    valorInicial: 'Producto',
    opciones: [],
    bloqueRelacionadoId: null,
    posicion: 0,
  },
  {
    id: '00000000-0000-4000-8000-000000000002',
    nombre: 'Precio',
    tipo: 'NUMERO',
    obligatorio: true,
    valorInicial: '0',
    opciones: [],
    bloqueRelacionadoId: null,
    posicion: 1,
  },
  {
    id: '00000000-0000-4000-8000-000000000003',
    nombre: 'Entrega',
    tipo: 'FECHA',
    obligatorio: false,
    valorInicial: '2026-10-10',
    opciones: [],
    bloqueRelacionadoId: null,
    posicion: 2,
  },
  {
    id: '00000000-0000-4000-8000-000000000004',
    nombre: 'Rubro',
    tipo: 'OPCION',
    obligatorio: false,
    valorInicial: 'Insumos',
    opciones: ['Insumos', 'Terminados'],
    bloqueRelacionadoId: null,
    posicion: 3,
  },
  {
    id: '00000000-0000-4000-8000-000000000005',
    nombre: 'Entregado',
    tipo: 'SI_NO',
    obligatorio: true,
    valorInicial: 'false',
    opciones: [],
    bloqueRelacionadoId: null,
    posicion: 4,
  },
];

// Fuera de `camposFormulario`: su selector pide filas a la API y esos tests no la simulan.
export const campoRelacion: Campo = {
  id: '00000000-0000-4000-8000-000000000006',
  nombre: 'Producto',
  tipo: 'RELACION',
  obligatorio: false,
  valorInicial: null,
  opciones: [],
  bloqueRelacionadoId: '00000000-0000-4000-8000-0000000000b1',
  posicion: 5,
};

export function campoPorTipo(tipo: Campo['tipo']): Campo {
  const campo = [...camposFormulario, campoRelacion].find((item) => item.tipo === tipo);
  if (!campo) throw new Error(`Falta el campo de prueba ${tipo}`);
  return campo;
}
