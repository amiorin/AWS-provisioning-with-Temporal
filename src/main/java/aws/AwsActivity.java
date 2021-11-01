package aws;

import io.temporal.activity.ActivityInterface;

import java.io.IOException;

@ActivityInterface
public interface AwsActivity {
    void createS3Bucket(String bucketName);
    void destroyS3Bucket(String bucketName);
    void sendCommand(String removePrefix, String manifest, String bucketName, String keyFolder) throws IOException;
}
