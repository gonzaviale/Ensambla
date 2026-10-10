package com.ensambla.backend.domain.dto.procesamiento;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PropuestaItem(String tipoRegistro, String nombre, UUID bloqueExistenteId, List<CampoItem> campos,
                           List<FilaItem> filas, List<String> advertencias, Map<String, UUID> mapeoCampos,
                           ConsumoItem consumo) {

    public record CampoItem(String clave, String nombre, String tipo, List<String> opciones) {
    }

    public record FilaItem(int pagina, int renglon, Map<String, DatoItem> datos) {
    }

    public record DatoItem(String valor, double confianza) {
    }

    public record ConsumoItem(String proveedor, String modelo, String versionPrompt, int paginas,
                              int tokensEntrada, int tokensSalida, long duracionMs) {
    }
}
