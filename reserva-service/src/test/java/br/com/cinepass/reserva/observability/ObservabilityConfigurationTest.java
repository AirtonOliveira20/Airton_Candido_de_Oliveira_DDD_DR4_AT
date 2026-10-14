package br.com.cinepass.reserva.observability;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class ObservabilityConfigurationTest {
    @Test void everyServiceHasStructuredLogsAndZipkinConfiguration() throws Exception {
        for(String module:new String[]{"api-gateway","discovery-server","reserva-service","pagamento-service","ingresso-service","notificacao-service","historico-service","auditoria-service"}){
            var source=new YamlPropertySourceLoader().load(module,new FileSystemResource(Path.of("..",module,"src/main/resources/application.yml"))).getFirst();
            assertEquals(module,source.getProperty("spring.application.name"));
            assertEquals("ecs",source.getProperty("logging.structured.format.file"));
            assertEquals("OFF",source.getProperty("logging.level.org.hibernate.orm.jdbc.bind"));
            assertNotNull(source.getProperty("management.tracing.export.zipkin.endpoint"));
        }
    }
}
