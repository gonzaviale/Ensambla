package com.ensambla.backend.domain.model.preferencia;

import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Lo que la persona decidió sobre los avisos y la bienvenida; viaja con la cuenta. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciaUsuario {

    private UsuarioId usuarioId;

    /** Avisos que descartó. */
    @Builder.Default
    private List<String> descartadas = List.of();

    /** Grupos de avisos que apagó (por defecto todos están prendidos). */
    @Builder.Default
    private Map<String, Boolean> grupos = Map.of();

    @Builder.Default
    private boolean bienvenidaVista = false;

    /** Cómo quiere que Sami lo llame; si falta, se usa el nombre de la cuenta. */
    private String nombre;

    /** Si pidió que Sami le mande por WhatsApp el resumen de la mañana. */
    @Builder.Default
    private boolean avisosPorWhatsapp = false;

    /** El último día (argentino) en que se le mandó el resumen; {@code null} si nunca. */
    private LocalDate resumenWhatsappEnviadoEl;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    public static PreferenciaUsuario inicial(UsuarioId usuarioId, OffsetDateTime ahora) {
        return PreferenciaUsuario.builder()
                .usuarioId(usuarioId)
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();
    }

    public void actualizar(List<String> descartadas, Map<String, Boolean> grupos, boolean bienvenidaVista,
                           String nombre, OffsetDateTime ahora) {
        this.descartadas = List.copyOf(descartadas);
        this.grupos = Map.copyOf(grupos);
        this.bienvenidaVista = bienvenidaVista;
        this.nombre = nombre == null || nombre.isBlank() ? null : nombre.strip();
        this.actualizadoEn = ahora;
    }

    public void configurarAvisosPorWhatsapp(boolean activos, OffsetDateTime ahora) {
        this.avisosPorWhatsapp = activos;
        this.actualizadoEn = ahora;
    }

    public boolean yaRecibioElResumenDe(LocalDate dia) {
        return dia.equals(resumenWhatsappEnviadoEl);
    }

    public void registrarResumenEnviado(LocalDate dia, OffsetDateTime ahora) {
        this.resumenWhatsappEnviadoEl = dia;
        this.actualizadoEn = ahora;
    }

    /** Los grupos de avisos están prendidos salvo que la persona los haya apagado. */
    public boolean recibeAvisosDelGrupo(GrupoAviso grupo) {
        return grupos.getOrDefault(grupo.name(), true);
    }
}
