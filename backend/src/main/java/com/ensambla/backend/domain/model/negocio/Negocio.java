package com.ensambla.backend.domain.model.negocio;

import com.ensambla.backend.domain.model.enums.RubroNegocio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Espacio del negocio: todo lo que construye el usuario cuelga de él (aislamiento de datos). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Negocio {

    private UUID id;

    private String nombre;

    private UUID duenioId;

    @Builder.Default
    private RubroNegocio rubro = RubroNegocio.OTRO;

    private String direccion;

    private String localidad;

    private BigDecimal latitud;

    private BigDecimal longitud;

    private UUID zonaId;

    /** Si es false, el negocio no aporta a los datos agregados de su zona. */
    @Builder.Default
    private Boolean comparteDatosZona = true;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;
}
