import { createFileRoute } from '@tanstack/react-router';
import { PlantillasPage } from '@/pages/Plantillas/PlantillasPage';

export const Route = createFileRoute('/_app/plantillas')({
  component: PlantillasPage,
});
