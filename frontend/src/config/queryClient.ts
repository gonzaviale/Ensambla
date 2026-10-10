import { QueryClient } from '@tanstack/react-query';
import { ApiError } from '@/api';

const MAX_REINTENTOS = 2;

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      retry: (intentos, error) =>
        !(error instanceof ApiError && error.isClientError) && intentos < MAX_REINTENTOS,
    },
  },
});
