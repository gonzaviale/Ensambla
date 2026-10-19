package com.ensambla.backend.domain.dto.sami;

import java.util.List;

public record PreguntaSamiItem(String id, String texto, List<OpcionSamiItem> opciones) {
}
