package br.com.cinepass.reserva.observability;
import io.temporal.common.context.ContextPropagator;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.interceptors.*;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.spring.boot.TemporalOptionsCustomizer;
import io.temporal.common.converter.DefaultDataConverter;
import org.springframework.context.annotation.*;
import java.util.*;

@Configuration
public class TemporalTracingConfig {
    private final ThreadLocal<Map<String,String>> carried=new ThreadLocal<>();
    @Bean
    ContextPropagator cinepassContext(TraceSupport traces) {
        return new ContextPropagator(){
            public String getName(){return "cinepass-trace";}
            public Object getCurrentContext(){
                if(io.temporal.workflow.unsafe.WorkflowUnsafe.isWorkflowThread()) return carried.get()==null?Map.of():carried.get();
                return traces.capture();
            }
            public void setCurrentContext(Object context){carried.set((Map<String,String>)context);}
            public Map<String,Payload> serializeContext(Object context){
                return Map.of("cinepass-trace",DefaultDataConverter.STANDARD_INSTANCE.toPayload(context).orElseThrow());
            }
            public Object deserializeContext(Map<String,Payload> context){
                var payload=context.get("cinepass-trace");
                return payload==null?Map.of():DefaultDataConverter.STANDARD_INSTANCE.fromPayload(payload,Map.class,Map.class);
            }
        };
    }
    @Bean TemporalOptionsCustomizer<WorkflowClientOptions.Builder> traceClient(ContextPropagator cinepassContext){
        return builder->builder.setContextPropagators(List.of(cinepassContext));
    }
    @Bean TemporalOptionsCustomizer<WorkerFactoryOptions.Builder> traceActivities(TraceSupport traces){
        return builder->builder.setWorkerInterceptors(new WorkerInterceptorBase(){
            @Override public ActivityInboundCallsInterceptor interceptActivity(ActivityInboundCallsInterceptor next){
                return new ActivityInboundCallsInterceptorBase(next){
                    @Override public ActivityOutput execute(ActivityInput input){
                        try {
                            return traces.within(carried.get()==null?Map.of():carried.get(),"temporal.activity",()->{
                                var log=org.slf4j.LoggerFactory.getLogger(TemporalTracingConfig.class);
                                log.info("operacao=ACTIVITY resultado=INICIO");
                                try {var output=super.execute(input);log.info("operacao=ACTIVITY resultado=CONCLUSAO");return output;}
                                catch(RuntimeException ex){log.error("operacao=ACTIVITY resultado=FALHA erro={}",ex.getClass().getSimpleName());throw ex;}
                            });
                        }finally{carried.remove();}
                    }
                };
            }
        });
    }
}
