package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Dos productos que se venden juntos y alguno rota poco: el aviso ya calculó el precio del combo. Se promociona el
 * combo contra la suma de los precios sueltos.
 */
public class PromocionPorCombo implements FuenteDePromocion {

    private static final String DATO_PRECIO_A = "precioA";
    private static final String DATO_PRECIO_B = "precioB";
    private static final String DATO_SUGERIDO = "sugerido";
    private static final String SEPARADOR_DEL_TITULO = " y ";
    private static final String SUFIJO_DEL_TITULO = " se venden juntos";

    @Override
    public TipoAviso tipoDeAviso() {
        return TipoAviso.COMBO;
    }

    @Override
    public Optional<PromocionSugerida> de(Aviso aviso) {
        return numero(aviso, DATO_PRECIO_A).flatMap(precioA -> numero(aviso, DATO_PRECIO_B)
                .map(precioB -> precioA.add(precioB))
                .flatMap(suma -> numero(aviso, DATO_SUGERIDO)
                        .filter(sugerido -> sugerido.compareTo(suma) < 0)
                        .map(sugerido -> new PromocionSugerida(TipoDeCartel.COMBO, productosDe(aviso), suma, sugerido))));
    }

    private Optional<BigDecimal> numero(Aviso aviso, String dato) {
        return Optional.ofNullable(aviso.getDatos().get(dato)).map(valor -> new BigDecimal(valor.toString()));
    }

    private List<String> productosDe(Aviso aviso) {
        return Arrays.asList(aviso.getTitulo().replace(SUFIJO_DEL_TITULO, "").split(SEPARADOR_DEL_TITULO, 2));
    }
}
