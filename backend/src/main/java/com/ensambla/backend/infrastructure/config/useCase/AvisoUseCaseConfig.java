package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.aviso.ReglaAviso;
import com.ensambla.backend.domain.model.aviso.ReglaComboBajaRotacion;
import com.ensambla.backend.domain.model.aviso.ReglaPrecioZona;
import com.ensambla.backend.domain.model.aviso.ReglaProductoAltaRotacion;
import com.ensambla.backend.domain.model.aviso.ReglaProductoEstancado;
import com.ensambla.backend.domain.model.aviso.ReglaRevisarPrecioTrasAumento;
import com.ensambla.backend.domain.model.aviso.ReglaStockBajo;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.precio.HistorialPrecioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.aviso.aplicarAviso.AplicarAviso;
import com.ensambla.backend.domain.useCase.aviso.contarAvisos.ContarAvisos;
import com.ensambla.backend.domain.useCase.aviso.descartarAviso.DescartarAviso;
import com.ensambla.backend.domain.useCase.aviso.ejecutarMotorAvisos.EjecutarMotorAvisos;
import com.ensambla.backend.domain.useCase.aviso.listarAvisos.ListarAvisos;
import com.ensambla.backend.domain.useCase.aviso.medirImpactoAvisos.MedirImpactoAvisos;
import com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos.ObtenerImpactoAvisos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

@Configuration
public class AvisoUseCaseConfig {

    @Bean
    public EjecutarMotorAvisos ejecutarMotorAvisos(
            NegocioRepository negocioRepository,
            List<ReglaAviso> reglas,
            ProyectoRepository proyectoRepository,
            RegistroRepository registroRepository,
            ZonaRepository zonaRepository,
            AgregadoZonaRepository agregadoZonaRepository,
            HistorialPrecioRepository historialPrecioRepository,
            MetricasVentas metricasVentas,
            AvisoRepository avisoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new EjecutarMotorAvisos(negocioRepository, reglas, proyectoRepository, registroRepository,
                zonaRepository, agregadoZonaRepository, historialPrecioRepository, metricasVentas, avisoRepository,
                transaccionPort, clock);
    }

    @Bean
    public ListarAvisos listarAvisos(
            NegocioRepository negocioRepository,
            AvisoRepository avisoRepository,
            ControlPlanService controlPlanService, Clock clock) {
        return new ListarAvisos(negocioRepository, avisoRepository, controlPlanService, clock);
    }

    @Bean
    public ContarAvisos contarAvisos(NegocioRepository negocioRepository, AvisoRepository avisoRepository) {
        return new ContarAvisos(negocioRepository, avisoRepository);
    }

    @Bean
    public AplicarAviso aplicarAviso(
            NegocioRepository negocioRepository,
            ControlPlanService controlPlanService,
            RegistroDatosService registroDatosService,
            RegistroRepository registroRepository,
            CampoRepository campoRepository,
            AvisoRepository avisoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new AplicarAviso(negocioRepository, controlPlanService, registroDatosService, registroRepository,
                campoRepository, avisoRepository, transaccionPort, clock);
    }

    @Bean
    public DescartarAviso descartarAviso(
            NegocioRepository negocioRepository,
            ControlPlanService controlPlanService,
            AvisoRepository avisoRepository,
            TransaccionPort transaccionPort,
            Clock clock) {
        return new DescartarAviso(
                negocioRepository, controlPlanService, avisoRepository, transaccionPort, clock);
    }

    /** La ventana (días antes y días después del aviso) sale de {@code ensambla.avisos.impacto.ventana-dias}. */
    @Bean
    public MedirImpactoAvisos medirImpactoAvisos(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository,
            RegistroRepository registroRepository,
            AvisoRepository avisoRepository,
            MetricasVentas metricasVentas,
            TransaccionPort transaccionPort,
            Clock clock,
            @Value("${ensambla.avisos.impacto.ventana-dias:14}") int ventanaDias) {
        return new MedirImpactoAvisos(negocioRepository, proyectoRepository, registroRepository, avisoRepository,
                metricasVentas, transaccionPort, clock, ventanaDias);
    }

    @Bean
    public ObtenerImpactoAvisos obtenerImpactoAvisos(
            NegocioRepository negocioRepository, AvisoRepository avisoRepository) {
        return new ObtenerImpactoAvisos(negocioRepository, avisoRepository);
    }

    @Bean
    public ReglaStockBajo reglaStockBajo() {
        return new ReglaStockBajo();
    }

    /** El período y el factor de baja rotación salen de {@code ensambla.avisos.*}. */
    @Bean
    public MetricasVentas metricasVentas(
            @Value("${ensambla.avisos.periodo-dias:30}") int periodoDias,
            @Value("${ensambla.avisos.baja-rotacion-factor:0.5}") BigDecimal factorBajaRotacion) {
        return new MetricasVentas(periodoDias, factorBajaRotacion);
    }

    @Bean
    public ReglaPrecioZona reglaPrecioZona() {
        return new ReglaPrecioZona();
    }

    /** La frecuencia mínima del par y el descuento salen de {@code ensambla.avisos.combo.*}. */
    @Bean
    public ReglaComboBajaRotacion reglaComboBajaRotacion(
            MetricasVentas metricasVentas,
            @Value("${ensambla.avisos.combo.frecuencia-minima:3}") int frecuenciaMinima,
            @Value("${ensambla.avisos.combo.descuento:0.1}") BigDecimal descuento) {
        return new ReglaComboBajaRotacion(metricasVentas, frecuenciaMinima, descuento);
    }

    @Bean
    public ReglaProductoAltaRotacion reglaProductoAltaRotacion(MetricasVentas metricasVentas) {
        return new ReglaProductoAltaRotacion(metricasVentas);
    }

    /** La ventana (días antes y después del aumento) y la caída mínima salen de {@code ensambla.avisos.revision-precio.*}. */
    @Bean
    public ReglaRevisarPrecioTrasAumento reglaRevisarPrecioTrasAumento(
            MetricasVentas metricasVentas,
            @Value("${ensambla.avisos.revision-precio.ventana-dias:7}") int ventanaDias,
            @Value("${ensambla.avisos.revision-precio.caida-minima:0.2}") BigDecimal caidaMinima) {
        return new ReglaRevisarPrecioTrasAumento(metricasVentas, ventanaDias, caidaMinima);
    }

    @Bean
    public ReglaProductoEstancado reglaProductoEstancado(MetricasVentas metricasVentas) {
        return new ReglaProductoEstancado(metricasVentas);
    }
}
