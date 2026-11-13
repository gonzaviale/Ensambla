import { createFileRoute, redirect } from '@tanstack/react-router';

// «Mis cuadernos» ya no existe: el Inicio muestra el negocio y sus secciones. Un enlace viejo llega al Inicio.
export const Route = createFileRoute('/_app/cuadernos')({
  beforeLoad: () => {
    throw redirect({ to: '/inicio', replace: true });
  },
});
