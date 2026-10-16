import { createFileRoute } from '@tanstack/react-router';
import { SubirPage } from '@/pages/Subir/SubirPage';

export const Route = createFileRoute('/_app/subir')({
  component: SubirPage,
});
