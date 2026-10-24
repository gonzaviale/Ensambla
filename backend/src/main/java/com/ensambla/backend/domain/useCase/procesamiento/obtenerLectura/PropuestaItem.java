package com.ensambla.backend.domain.useCase.procesamiento.obtenerLectura;

import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.useCase.procesamiento.ConsumoItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PropuestaItem(String tipoRegistro, String nombre, UUID bloqueExistenteId, List<CampoItem> campos,
                           List<FilaItem> filas, List<String> advertencias, Map<String, UUID> mapeoCampos,
                           List<CampoItem> camposNuevos, ConsumoItem consumo) {

    /** {@code null} si la lectura todavía no tiene propuesta. */
    static PropuestaItem de(PropuestaLectura propuesta) {
        if (propuesta == null) {
            return null;
        }
        LecturaCuaderno lectura = propuesta.lectura();
        ConsumoIa consumo = lectura.consumo();
        return new PropuestaItem(lectura.tipoRegistro().name(), lectura.bloque().nombre(),
                lectura.bloque().bloqueExistenteId(), lectura.bloque().campos().stream().map(CampoItem::de).toList(),
                lectura.filas().stream().map(FilaItem::de).toList(), lectura.advertencias(), propuesta.mapeoCampos(),
                propuesta.camposNuevos().stream().map(CampoItem::de).toList(),
                new ConsumoItem(consumo.proveedor(), consumo.modelo(), consumo.versionPrompt(),
                        consumo.paginas(), consumo.tokensEntrada(), consumo.tokensSalida(), consumo.duracionMs()));
    }

    public record CampoItem(String clave, String nombre, String tipo, List<String> opciones) {

        static CampoItem de(CampoPropuesto campo) {
            return new CampoItem(campo.clave(), campo.nombre(), campo.tipo().name(), campo.opciones());
        }
    }

    public record FilaItem(int pagina, int renglon, Map<String, DatoItem> datos) {

        static FilaItem de(FilaLeida fila) {
            Map<String, DatoItem> datos = new LinkedHashMap<>();
            fila.datos().forEach((clave, dato) -> datos.put(clave, new DatoItem(dato.valor(), dato.confianza())));
            return new FilaItem(fila.pagina(), fila.renglon(), datos);
        }
    }

    public record DatoItem(String valor, double confianza) {
    }

    public record ConsumoItem(String proveedor, String modelo, String versionPrompt, int paginas,
                              int tokensEntrada, int tokensSalida, long duracionMs) {
    }
}
