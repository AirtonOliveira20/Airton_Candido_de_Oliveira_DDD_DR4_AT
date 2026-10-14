package br.com.cinepass.reserva.observability;
import brave.Tracing;
import brave.propagation.ThreadLocalCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.*;
import io.temporal.activity.*;
import io.temporal.workflow.*;
import io.temporal.client.*;
import io.temporal.worker.*;
import io.temporal.testing.*;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class TemporalTracingTest {
    @WorkflowInterface public interface DemoWorkflow { @WorkflowMethod String execute(); }
    @ActivityInterface public interface DemoActivity { @ActivityMethod String execute(); }
    public static class DemoWorkflowImpl implements DemoWorkflow {
        public String execute(){return Workflow.newActivityStub(DemoActivity.class,ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(5)).build()).execute();}
    }
    @Test void gatewayTraceAndCorrelationReachTemporalActivity(){
        var context=ThreadLocalCurrentTraceContext.newBuilder().build();
        try(var brave=Tracing.newBuilder().currentTraceContext(context).build()){
            var tracer=new BraveTracer(brave.tracer(),new BraveCurrentTraceContext(context));
            var traces=new TraceSupport(tracer,new BravePropagator(brave));
            var config=new TemporalTracingConfig();
            var clientOptions=config.traceClient(config.cinepassContext(traces)).customize(WorkflowClientOptions.newBuilder()).build();
            var factoryOptions=config.traceActivities(traces).customize(WorkerFactoryOptions.newBuilder()).build();
            try(var env=TestWorkflowEnvironment.newInstance(TestEnvironmentOptions.newBuilder()
                    .setWorkflowClientOptions(clientOptions).setWorkerFactoryOptions(factoryOptions).build())){
                var worker=env.newWorker("trace-test");
                worker.registerWorkflowImplementationTypes(DemoWorkflowImpl.class);
                worker.registerActivitiesImplementations((DemoActivity)()->tracer.currentSpan().context().traceId()+":"+MDC.get("correlationId"));
                env.start();
                var parent=tracer.nextSpan().name("gateway").start();
                try(var scope=tracer.withSpan(parent)){
                    MDC.put("correlationId","temporal-demo");
                    var workflow=env.getWorkflowClient().newWorkflowStub(DemoWorkflow.class,WorkflowOptions.newBuilder().setTaskQueue("trace-test").build());
                    assertEquals(parent.context().traceId()+":temporal-demo",workflow.execute());
                }finally{MDC.clear();parent.end();}
            }
        }
    }
}
