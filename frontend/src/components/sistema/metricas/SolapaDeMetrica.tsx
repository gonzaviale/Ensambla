import { useState, type FormEvent } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { GraficoDeLaMetrica } from '@/components/sistema/metricas/GraficoDeLaMetrica';
import { NombreDeLaMetrica } from '@/components/sistema/metricas/NombreDeLaMetrica';
import { MenuDeLaMetrica } from '@/components/sistema/metricas/MenuDeLaMetrica';
import { SelectorDeGrafico } from '@/components/sistema/metricas/SelectorDeGrafico';
import { VentanaFlotante } from '@/components/sistema/metricas/VentanaFlotante';
import { useRenombrarMetrica } from '@/hooks/metricas/mutations';
import { useGraficoDeLaMetrica } from '@/hooks/metricas/useGraficoDeLaMetrica';
import { resumenDeLaReceta, textoDeDondeSale } from '@/lib/metricas/metricasAMedida';
import type { Metrica, ResultadoDeMetrica } from '@/types/metricas';

type Props = {
  metrica: Metrica;
  sistemaId: string;
  lugar: number;
  ultimoLugar: number;
  color: string;
  onQuitada: () => void;
};

// Lo que muestra la solapa de una métrica: su nombre, qué calcula, el número que da hoy con el gráfico que eligió la
// persona (o por qué no se pudo, nunca un cero inventado) y de dónde sale. El menú permite renombrarla, cambiarla de
// lugar o quitarla.
export function SolapaDeMetrica(props: Props) {
  const [renombrando, setRenombrando] = useState(false);
  const { metrica, sistemaId, color } = props;
  const { grafico, posibles, elegir, fallo } = useGraficoDeLaMetrica(metrica, sistemaId);
  return (
    <article className="panel-negocio-metrica" data-color={color}>
      <header className="panel-negocio-metrica-cabecera">
        <TituloDeLaMetrica metrica={metrica} />
        <div className="panel-negocio-metrica-herramientas">
          <SelectorDeGrafico grafico={grafico} posibles={posibles} onElegir={elegir} />
          <DeDondeSale resultado={metrica.resultado} />
          <MenuDeLaMetrica {...props} onRenombrar={() => setRenombrando(true)} />
        </div>
      </header>
      {renombrando && (
        <RenombrarLaMetrica
          metrica={metrica}
          sistemaId={sistemaId}
          onCerrar={() => setRenombrando(false)}
        />
      )}
      {fallo && (
        <p className="metrica-medida-error" role="alert">
          No pudimos cambiar el gráfico. Probá de nuevo.
        </p>
      )}
      <GraficoDeLaMetrica
        definicion={metrica.definicion}
        resultado={metrica.resultado}
        grafico={grafico}
      />
    </article>
  );
}

function TituloDeLaMetrica({ metrica }: { metrica: Metrica }) {
  return (
    <div>
      <NombreDeLaMetrica nombre={metrica.definicion.nombre} resultado={metrica.resultado} />
      <p className="metrica-medida-receta">{resumenDeLaReceta(metrica.definicion)}</p>
    </div>
  );
}

type PropsDeRenombrar = { metrica: Metrica; sistemaId: string; onCerrar: () => void };

function RenombrarLaMetrica({ metrica, sistemaId, onCerrar }: PropsDeRenombrar) {
  const { nombre, setNombre, aviso, guardando, guardar } = useNombreNuevo(
    metrica,
    sistemaId,
    onCerrar,
  );
  return (
    <VentanaFlotante titulo="Cambiar el nombre" tamano="chica" onCerrar={onCerrar}>
      <form className="metrica-medida-renombrar" onSubmit={guardar} noValidate>
        <label htmlFor={`nombre-${metrica.id}`}>Nombre de la métrica</label>
        <input
          id={`nombre-${metrica.id}`}
          value={nombre}
          maxLength={80}
          autoFocus
          onChange={(evento) => setNombre(evento.target.value)}
        />
        {aviso && (
          <p className="metrica-medida-error" role="alert">
            {aviso}
          </p>
        )}
        <div className="metrica-medida-renombrar-botones">
          <button type="submit" className="metrica-medida-guardar" disabled={guardando}>
            Guardar nombre
          </button>
          <button type="button" className="metrica-medida-cancelar" onClick={onCerrar}>
            Cancelar
          </button>
        </div>
      </form>
    </VentanaFlotante>
  );
}

/** El nombre que la persona está escribiendo y lo que pasa al guardarlo: nunca se guarda vacío. */
function useNombreNuevo(metrica: Metrica, sistemaId: string, onCerrar: () => void) {
  const renombrar = useRenombrarMetrica(sistemaId, metrica.id);
  const [nombre, setNombre] = useState(metrica.definicion.nombre);
  const [faltaElNombre, setFaltaElNombre] = useState(false);
  const guardar = (evento: FormEvent) => {
    evento.preventDefault();
    const limpio = nombre.trim();
    setFaltaElNombre(limpio === '');
    if (limpio !== '') renombrar.mutate(limpio, { onSuccess: onCerrar });
  };
  const aviso = avisoDelNombre(faltaElNombre, renombrar.isError);
  return { nombre, setNombre, aviso, guardando: renombrar.isPending, guardar };
}

function avisoDelNombre(faltaElNombre: boolean, fallo: boolean) {
  if (faltaElNombre) return 'Ponele un nombre.';
  if (fallo) return 'No pudimos cambiarle el nombre.';
  return null;
}

function DeDondeSale({ resultado }: { resultado: ResultadoDeMetrica }) {
  const [abierta, setAbierta] = useState(false);
  return (
    <div className="panel-negocio-metrica-origen">
      <button
        type="button"
        className="metrica-medida-origen"
        aria-haspopup="dialog"
        aria-label="¿De dónde sale?"
        title="¿De dónde sale?"
        onClick={() => setAbierta(true)}
      >
        <Icon name="help" />
      </button>
      {abierta && (
        <VentanaFlotante titulo="¿De dónde sale?" tamano="chica" onCerrar={() => setAbierta(false)}>
          <p className="metrica-medida-detalle">{textoDeDondeSale(resultado)}</p>
        </VentanaFlotante>
      )}
    </div>
  );
}
