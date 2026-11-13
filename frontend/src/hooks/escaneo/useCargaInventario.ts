import { useCallback, useEffect, useRef, useState } from 'react';
import { useBlocker } from '@tanstack/react-router';
import { consultarCatalogos } from '@/api/catalogosProductos';
import { findBlock } from '@/lib/sistema/core';
import {
  buscarProductoPorCodigo,
  guardarProductoEscaneado,
  prepararProducto,
  sumarStockProductoEscaneado,
} from '@/lib/escaneo/inventario';
import { codigoGtin, mismoGtin } from '@/lib/escaneo/codigo';
import { sugerirProducto } from '@/lib/escaneo/catalogos';
import { useSistemaStore } from '@/stores/sistemaStore';
import { useConfirmarCambios } from '@/hooks/useConfirmarCambios';
import type { ErroresFila, Fila, Valor } from '@/lib/sistema/tipos';
import type { BorradorProductoInventario, ResultadoCatalogos } from '@/types/escaneo';

export function useCargaInventario(sistemaId: string, inventarioId: string) {
  const cambios = useConfirmarCambios();
  const confirmarCambio = cambios.confirmar;
  const usuarioId = useSistemaStore((estado) => estado.usuarioId);
  const clave = `ensambla:producto-pendiente:${usuarioId}:${sistemaId}:${inventarioId}`;
  const [recuperado] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem(clave) ?? 'null') as {
        borrador: BorradorProductoInventario;
        cantidad: string;
      } | null;
    } catch {
      return null;
    }
  });
  const sistema = useSistemaStore((estado) =>
    estado.sistemas.find((item) => item.id === sistemaId),
  );
  const pendiente = useSistemaStore((estado) => estado.pendientes.includes(sistemaId));
  const inventario = findBlock(sistema, inventarioId);
  const [codigo, setCodigo] = useState('');
  const [borrador, setBorrador] = useState<BorradorProductoInventario | null>(
    recuperado?.borrador ?? null,
  );
  const [resultado, setResultado] = useState<ResultadoCatalogos | null>(null);
  const [buscando, setBuscando] = useState(false);
  const [consultandoReferencia, setConsultandoReferencia] = useState(false);
  const [cantidadAgregar, setCantidadAgregar] = useState(recuperado?.cantidad ?? '');
  useEffect(() => {
    if (borrador)
      localStorage.setItem(clave, JSON.stringify({ borrador, cantidad: cantidadAgregar }));
    else localStorage.removeItem(clave);
  }, [clave, borrador, cantidadAgregar]);
  const [unidadesAgregadas, setUnidadesAgregadas] = useState<number | null>(null);
  const [errores, setErrores] = useState<ErroresFila>({});
  const [mensaje, setMensaje] = useState('');
  const [ultimo, setUltimo] = useState<Fila | null>(null);
  const esReposicion =
    !!borrador?.productoId && mismoGtin(borrador.originales.barcode, borrador.codigo);
  const productoActual = inventario?.rows.find((fila) => fila.id === borrador?.productoId);
  const formulario = useRef<HTMLFormElement>(null);
  const entradaCodigo = useRef<HTMLInputElement>(null);
  const revisionProducto = useRef<HTMLDivElement>(null);
  const confirmacionCarga = useRef<HTMLDivElement>(null);
  const codigoEnRevision = borrador?.codigo;
  useEffect(() => {
    if (codigoEnRevision && !buscando && window.matchMedia?.('(max-width: 850px)').matches) {
      revisionProducto.current?.scrollIntoView({
        block: 'start',
        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches
          ? 'instant'
          : 'smooth',
      });
    }
  }, [codigoEnRevision, buscando]);
  useEffect(() => {
    if (ultimo) confirmacionCarga.current?.scrollIntoView({ block: 'start', behavior: 'instant' });
  }, [ultimo]);
  const origenLectura = useRef<'codigo' | 'camara'>('codigo');
  const [retomarLectura, setRetomarLectura] = useState(0);
  useEffect(() => {
    if (retomarLectura > 0 && origenLectura.current === 'codigo')
      entradaCodigo.current?.focus({ preventScroll: true });
  }, [retomarLectura]);
  const [revisionErrores, setRevisionErrores] = useState(0);
  useEffect(() => {
    formulario.current?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
  }, [revisionErrores]);
  const consulta = useRef<AbortController | null>(null);
  const secuencia = useRef(0);
  useEffect(
    () => () => {
      secuencia.current++;
      consulta.current?.abort();
    },
    [],
  );
  useBlocker({
    shouldBlockFn: async () => {
      if (!borrador) return false;
      const salir = await cambios.confirmar(
        'Hay un producto sin guardar. Si salís, se descartará esta carga.',
      );
      if (salir) {
        localStorage.removeItem(clave);
        setBorrador(null);
      }
      return !salir;
    },
    enableBeforeUnload: false,
  });

  const detener = () => {
    secuencia.current++;
    consulta.current?.abort();
    setBuscando(false);
    setConsultandoReferencia(false);
  };
  const limpiar = () => {
    localStorage.removeItem(clave);
    setBorrador(null);
    setResultado(null);
    setCodigo('');
    setErrores({});
    setCantidadAgregar('');
  };
  const buscar = useCallback(
    async (entrada: string, origen: 'codigo' | 'camara' = 'codigo') => {
      if (!inventario || inventario.type !== 'inventory') return;
      let valido: string;
      try {
        valido = codigoGtin(entrada);
      } catch (e) {
        setErrores({ _form: e instanceof Error ? e.message : 'Revisá el código.' });
        return;
      }
      if (
        borrador &&
        !(await confirmarCambio(
          'Hay un producto sin guardar. Buscar otro código descartará esta carga.',
        ))
      )
        return;
      consulta.current?.abort();
      origenLectura.current = origen;
      const numero = ++secuencia.current;
      setErrores({});
      setMensaje('');
      setUltimo(null);
      setUnidadesAgregadas(null);
      setCantidadAgregar('');
      setResultado(null);
      setBuscando(false);
      setCodigo(valido);
      const propio = buscarProductoPorCodigo(inventario, valido);
      setBorrador(prepararProducto(inventario, valido, propio));
      const controlador = new AbortController();
      consulta.current = controlador;
      const plazo = setTimeout(() => controlador.abort(), 12_000);
      setBuscando(!propio);
      setConsultandoReferencia(true);
      try {
        const encontrados = await consultarCatalogos(valido, controlador.signal);
        if (numero !== secuencia.current) return;
        setResultado(encontrados);
        const sugerencia = sugerirProducto(encontrados);
        if (!propio && sugerencia.nombre)
          setBorrador((actual) =>
            actual && !String(actual.valores.name ?? '').trim()
              ? { ...actual, valores: { ...actual.valores, name: sugerencia.nombre } }
              : actual,
          );
      } catch (e) {
        if (!propio && numero === secuencia.current)
          setErrores({
            _form:
              e instanceof Error && e.name === 'AbortError'
                ? 'La consulta tardó demasiado. Podés completar los datos a mano.'
                : 'No pudimos consultar los catálogos. Podés cargar el producto a mano.',
          });
      } finally {
        clearTimeout(plazo);
        if (numero === secuencia.current) {
          setBuscando(false);
          setConsultandoReferencia(false);
        }
      }
    },
    [inventario, borrador, confirmarCambio],
  );

  const guardar = () => {
    if (!borrador) return;
    const estado = useSistemaStore.getState();
    const actual = estado.sistemas.find((item) => item.id === sistemaId);
    if (!actual || estado.activeId !== sistemaId) {
      setErrores({ _form: 'El negocio ya no existe.' });
      return;
    }
    const guardado =
      esReposicion && borrador.productoId
        ? sumarStockProductoEscaneado(
            actual,
            inventarioId,
            borrador.productoId,
            borrador.codigo,
            cantidadAgregar,
          )
        : guardarProductoEscaneado(actual, inventarioId, borrador);
    if (guardado.errores) {
      setErrores(guardado.errores);
      setRevisionErrores((revision) => revision + 1);
      return;
    }
    estado.commit((item) => {
      item.sections = guardado.sistema.sections;
    });
    detener();
    limpiar();
    setUltimo(guardado.producto);
    setUnidadesAgregadas(esReposicion ? Number(cantidadAgregar) : null);
    setRetomarLectura((revision) => revision + 1);
    setMensaje(
      borrador.productoId
        ? 'Cambios del producto aplicados al inventario.'
        : 'Producto agregado al inventario. Ya podés escanear el siguiente.',
    );
  };
  return {
    sistema,
    confirmacion: cambios.confirmacion,
    aceptarDescarte: cambios.aceptar,
    conservarCambios: cambios.conservar,
    inventario,
    pendiente,
    codigo,
    borrador,
    resultado,
    buscando,
    consultandoReferencia,
    esReposicion,
    productoActual,
    cantidadAgregar,
    unidadesAgregadas,
    cambiarCantidadAgregar: (cantidad: string) => {
      setCantidadAgregar(cantidad);
      setErrores((actual) => ({ ...actual, quantity: '' }));
    },
    errores,
    mensaje,
    ultimo,
    sugerencia: resultado ? sugerirProducto(resultado) : null,
    formulario,
    entradaCodigo,
    revisionProducto,
    confirmacionCarga,
    productosVinculables:
      inventario?.rows
        .filter(
          (producto) => !producto.barcode || mismoGtin(producto.barcode, borrador?.codigo ?? ''),
        )
        .map((producto) => ({ id: producto.id, nombre: String(producto.name || producto.id) })) ??
      [],
    grupos: [
      {
        titulo: 'Identificación del producto',
        ayuda: 'Revisá la descripción que se usará en tu inventario.',
        claves: ['name'],
      },
      {
        titulo: 'Datos de tu negocio',
        ayuda: 'Completá tu conteo real, costos y precios. Estos datos los definís vos.',
        claves: ['quantity', 'cost', 'price', 'minimum'],
      },
      {
        titulo: 'Otros datos de tu inventario',
        ayuda: 'Son los campos que configuraste en esta tabla.',
        claves: null,
      },
    ]
      .map((grupo) => ({
        ...grupo,
        campos:
          inventario?.fields.filter(
            (campo) =>
              campo.key !== 'barcode' &&
              (grupo.claves
                ? grupo.claves.includes(campo.key)
                : !['name', 'quantity', 'cost', 'price', 'minimum'].includes(campo.key)),
          ) ?? [],
      }))
      .filter((grupo) => grupo.campos.length > 0),
    setCodigo,
    buscar,
    guardar,
    cambiar: (clave: string, valor: Valor) => {
      setBorrador((actual) =>
        actual ? { ...actual, valores: { ...actual.valores, [clave]: valor } } : actual,
      );
      setErrores((actual) => ({ ...actual, [clave]: '' }));
    },
    elegirExistente: (id: string) => {
      if (!inventario || !borrador) return;
      const propio = inventario.rows.find((fila) => fila.id === id);
      const nuevo = prepararProducto(inventario, borrador.codigo, propio);
      if (!propio && resultado) nuevo.valores.name = sugerirProducto(resultado).nombre;
      setBorrador(nuevo);
      setErrores({});
    },
    cancelar: async () => {
      if (
        borrador &&
        !(await cambios.confirmar('Esta carga todavía no se guardó en el inventario.'))
      )
        return;
      detener();
      limpiar();
      setUltimo(null);
      setUnidadesAgregadas(null);
      setRetomarLectura((revision) => revision + 1);
      setMensaje('Carga descartada. El inventario conserva sus datos.');
    },
    otraLectura: async () => {
      if (
        borrador &&
        !(await cambios.confirmar(
          'Hay una carga sin confirmar. Escanear otro producto descartará estos datos.',
        ))
      )
        return;
      detener();
      limpiar();
      setUltimo(null);
      setUnidadesAgregadas(null);
      setMensaje('');
      origenLectura.current = 'codigo';
      setRetomarLectura((revision) => revision + 1);
    },
  };
}
