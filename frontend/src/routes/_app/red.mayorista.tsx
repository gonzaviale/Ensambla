import { createFileRoute } from '@tanstack/react-router';
import { MayoristaPage } from '@/pages/Red/MayoristaPage';

export const Route = createFileRoute('/_app/red/mayorista')({
  component: MayoristaPage,
});
