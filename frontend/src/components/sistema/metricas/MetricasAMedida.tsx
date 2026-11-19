import { useState, type FormEvent } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { AgregarMetrica } from '@/components/sistema/metricas/AgregarMetrica';
import { useMetricasAMedida } from '@/hooks/metricas/queries';
import { useMoverMetrica, useQuitarMetrica, useRenombrarMetrica } from '@/hooks/metricas/mutations';
import {
  formatoDelValor,
  resumenDeLaReceta,
  textoDeDondeSale,
} from '@/lib/metricas/metricasAMedida';
import type { Metrica, ResultadoDeMetrica } from '@/types/metricas';

// «Tus métricas»: las que la persona eligió para su cuaderno. Cada una muestra el número que calcula el servidor (o
// por qué no se pudo, nunca un cero inventado) y de dónde sale. Desde acá también se agregan, se renombran, se
// cambian de lugar y se quitan.
export function MetricasAMedida({ sistemaId, cuaderno }: { sistemaId: string; cuaderno: string }) {
  const [agregando, setAgregando] = useState(false);

  return (
    <section className="metricas-medida" aria-label="Métricas a medida">
      <header className="metricas-medida-cabecera">
        <div>
          <h2>Tus métricas</h2>
          <p>Lo que elegiste mirar de «{cuaderno}». Los números salen de lo que cargaste.</p>
        </div>
        {!agregando && (
          <button
            type="button"
            className="metricas-medida-agregar"
            onClick={() => setAgregando(true)}
          >
            <Icon name="plus" />
            Agregar métrica
          </button>
        )}
      </header>
      {agregando && <AgregarMetrica sistemaId={sistemaId} onCerrar={() => setAgregando(false)} />}
      <PanelDeMetricas sistemaId={sistemaId} />
    </section>
  );
}

function PanelDeMetricas({ sistemaId }: { sistemaId: string }) {
  const { metricas, fallo, reintentar } = useMetricasAMedida(sistemaId);
  if (fallo) return <AvisoDeFallo onReintentar={() => void reintentar()} />;
  if (!metricas)
    return (
      <p className="metricas-medida-aviso" role="status">
        Mirando tus métricas…
      </p>
    );
  if (metricas.length === 0)
    return (
      <p className="metricas-medida-aviso">
        Todavía no elegiste ninguna. Tocá «Agregar métrica» y probá con las que te sugerimos.
      </p>
    );
  return <ListaDeMetricas metricas={metricas} sistemaId={sistemaId} />;
}

function AvisoDeFallo({ onReintentar }: { onReintentar: () => void }) {
  return (
    <p className="metricas-medida-aviso" role="status">
      No pudimos traer tus métricas ahora.{' '}
      <button type="button" onClick={onReintentar}>
        Probar de nuevo
      </button>
    </p>
  );
}

function ListaDeMetricas({ metricas, sistemaId }: { metricas: Metrica[]; sistemaId: string }) {
  const ultimoLugar = metricas.length - 1;
  return (
    <ul className="metricas-medida-lista" aria-label="Métricas del panel">
      {metricas.map((metrica, lugar) => (
        <TarjetaDeMetrica
          key={metrica.id}
          metrica={metrica}
          sistemaId={sistemaId}
          lugar={lugar}
          ultimoLugar={ultimoLugar}
        />
      ))}
    </ul>
  );
}

type PropsDeTarjeta = { metrica: Metrica; sistemaId: string; lugar: number; ultimoLugar: number };

function TarjetaDeMetrica({ metrica, sistemaId, lugar, ultimoLugar }: PropsDeTarjeta) {
  const [renombrando, setRenombrando] = useState(false);
  const { definicion, resultado } = metrica;
  return (
    <li className="metrica-medida">
      {renombrando ? (
        <RenombrarLaMetrica
          metrica={metrica}
          sistemaId={sistemaId}
          onCerrar={() => setRenombrando(false)}
        />
      ) : (
        <TituloDeLaMetrica
          metrica={metrica}
          sistemaId={sistemaId}
          onRenombrar={() => setRenombrando(true)}
        />
      )}
      <p className="metrica-medida-receta">{resumenDeLaReceta(definicion)}</p>
      <ResultadoDeLaMetrica resultado={resultado} />
      <DeDondeSale resultado={resultado} />
      <LugarDeLaMetrica
        metrica={metrica}
        sistemaId={sistemaId}
        lugar={lugar}
        ultimoLugar={ultimoLugar}
      />
    </li>
  );
}

type PropsDelTitulo = { metrica: Metrica; sistemaId: string; onRenombrar: () => void };

