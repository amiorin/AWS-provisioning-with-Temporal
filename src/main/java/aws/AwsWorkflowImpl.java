package aws;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AwsWorkflowImpl implements AwsWorkflow {
    List<String> messageQueue = new ArrayList<>(10);
    boolean exit = false;
    // RetryOptions specify how to automatically handle retries when Activities fail.
    private final RetryOptions retryoptions = RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumInterval(Duration.ofSeconds(100))
            .setBackoffCoefficient(2)
            .setMaximumAttempts(500)
            .build();
    private final ActivityOptions defaultActivityOptions = ActivityOptions.newBuilder()
            // Timeout options specify when to automatically timeout Activities if the process is taking too long.
            .setStartToCloseTimeout(Duration.ofSeconds(5))
            // Optionally provide customized RetryOptions.
            // Temporal retries failures by default, this is simply an example.
            .setRetryOptions(retryoptions)
            .build();
    // ActivityStubs enable calls to methods as if the Activity object is local, but actually perform an RPC.
    private final Map<String, ActivityOptions> perActivityMethodOptions = new HashMap<>() {{
        put("doCreateS3Bucket", ActivityOptions.newBuilder().setHeartbeatTimeout(Duration.ofSeconds(5)).build());
    }};
    private final AwsActivity aws = Workflow.newActivityStub(AwsActivity.class, defaultActivityOptions, perActivityMethodOptions);

    @Override
    public void eventloop() {
        while (true) {
            // Block current thread until the unblocking condition is evaluated to true
            Workflow.await(() -> !messageQueue.isEmpty() || exit);
            if (messageQueue.isEmpty() && exit) {
                System.out.println("foo");
                return;
            }
            String bucketName = messageQueue.remove(0);
            aws.createS3Bucket(bucketName);
            aws.destroyS3Bucket(bucketName);
        }
    }

    @Override
    public void createBucket(String name) {
        messageQueue.add(name);
    }

    @Override
    public void exit() {
        exit = true;
    }
}
