import { createFileRoute } from '@tanstack/react-router';
import { ProveedoresPage } from '@/pages/Proveedores/ProveedoresPage';

export const Route = createFileRoute('/_app/proveedores')({ component: ProveedoresPage });
