package br.com.cinepass.gateway.observability;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.slf4j.*;

@Component
public class CorrelationFilter implements GlobalFilter, Ordered {
    private static final Logger log=LoggerFactory.getLogger(CorrelationFilter.class);
    public int getOrder(){return -100;}
    public Mono<Void> filter(ServerWebExchange exchange,GatewayFilterChain chain) {
        String id=TraceSupport.correlation(exchange.getRequest().getHeaders().getFirst("X-Correlation-Id"));
        var changed=exchange.mutate().request(r->r.headers(h->h.set("X-Correlation-Id",id))).build();
        exchange.getResponse().getHeaders().set("X-Correlation-Id",id);
        log.atInfo().addKeyValue("correlationId",id).log("operacao=GATEWAY resultado=INICIO");
        return chain.filter(changed)
            .doOnError(ex->log.atError().addKeyValue("correlationId",id).addKeyValue("erro",ex.getClass().getSimpleName()).log("operacao=GATEWAY resultado=FALHA"))
            .doFinally(signal->log.atInfo().addKeyValue("correlationId",id).addKeyValue("status",exchange.getResponse().getStatusCode()).log("operacao=GATEWAY resultado=CONCLUSAO"));
    }
}
