package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** La orden de compra que un cliente confirmó por WhatsApp y que el dueño del comercio gestiona desde la app. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompra {

    private OrdenCompraId id;

    private NegocioId negocioId;

    private ClienteId clienteId;

    @Builder.Default
    private EstadoOrdenCompra estado = EstadoOrdenCompra.PENDIENTE;

    private List<ItemDeOrden> items;

    private BigDecimal total;

    /** Por qué el comercio la rechazó; vacío si no está rechazada. */
    private String motivoRechazo;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    /** La orden nueva de lo que el cliente confirmó: pendiente de que el dueño la acepte. */
    public static OrdenCompra pendiente(NegocioId negocioId, ClienteId clienteId, List<LineaDeCompra> lineas,
                                        OffsetDateTime ahora) {
        return OrdenCompra.builder()
                .negocioId(negocioId)
                .clienteId(clienteId)
                .items(lineas.stream().map(ItemDeOrden::de).toList())
                .total(ArmadoDeLista.total(lineas))
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }
}
