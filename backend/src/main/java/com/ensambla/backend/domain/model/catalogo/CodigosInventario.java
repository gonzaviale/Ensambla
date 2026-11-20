package com.ensambla.backend.domain.model.catalogo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.registro.Registro;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CodigosInventario {

    private CodigosInventario() {
    }

    public static void validar(Bloque bloque, List<Registro> registros) {
        if (bloque.getTipo() != TipoBloque.INVENTARIO) {
            return;
        }
        bloque.getCampos().stream().filter(campo -> "barcode".equals(campo.getClave())).forEach(campo -> {
            if (campo.getTipo() != TipoCampo.TEXTO) {
                throw new BusinessRuleException("El código de barras debe guardarse como texto.");
            }
            Set<String> codigos = new HashSet<>();
            for (Registro registro : registros) {
                Object valor = registro.getValores().get(campo.getId().toString());
                if (valor == null || "".equals(valor)) {
                    continue;
                }
                if (!(valor instanceof String texto)) {
                    throw new BusinessRuleException("El código de barras debe ser texto, conservando los ceros.");
                }
                if (!codigos.add(new CodigoBarras(texto).identidad())) {
                    throw new DuplicateResourceException("Este código ya está cargado en el inventario.");
                }
                Registro.validar(bloque.getCampos(), registro.getValores());
            }
        });
    }
}
