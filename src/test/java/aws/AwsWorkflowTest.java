package aws;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AwsWorkflowTest {

    private TestWorkflowEnvironment testEnv;
    private Worker worker;
    private WorkflowClient workflowClient;

    @Before
    public void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        worker = testEnv.newWorker(Shared.AWS_TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(AwsWorkflowImpl.class);
        workflowClient = testEnv.getWorkflowClient();
    }

    @After
    public void tearDown() {
        testEnv.close();
    }

    @Test
    public void testEventloop() {
        AwsActivity activities = mock(AwsActivity.class);
        worker.registerActivitiesImplementations(activities);
        testEnv.start();
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(Shared.AWS_TASK_QUEUE)
                .build();
        AwsWorkflow workflow = workflowClient.newWorkflowStub(AwsWorkflow.class, options);
        WorkflowExecution we = WorkflowClient.start(workflow::eventloop);
        testEnv.sleep(Duration.ofDays(1));
        workflow.createBucket("foobar");
        workflow.exit();
        workflow.eventloop();
        verify(activities).createS3Bucket(eq("foobar"));
        verify(activities).destroyS3Bucket(eq("foobar"));
    }
}
