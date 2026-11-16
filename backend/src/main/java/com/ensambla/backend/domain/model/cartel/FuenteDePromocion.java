package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;

import java.util.Optional;

/** De dónde sale una sugerencia de qué promocionar: cada fuente sabe leer un tipo de aviso del motor. */
public interface FuenteDePromocion {

    /** El tipo de aviso del que lee. */
    TipoAviso tipoDeAviso();

    /** La promoción que sugiere el aviso, si tiene los datos para armarla. */
    Optional<PromocionSugerida> de(Aviso aviso);
}
