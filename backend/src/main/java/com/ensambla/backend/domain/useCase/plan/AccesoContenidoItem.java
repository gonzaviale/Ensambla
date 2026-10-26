package com.ensambla.backend.domain.useCase.plan;

import java.time.OffsetDateTime;
import java.util.List;

public record AccesoContenidoItem(List<String> autorizadas, int usadas, Integer limite,
                                 int total, int bloqueadas, OffsetDateTime finPeriodo) {
}
