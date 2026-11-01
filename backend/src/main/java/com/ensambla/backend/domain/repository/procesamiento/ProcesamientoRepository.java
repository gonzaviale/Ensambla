package com.ensambla.backend.domain.repository.procesamiento;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;
import com.ensambla.backend.domain.model.procesamiento.FotoLectura;
import com.ensambla.backend.domain.model.procesamiento.LoteTrabajo;
import com.ensambla.backend.domain.model.procesamiento.ResultadoIntento;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ProcesamientoRepository {

    Optional<LoteTrabajo> reclamar(OffsetDateTime ahora, OffsetDateTime vence);

    List<LoteTrabajo> vencidos(OffsetDateTime ahora);

    List<FotoLectura> fotos(LoteTrabajo lote);

    List<BloqueLectura> bloques(NegocioId negocioId);

    /** Las tablas del negocio donde se puede cargar datos (con al menos una columna que no sea una fórmula). */
    List<TablaDisponible> tablas(NegocioId negocioId);

    boolean reservar(LoteTrabajo lote, int paginas, int limite, OffsetDateTime ahora);

    void finalizar(LoteTrabajo lote, ResultadoIntento resultado, BigDecimal costo,
                   OffsetDateTime ahora, OffsetDateTime reintentarEn);

    Optional<EstadoLectura> obtener(NegocioId negocioId, LoteId loteId);

    boolean reencolar(NegocioId negocioId, LoteId loteId, OffsetDateTime ahora);

    ConsumoDiario consumo(NegocioId negocioId, LocalDate fecha);

    /** El estado de lectura del lote del negocio; uno ajeno responde como inexistente. */
    default EstadoLectura obtenerEstado(NegocioId negocioId, LoteId loteId) {
        return obtener(negocioId, loteId).orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado"));
    }
}
