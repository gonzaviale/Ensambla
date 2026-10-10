import type { QueryClient } from '@tanstack/react-query';
import { createRouter, type RouterHistory } from '@tanstack/react-router';
import { routeTree } from '@/routeTree.gen';
import { PageSpinner } from '@/components/ui';
import { queryClient } from './queryClient';
import { RouteError, RouteNotFound } from '@/components/ui/RouteStates';

export function createAppRouter(client: QueryClient, history?: RouterHistory) {
  return createRouter({
    routeTree,
    history,
    context: { queryClient: client },
    defaultPreload: 'intent',
    defaultPreloadStaleTime: 0,
    defaultPendingComponent: PageSpinner,
    defaultErrorComponent: RouteError,
    defaultNotFoundComponent: RouteNotFound,
    scrollRestoration: true,
  });
}

export const router = createAppRouter(queryClient);

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}
