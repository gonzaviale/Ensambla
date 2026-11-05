package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.promocion.CanalEstadoWhatsapp;
import com.ensambla.backend.domain.model.promocion.CanalInstagram;
import com.ensambla.backend.domain.model.promocion.CanalMensajeAClientes;
import com.ensambla.backend.domain.model.promocion.CanalPromocion;
import com.ensambla.backend.domain.model.promocion.CatalogoDeCanales;
import com.ensambla.backend.domain.model.promocion.PromocionLiquidar;
import com.ensambla.backend.domain.model.promocion.PromocionProductoEstrella;
import com.ensambla.backend.domain.model.promocion.ReglaPromocion;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocion;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.SugerirPromociones;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.Clock;
import java.util.List;

@Configuration
public class PromocionUseCaseConfig {

    @Bean
    @Order(1)
    public ReglaPromocion promocionProductoEstrella() {
        return new PromocionProductoEstrella();
    }

    @Bean
    @Order(2)
    public ReglaPromocion promocionLiquidar() {
        return new PromocionLiquidar();
    }

    @Bean
    @Order(1)
    public CanalPromocion canalInstagram() {
        return new CanalInstagram();
    }

    @Bean
    @Order(2)
    public CanalPromocion canalEstadoWhatsapp() {
        return new CanalEstadoWhatsapp();
    }

    @Bean
    @Order(3)
    public CanalPromocion canalMensajeAClientes() {
        return new CanalMensajeAClientes();
    }

    /** El catálogo recibe todos los canales que haya registrados: para sumar uno alcanza con un bean nuevo. */
    @Bean
    public CatalogoDeCanales catalogoDeCanales(List<CanalPromocion> canales) {
        return new CatalogoDeCanales(canales);
    }

    @Bean
    public SugerirPromociones sugerirPromociones(
            NegocioRepository negocioRepository,
            ProyectoRepository proyectoRepository,
            DatosDeProyectoService datosDeProyectoService,
            MetricasVentas metricasVentas,
            List<ReglaPromocion> reglas,
            CatalogoDeCanales catalogoDeCanales,
            Clock clock) {
        return new SugerirPromociones(negocioRepository, proyectoRepository, datosDeProyectoService, metricasVentas,
                reglas, catalogoDeCanales, clock);
    }

    @Bean
    public RedactarPromocion redactarPromocion(
            NegocioRepository negocioRepository,
            CatalogoDeCanales catalogoDeCanales) {
        return new RedactarPromocion(negocioRepository, catalogoDeCanales);
    }
}
