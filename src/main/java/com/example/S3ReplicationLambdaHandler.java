package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.UnsupportedEncodingException;

public class S3ReplicationLambdaHandler implements RequestHandler<S3EventNotification, String> {

    @Override
    public String handleRequest(final S3EventNotification s3Event, final Context context) {
        // Logger
        final LambdaLogger logger = context.getLogger();

        // Parameters
        final boolean allowOverwrite = Boolean.valueOf(System.getenv("ALLOW_OVERWRITE"));
        final boolean useSSE = Boolean.valueOf(System.getenv("USE_SSE"));
        final String destinationBucket = System.getenv("DESTINATION_BUCKET");
        final String destinationRegion = System.getenv("DESTINATION_REGION");

        // Client
        final AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(destinationRegion).build();

        for (S3EventNotification.S3EventNotificationRecord record : s3Event.getRecords()) {
            final String sourceKey = record.getS3().getObject().getKey();
            final String sourceBucket = record.getS3().getBucket().getName();
            String decodedSourceKey = sourceKey;

            // Decode Object Key
            try {
                decodedSourceKey = java.net.URLDecoder.decode(sourceKey, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(String.format("Failed to decode the key %s.", sourceKey), e);
            }

            // Check object existence
            if (s3Client.doesObjectExist(destinationBucket, decodedSourceKey) && !allowOverwrite) {
                logger.log(String.format("Object with the key %s already exists in the destination bucket %s, overwrite is not allowed.", decodedSourceKey, destinationBucket));
            } else {
                CopyObjectRequest copyObjectRequest = new CopyObjectRequest(sourceBucket, decodedSourceKey, destinationBucket, decodedSourceKey);
                ObjectMetadata objectMetadata = new ObjectMetadata();

                // Set SSE if enabled
                if (useSSE) {
                    objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
                    copyObjectRequest.setNewObjectMetadata(objectMetadata);
                }

                s3Client.copyObject(copyObjectRequest);
            }

            s3Client.setObjectAcl(destinationBucket, decodedSourceKey, CannedAccessControlList.BucketOwnerFullControl);
        }

        return "success";
    }
}
