package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Recomendación del motor de monitoreo sobre una entidad del negocio, con su motivo y las acciones sugeridas. */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Aviso {

    /** Desde esta prioridad el aviso se considera urgente (3 urgente, 2 para hoy, 1 para mirar). */
    public static final short PRIORIDAD_URGENTE = 2;

    private AvisoId id;

    private NegocioId negocioId;

    private ProyectoId proyectoId;

    private TipoAviso tipo;

    private GrupoAviso grupo;

    private Short prioridad;

    /** Entidad que afecta, con la forma {@code registro:<uuid>}. */
    private String entidad;

    /** Identificador estable: depende del estado de los datos que dispararon el aviso. */
    private String huella;

    private Map<String, Object> datos;

    private String titulo;

    private String detalle;

    private List<Object> motivos;

    private List<Object> acciones;

    @Builder.Default
    private EstadoAviso estado = EstadoAviso.PENDIENTE;

    private OffsetDateTime resueltoEn;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    public static Aviso nuevo(NegocioId negocioId, TipoAviso tipo, ResultadoRegla resultado, OffsetDateTime ahora) {
        Aviso aviso = Aviso.builder()
                .negocioId(negocioId)
                .tipo(tipo)
                .grupo(tipo.grupo())
                .creadoEn(ahora)
                .build();
        aviso.copiarDe(resultado, ahora);
        return aviso;
    }

    public boolean estaPendiente() {
        return estado == EstadoAviso.PENDIENTE;
    }

    public boolean esUrgente() {
        return prioridad >= PRIORIDAD_URGENTE;
    }

    /** Vuelve a leer el aviso con los datos de la corrida actual; si había vencido, queda pendiente otra vez. */
    public void actualizarCon(ResultadoRegla resultado, OffsetDateTime ahora) {
        if (estado == EstadoAviso.APLICADO || estado == EstadoAviso.DESCARTADO) {
            throw new BusinessRuleException("El aviso ya fue resuelto");
        }
        copiarDe(resultado, ahora);
        estado = EstadoAviso.PENDIENTE;
        resueltoEn = null;
    }

    /**
     * Si todavía se puede medir qué pasó después de aplicarlo: lo aplicó la persona sobre un producto, de un tipo cuyo
     * efecto se ve en las ventas, y aún no se midió.
     */
    public boolean esMedible() {
        return estado == EstadoAviso.APLICADO
                && resueltoEn != null
                && (tipo == TipoAviso.ESTANCADO || tipo == TipoAviso.STOCK)
                && ResultadoRegla.registroDe(entidad).isPresent()
                && impacto().isEmpty();
    }

    /** El producto sobre el que avisó, si la entidad es un registro. */
    public Optional<RegistroId> productoId() {
        return ResultadoRegla.registroDe(entidad);
    }

    public Optional<ImpactoAviso> impacto() {
        return ImpactoAviso.de(datos);
    }

    /** Guarda el impacto medido junto a los datos del aviso; no cambia su estado ni su huella. */
    public void registrarImpacto(ImpactoAviso impacto, OffsetDateTime ahora) {
        Map<String, Object> conImpacto = new LinkedHashMap<>(datos == null ? Map.of() : datos);
        conImpacto.put(ImpactoAviso.CLAVE, impacto.aMapa());
        datos = conImpacto;
        actualizadoEn = ahora;
    }

    public void aplicar(OffsetDateTime ahora) {
        resolver(EstadoAviso.APLICADO, ahora);
    }

    public void descartar(OffsetDateTime ahora) {
        resolver(EstadoAviso.DESCARTADO, ahora);
    }

    /** El motor dejó de detectarlo: ya no aplica y sale de la lista. */
    public void vencer(OffsetDateTime ahora) {
        exigirPendiente();
        estado = EstadoAviso.VENCIDO;
        actualizadoEn = ahora;
    }

    private void resolver(EstadoAviso nuevoEstado, OffsetDateTime ahora) {
        exigirPendiente();
        estado = nuevoEstado;
        resueltoEn = ahora;
        actualizadoEn = ahora;
    }

    private void exigirPendiente() {
        if (!estaPendiente()) {
            throw new BusinessRuleException("El aviso ya fue resuelto");
        }
    }

    private void copiarDe(ResultadoRegla resultado, OffsetDateTime ahora) {
        proyectoId = resultado.proyectoId();
        prioridad = resultado.prioridad();
        entidad = resultado.entidad();
        huella = resultado.huellaPara(tipo);
        datos = resultado.datos();
        titulo = resultado.titulo();
        detalle = resultado.detalle();
        motivos = resultado.motivos();
        acciones = resultado.acciones();
        actualizadoEn = ahora;
    }
}
