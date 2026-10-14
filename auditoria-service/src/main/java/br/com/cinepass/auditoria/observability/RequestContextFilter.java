package br.com.cinepass.auditoria.observability;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.*;
import java.io.IOException;
import java.util.*;

@Component("cinepassRequestContextFilter")
public class RequestContextFilter extends OncePerRequestFilter {
    private static final Logger log=LoggerFactory.getLogger(RequestContextFilter.class);
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        var previous=MDC.getCopyOfContextMap();
        String correlation=TraceSupport.correlation(request.getHeader("X-Correlation-Id"));
        MDC.put("correlationId",correlation);
        MDC.put("reservaId",TraceSupport.safe(request.getHeader("X-Reserva-Id")));
        response.setHeader("X-Correlation-Id",correlation);
        var wrapped=new HttpServletRequestWrapper(request) {
            @Override public java.util.Enumeration<String> getHeaders(String name) {
                return "X-Correlation-Id".equalsIgnoreCase(name)?java.util.Collections.enumeration(java.util.List.of(correlation)):super.getHeaders(name);
            }
            @Override public java.util.Enumeration<String> getHeaderNames() {
                var names=new java.util.HashSet<>(java.util.Collections.list(super.getHeaderNames()));
                names.add("X-Correlation-Id");return java.util.Collections.enumeration(names);
            }
            @Override public String getHeader(String name) {
                return "X-Correlation-Id".equalsIgnoreCase(name)?correlation:super.getHeader(name);
            }
        };
        long start=System.nanoTime();
        log.info("operacao=HTTP resultado=INICIO metodo={}",request.getMethod());
        try { chain.doFilter(wrapped,response); }
        catch(ServletException|IOException|RuntimeException ex) {
            log.error("operacao=HTTP resultado=FALHA erro={}",ex.getClass().getSimpleName()); throw ex;
        } finally {
            log.info("operacao=HTTP resultado=CONCLUSAO status={} duracaoMs={}",response.getStatus(),(System.nanoTime()-start)/1_000_000);
            if(previous==null) MDC.clear(); else MDC.setContextMap(previous);
        }
    }
}
