import { createFileRoute, redirect } from '@tanstack/react-router';

export const Route = createFileRoute('/_app/donde-abrir')({
  beforeLoad: () => {
    throw redirect({ to: '/zona/donde-abrir', replace: true });
  },
});
