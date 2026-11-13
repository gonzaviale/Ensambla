import { createFileRoute } from '@tanstack/react-router';
import { DondeAbrirPage } from '@/pages/DondeAbrir/DondeAbrirPage';

export const Route = createFileRoute('/_app/zona/donde-abrir')({ component: DondeAbrirPage });
