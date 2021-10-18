package aws;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface AwsActivity {
    void createS3Bucket(String bucketName);
    void destroyS3Bucket(String bucketName);
}
