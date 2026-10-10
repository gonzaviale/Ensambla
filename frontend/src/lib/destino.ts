export const DESTINO_POR_DEFECTO = '/proyectos';

// El destino post-login viaja en la URL: solo se acepta una ruta interna (evita redirecciones abiertas).
export function destinoSeguro(destino: string | undefined): string {
  if (!destino) return DESTINO_POR_DEFECTO;
  if (!destino.startsWith('/') || destino.startsWith('//')) return DESTINO_POR_DEFECTO;
  return destino;
}
