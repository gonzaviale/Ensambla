# Archivos de ENS-96

32 archivos creados y 16 modificados en ENS-96, comparados con el estado local al comenzar esta subtarea. No se incluyen archivos anteriores de ENS-94/95 que no se tocaron en ENS-96.

El inventario corresponde al desarrollo inicial en local. Posteriormente el usuario autorizó
commits explicativos y publicación de `feat/ens-21-planes-suscripciones`.

Incluye código, configuración, pruebas y documentación. Excluye artefactos de compilación y logs ignorados.

## Producción · creados (18)

- [backend/src/main/java/com/ensambla/backend/domain/dto/plan/IniciarPruebaRequest.java](../backend/src/main/java/com/ensambla/backend/domain/dto/plan/IniciarPruebaRequest.java)
- [backend/src/main/java/com/ensambla/backend/domain/dto/plan/PruebaPremiumResponse.java](../backend/src/main/java/com/ensambla/backend/domain/dto/plan/PruebaPremiumResponse.java)
- [backend/src/main/java/com/ensambla/backend/domain/mapper/plan/IniciarPruebaMapper.java](../backend/src/main/java/com/ensambla/backend/domain/mapper/plan/IniciarPruebaMapper.java)
- [backend/src/main/java/com/ensambla/backend/domain/model/plan/PruebaPremium.java](../backend/src/main/java/com/ensambla/backend/domain/model/plan/PruebaPremium.java)
- [backend/src/main/java/com/ensambla/backend/domain/repository/plan/PruebaPremiumRepository.java](../backend/src/main/java/com/ensambla/backend/domain/repository/plan/PruebaPremiumRepository.java)
- [backend/src/main/java/com/ensambla/backend/domain/service/plan/PruebaPremiumService.java](../backend/src/main/java/com/ensambla/backend/domain/service/plan/PruebaPremiumService.java)
- [backend/src/main/java/com/ensambla/backend/domain/useCase/plan/IniciarPruebaUseCase.java](../backend/src/main/java/com/ensambla/backend/domain/useCase/plan/IniciarPruebaUseCase.java)
- [backend/src/main/java/com/ensambla/backend/domain/useCase/plan/VencerPruebasUseCase.java](../backend/src/main/java/com/ensambla/backend/domain/useCase/plan/VencerPruebasUseCase.java)
- [backend/src/main/java/com/ensambla/backend/infrastructure/config/useCase/PlanUseCaseConfig.java](../backend/src/main/java/com/ensambla/backend/infrastructure/config/useCase/PlanUseCaseConfig.java)
- [backend/src/main/java/com/ensambla/backend/infrastructure/plan/VencimientoPruebasWorker.java](../backend/src/main/java/com/ensambla/backend/infrastructure/plan/VencimientoPruebasWorker.java)
- [backend/src/main/java/com/ensambla/backend/infrastructure/presentation/controller/PlanController.java](../backend/src/main/java/com/ensambla/backend/infrastructure/presentation/controller/PlanController.java)
- [backend/src/main/java/com/ensambla/backend/infrastructure/presentation/dto/plan/PruebaResponse.java](../backend/src/main/java/com/ensambla/backend/infrastructure/presentation/dto/plan/PruebaResponse.java)
- [backend/src/main/java/com/ensambla/backend/infrastructure/presentation/mapper/plan/PlanPresentationMapper.java](../backend/src/main/java/com/ensambla/backend/infrastructure/presentation/mapper/plan/PlanPresentationMapper.java)
- [backend/src/main/java/com/ensambla/backend/infrastructure/repository/entity/PruebaPremiumEntity.java](../backend/src/main/java/com/ensambla/backend/infrastructure/repository/entity/PruebaPremiumEntity.java)
- [backend/src/main/java/com/ensambla/backend/infrastructure/repository/jpaRepository/implementation/PruebaPremiumRepositoryImpl.java](../backend/src/main/java/com/ensambla/backend/infrastructure/repository/jpaRepository/implementation/PruebaPremiumRepositoryImpl.java)
- [backend/src/main/java/com/ensambla/backend/infrastructure/repository/jpaRepository/interfaces/IPruebaPremiumJpaRepository.java](../backend/src/main/java/com/ensambla/backend/infrastructure/repository/jpaRepository/interfaces/IPruebaPremiumJpaRepository.java)
- [backend/src/main/java/com/ensambla/backend/infrastructure/repository/mapper/PruebaPremiumRepositoryMapper.java](../backend/src/main/java/com/ensambla/backend/infrastructure/repository/mapper/PruebaPremiumRepositoryMapper.java)
- [backend/src/main/resources/db/migration/V14__prueba_premium.sql](../backend/src/main/resources/db/migration/V14__prueba_premium.sql)

## Producción · modificados (5)

- [backend/src/main/java/com/ensambla/backend/domain/model/plan/Suscripcion.java](../backend/src/main/java/com/ensambla/backend/domain/model/plan/Suscripcion.java)
- [backend/src/main/java/com/ensambla/backend/domain/service/plan/ConsumoIaInmediatoService.java](../backend/src/main/java/com/ensambla/backend/domain/service/plan/ConsumoIaInmediatoService.java)
- [backend/src/main/java/com/ensambla/backend/domain/service/plan/ControlPlanService.java](../backend/src/main/java/com/ensambla/backend/domain/service/plan/ControlPlanService.java)
- [backend/src/main/java/com/ensambla/backend/domain/service/procesamiento/ReservaIaService.java](../backend/src/main/java/com/ensambla/backend/domain/service/procesamiento/ReservaIaService.java)
- [backend/src/main/resources/application.properties](../backend/src/main/resources/application.properties)

