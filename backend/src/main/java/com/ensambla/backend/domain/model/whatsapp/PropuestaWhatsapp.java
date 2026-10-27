package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/** Carga que Sami armó desde un texto o una foto y que sólo se guarda cuando la persona la confirma en el chat. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PropuestaWhatsapp {

    public static final Duration VIGENCIA = Duration.ofMinutes(30);

    private PropuestaWhatsappId id;

    private NegocioId negocioId;

    private String telefono;

    private OrigenPropuestaWhatsapp origen;

    private EstadoPropuestaWhatsapp estado;

    /** La tabla destino; null mientras la persona todavía tiene que elegirla. */
    private BloqueId bloqueId;

    /** El mensaje original, para interpretarlo de nuevo cuando elige la tabla. */
    private String texto;

    /** Lo leído de la foto, para asignarlo a la tabla elegida sin volver a llamar a la IA. */
    private LecturaCuaderno lectura;

    /** Las tablas ofrecidas, en el orden de la lista numerada. */
    @Builder.Default
    private List<BloqueId> opciones = List.of();

    @Builder.Default
    private List<FilaPropuesta> filas = List.of();

    private OffsetDateTime creadoEn;

    private OffsetDateTime expiraEn;

    private OffsetDateTime cerradoEn;

    public static PropuestaWhatsapp eligiendoTabla(NegocioId negocioId, String telefono, OrigenPropuestaWhatsapp origen,
                                                   String texto, LecturaCuaderno lectura, List<BloqueId> opciones,
                                                   OffsetDateTime ahora) {
        return PropuestaWhatsapp.builder()
                .negocioId(negocioId)
                .telefono(telefono)
                .origen(origen)
                .estado(EstadoPropuestaWhatsapp.ELIGIENDO_TABLA)
                .texto(texto)
                .lectura(lectura)
                .opciones(opciones)
                .creadoEn(ahora)
                .expiraEn(ahora.plus(VIGENCIA))
                .build();
    }

    public static PropuestaWhatsapp pendiente(NegocioId negocioId, String telefono, OrigenPropuestaWhatsapp origen,
                                              String texto, BloqueId bloqueId, List<FilaPropuesta> filas,
                                              OffsetDateTime ahora) {
        return PropuestaWhatsapp.builder()
                .negocioId(negocioId)
                .telefono(telefono)
                .origen(origen)
                .estado(EstadoPropuestaWhatsapp.PENDIENTE)
                .texto(texto)
                .bloqueId(bloqueId)
                .filas(filas)
                .creadoEn(ahora)
                .expiraEn(ahora.plus(VIGENCIA))
                .build();
    }

    public boolean estaAbierta() {
        return estado == EstadoPropuestaWhatsapp.ELIGIENDO_TABLA || estado == EstadoPropuestaWhatsapp.PENDIENTE;
    }

    public boolean estaVigente(OffsetDateTime ahora) {
        return estaAbierta() && ahora.isBefore(expiraEn);
    }

    /** La tabla que corresponde al número de la lista que respondió la persona (empieza en 1). */
    public BloqueId elegir(int numero) {
        if (estado != EstadoPropuestaWhatsapp.ELIGIENDO_TABLA || numero < 1 || numero > opciones.size()) {
            throw new BusinessRuleException("Elegí un número de la lista");
        }
        return opciones.get(numero - 1);
    }

    /** Con la tabla ya elegida, la carga queda a la espera del sí o el no. */
    public void aPendiente(BloqueId destino, List<FilaPropuesta> filasArmadas) {
        estado = EstadoPropuestaWhatsapp.PENDIENTE;
        bloqueId = destino;
        filas = filasArmadas;
    }

    /** Sólo una carga con la tabla ya elegida se puede confirmar. */
    public void exigirPendiente() {
        if (estado != EstadoPropuestaWhatsapp.PENDIENTE) {
            throw new BusinessRuleException("Primero elegí la tabla respondiendo con su número");
        }
    }

    public void confirmar(OffsetDateTime ahora) {
        exigirPendiente();
        cerrar(EstadoPropuestaWhatsapp.CONFIRMADA, ahora);
    }

    public void descartar(OffsetDateTime ahora) {
        cerrar(EstadoPropuestaWhatsapp.DESCARTADA, ahora);
    }

    public void vencer(OffsetDateTime ahora) {
        cerrar(EstadoPropuestaWhatsapp.VENCIDA, ahora);
    }

    private void cerrar(EstadoPropuestaWhatsapp nuevoEstado, OffsetDateTime ahora) {
        estado = nuevoEstado;
        cerradoEn = ahora;
    }
}
