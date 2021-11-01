package aws;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ssm.SsmClient;

public class AwsWorker {
    public static void main(String[] args) {

        // WorkflowServiceStubs is a gRPC stubs wrapper that talks to the local Docker instance of the Temporal server.
        WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
        WorkflowClient client = WorkflowClient.newInstance(service);
        // Worker factory is used to create Workers that poll specific Task Queues.
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(Shared.AWS_TASK_QUEUE);
        // This Worker hosts both Workflow and Activity implementations.
        // Workflows are stateful so a type is needed to create instances.
        worker.registerWorkflowImplementationTypes(AwsWorkflowImpl.class);
        // Activities are stateless and thread safe so a shared instance is used.
        Region region = Region.US_WEST_2;
        S3Client s3 = S3Client.builder().region(region).build();
        SsmClient ssm = SsmClient.builder().region(region).build();
        worker.registerActivitiesImplementations(new AwsActivityImpl(s3, ssm));
        // Start listening to the Task Queue.
        factory.start();
    }
}
