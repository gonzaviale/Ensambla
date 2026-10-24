package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.mapper.plantilla.ListarPlantillasMapper;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.useCase.plantilla.ListarPlantillasUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlantillaUseCaseConfig {

    @Bean
    public ListarPlantillasMapper listarPlantillasMapper() {
        return new ListarPlantillasMapper();
    }

    @Bean
    public ListarPlantillasUseCase listarPlantillasUseCase(PlantillaService plantillaService,
                                                           ListarPlantillasMapper listarPlantillasMapper) {
        return new ListarPlantillasUseCase(plantillaService, listarPlantillasMapper);
    }

    @Bean
    public PlantillaService plantillaService(PlantillaRepository plantillaRepository) {
        return new PlantillaService(plantillaRepository);
    }
}
