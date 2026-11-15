package com.ensambla.backend.domain.useCase.caja.cobrarVenta;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.caja.LineaVentaCaja;
import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.domain.model.caja.ModoVenta;
import com.ensambla.backend.domain.model.caja.PagoCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

@RequiredArgsConstructor
public class CobrarVenta {

    private final NegocioRepository negocioRepository;
    private final UsoPlanRepository usoPlanRepository;
    private final SistemaRepository sistemaRepository;
    private final VentaCajaRepository ventaCajaRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final ControlPlanService controlPlanService;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public CobrarVentaResponse execute(CobrarVentaRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        return CobrarVentaResponse.de(confirmarEnTransaccion(negocioId, request));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private VentaCaja confirmarEnTransaccion(NegocioId negocioId, CobrarVentaRequest request) {
        return transaccionPort.ejecutar(() -> cobrar(negocioId, request));
    }

    private VentaCaja cobrar(NegocioId negocioId, CobrarVentaRequest request) {
        usoPlanRepository.bloquearNegocio(negocioId);
        return ventaCajaRepository.findByIdAndNegocioId(request.id(), negocioId)
                .map(venta -> comprobarReintento(venta, request)).orElseGet(() -> confirmarVentaNueva(negocioId, request));
    }

    private VentaCaja comprobarReintento(VentaCaja anterior, CobrarVentaRequest request) {
        if (!anterior.huella().equals(request.huella())) {
            throw new DuplicateResourceException("Esta operación ya se confirmó con otros datos");
        }
        return anterior;
    }

    private VentaCaja confirmarVentaNueva(NegocioId negocioId, CobrarVentaRequest request) {
        EntornoCobro entorno = prepararEntorno(negocioId, request);
        validarLimiteDeRegistros(negocioId, request, entorno.ahora());
        List<LineaVentaCaja> lineas = aplicarLineas(entorno.sistema(), entorno.inventario(), request, entorno.ahora());
        VentaCaja venta = crearVenta(negocioId, request, entorno, lineas);
        registrarLineasVentas(entorno.sistema(), entorno.ventas(), venta, entorno.ahora());
        guardarInventarioYVentas(entorno);
        return guardarComprobante(venta);
    }

    private EntornoCobro prepararEntorno(NegocioId negocioId, CobrarVentaRequest request) {
        Sistema sistema = obtenerSistema(negocioId, request);
        Bloque inventario = obtenerBloque(sistema, request.inventarioId(), TipoBloque.INVENTARIO);
        Bloque ventas = obtenerBloque(sistema, request.ventasId(), TipoBloque.VENTAS);
        String clienteNombre = obtenerCliente(sistema, ventas, request.clienteId());
        exigirInventarioRelacionado(inventario, ventas);
        return new EntornoCobro(sistema, inventario, ventas, clienteNombre, OffsetDateTime.now(clock));
    }

    private Sistema obtenerSistema(NegocioId negocioId, CobrarVentaRequest request) {
        return sistemaRepository.findAllByNegocioId(negocioId).stream()
                .filter(item -> item.id().equals(request.proyectoId())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Sistema no encontrado"));
    }

    private void exigirInventarioRelacionado(Bloque inventario, Bloque ventas) {
        if (!inventario.getId().equals(campo(ventas, "product").getBloqueRelacionadoId())) {
            throw new BusinessRuleException("La tabla de ventas debe usar el inventario elegido");
        }
    }

    private void validarLimiteDeRegistros(NegocioId negocioId, CobrarVentaRequest request, OffsetDateTime ahora) {
        controlPlanService.validarAltaRegistros(negocioId, request.lineas().size(), ahora);
    }

    private VentaCaja crearVenta(NegocioId negocioId, CobrarVentaRequest request, EntornoCobro entorno,
            List<LineaVentaCaja> lineas) {
        List<PagoCaja> pagos = Optional.ofNullable(request.pagos()).map(elegidos -> elegidos.stream()
                .map(pago -> new PagoCaja(MedioPagoCaja.valueOf(pago.medio()), pago.importe())).toList()).orElse(null);
        return VentaCaja.nueva(request.id(), negocioId, entorno.sistema().id(), request.inventarioId(),
                request.huella(), lineas, MedioPagoCaja.valueOf(request.medio()), request.recibido(), entorno.ahora(),
                pagos, request.clienteId(), entorno.clienteNombre());
    }

    private void guardarInventarioYVentas(EntornoCobro entorno) {
        entorno.sistema().proyecto().setActualizadoEn(entorno.ahora());
        sistemaRepository.guardar(entorno.sistema());
    }

    private VentaCaja guardarComprobante(VentaCaja venta) {
        return ventaCajaRepository.save(venta);
    }

    private record EntornoCobro(Sistema sistema, Bloque inventario, Bloque ventas, String clienteNombre,
                                OffsetDateTime ahora) {
    }

    private List<LineaVentaCaja> aplicarLineas(Sistema sistema, Bloque inventario,
            CobrarVentaRequest request, OffsetDateTime ahora) {
        Map<RegistroId, BigDecimal> stockRegistrado = consultarStockRegistrado(sistema, request);
        return request.lineas().stream().map(entrada -> entrada.resolver(item ->
                lineaDeInventario(sistema, inventario, item, ahora, stockRegistrado, request.id().toString()))).toList();
    }

    private LineaVentaCaja lineaDeInventario(Sistema sistema, Bloque inventario,
            CobrarVentaRequest.LineaRequest entrada, OffsetDateTime ahora,
            Map<RegistroId, BigDecimal> stockRegistrado, String operacion) {
        Registro producto = obtenerProducto(sistema, inventario, entrada.productoId());
        BigDecimal precio = numero(valor(inventario, producto, "price"));
        ModoVenta modo = "peso".equals(valor(inventario, producto, "saleMode")) ? ModoVenta.PESO : ModoVenta.UNIDAD;
        exigirPrecioYModoActuales(entrada, precio, modo);
        LineaVentaCaja linea = LineaVentaCaja.de(producto.getId(),
                String.valueOf(valor(inventario, producto, "name")), modo, entrada.cantidad(), precio);
        descontarStock(inventario, producto, linea, ahora, stockRegistrado, operacion);
        return linea;
    }

    private Registro obtenerProducto(Sistema sistema, Bloque inventario, RegistroId id) {
        return sistema.registrosPorBloque().getOrDefault(inventario.getId(), List.of()).stream()
                .filter(item -> item.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado en el inventario"));
    }

    private void exigirPrecioYModoActuales(CobrarVentaRequest.LineaRequest entrada, BigDecimal precio, ModoVenta modo) {
        if (precio.compareTo(entrada.precio()) != 0 || modo != ModoVenta.valueOf(entrada.modo())) {
            throw new DuplicateResourceException("El precio o la forma de venta cambió. Actualizá la compra");
        }
    }

    private void registrarLineasVentas(Sistema sistema, Bloque ventas, VentaCaja venta, OffsetDateTime ahora) {
        List<Registro> filas = new ArrayList<>(sistema.registrosPorBloque().getOrDefault(ventas.getId(), List.of()));
        for (LineaVentaCaja linea : venta.lineas()) {
            filas.add(registroVenta(sistema, ventas, linea, venta, filas.size(), ahora));
        }
        sistema.registrosPorBloque().put(ventas.getId(), filas);
    }

    private void descontarStock(Bloque inventario, Registro producto, LineaVentaCaja linea, OffsetDateTime ahora,
            Map<RegistroId, BigDecimal> stockRegistrado, String operacion) {
        if (Boolean.FALSE.equals(valor(inventario, producto, "stockControl"))) {
            return;
        }
        BigDecimal saldo = numero(valor(inventario, producto, "quantity")).subtract(linea.cantidad());
        if (saldo.signum() < 0) {
            throw new DuplicateResourceException("No alcanza el stock de " + linea.nombre());
        }
        registrarSalida(producto, linea, saldo, stockRegistrado, operacion, ahora);
        actualizarCantidad(inventario, producto, saldo, ahora);
    }

    private void actualizarCantidad(Bloque inventario, Registro producto, BigDecimal saldo, OffsetDateTime ahora) {
        producto.setValores(new LinkedHashMap<>(producto.getValores()));
        producto.getValores().put(campo(inventario, "quantity").getId().toString(), saldo);
        producto.setEsEjemplo(false);
        producto.setActualizadoEn(ahora);
    }

    private Map<RegistroId, BigDecimal> consultarStockRegistrado(Sistema sistema, CobrarVentaRequest request) {
        List<RegistroId> productos = request.lineas().stream().map(CobrarVentaRequest.LineaRequest::productoId)
                .filter(Objects::nonNull).distinct().toList();
        if (productos.isEmpty()) return new LinkedHashMap<>();
        return new LinkedHashMap<>(movimientoStockRepository.stockDe(productos, sistema.proyecto().getNegocioId()));
    }

    private void registrarSalida(Registro producto, LineaVentaCaja linea, BigDecimal saldo,
            Map<RegistroId, BigDecimal> stockRegistrado, String operacion, OffsetDateTime ahora) {
        BigDecimal antes = saldo.add(linea.cantidad());
        BigDecimal ajuste = antes.subtract(stockRegistrado.getOrDefault(producto.getId(), BigDecimal.ZERO));
        registrarVariacion(producto, ajuste, "Ajuste al stock del inventario antes de cobrar", ahora);
        registrarVariacion(producto, linea.cantidad().negate(), "Venta de caja " + operacion, ahora);
        stockRegistrado.put(producto.getId(), saldo);
    }

    private void registrarVariacion(Registro producto, BigDecimal variacion, String nota, OffsetDateTime ahora) {
        if (variacion.signum() == 0) return;
        String tipo = variacion.signum() > 0 ? "ENTRADA" : "SALIDA";
        MovimientoStock movimiento = MovimientoStock.nuevo(producto.getNegocioId(), producto.getId(), tipo,
                variacion.abs(), ZonaNegocio.dia(ahora), nota, ZonaNegocio.dia(ahora), ahora);
        movimientoStockRepository.save(movimiento);
    }

    private Registro registroVenta(Sistema sistema, Bloque ventas, LineaVentaCaja linea,
            VentaCaja venta, int orden, OffsetDateTime ahora) {
        Map<String, Object> valores = valoresDeVenta(ventas, linea, venta, ahora);
        Registro fila = Registro.nuevo(sistema.proyecto().getNegocioId(), ventas.getId(),
                Registro.validar(camposDeVenta(ventas, linea), valores), ahora);
        fila.setId(RegistroId.nuevo());
        fila.setOrden(orden);
        fila.setOrigen("Caja " + venta.id() + (linea.productoId() == null ? " · " + linea.nombre() : ""));
        return fila;
    }

    private Map<String, Object> valoresDeVenta(Bloque ventas, LineaVentaCaja linea, VentaCaja venta, OffsetDateTime ahora) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(campo(ventas, "product").getId().toString(), Objects.toString(linea.productoId(), null));
        valores.put(campo(ventas, "quantity").getId().toString(), linea.cantidad());
        valores.put(campo(ventas, "price").getId().toString(), linea.precio());
        valores.put(campo(ventas, "date").getId().toString(), ZonaNegocio.dia(ahora).toString());
        agregarClienteYDeuda(ventas, venta, valores);
        return valores;
    }

    private void agregarClienteYDeuda(Bloque ventas, VentaCaja venta, Map<String, Object> valores) {
        ventas.getCampos().stream().filter(item -> "customer".equals(item.getClave())).findFirst()
                .ifPresent(item -> valores.put(item.getId().toString(), valorDelCliente(item, venta)));
        ventas.getCampos().stream().filter(item -> "credit".equals(item.getClave())).findFirst()
                .ifPresent(item -> valores.put(item.getId().toString(), venta.saldo().signum() > 0));
    }

    private String valorDelCliente(Campo campo, VentaCaja venta) {
        if (venta.clienteId() == null) return null;
        return campo.getBloqueRelacionadoId() == null ? venta.clienteNombre() : venta.clienteId().toString();
    }

    private List<Campo> camposDeVenta(Bloque ventas, LineaVentaCaja linea) {
        if (linea.productoId() != null) return ventas.getCampos();
        return ventas.getCampos().stream().filter(item -> !"product".equals(item.getClave())).toList();
    }

    private Bloque obtenerBloque(Sistema sistema, BloqueId id, TipoBloque tipo) {
        return sistema.proyecto().getBloques().stream()
                .filter(bloque -> bloque.getId().equals(id) && bloque.getTipo() == tipo).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Tabla de caja no encontrada"));
    }

    private String obtenerCliente(Sistema sistema, Bloque ventas, RegistroId clienteId) {
        if (clienteId == null) {
            return null;
        }
        BloqueId destino = ventas.getCampos().stream().filter(item -> "customer".equals(item.getClave()))
                .map(Campo::getBloqueRelacionadoId).filter(Objects::nonNull).findFirst().orElse(null);
        return sistema.proyecto().getBloques().stream().filter(tabla -> esTablaDeClientes(tabla, destino))
                .flatMap(tabla -> nombreDelCliente(sistema, tabla, clienteId).stream()).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado en el negocio"));
    }

    private boolean esTablaDeClientes(Bloque tabla, BloqueId destino) {
        return tabla.getTipo() == TipoBloque.CLIENTES && (destino == null || destino.equals(tabla.getId()));
    }

    private Optional<String> nombreDelCliente(Sistema sistema, Bloque tabla, RegistroId id) {
        return sistema.registrosPorBloque().getOrDefault(tabla.getId(), List.of()).stream()
                .filter(fila -> fila.getId().equals(id) && !Boolean.TRUE.equals(fila.getEsEjemplo()))
                .findFirst().map(fila -> String.valueOf(valor(tabla, fila, "name")));
    }

    private Campo campo(Bloque bloque, String clave) {
        return bloque.getCampos().stream().filter(item -> clave.equals(item.getClave())).findFirst()
                .orElseThrow(() -> new BusinessRuleException("Falta el campo " + clave + " en " + bloque.getNombre()));
    }

    private Object valor(Bloque bloque, Registro fila, String clave) {
        return bloque.getCampos().stream().filter(item -> clave.equals(item.getClave())).findFirst()
                .map(item -> fila.getValores().get(item.getId().toString())).orElse(null);
    }

    private BigDecimal numero(Object valor) {
        if (!(valor instanceof Number)) {
            throw new BusinessRuleException("Revisá el precio y la cantidad del inventario");
        }
        return new BigDecimal(valor.toString());
    }
}
