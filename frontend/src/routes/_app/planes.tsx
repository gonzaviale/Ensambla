import { createFileRoute } from '@tanstack/react-router';
import { PlanesPage } from '@/pages/Planes/PlanesPage';

export const Route = createFileRoute('/_app/planes')({ component: PlanesPage });
