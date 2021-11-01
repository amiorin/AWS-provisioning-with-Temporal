package aws;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.SendCommandRequest;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AwsActivityImpl implements AwsActivity {
    private static final Logger logger = LoggerFactory.getLogger(AwsActivity.class);
    private S3Client s3;
    private SsmClient ssm;

    public AwsActivityImpl(S3Client s3, SsmClient ssm) {
        this.s3 = s3;
        this.ssm = ssm;
    }

    @Override
    public void createS3Bucket(String bucketName) {
        S3Waiter s3Waiter = s3.waiter();
        CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                .bucket(bucketName)
                .build();

        s3.createBucket(bucketRequest);
        HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();


        // Wait until the bucket is created and print out the response
        WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
        waiterResponse.matched().response().ifPresent(System.out::println);
        System.out.println(bucketName +" is ready");
    }

    @Override
    public void destroyS3Bucket(String bucketName) {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucketName).build();
        s3.deleteBucket(deleteBucketRequest);
    }

    @Override
    public void sendCommand(String removePrefix, String manifest, String bucketName, String keyFolder) throws IOException {
        URL url = createZipOnS3(removePrefix, manifest, bucketName, keyFolder);
        Map<String, List<String>> parameters = ImmutableMap.of(
                "SourceType", ImmutableList.of("S3"),
                "SourceInfo", ImmutableList.of("{\"path\":\"" + url.toString() + "\"}"),
                "InstallDependencies", ImmutableList.of("False"),
                "PlaybookFile", ImmutableList.of("main.yml"),
                "Check", ImmutableList.of("False")
        );
        SendCommandRequest command = SendCommandRequest.builder()
                .instanceIds("i-08f741dd01ee49d94")
                .parameters(parameters)
                .documentName("AWS-ApplyAnsiblePlaybooks")
                .build();
        String commandId = ssm.sendCommand(command).command().commandId();
        logger.info(commandId);
    }

//    @Override
//    public void sendCommand() {
//        List<String> script = ImmutableList.of(
//                "echo Hello World!"
//        );
//        Map<String, List<String>> parameters = ImmutableMap.of(
//                "commands", script
//        );
//        SendCommandRequest command = SendCommandRequest.builder()
//                .instanceIds("i-08f741dd01ee49d94")
//                .parameters(parameters)
//                .documentName("AWS-RunShellScript")
//                .build();
//        String commandId = ssm.sendCommand(command).command().commandId();
//        System.out.println(commandId);
//    }

    public URL createZipOnS3(String removePrefix, String manifest, String bucketName, String keyFolder)
            throws IOException {
        Path zipFile = createZipLocally(removePrefix, manifest);
        String key = Paths.get(keyFolder, zipFile.getFileName().toString()).toString().replaceFirst("^/",
                "");
        logger.info("S3 key is: " + key);
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3.putObject(objectRequest, zipFile);
        GetBucketLocationRequest bucketLocationRequest = GetBucketLocationRequest.builder()
                .bucket(bucketName)
                .build();
        GetBucketLocationResponse bucketLocation = s3.getBucketLocation(bucketLocationRequest);
        logger.info("Region of the bucket is: " + bucketLocation.locationConstraintAsString());
        Region region = Region.of(bucketLocation.locationConstraintAsString());
        S3Utilities utilities = S3Utilities.builder().region(region).build();
        GetUrlRequest request = GetUrlRequest.builder().bucket(bucketName).key(key).build();
        URL url = utilities.getUrl(request);
        logger.info("Ansible zip url is: " + url);
        return url;
    }

    public Path createZipLocally(String removePrefix, String manifest) throws IOException {
        Path assetDir = Files.createTempDirectory("ansible_");
        InputStream manifestInputStream = AwsActivityImpl.class.getResourceAsStream(manifest);
        BufferedReader reader = new BufferedReader(new InputStreamReader(manifestInputStream));
        String assetResource;
        while ((assetResource = reader.readLine()) != null) {
            Path assetFile = assetDir.resolve(assetResource.replaceFirst("^" + removePrefix, ""));
            Files.createDirectories(assetFile.getParent());
            try (InputStream asset = AwsActivityImpl.class.getResourceAsStream(assetResource)) {
                Files.copy(asset, assetFile);
            }
        }
        Path tmpFile = Files.createTempFile("ansible_", ".zip");
        ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tmpFile));
        Files.walk(assetDir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    ZipEntry zipEntry = new ZipEntry(assetDir.relativize(path).toString());
                    try {
                        zipOutputStream.putNextEntry(zipEntry);
                        Files.copy(path, zipOutputStream);
                        zipOutputStream.closeEntry();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        zipOutputStream.close();
        logger.info("Ansible asset folder: " + assetDir);
        logger.info("Ansible zip file: " + tmpFile);
        Files.walk(assetDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        return tmpFile;
    }
}
