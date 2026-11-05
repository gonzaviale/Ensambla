import { useMemo, useState, type ReactNode } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { varsDeColor } from '@/components/sistema/colores';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useResumenInicio } from '@/hooks/reportes/queries';
import { formatoPesos } from '@/lib/sistema/formato';
import {
  comparacionConLaSemanaPasada,
  cuandoFue,
  cuadernosConDatos,
  formatoCantidad,
  textoUnidades,
} from '@/lib/sistema/metricasInicio';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type { Color } from '@/lib/sistema/tipos';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { ResumenInicio } from '@/types/resumenInicio';

const CLAVE_DEL_CUADERNO = 'ensambla.inicio.cuaderno';

const leerCuadernoElegido = () => {
  try {
    return window.localStorage.getItem(CLAVE_DEL_CUADERNO);
  } catch {
    return null;
  }
};

const recordarCuaderno = (id: string) => {
  try {
    window.localStorage.setItem(CLAVE_DEL_CUADERNO, id);
  } catch {
    // Sin almacenamiento el inicio funciona igual: sólo no se acuerda de la última elección.
  }
};

// «Cómo te va»: los números del cuaderno que la persona eligió (por defecto, el último que tocó). No aparece si
// ningún cuaderno tiene ventas o productos cargados, ni mientras el servidor no contesta: nunca muestra ceros de relleno.
export function MetricasDelInicio() {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const navegacion = useNavegacion();
  const cuadernos = useMemo(() => cuadernosConDatos(sistemas), [sistemas]);
  const [elegido, setElegido] = useState(leerCuadernoElegido);
  const cuaderno = cuadernos.find((item) => item.id === elegido) ?? cuadernos[0];
  const resumen = useResumenInicio(cuaderno?.id);

  if (!cuaderno || !resumen) return null;
  if (!resumen.hayVentas && !resumen.hayProductos) return null;

  const elegirCuaderno = (id: string) => {
    setElegido(id);
    recordarCuaderno(id);
  };

  return (
    <section className="inicio-metricas" aria-label="Cómo te va">
      <div className="inicio-metricas-cabecera">
        <div className="inicio-metricas-titulo">
          <h2>Cómo te va</h2>
          {cuadernos.length > 1 ? (
            <label className="inicio-metricas-cuaderno">
              <span>en</span>
              <select
                value={cuaderno.id}
                onChange={(evento) => elegirCuaderno(evento.target.value)}
                aria-label="Cuaderno"
              >
                {cuadernos.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name}
                  </option>
                ))}
              </select>
            </label>
          ) : (
            <p className="inicio-metricas-cuaderno">
              <span>en</span>
              <strong>{cuaderno.name}</strong>
            </p>
          )}
        </div>
        <Btn cls="small ghost" onClick={() => void navegacion.abrirSistema(cuaderno.id)}>
          Abrir el cuaderno <Icon name="arrow" />
        </Btn>
      </div>

      <div className="inicio-metricas-tarjetas">
        {resumen.hayVentas && <TarjetaDeHoy resumen={resumen} />}
        {resumen.masVendido && <TarjetaMasVendido resumen={resumen} />}
        {resumen.hayProductos && <TarjetaDeFaltantes resumen={resumen} />}
        {resumen.parados.length > 0 && <TarjetaDeParados resumen={resumen} />}
      </div>
    </section>
  );
}

type PropsDeTarjeta = { resumen: ResumenInicio };

function Tarjeta({
  color,
  icono,
  titulo,
  children,
}: {
  color: Color;
  icono: NombreDeIcono;
  titulo: string;
  children: ReactNode;
}) {
  return (
    <article className="inicio-metrica" style={varsDeColor(color)} data-color={color}>
      <header>
        <span className="inicio-metrica-icono">
          <Icon name={icono} />
        </span>
        <h3>{titulo}</h3>
      </header>
      {children}
    </article>
  );
}

