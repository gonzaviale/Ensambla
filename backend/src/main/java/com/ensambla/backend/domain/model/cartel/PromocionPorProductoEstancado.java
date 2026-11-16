package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Un producto caro que no se mueve: el aviso ya calculó a cuánto conviene rebajarlo. Se promociona con el precio de
 * antes tachado y el nuevo.
 */
public class PromocionPorProductoEstancado implements FuenteDePromocion {

    private static final String DATO_PRECIO = "precio";
    private static final String DATO_SUGERIDO = "sugerido";
    private static final String SUFIJO_DEL_TITULO = " está caro y no se mueve";

    @Override
    public TipoAviso tipoDeAviso() {
        return TipoAviso.ESTANCADO;
    }

    @Override
    public Optional<PromocionSugerida> de(Aviso aviso) {
        return numero(aviso, DATO_PRECIO).flatMap(precio -> numero(aviso, DATO_SUGERIDO)
                .filter(sugerido -> sugerido.compareTo(precio) < 0)
                .map(sugerido -> new PromocionSugerida(
                        TipoDeCartel.PROMO_PRECIO, List.of(nombreDe(aviso)), precio, sugerido)));
    }

    private Optional<BigDecimal> numero(Aviso aviso, String dato) {
        return Optional.ofNullable(aviso.getDatos().get(dato)).map(valor -> new BigDecimal(valor.toString()));
    }

    private String nombreDe(Aviso aviso) {
        return aviso.getTitulo().replace(SUFIJO_DEL_TITULO, "");
    }
}
