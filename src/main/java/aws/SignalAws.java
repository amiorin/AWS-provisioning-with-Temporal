package aws;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class SignalAws {

    public static void main(String[] args) {
        // WorkflowServiceStubs is a gRPC stubs wrapper that talks to the local Docker instance of the Temporal server.
        WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(Shared.AWS_TASK_QUEUE)
                // A WorkflowId prevents this it from having duplicate instances, remove it to duplicate.
                .setWorkflowId("aws-workflow")
                .build();
        // WorkflowClient can be used to start, signal, query, cancel, and terminate Workflows.
        WorkflowClient client = WorkflowClient.newInstance(service);
        AwsWorkflow workflowById = client.newWorkflowStub(AwsWorkflow.class, "aws-workflow");

        // Send the second signal to our workflow
        workflowById.createBucket("czlfffhjjm");
        workflowById.exit();
    }
}
