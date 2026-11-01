package com.ensambla.backend.domain.useCase.aviso.medirImpactoAvisos;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mide qué pasó con las ventas de un producto después de que la persona aplicó un aviso: compara las unidades y el
 * ingreso por día de los días anteriores con los de los días posteriores y lo guarda en el aviso. Mide una sola vez,
 * cuando ya pasó la ventana completa. Cada negocio va en su propia transacción y uno con error no frena a los demás.
 */
@Slf4j
@RequiredArgsConstructor
public class MedirImpactoAvisos {

    private static final ZoneId ZONA_NEGOCIO = ZonaNegocio.ZONA;

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final RegistroRepository registroRepository;

    private final AvisoRepository avisoRepository;

    private final MetricasVentas metricasVentas;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    private final int ventanaDias;

    public void execute() {
        for (NegocioId negocioId : negocioRepository.findAllIds()) {
            medirNegocio(negocioId);
        }
    }

    private void medirNegocio(NegocioId negocioId) {
        try {
            OffsetDateTime ahora = OffsetDateTime.now(clock);
            transaccionPort.ejecutar(() -> medir(negocioId, ahora));
        } catch (RuntimeException e) {
            log.warn("No se pudo medir el impacto de los avisos del negocio {}", negocioId, e);
        }
    }

    private void medir(NegocioId negocioId, OffsetDateTime ahora) {
        List<Aviso> medibles = avisoRepository.findByNegocioIdAndEstado(negocioId, EstadoAviso.APLICADO).stream()
                .filter(Aviso::esMedible)
                .toList();
        if (medibles.isEmpty()) {
            return;
        }
        LocalDate hoy = diaDe(ahora);
        List<VentaProducto> ventas = metricasVentas.ventasValidas(ventasDe(negocioId), hoy);
        medibles.forEach(aviso -> medirAviso(aviso, ventas, hoy, ahora));
    }

    private void medirAviso(Aviso aviso, List<VentaProducto> ventas, LocalDate hoy, OffsetDateTime ahora) {
        RegistroId productoId = aviso.productoId().orElseThrow();
        List<VentaProducto> delProducto = ventas.stream()
                .filter(venta -> venta.productoId().equals(productoId))
                .toList();
        Optional<ImpactoAviso> impacto = ImpactoAviso.medir(delProducto, diaDe(aviso.getResueltoEn()), hoy, ventanaDias);
        impacto.ifPresent(medido -> {
            aviso.registrarImpacto(medido, ahora);
            avisoRepository.save(aviso);
        });
    }

    /** El día según el reloj del negocio: las ventas se anotan con fecha argentina. */
    private static LocalDate diaDe(OffsetDateTime instante) {
        return instante.atZoneSameInstant(ZONA_NEGOCIO).toLocalDate();
    }

    private List<BloqueVentas> ventasDe(NegocioId negocioId) {
        return proyectoRepository.findAllByNegocioId(negocioId).stream()
                .map(proyecto -> proyectoRepository.findByIdAndNegocioId(proyecto.getId(), negocioId))
                .flatMap(Optional::stream)
                .flatMap(proyecto -> ventasDe(proyecto, negocioId).stream())
                .toList();
    }

    private List<BloqueVentas> ventasDe(Proyecto proyecto, NegocioId negocioId) {
        return proyecto.getBloques().stream()
                .filter(bloque -> bloque.getTipo() == TipoBloque.VENTAS)
                .map(bloque -> new BloqueVentas(bloque, camposPorClave(bloque), registrosReales(bloque, negocioId)))
                .toList();
    }

    /** Las filas de ejemplo de la plantilla no son ventas del negocio. */
    private List<Registro> registrosReales(Bloque bloque, NegocioId negocioId) {
        return registroRepository.findAllByBloqueIdAndNegocioId(bloque.getId(), negocioId).stream()
                .filter(registro -> !registro.esDeEjemplo())
                .toList();
    }

    private static Map<String, Campo> camposPorClave(Bloque bloque) {
        Map<String, Campo> porClave = new LinkedHashMap<>();
        bloque.getCampos().stream()
                .filter(campo -> Objects.nonNull(campo.getClave()))
                .forEach(campo -> porClave.put(campo.getClave(), campo));
        return porClave;
    }
}
