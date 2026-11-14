package com.ensambla.backend.domain.useCase.metrica.crearMetrica;

import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record CrearMetricaRequest(
        UsuarioId usuarioId,
        ProyectoId proyectoId,
        String nombre,
        FuenteMetrica fuente,
        CalculoMetrica calculo,
        AgrupacionMetrica agrupar,
        PeriodoMetrica periodo,
        boolean soloFiado,
        AutoriaMetrica autoria) {
}
