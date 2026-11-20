import { lazy, Suspense, type ComponentProps } from 'react';

const CodigoQr = lazy(() => import('./CodigoQr').then((modulo) => ({ default: modulo.CodigoQr })));

/** El generador de QR pesa bastante: se baja recién cuando hay que mostrar un QR. */
export function CodigoQrDiferido(props: ComponentProps<typeof CodigoQr>) {
  return (
    <Suspense fallback={<p role="status">Armando el QR…</p>}>
      <CodigoQr {...props} />
    </Suspense>
  );
}