function TarjetaDeHoy({ resumen }: PropsDeTarjeta) {
  const { ventasDeHoy, ultimoDiaConVentas, hoy } = resumen;
  if (ventasDeHoy.cantidad === 0) {
    return (
      <Tarjeta color="petrol" icono="wallet" titulo="Hoy">
        <p className="inicio-metrica-valor">Todavía no anotaste ventas</p>
        {ultimoDiaConVentas && (
          <p className="inicio-metrica-detalle">
            Lo último que anotaste fue {cuandoFue(ultimoDiaConVentas.fecha, hoy)}:{' '}
            {ultimoDiaConVentas.ingresos > 0
              ? formatoPesos(ultimoDiaConVentas.ingresos)
              : 'ventas sin precio'}
            .
          </p>
        )}
      </Tarjeta>
    );
  }
  const conPlata = ventasDeHoy.ingresos > 0;
  const comparacion = conPlata
    ? comparacionConLaSemanaPasada(ventasDeHoy.ingresos, ventasDeHoy.ingresosSemanaPasada, hoy)
    : null;
  return (
    <Tarjeta color="petrol" icono="wallet" titulo="Hoy vendiste">
      <p className="inicio-metrica-valor">
        {conPlata ? formatoPesos(ventasDeHoy.ingresos) : textoUnidades(ventasDeHoy.unidades)}
      </p>
      {comparacion && <p className="inicio-metrica-detalle">{comparacion}</p>}
      {!conPlata && (
        <p className="inicio-metrica-detalle">
          Esas ventas no tienen precio, así que no puedo sumar la plata.
        </p>
      )}
      {conPlata && ventasDeHoy.sinPrecio > 0 && (
        <p className="inicio-metrica-nota">
          {ventasDeHoy.sinPrecio === 1
            ? '1 venta sin precio no está sumada.'
            : `${ventasDeHoy.sinPrecio} ventas sin precio no están sumadas.`}
        </p>
      )}
    </Tarjeta>
  );
}

function TarjetaMasVendido({ resumen }: PropsDeTarjeta) {
  const producto = resumen.masVendido;
  if (!producto) return null;
  return (
    <Tarjeta color="orange" icono="spark" titulo="Lo que más salió este mes">
      <p className="inicio-metrica-valor">{producto.nombre}</p>
      <p className="inicio-metrica-detalle">{textoUnidades(producto.unidades)} en los últimos 30 días</p>
      {producto.ganancia != null && (
        <p className="inicio-metrica-nota">Te dejó cerca de {formatoPesos(producto.ganancia)}.</p>
      )}
    </Tarjeta>
  );
}

function TarjetaDeFaltantes({ resumen }: PropsDeTarjeta) {
  const { total, primeros } = resumen.faltantes;
  if (total === 0) {
    return (
      <Tarjeta color="petrol" icono="box" titulo="Tu stock">
        <p className="inicio-metrica-valor">Nada se acabó</p>
        <p className="inicio-metrica-detalle">Ningún producto está agotado ni en su mínimo.</p>
      </Tarjeta>
    );
  }
  return (
    <Tarjeta color="coral" icono="box" titulo="Se te está acabando">
      <p className="inicio-metrica-valor">{total === 1 ? '1 producto' : `${total} productos`}</p>
      <ul className="inicio-metrica-lista">
        {primeros.map((producto) => (
          <li key={producto.nombre}>
            <span>{producto.nombre}</span>
            <strong>
              {producto.cantidad <= 0 ? 'Agotado' : `Quedan ${formatoCantidad(producto.cantidad)}`}
            </strong>
          </li>
        ))}
      </ul>
      {total > primeros.length && (
        <p className="inicio-metrica-nota">y {total - primeros.length} más.</p>
      )}
    </Tarjeta>
  );
}

function TarjetaDeParados({ resumen }: PropsDeTarjeta) {
  return (
    <Tarjeta color="blue" icono="clock" titulo="Sin venderse hace 2 semanas">
      <p className="inicio-metrica-detalle">Productos con stock que no tuvieron ventas:</p>
      <ul className="inicio-metrica-lista">
        {resumen.parados.map((producto) => (
          <li key={producto.nombre}>
            <span>{producto.nombre}</span>
            <strong>
              {producto.capital != null
                ? `${formatoPesos(producto.capital)} en stock`
                : textoUnidades(producto.cantidad)}
            </strong>
          </li>
        ))}
      </ul>
    </Tarjeta>
  );
}
