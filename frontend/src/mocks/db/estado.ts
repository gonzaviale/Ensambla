import type { Plantilla } from '@/types/plantilla';

export type UsuarioMock = {
  id: string;
  nombre: string;
  email: string;
  contrasenia: string;
};

type Estado = {
  usuarios: Map<string, UsuarioMock>;
  sesionUsuarioId: string | null;
};

export const estado: Estado = {
  usuarios: new Map(),
  sesionUsuarioId: null,
};

export function reiniciarEstado() {
  estado.usuarios = new Map();
  estado.sesionUsuarioId = null;
}

export function sembrarUsuario(datos: Partial<UsuarioMock> & { conSesion?: boolean } = {}) {
  const { conSesion, ...resto } = datos;
  const usuario: UsuarioMock = {
    id: crypto.randomUUID(),
    nombre: 'Lucía Gómez',
    email: 'lucia@example.com',
    contrasenia: 'una-contrasenia-segura',
    ...resto,
  };
  estado.usuarios.set(usuario.email, usuario);
  if (conSesion) estado.sesionUsuarioId = usuario.id;
  return usuario;
}

// Mismas plantillas que sembrará T-013 (Inventario, Pedidos, Clientes), para trabajar sin backend.
export const plantillasMock: Plantilla[] = [
  {
    id: '00000000-0000-4000-8000-0000000000a1',
    nombre: 'Inventario',
    descripcion: 'Controlá tus productos, su stock y el costo de cada uno.',
    icono: 'box',
    estructura: {
      bloques: [
        {
          nombre: 'Productos',
          campos: [
            { nombre: 'Nombre', tipo: 'TEXTO', obligatorio: true },
            { nombre: 'Stock', tipo: 'NUMERO', obligatorio: true, valorInicial: '0' },
            { nombre: 'Rubro', tipo: 'OPCION', opciones: ['Insumos', 'Terminados'] },
          ],
        },
      ],
    },
  },
  {
    id: '00000000-0000-4000-8000-0000000000a2',
    nombre: 'Pedidos',
    descripcion: 'Anotá los pedidos de tus clientes y seguí su estado.',
    icono: 'list-checks',
    estructura: {
      bloques: [
        {
          nombre: 'Pedidos',
          campos: [
            { nombre: 'Cliente', tipo: 'TEXTO', obligatorio: true },
            { nombre: 'Entrega', tipo: 'FECHA' },
            { nombre: 'Entregado', tipo: 'SI_NO', valorInicial: 'false' },
          ],
        },
      ],
    },
  },
  {
    id: '00000000-0000-4000-8000-0000000000a3',
    nombre: 'Clientes',
    descripcion: 'Tu agenda de clientes con sus datos de contacto.',
    icono: 'users',
    estructura: {
      bloques: [
        {
          nombre: 'Clientes',
          campos: [
            { nombre: 'Nombre', tipo: 'TEXTO', obligatorio: true },
            { nombre: 'Teléfono', tipo: 'TEXTO' },
          ],
        },
      ],
    },
  },
];
