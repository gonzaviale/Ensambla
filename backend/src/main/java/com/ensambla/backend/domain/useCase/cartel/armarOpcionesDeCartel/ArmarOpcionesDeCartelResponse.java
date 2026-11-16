package com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel;

import com.ensambla.backend.domain.useCase.cartel.OpcionDeCartelItem;

import java.util.List;

public record ArmarOpcionesDeCartelResponse(List<OpcionDeCartelItem> opciones) {
}
