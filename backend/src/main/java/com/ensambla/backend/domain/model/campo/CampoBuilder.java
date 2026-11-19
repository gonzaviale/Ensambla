package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Reúne los datos de un campo y lo arma con la clase que corresponde a su tipo. */
@Setter
@Accessors(fluent = true, chain = true)
public class CampoBuilder {

    CampoId id;

    NegocioId negocioId;

    BloqueId bloqueId;

    String nombre;

    TipoCampo tipo = TipoCampo.TEXTO;

    Boolean obligatorio;

    String valorInicial;

    List<String> opciones = List.of();

    BloqueId bloqueRelacionadoId;

    String formula;

    String clave;

    BigDecimal minimo;

    Short posicion;

    OffsetDateTime creadoEn;

    OffsetDateTime actualizadoEn;

    public Campo build() {
        return tipo.crear(this);
    }
}
