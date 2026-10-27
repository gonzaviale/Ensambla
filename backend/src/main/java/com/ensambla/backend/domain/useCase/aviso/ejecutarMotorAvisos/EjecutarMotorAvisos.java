package com.ensambla.backend.domain.useCase.aviso.ejecutarMotorAvisos;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.ContextoNegocio;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ReglaAviso;
import com.ensambla.backend.domain.model.aviso.ResultadoRegla;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Corre todas las reglas registradas sobre cada negocio y deja sus avisos al día: crea los nuevos, actualiza los que
 * cambiaron, vence los que ya no aplican y no vuelve a crear uno descartado o aplicado mientras sus datos de origen
 * (su huella) sigan iguales. Cada negocio va en su propia transacción y uno con error no frena a los demás. Una regla
 * nueva se suma registrándola como bean, sin tocar el motor.
 */
@Slf4j
@RequiredArgsConstructor
public class EjecutarMotorAvisos {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Argentina/Buenos_Aires");

    private final NegocioRepository negocioRepository;

    private final List<ReglaAviso> reglas;

    private final ProyectoRepository proyectoRepository;

    private final RegistroRepository registroRepository;

    private final ZonaRepository zonaRepository;

    private final AgregadoZonaRepository agregadoZonaRepository;

    private final AvisoRepository avisoRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public void execute() {
        for (NegocioId negocioId : negocioRepository.findAllIds()) {
            ejecutarNegocio(negocioId);
        }
    }

    private void ejecutarNegocio(NegocioId negocioId) {
        try {
            OffsetDateTime ahora = OffsetDateTime.now(clock);
            transaccionPort.ejecutar(() -> {
                ContextoNegocio contexto = armarContexto(negocioId, hoyEn(ahora));
                reglas.forEach(regla -> sincronizar(negocioId, regla.tipo(), regla.evaluar(contexto), ahora));
            });
        } catch (RuntimeException e) {
            log.warn("El motor de avisos falló en el negocio {}", negocioId, e);
        }
    }

    /** El día de hoy según el reloj del negocio: las ventas se anotan con fecha argentina. */
    private static LocalDate hoyEn(OffsetDateTime ahora) {
        return ahora.atZoneSameInstant(ZONA_NEGOCIO).toLocalDate();
    }

    // --- contexto: las tablas de inventario y de ventas del negocio con sus campos y registros, y su zona ---

    private ContextoNegocio armarContexto(NegocioId negocioId, LocalDate hoy) {
        List<Proyecto> proyectos = proyectoRepository.findAllByNegocioId(negocioId).stream()
                .map(proyecto -> proyectoRepository.findByIdAndNegocioId(proyecto.getId(), negocioId))
                .flatMap(Optional::stream)
                .toList();
        List<BloqueInventario> inventarios = proyectos.stream()
                .flatMap(proyecto -> inventariosDe(proyecto, negocioId).stream())
                .toList();
        List<BloqueVentas> ventas = proyectos.stream()
                .flatMap(proyecto -> ventasDe(proyecto, negocioId).stream())
                .toList();
        return new ContextoNegocio(negocioId, hoy, inventarios, ventas, zonaDe(negocioId));
    }

    /** Los agregados de la zona del negocio, o una zona vacía si todavía no tiene una asignada. */
    private AnalisisZona zonaDe(NegocioId negocioId) {
        return negocioRepository.findZonaIdById(negocioId)
                .flatMap(zonaRepository::findById)
                .map(agregadoZonaRepository::analizar)
                .orElseGet(AnalisisZona::sinZona);
    }

    private List<BloqueInventario> inventariosDe(Proyecto proyecto, NegocioId negocioId) {
        BloqueId entradasId = proyecto.getBloques().stream()
                .filter(bloque -> bloque.getTipo() == TipoBloque.ENTRADAS)
                .map(Bloque::getId)
                .findFirst()
                .orElse(null);
        return proyecto.getBloques().stream()
                .filter(bloque -> bloque.getTipo() == TipoBloque.INVENTARIO)
                .map(bloque -> new BloqueInventario(
                        bloque,
                        entradasId,
                        camposPorClave(bloque),
                        registrosReales(bloque, negocioId)))
                .toList();
    }

    private List<BloqueVentas> ventasDe(Proyecto proyecto, NegocioId negocioId) {
        return proyecto.getBloques().stream()
                .filter(bloque -> bloque.getTipo() == TipoBloque.VENTAS)
                .map(bloque -> new BloqueVentas(bloque, camposPorClave(bloque), registrosReales(bloque, negocioId)))
                .toList();
    }

    /** Las filas de ejemplo de la plantilla no son datos del negocio: ninguna regla avisa sobre ellas. */
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

    // --- sincronización de los avisos de una regla ---

    private void sincronizar(NegocioId negocioId, TipoAviso tipo, List<ResultadoRegla> resultados,
            OffsetDateTime ahora) {
        List<Aviso> pendientes = avisoRepository.findByNegocioIdAndTipoAndEstado(
                negocioId, tipo, EstadoAviso.PENDIENTE);
        Set<AvisoId> vigentes = new HashSet<>();
        for (ResultadoRegla resultado : resultados) {
            Optional<Aviso> existente = avisoRepository.findByNegocioIdAndHuella(
                    negocioId, resultado.huellaPara(tipo));
            if (existente.isPresent()) {
                if (existente.get().estaPendiente() || existente.get().getEstado() == EstadoAviso.VENCIDO) {
                    vencerOtrosPendientesDe(resultado.entidad(), existente.get(), pendientes, ahora);
                    vigentes.add(avisoRepository.save(reactivar(existente.get(), resultado, ahora)).getId());
                }
                continue;
            }
            Aviso aviso = pendienteDe(resultado.entidad(), pendientes)
                    .map(pendiente -> reactivar(pendiente, resultado, ahora))
                    .orElseGet(() -> Aviso.nuevo(negocioId, tipo, resultado, ahora));
            vigentes.add(avisoRepository.save(aviso).getId());
        }
        pendientes.stream()
                .filter(pendiente -> !vigentes.contains(pendiente.getId()))
                .filter(Aviso::estaPendiente)
                .forEach(pendiente -> avisoRepository.save(vencida(pendiente, ahora)));
    }

    private void vencerOtrosPendientesDe(String entidad, Aviso conservado, List<Aviso> pendientes,
                                         OffsetDateTime ahora) {
        pendientes.stream()
                .filter(pendiente -> pendiente.getEntidad().equals(entidad))
                .filter(pendiente -> !pendiente.getId().equals(conservado.getId()))
                .forEach(pendiente -> avisoRepository.save(vencida(pendiente, ahora)));
    }

    private static Optional<Aviso> pendienteDe(String entidad, List<Aviso> pendientes) {
        return pendientes.stream().filter(pendiente -> pendiente.getEntidad().equals(entidad)).findFirst();
    }

    private static Aviso reactivar(Aviso aviso, ResultadoRegla resultado, OffsetDateTime ahora) {
        aviso.actualizarCon(resultado, ahora);
        return aviso;
    }

    private static Aviso vencida(Aviso aviso, OffsetDateTime ahora) {
        aviso.vencer(ahora);
        return aviso;
    }
}
