package com.ensambla.backend.domain.useCase.caja.cobrarVenta;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import com.ensambla.backend.domain.model.caja.LineaVentaCaja;
import com.ensambla.backend.domain.exception.BusinessRuleException;

public record CobrarVentaRequest(UsuarioId usuarioId, VentaCajaId id, ProyectoId proyectoId,
                                 BloqueId inventarioId, BloqueId ventasId, List<LineaRequest> lineas,
                                 String medio, BigDecimal recibido, List<PagoRequest> pagos, RegistroId clienteId) {

    public CobrarVentaRequest(UsuarioId usuarioId, VentaCajaId id, ProyectoId proyectoId,
            BloqueId inventarioId, BloqueId ventasId, List<LineaRequest> lineas, String medio,
            BigDecimal recibido, List<PagoRequest> pagos) {
        this(usuarioId, id, proyectoId, inventarioId, ventasId, lineas, medio, recibido, pagos, null);
    }

    public CobrarVentaRequest(UsuarioId usuarioId, VentaCajaId id, ProyectoId proyectoId,
            BloqueId inventarioId, BloqueId ventasId, List<LineaRequest> lineas, String medio, BigDecimal recibido) {
        this(usuarioId, id, proyectoId, inventarioId, ventasId, lineas, medio, recibido, null);
    }

    public record PagoRequest(String medio, BigDecimal importe) {
    }

    public record LineaRequest(RegistroId productoId, BigDecimal cantidad, BigDecimal precio, String modo,
                               String descripcion) {
        public LineaRequest(RegistroId productoId, BigDecimal cantidad, BigDecimal precio, String modo) {
            this(productoId, cantidad, precio, modo, null);
        }

        public LineaVentaCaja resolver(Function<LineaRequest, LineaVentaCaja> desdeInventario) {
            exigirDatosCoherentes();
            return Optional.ofNullable(productoId).map(id -> desdeInventario.apply(this))
                    .orElseGet(() -> LineaVentaCaja.importeLibre(descripcion, cantidad, precio));
        }

        private void exigirDatosCoherentes() {
            if (productoId == null && !"UNIDAD".equals(modo)) {
                throw new BusinessRuleException("El importe libre se registra por monto, sin peso");
            }
            if (productoId != null && descripcion != null) {
                throw new BusinessRuleException("Elegí un artículo o escribí un importe libre");
            }
        }

        @Override
        public String toString() {
            String anterior = "LineaRequest[productoId=" + productoId + ", cantidad=" + cantidad
                    + ", precio=" + precio + ", modo=" + modo;
            return anterior + Optional.ofNullable(descripcion).map(texto -> ", descripcion=" + texto).orElse("") + "]";
        }
    }

    public String huella() {
        String base = proyectoId + ":" + inventarioId + ":" + ventasId + ":" + medio + ":" + recibido + ":" + lineas;
        String conPagos = pagos == null ? base : base + ":" + pagos;
        return clienteId == null ? conPagos : conPagos + ":cliente:" + clienteId;
    }
}
