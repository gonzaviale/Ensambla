import { useState, type FormEvent } from 'react';
import { MagnifyingGlassIcon } from '@phosphor-icons/react';
import { Button, InputField, SelectField } from '@/components/ui';
import type { Bloque } from '@/types/proyecto';
import type { FiltroReporte } from '@/types/reporte';

type Props = {
  bloque: Bloque;
  /** Los filtros que se están viendo en pantalla. */
  aplicado: FiltroReporte;
  onAplicar: (filtro: FiltroReporte) => void;
};

type Borrador = {
  q: string;
  desde: string;
  hasta: string;
  campoId: string;
  valor: string;
};

const VACIO: Borrador = { q: '', desde: '', hasta: '', campoId: '', valor: '' };

const borradorDe = (filtro: FiltroReporte): Borrador => ({
  q: filtro.q ?? '',
  desde: filtro.desde ?? '',
  hasta: filtro.hasta ?? '',
  campoId: filtro.campoId ?? '',
  valor: filtro.valor ?? '',
});

function filtroDe(borrador: Borrador): FiltroReporte {
  const filtro: FiltroReporte = {};
  if (borrador.q.trim()) filtro.q = borrador.q.trim();
  if (borrador.desde) filtro.desde = borrador.desde;
  if (borrador.hasta) filtro.hasta = borrador.hasta;
  if (borrador.campoId && borrador.valor) {
    filtro.campoId = borrador.campoId;
    filtro.valor = borrador.valor;
  }
  return filtro;
}

const RESPUESTAS_SI_NO = [
  { valor: 'true', texto: 'Sí' },
  { valor: 'false', texto: 'No' },
];

// Filtros del reporte: texto, rango de fechas de carga y el valor de una columna de opción o de sí/no.
// Nada cambia en el reporte hasta tocar «Ver reporte»: lo que se ve y lo que se descarga son siempre lo mismo.
export function FiltrosReporte({ bloque, aplicado, onAplicar }: Props) {
  const [borrador, setBorrador] = useState(() => borradorDe(aplicado));
  const [errorFechas, setErrorFechas] = useState<string>();

  const columnas = [...bloque.campos]
    .filter((campo) => campo.tipo === 'OPCION' || campo.tipo === 'SI_NO')
    .sort((a, b) => a.posicion - b.posicion);
  const columna = columnas.find((campo) => campo.id === borrador.campoId);
  const valores = columna
    ? columna.tipo === 'SI_NO'
      ? RESPUESTAS_SI_NO
      : columna.opciones.map((opcion) => ({ valor: opcion, texto: opcion }))
    : [];

  const cambiar = (cambios: Partial<Borrador>) => {
    setBorrador((actual) => ({ ...actual, ...cambios }));
    setErrorFechas(undefined);
  };

  function enviar(event: FormEvent) {
    event.preventDefault();
    if (borrador.desde && borrador.hasta && borrador.desde > borrador.hasta) {
      setErrorFechas('La fecha desde no puede ser posterior a la fecha hasta.');
      return;
    }
    onAplicar(filtroDe(borrador));
  }

  function limpiar() {
    setBorrador(VACIO);
    setErrorFechas(undefined);
    onAplicar({});
  }

  return (
    <form
      onSubmit={enviar}
      noValidate
      aria-label="Filtros del reporte"
      className="flex flex-col gap-4"
    >
      <div className="grid gap-4 sm:grid-cols-3">
        <InputField
          label={`Buscar en ${bloque.nombre}`}
          type="search"
          placeholder="Escribí para filtrar"
          value={borrador.q}
          onChange={(event) => cambiar({ q: event.target.value })}
          trailing={<MagnifyingGlassIcon aria-hidden size={22} className="mr-3 text-muted" />}
        />
        <InputField
          label="Cargado desde"
          type="date"
          value={borrador.desde}
          onChange={(event) => cambiar({ desde: event.target.value })}
        />
        <InputField
          label="Cargado hasta"
          type="date"
          value={borrador.hasta}
          onChange={(event) => cambiar({ hasta: event.target.value })}
          error={errorFechas}
        />
      </div>

      {columnas.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-2">
          <SelectField
            label="Filtrar por una columna"
            value={borrador.campoId}
            onChange={(event) => cambiar({ campoId: event.target.value, valor: '' })}
          >
            <option value="">Sin filtro por columna</option>
            {columnas.map((campo) => (
              <option key={campo.id} value={campo.id}>
                {campo.nombre}
              </option>
            ))}
          </SelectField>
          {columna && (
            <SelectField
              label={`Valor de ${columna.nombre}`}
              value={borrador.valor}
              onChange={(event) => cambiar({ valor: event.target.value })}
            >
              <option value="">Elegí un valor</option>
              {valores.map((opcion) => (
                <option key={opcion.valor} value={opcion.valor}>
                  {opcion.texto}
                </option>
              ))}
            </SelectField>
          )}
        </div>
      )}

      <div className="flex flex-wrap gap-3">
        <Button type="submit" variant="solid">
          Ver reporte
        </Button>
        <Button onClick={limpiar}>Limpiar filtros</Button>
      </div>
    </form>
  );
}
