package com.ensambla.backend.domain.dto.sami;

import java.util.List;

public record ColumnaSamiItem(String nombre, String tipo, List<String> opciones) {
}
