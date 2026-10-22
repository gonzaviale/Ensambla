import type { ReactNode } from 'react';
import { Link } from '@tanstack/react-router';
import { PageHeading, Panel } from '@/components/ui';
import { cn } from '@/lib/cn';
import { ArrowRightIcon, CheckCircleIcon } from '@phosphor-icons/react';

type AuthCardProps = {
  title: string;
  lead: string;
  children: ReactNode;
  footer: ReactNode;
  /** Lo que dice Sami en el globito, asomada al costado de la tarjeta. */
  saludoSami: string;
  /** Sami se asoma por el borde izquierdo de la tarjeta en vez del derecho. */
  samiALaIzquierda?: boolean;
  /** Muestra el recuadro con el eslogan al lado del formulario. */
  conEslogan?: boolean;
};

export function AuthCard({
  title,
  lead,
  children,
  footer,
  saludoSami,
  samiALaIzquierda = false,
  conEslogan = false,
}: AuthCardProps) {
  return (
    <div
      className={cn(
        'mx-auto grid w-full max-w-6xl animate-fade-in gap-8 px-5 py-8 sm:px-8 sm:py-12 lg:py-16',
        conEslogan && 'lg:grid-cols-[1fr_1fr] lg:items-center lg:gap-16',
      )}
    >
      {conEslogan && (
        <aside className="relative order-2 overflow-hidden rounded-3xl border border-petrol/10 bg-sage p-6 sm:p-10 lg:order-none">
          <div aria-hidden className="mb-6 flex gap-2">
            <span className="size-3 rounded-full bg-blue" />
            <span className="size-3 rounded-full bg-yellow" />
            <span className="size-3 rounded-full bg-accent-500" />
          </div>
          <p className="mb-3 text-caption font-bold tracking-widest text-petrol uppercase">
            Un sistema a tu manera
          </p>
          <h2 className="m-0 max-w-sm text-[clamp(1.8rem,3vw,2.8rem)] leading-tight font-bold tracking-tight text-petrol">
            Tu negocio tiene su forma. Tu sistema también.
          </h2>
          <p className="mt-4 max-w-sm text-body leading-relaxed text-soft">
            Juntá tus pedidos, clientes y cuentas en un solo lugar. Empezá con lo que ya tenés y
            armalo a tu ritmo.
          </p>
          <div className="mt-6 flex items-center gap-2 text-hint font-semibold text-petrol">
            <CheckCircleIcon aria-hidden size={22} weight="fill" />
            Pequeñas piezas. Todo conectado.
          </div>
        </aside>
      )}
      <div
        className={cn(
          'order-1 mx-auto flex w-full max-w-md flex-col gap-6 lg:order-none',
          // Con el eslogan al lado queda poco margen: se le hace lugar a Sami hasta que la
          // pantalla es bien ancha.
          conEslogan && (samiALaIzquierda ? 'lg:pl-24 xl:pl-0' : 'lg:pr-24 xl:pr-0'),
        )}
      >
        <div className="flex items-center gap-2 text-caption font-semibold text-petrol">
          <span className="size-2 rounded-full bg-accent-500" aria-hidden />
          Tu espacio en Ensambla
          <ArrowRightIcon aria-hidden size={16} />
        </div>
        <div className="flex items-center gap-3">
          <div className="min-w-0 flex-1">
            <PageHeading title={title} lead={lead} />
          </div>
          {/* En celular Sami no entra al costado de la tarjeta: saluda al lado del título. */}
          <img src="/mockup/sami-saludando.webp" alt="" className="w-20 shrink-0 sm:hidden" />
        </div>
        <div className="relative">
          <Panel className="p-5 sm:p-7">{children}</Panel>
          {/* Sami agarrada de la esquina de la tarjeta, asomándose desde atrás. El globito se
              corre hacia afuera para no tapar el texto de arriba; en pantallas angostas no entra. */}
          <div
            aria-hidden
            className={cn(
              'pointer-events-none absolute top-0 z-10 hidden w-24 sm:block lg:w-28',
              samiALaIzquierda ? 'left-0 -translate-x-[87%]' : 'right-0 translate-x-[87%]',
            )}
          >
            <p
              className={cn(
                'absolute bottom-full m-0 mb-3 hidden w-max max-w-28 -translate-x-1/2 rounded-2xl bg-petrol px-3 py-2 text-center text-hint leading-snug font-bold text-white shadow-md motion-safe:animate-flotar md:block lg:max-w-32',
                samiALaIzquierda ? 'left-[20%]' : 'left-[70%]',
              )}
            >
              {saludoSami}
              <span
                className={cn(
                  'absolute top-full size-0 -translate-x-1/2 border-x-8 border-t-8 border-x-transparent border-t-petrol',
                  samiALaIzquierda ? 'left-[80%]' : 'left-[30%]',
                )}
              />
            </p>
            <img
              src="/mockup/sami-colgada.webp"
              alt=""
              className={cn('w-full', samiALaIzquierda && '-scale-x-100')}
            />
          </div>
        </div>
        <p className="m-0 text-center text-hint text-soft">{footer}</p>
      </div>
    </div>
  );
}

export function EnlaceAuth({ to, children }: { to: '/login' | '/registro'; children: ReactNode }) {
  return (
    <Link
      to={to}
      className="font-bold text-petrol underline decoration-petrol/25 underline-offset-4 hover:decoration-petrol"
    >
      {children}
    </Link>
  );
}
