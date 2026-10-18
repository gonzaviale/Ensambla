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