function TituloDeLaMetrica({ metrica, sistemaId, onRenombrar }: PropsDelTitulo) {
  const quitar = useQuitarMetrica(sistemaId);
  const { nombre } = metrica.definicion;
  return (
    <>
      <div className="metrica-medida-titulo">
        <h3>{nombre}</h3>
        <div className="metrica-medida-acciones">
          <button
            type="button"
            className="metrica-medida-icono"
            aria-label={`Renombrar ${nombre}`}
            onClick={onRenombrar}
          >
            <Icon name="edit" />
          </button>
          <button
            type="button"
            className="metrica-medida-icono"
            aria-label={`Quitar ${nombre}`}
            disabled={quitar.isPending}
            onClick={() => quitar.mutate(metrica.id)}
          >
            <Icon name="trash" />
          </button>
        </div>
      </div>
      {quitar.isError && <ErrorDeLaTarjeta texto="No pudimos quitarla. Probá de nuevo." />}
    </>
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
    <form className="metrica-medida-renombrar" onSubmit={guardar} noValidate>
      <label htmlFor={`nombre-${metrica.id}`}>Nombre de la métrica</label>
      <input
        id={`nombre-${metrica.id}`}
        value={nombre}
        maxLength={80}
        autoFocus
        onChange={(evento) => setNombre(evento.target.value)}
      />
      {aviso && <ErrorDeLaTarjeta texto={aviso} />}
      <div className="metrica-medida-renombrar-botones">
        <button type="submit" className="metrica-medida-guardar" disabled={guardando}>
          Guardar nombre
        </button>
        <button type="button" className="metrica-medida-cancelar" onClick={onCerrar}>
          Cancelar
        </button>
      </div>
    </form>
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

function LugarDeLaMetrica({ metrica, sistemaId, lugar, ultimoLugar }: PropsDeTarjeta) {
  const mover = useMoverMetrica(sistemaId, metrica.id);
  const { nombre } = metrica.definicion;
  return (
    <div className="metrica-medida-lugar">
      <button
        type="button"
        aria-label={`Subir ${nombre}`}
        disabled={lugar === 0 || mover.isPending}
        onClick={() => mover.mutate(lugar - 1)}
      >
        <Icon name="up" />
        Subir
      </button>
      <button
        type="button"
        aria-label={`Bajar ${nombre}`}
        disabled={lugar === ultimoLugar || mover.isPending}
        onClick={() => mover.mutate(lugar + 1)}
      >
        <Icon name="down" />
        Bajar
      </button>
      {mover.isError && <ErrorDeLaTarjeta texto="No pudimos cambiarla de lugar. Probá de nuevo." />}
    </div>
  );
}

function DeDondeSale({ resultado }: { resultado: ResultadoDeMetrica }) {
  const [conDetalle, setConDetalle] = useState(false);
  return (
    <>
      <button
        type="button"
        className="metrica-medida-origen"
        aria-expanded={conDetalle}
        onClick={() => setConDetalle(!conDetalle)}
      >
        ¿De dónde sale?
      </button>
      {conDetalle && <p className="metrica-medida-detalle">{textoDeDondeSale(resultado)}</p>}
    </>
  );
}

function ErrorDeLaTarjeta({ texto }: { texto: string }) {
  return (
    <p className="metrica-medida-error" role="alert">
      {texto}
    </p>
  );
}

/** El número grande y, si la métrica se reparte, las barras; si no se pudo calcular, el motivo en palabras. */
export function ResultadoDeLaMetrica({ resultado }: { resultado: ResultadoDeMetrica }) {
  if (resultado.valor == null)
    return (
      <p className="metrica-medida-motivo" role="status">
        {resultado.motivo ?? 'Todavía no se puede calcular.'}
      </p>
    );
  return (
    <>
      <p className="metrica-medida-valor">{formatoDelValor(resultado.valor, resultado.formato)}</p>
      {resultado.filas.length > 0 && <RepartoDeLaMetrica resultado={resultado} />}
    </>
  );
}

function RepartoDeLaMetrica({ resultado }: { resultado: ResultadoDeMetrica }) {
  const maximo = Math.max(...resultado.filas.map((fila) => fila.valor), 0);
  return (
    <ul className="metrica-medida-reparto">
      {resultado.filas.map((fila) => (
        <li key={fila.etiqueta}>
          <span className="metrica-medida-etiqueta">{fila.etiqueta}</span>
          <span className="metrica-medida-barra" aria-hidden="true">
            <span style={{ width: anchoDeLaBarra(fila.valor, maximo) }} />
          </span>
          <span className="metrica-medida-numero">
            {formatoDelValor(fila.valor, resultado.formato)}
          </span>
        </li>
      ))}
    </ul>
  );
}

const anchoDeLaBarra = (valor: number, maximo: number) =>
  `${maximo > 0 ? Math.max(4, (valor / maximo) * 100) : 0}%`;