## Pruebas · creados (12)

- [backend/src/test/java/com/ensambla/backend/domain/mapper/plan/IniciarPruebaMapperTest.java](../backend/src/test/java/com/ensambla/backend/domain/mapper/plan/IniciarPruebaMapperTest.java)
- [backend/src/test/java/com/ensambla/backend/domain/model/plan/PruebaPremiumTest.java](../backend/src/test/java/com/ensambla/backend/domain/model/plan/PruebaPremiumTest.java)
- [backend/src/test/java/com/ensambla/backend/domain/service/plan/PruebaPremiumServiceTest.java](../backend/src/test/java/com/ensambla/backend/domain/service/plan/PruebaPremiumServiceTest.java)
- [backend/src/test/java/com/ensambla/backend/domain/useCase/plan/IniciarPruebaUseCaseTest.java](../backend/src/test/java/com/ensambla/backend/domain/useCase/plan/IniciarPruebaUseCaseTest.java)
- [backend/src/test/java/com/ensambla/backend/domain/useCase/plan/VencerPruebasUseCaseTest.java](../backend/src/test/java/com/ensambla/backend/domain/useCase/plan/VencerPruebasUseCaseTest.java)
- [backend/src/test/java/com/ensambla/backend/infrastructure/config/useCase/PlanUseCaseConfigTest.java](../backend/src/test/java/com/ensambla/backend/infrastructure/config/useCase/PlanUseCaseConfigTest.java)
- [backend/src/test/java/com/ensambla/backend/infrastructure/plan/VencimientoPruebasWorkerTest.java](../backend/src/test/java/com/ensambla/backend/infrastructure/plan/VencimientoPruebasWorkerTest.java)
- [backend/src/test/java/com/ensambla/backend/infrastructure/presentation/controller/PlanControllerTest.java](../backend/src/test/java/com/ensambla/backend/infrastructure/presentation/controller/PlanControllerTest.java)
- [backend/src/test/java/com/ensambla/backend/infrastructure/presentation/mapper/plan/PlanPresentationMapperTest.java](../backend/src/test/java/com/ensambla/backend/infrastructure/presentation/mapper/plan/PlanPresentationMapperTest.java)
- [backend/src/test/java/com/ensambla/backend/infrastructure/repository/jpaRepository/implementation/PruebaPremiumRepositoryImplTest.java](../backend/src/test/java/com/ensambla/backend/infrastructure/repository/jpaRepository/implementation/PruebaPremiumRepositoryImplTest.java)
- [backend/src/test/java/com/ensambla/backend/infrastructure/repository/mapper/PruebaPremiumRepositoryMapperTest.java](../backend/src/test/java/com/ensambla/backend/infrastructure/repository/mapper/PruebaPremiumRepositoryMapperTest.java)
- [backend/src/test/java/com/ensambla/backend/infrastructure/repository/PruebaPremiumPostgresIntegrationTest.java](../backend/src/test/java/com/ensambla/backend/infrastructure/repository/PruebaPremiumPostgresIntegrationTest.java)

## Pruebas · modificados (7)

- [backend/src/test/java/com/ensambla/backend/domain/model/plan/SuscripcionTest.java](../backend/src/test/java/com/ensambla/backend/domain/model/plan/SuscripcionTest.java)
- [backend/src/test/java/com/ensambla/backend/domain/service/plan/ControlPlanServiceTest.java](../backend/src/test/java/com/ensambla/backend/domain/service/plan/ControlPlanServiceTest.java)
- [backend/src/test/java/com/ensambla/backend/domain/service/procesamiento/ReservaIaServiceTest.java](../backend/src/test/java/com/ensambla/backend/domain/service/procesamiento/ReservaIaServiceTest.java)
- [backend/src/test/java/com/ensambla/backend/infrastructure/presentation/controller/PlanSeguridadIntegrationTest.java](../backend/src/test/java/com/ensambla/backend/infrastructure/presentation/controller/PlanSeguridadIntegrationTest.java)
- [backend/src/test/java/com/ensambla/backend/infrastructure/repository/MigracionPlanesPostgresIntegrationTest.java](../backend/src/test/java/com/ensambla/backend/infrastructure/repository/MigracionPlanesPostgresIntegrationTest.java)
- [backend/src/test/resources/application-h2-test.properties](../backend/src/test/resources/application-h2-test.properties)
- [backend/src/test/resources/application-postgres-test.properties](../backend/src/test/resources/application-postgres-test.properties)

## Documentación · creados (2)

- [docs/archivos-ENS-96.md](../docs/archivos-ENS-96.md)
- [docs/plan-ENS-96.md](../docs/plan-ENS-96.md)

## Documentación · modificados (4)

- [backend/README.md](../backend/README.md)
- [backend/TESTING.md](../backend/TESTING.md)
- [docs/modelo-datos.md](../docs/modelo-datos.md)
- [docs/plan-ENS-21.md](../docs/plan-ENS-21.md)
