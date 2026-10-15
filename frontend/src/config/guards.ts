import type { QueryClient } from '@tanstack/react-query';
import { redirect } from '@tanstack/react-router';
import { sesionQuery } from '@/hooks/auth/queries';

type Contexto = {
  context: { queryClient: QueryClient };
  location: { href: string };
};

// Pantallas privadas: sin sesión se va al login y, al entrar, se vuelve a donde se quería ir (T-009).
export async function requerirUsuario({ context, location }: Contexto) {
  const usuario = await context.queryClient.fetchQuery(sesionQuery());
  if (!usuario) throw redirect({ to: '/login', search: { redirect: location.href } });
  return usuario;
}

// Pantallas públicas (landing, login, registro): con sesión no tienen sentido.
export async function redirigirSiHaySesion({ context }: Pick<Contexto, 'context'>) {
  const usuario = await context.queryClient.fetchQuery(sesionQuery());
  if (usuario) throw redirect({ to: '/hoy' });
}
