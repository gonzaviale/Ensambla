import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query';
import { useRouter } from '@tanstack/react-router';
import { cerrarSesion, iniciarSesion, registrarUsuario } from '@/api/auth';
import type { Login, Registro, Usuario } from '@/types/auth';
import { sesionKeys } from './queries';

// Todo lo que está cacheado pertenece a la persona anterior: se descarta salvo la sesión.
function descartarDatosPrivados(queryClient: QueryClient) {
  queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== 'auth' });
}

function abrirSesionDe(queryClient: QueryClient, usuario: Usuario) {
  descartarDatosPrivados(queryClient);
  queryClient.setQueryData(sesionKeys.actual, usuario);
}

export function useLogin() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (datos: Login) => iniciarSesion(datos),
    onSuccess: (usuario) => abrirSesionDe(queryClient, usuario),
  });
}

export function useRegistro() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (datos: Registro) => {
      await registrarUsuario(datos);
      return iniciarSesion({ email: datos.email, contrasenia: datos.contrasenia });
    },
    onSuccess: (usuario) => abrirSesionDe(queryClient, usuario),
  });
}

// Solo confirma la salida cuando el servidor invalidó la sesión.
export function useLogout() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: cerrarSesion,
    onSuccess: () => {
      queryClient.setQueryData(sesionKeys.actual, null);
    },
  });
}

// Para la UI: cierra la sesión, vuelve al login y recién ahí descarta los datos privados
// (si se descartaran antes, las pantallas privadas todavía montadas volverían a pedirlos).
export function useCerrarSesion() {
  const logout = useLogout();
  const queryClient = useQueryClient();
  const router = useRouter();

  const cerrar = async () => {
    await logout.mutateAsync();
    await router.navigate({ to: '/login', search: { redirect: undefined } });
    descartarDatosPrivados(queryClient);
  };

  return { cerrarSesion: cerrar, cerrando: logout.isPending, error: logout.error };
}
