package aws;

import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.io.IOException;

public class AwsActivitiesTest {
    @Test
    public void sendCommand() throws IOException {
        S3Client s3 = S3Client.builder().region(Region.EU_CENTRAL_1).build();
        SsmClient ssm = SsmClient.builder().region(Region.US_WEST_2).build();
        AwsActivityImpl activities = new AwsActivityImpl(s3, ssm);
        activities.sendCommand(
                "/ansible/",
                "/ansible.manifest",
                "amiorin-lake",
                "assets/ansible");
    }

    @Test
    public void runAnsible() throws IOException {
        Region region = Region.EU_CENTRAL_1;
        S3Client s3 = S3Client.builder().region(region).build();
        SsmClient ssm = SsmClient.builder().region(region).build();
        AwsActivityImpl activities = new AwsActivityImpl(s3, ssm);
        activities.createZipOnS3(
                "/ansible/",
                "/ansible.manifest",
                "amiorin-lake",
                "assets/ansible");
    }
}
