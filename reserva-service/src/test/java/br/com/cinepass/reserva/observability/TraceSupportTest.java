package br.com.cinepass.reserva.observability;
import brave.Tracing;
import brave.propagation.ThreadLocalCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.*;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import tools.jackson.databind.json.JsonMapper;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TraceSupportTest {
    @Test void serializedCarrierPreservesTraceAcrossAsyncBoundaryAndCleansThread(){
        var context=ThreadLocalCurrentTraceContext.newBuilder().build();
        try(var brave=Tracing.newBuilder().currentTraceContext(context).build()){
            var tracer=new BraveTracer(brave.tracer(),new BraveCurrentTraceContext(context));
            var support=new TraceSupport(tracer,new BravePropagator(brave));
            var parent=tracer.nextSpan().name("gateway").start();
            String stored;
            MDC.put("correlationId","operation-123");
            try(var scope=tracer.withSpan(parent)){
                stored=JsonMapper.builder().build().writeValueAsString(support.capture());
            } finally{parent.end();MDC.clear();}
            var carrier=JsonMapper.builder().build().readValue(stored,new tools.jackson.core.type.TypeReference<Map<String,String>>(){});
            support.within(carrier,"outbox.publish",()->{
                assertEquals(parent.context().traceId(),tracer.currentSpan().context().traceId());
                assertNotEquals(parent.context().spanId(),tracer.currentSpan().context().spanId());
                assertEquals("operation-123",MDC.get("correlationId"));
                var kafkaCarrier=support.capture();
                support.within(kafkaCarrier,"inbox.process",()->{
                    assertEquals(parent.context().traceId(),tracer.currentSpan().context().traceId());return null;
                });
                return null;
            });
            assertNull(tracer.currentSpan());assertNull(MDC.get("correlationId"));
        }
    }
    @Test void untrustedCorrelationCannotInjectLogLines(){
        assertEquals("",TraceSupport.safe("value\r\nAuthorization: secret"));
        assertEquals("",TraceSupport.safe("x".repeat(101)));
        assertTrue(TraceSupport.correlation(null).matches("[a-f0-9-]{36}"));
    }
}
