package br.com.cinepass.reserva.observability;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.propagation.Propagator;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.function.Supplier;

@Component
public class TraceSupport {
    private final Tracer tracer;
    private final Propagator propagator;
    public TraceSupport(Tracer tracer, Propagator propagator) { this.tracer=tracer; this.propagator=propagator; }
    public static String safe(String value) {
        return value != null && value.matches("[A-Za-z0-9_.:-]{1,100}") ? value : "";
    }
    public static String correlation(String value) {
        String clean=safe(value); return clean.isEmpty()?UUID.randomUUID().toString():clean;
    }
    public Map<String,String> capture() {
        Map<String,String> carrier=new HashMap<>();
        if(tracer.currentSpan()!=null) propagator.inject(tracer.currentSpan().context(),carrier,Map::put);
        carrier.put("correlationId",safe(MDC.get("correlationId")));
        carrier.put("eventId",safe(MDC.get("eventId")));
        carrier.put("reservaId",safe(MDC.get("reservaId")));
        return carrier;
    }
    public <T> T within(Map<String,String> carrier, String name, Supplier<T> work) {
        Map<String,String> old=MDC.getCopyOfContextMap();
        Span span=propagator.extract(carrier,Map::get).name(name).start();
        try(Tracer.SpanInScope scope=tracer.withSpan(span)) {
            MDC.put("correlationId",safe(carrier.get("correlationId")));
            MDC.put("eventId",safe(carrier.get("eventId")));
            MDC.put("reservaId",safe(carrier.get("reservaId")));
            span.tag("correlationId",safe(carrier.get("correlationId")));
            return work.get();
        } catch(RuntimeException ex) {

            span.tag("error",ex.getClass().getSimpleName()); throw ex;
        } finally { span.end(); if(old==null) MDC.clear(); else MDC.setContextMap(old); }
    }
}
