import { createFileRoute } from '@tanstack/react-router';
import { PedidosPage } from '@/pages/Pedidos/PedidosPage';

export const Route = createFileRoute('/_app/pedidos')({
  component: PedidosPage,
});
