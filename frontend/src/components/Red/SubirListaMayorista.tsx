import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { UploadSimpleIcon } from '@phosphor-icons/react';
import { Button, Panel, Toast, buttonClasses } from '@/components/ui';
import { useAccionesRed, useBarrio } from '@/hooks/red/useRed';
import { COMERCIOS_QUE_LE_COMPRAN, PROVEEDOR_DEMO_ID } from './datosDemo';

// Lado del mayorista, paso 1: sube su lista una vez y llega a todos los comercios que le compran.
export function SubirListaMayorista() {
  const barrio = useBarrio();
  const { subirLista } = useAccionesRed();
  const [subida, setSubida] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);

  const simular = () => {
    const listaId = subirLista(PROVEEDOR_DEMO_ID, 'excel');
    if (!listaId) return;
    setSubida(true);
    setAviso('Los comercios que te compran ya la tienen para revisar.');
  };

  return (
    <Panel aria-labelledby="mayorista-lista" className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <h2 id="mayorista-lista" className="m-0 text-h3">
          Subís tu lista una vez
        </h2>
        <p className="m-0 text-body text-soft">
          Por WhatsApp, PDF o Excel: Ensambla la lee, empareja tus productos y se la deja lista a
          cada comercio, con sus reglas de precio.
        </p>
      </div>

      <p className="m-0 text-lead">
        Tu lista llega a <strong>{COMERCIOS_QUE_LE_COMPRAN}</strong> de los{' '}
        <strong>{barrio.comercios}</strong> comercios de la red.
      </p>

      <div className="flex flex-wrap items-center gap-3">
        <Button variant="primary" onClick={simular}>
          <UploadSimpleIcon aria-hidden size={22} />
          Simular que subo una lista
        </Button>
        {subida && (
          <Link to="/listas" className={buttonClasses({ variant: 'ghost' })}>
            Ver cómo la recibe el comercio
          </Link>
        )}
      </div>

      {aviso && <Toast tipo="exito" mensaje={aviso} onClose={() => setAviso(null)} />}
    </Panel>
  );
}
