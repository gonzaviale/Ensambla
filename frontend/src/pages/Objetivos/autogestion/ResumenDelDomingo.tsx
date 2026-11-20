import { CheckIcon, CircleDashedIcon, SparkleIcon } from '@phosphor-icons/react';
import { diaCorto, domingoDe } from '@/lib/objetivosAutogestion';
import { estaVacio, type ResumenDelDomingo as Resumen } from '@/lib/resumenDelDomingo';

const MOTIVO: Record<Resumen['noCumplido'][number]['motivo'], string> = {
  EN_PARTE: 'en parte',
  NO_HECHO: 'no se pudo',
  CAMBIADO: 'lo cambiamos',
  SIN_RESPUESTA: 'sin contar',
};

// Lo que cerró el domingo, en tres listas cortas: lo que se cumplió, lo que no y lo nuevo que propone Sami. Sin
// alarmas: lo que no se dio se cuenta como un dato, no como un error.
export function ResumenDelDomingo({ lunes, resumen }: { lunes: string; resumen: Resumen }) {
  if (estaVacio(resumen)) return null;
  return (
    <section className="resumen-domingo" aria-labelledby="resumen-domingo-titulo">
      <h3 id="resumen-domingo-titulo">Resumen del domingo {diaCorto(domingoDe(lunes))}</h3>
      <div className="resumen-domingo-columnas">
        {resumen.cumplido.length > 0 && (
          <div data-parte="cumplido">
            <h4>Lo que cumpliste</h4>
            <ul>
              {resumen.cumplido.map((titulo) => (
                <li key={titulo}>
                  <CheckIcon aria-hidden size={16} weight="bold" /> {titulo}
                </li>
              ))}
            </ul>
          </div>
        )}
        {resumen.noCumplido.length > 0 && (
          <div data-parte="no-cumplido">
            <h4>Lo que no se dio</h4>
            <ul>
              {resumen.noCumplido.map((item) => (
                <li key={`${item.motivo}-${item.titulo}`}>
                  <CircleDashedIcon aria-hidden size={16} /> {item.titulo}{' '}
                  <span className="resumen-domingo-motivo">· {MOTIVO[item.motivo]}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
        {resumen.nuevo.length > 0 && (
          <div data-parte="nuevo">
            <h4>Lo nuevo que propone Sami</h4>
            <ul>
              {resumen.nuevo.map((titulo) => (
                <li key={titulo}>
                  <SparkleIcon aria-hidden size={16} /> {titulo}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </section>
  );
}
