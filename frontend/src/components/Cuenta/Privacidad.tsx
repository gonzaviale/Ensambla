import { CheckCircleIcon, ProhibitIcon } from '@phosphor-icons/react';
import { Panel } from '@/components/ui';

// Qué se guarda y de quién son los datos, dicho en claro.
export function Privacidad() {
  return (
    <Panel aria-labelledby="cuenta-privacidad" className="flex flex-col gap-4">
      <h2 id="cuenta-privacidad" className="m-0 text-h3">
        Privacidad y datos
      </h2>
      <p className="m-0 text-body text-soft">
        Lo que cargás en Ensambla es de tu negocio. Sólo lo ves vos.
      </p>

      <div className="flex flex-col gap-2">
        <h3 className="m-0 flex items-center gap-2 text-body font-bold">
          <CheckCircleIcon aria-hidden size={24} weight="fill" className="shrink-0 text-ok" />
          Lo que guardamos
        </h3>
        <ul className="m-0 flex list-disc flex-col gap-1 pl-6 text-body text-soft">
          <li>Tu nombre y tu correo, para que puedas ingresar.</li>
          <li>Los bloques de tu sistema y los datos que sumás en cada uno.</li>
          <li>Las fotos del cuaderno, sólo para leerlas y traer tu historia.</li>
        </ul>
      </div>

      <div className="flex flex-col gap-2">
        <h3 className="m-0 flex items-center gap-2 text-body font-bold">
          <ProhibitIcon aria-hidden size={24} weight="fill" className="shrink-0 text-bad" />
          Lo que nunca hacemos
        </h3>
        <ul className="m-0 flex list-disc flex-col gap-1 pl-6 text-body text-soft">
          <li>Vender ni compartir tus datos con otros comercios.</li>
          <li>Mostrar tus ventas, tus deudas ni tus ganancias a nadie más.</li>
          <li>Cobrarte sin avisarte: el plan y los pagos de esta demostración son simulados.</li>
        </ul>
      </div>
    </Panel>
  );
}
