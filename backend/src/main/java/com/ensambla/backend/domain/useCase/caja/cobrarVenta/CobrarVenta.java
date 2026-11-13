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
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
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

@RequiredArgsConstructor
public class CobrarVenta {

    private final NegocioRepository negocioRepository;
    private final UsoPlanRepository usoPlanRepository;
    private final SistemaRepository sistemaRepository;
    private final VentaCajaRepository ventaCajaRepository;
    private final ControlPlanService controlPlanService;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public CobrarVentaResponse execute(CobrarVentaRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        return CobrarVentaResponse.de(transaccionPort.ejecutar(() -> cobrar(negocioId, request)));
    }

    private VentaCaja cobrar(NegocioId negocioId, CobrarVentaRequest request) {
        usoPlanRepository.bloquearNegocio(negocioId);
        var anterior = ventaCajaRepository.findByIdAndNegocioId(request.id(), negocioId);
        if (anterior.isPresent()) {
            if (!anterior.get().huella().equals(request.huella())) {
                throw new DuplicateResourceException("Esta operación ya se confirmó con otros datos");
            }
            return anterior.get();
        }
        Sistema sistema = sistemaRepository.findAllByNegocioId(negocioId).stream()
                .filter(item -> item.id().equals(request.proyectoId())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Sistema no encontrado"));
        Bloque inventario = obtenerBloque(sistema, request.inventarioId(), TipoBloque.INVENTARIO);
        Bloque ventas = obtenerBloque(sistema, request.ventasId(), TipoBloque.VENTAS);
        Campo relacion = campo(ventas, "product");
        if (!inventario.getId().equals(relacion.getBloqueRelacionadoId())) {
            throw new BusinessRuleException("La tabla de ventas debe usar el inventario elegido");
        }
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        controlPlanService.validarAltaRegistros(negocioId, request.lineas().size(), ahora);
        List<LineaVentaCaja> lineas = aplicarLineas(sistema, inventario, ventas, request, ahora);
        VentaCaja venta = VentaCaja.nueva(request.id(), negocioId, sistema.id(), request.huella(),
                lineas, MedioPagoCaja.valueOf(request.medio()), request.recibido(), ahora);
        sistema.proyecto().setActualizadoEn(ahora);
        sistemaRepository.guardar(sistema);
        return ventaCajaRepository.save(venta);
    }

    private List<LineaVentaCaja> aplicarLineas(Sistema sistema, Bloque inventario, Bloque ventas,
            CobrarVentaRequest request, OffsetDateTime ahora) {
        List<LineaVentaCaja> lineas = new ArrayList<>();
        List<Registro> filasVentas = new ArrayList<>(sistema.registrosPorBloque()
                .getOrDefault(ventas.getId(), List.of()));
        for (var entrada : request.lineas()) {
            Registro producto = sistema.registrosPorBloque().getOrDefault(inventario.getId(), List.of()).stream()
                    .filter(item -> item.getId().equals(entrada.productoId())).findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Artículo no encontrado en el inventario"));
            BigDecimal precio = numero(valor(inventario, producto, "price"));
            ModoVenta modo = "peso".equals(valor(inventario, producto, "saleMode"))
                    ? ModoVenta.PESO : ModoVenta.UNIDAD;
            if (precio.compareTo(entrada.precio()) != 0 || modo != ModoVenta.valueOf(entrada.modo())) {
                throw new DuplicateResourceException("El precio o la forma de venta cambió. Actualizá la compra");
            }
            LineaVentaCaja linea = LineaVentaCaja.de(producto.getId(),
                    String.valueOf(valor(inventario, producto, "name")), modo, entrada.cantidad(), precio);
            descontarStock(inventario, producto, linea, ahora);
            filasVentas.add(registroVenta(sistema, ventas, linea, request, filasVentas.size(), ahora));
            lineas.add(linea);
        }
        sistema.registrosPorBloque().put(ventas.getId(), filasVentas);
        return lineas;
    }

    private void descontarStock(Bloque inventario, Registro producto, LineaVentaCaja linea, OffsetDateTime ahora) {
        if (Boolean.FALSE.equals(valor(inventario, producto, "stockControl"))) {
            return;
        }
        BigDecimal saldo = numero(valor(inventario, producto, "quantity")).subtract(linea.cantidad());
        if (saldo.signum() < 0) {
            throw new DuplicateResourceException("No alcanza el stock de " + linea.nombre());
        }
        producto.setValores(new LinkedHashMap<>(producto.getValores()));
        producto.getValores().put(campo(inventario, "quantity").getId().toString(), saldo);
        producto.setEsEjemplo(false);
        producto.setActualizadoEn(ahora);
    }

    private Registro registroVenta(Sistema sistema, Bloque ventas, LineaVentaCaja linea,
            CobrarVentaRequest request, int orden, OffsetDateTime ahora) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(campo(ventas, "product").getId().toString(), linea.productoId().toString());
        valores.put(campo(ventas, "quantity").getId().toString(), linea.cantidad());
        valores.put(campo(ventas, "price").getId().toString(), linea.precio());
        valores.put(campo(ventas, "date").getId().toString(),
                ZonaNegocio.dia(ahora).toString());
        Registro fila = Registro.nuevo(sistema.proyecto().getNegocioId(), ventas.getId(),
                Registro.validar(ventas.getCampos(), valores), ahora);
        fila.setId(RegistroId.nuevo());
        fila.setOrden(orden);
        fila.setOrigen("Caja " + request.id());
        return fila;
    }

    private Bloque obtenerBloque(Sistema sistema, BloqueId id, TipoBloque tipo) {
        return sistema.proyecto().getBloques().stream()
                .filter(bloque -> bloque.getId().equals(id) && bloque.getTipo() == tipo).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Tabla de caja no encontrada"));
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
