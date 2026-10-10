import type { Campo } from '@/types/proyecto';

export const camposFormulario: Campo[] = [
  {
    id: '00000000-0000-4000-8000-000000000001',
    nombre: 'Nombre',
    tipo: 'TEXTO',
    obligatorio: true,
    valorInicial: 'Producto',
    opciones: [],
    posicion: 0,
  },
  {
    id: '00000000-0000-4000-8000-000000000002',
    nombre: 'Precio',
    tipo: 'NUMERO',
    obligatorio: true,
    valorInicial: '0',
    opciones: [],
    posicion: 1,
  },
  {
    id: '00000000-0000-4000-8000-000000000003',
    nombre: 'Entrega',
    tipo: 'FECHA',
    obligatorio: false,
    valorInicial: '2026-10-10',
    opciones: [],
    posicion: 2,
  },
  {
    id: '00000000-0000-4000-8000-000000000004',
    nombre: 'Rubro',
    tipo: 'OPCION',
    obligatorio: false,
    valorInicial: 'Insumos',
    opciones: ['Insumos', 'Terminados'],
    posicion: 3,
  },
  {
    id: '00000000-0000-4000-8000-000000000005',
    nombre: 'Entregado',
    tipo: 'SI_NO',
    obligatorio: true,
    valorInicial: 'false',
    opciones: [],
    posicion: 4,
  },
];

export function campoPorTipo(tipo: Campo['tipo']): Campo {
  const campo = camposFormulario.find((item) => item.tipo === tipo);
  if (!campo) throw new Error(`Falta el campo de prueba ${tipo}`);
  return campo;
}
