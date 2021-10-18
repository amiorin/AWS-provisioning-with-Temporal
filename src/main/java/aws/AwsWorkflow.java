package aws;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface AwsWorkflow {

    // The Workflow method is called by the initiator either via code or CLI.
    @WorkflowMethod
    void eventloop();

    @SignalMethod
    void createBucket(String name);

    // Define the workflow exit signal method. This method is executed when the workflow receives a
    // signal.
    @SignalMethod
    void exit();
}
