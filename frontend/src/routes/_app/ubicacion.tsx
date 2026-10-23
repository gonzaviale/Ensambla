import { createFileRoute } from '@tanstack/react-router';
import { UbicacionPage } from '@/pages/Ubicacion/UbicacionPage';

export const Route = createFileRoute('/_app/ubicacion')({ component: UbicacionPage });
