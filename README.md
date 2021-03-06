# S3 Replicator
## Description
This repository contains:
* A simple AWS Lambda function that replicates objects from one S3 bucket to other S3 buckets whenever the object is uploaded to the S3 bucket. The source bucket will be created by the CloudFormation stack.
    * The replication works cross account.
    * The replication supports S3 Server Side Encryption.
* A [SAM](https://github.com/awslabs/serverless-application-model) template that defines the AWS CloudFormation stack.
## Parameters
* SourceBucketName: The name of the source S3 bucket.
    * S3 bucket name is globally unique, therefore do not use an existing bucket name.
* DestinationBucketNames: A list of destination S3 bucket name.
    * Use comma separated list. eg. `A, B, C`
    * The buckets must already exist.
* LambdaFunctionName: The name of the Lambda function.
* AllowOverwrite: Whether allow the replicator to overwrite any existing object in the destination S3 bucket.
* UseSSE: Whether to use S3 Server Side Encryption.
## What will be created by CloudFormation
1. A Source bucket that user can upload object(file) to.
2. A Lambda function that replicates objects whenever the object is uploaded to the S3 bucket.
3. An IAM Role that can be assumed by Lambda. The IAM Role gives the Lambda function **Full S3 Access**.
## Instruction
```bash
$ mvn package
```
```bash
$ aws cloudformation package \
    --template-file sam/template.yml \
    --s3-bucket <bucket-name> \
    --output-template-file package/packaged-template.yml
```
```bash
$ aws cloudformation deploy \
    --template-file package/packaged-template.yml \
    --stack-name S3Replication \
    --parameter-overrides "SourceBucketName=<source-bucket>" "DestinationBucketNames=<destination-bucket>" "USE_SSE=<true|false>" "ALLOW_OVERWRITE=<true|false>" \
    --capabilities CAPABILITY_NAMED_IAM
```
## FAQ
1. Does it work cross account?    
> If the destination S3 bucket is not in the same account as the source S3 bucket. A Bucket Policy is needed on the destination S3 bucket. The ACL on the replicated object will be set to the owner of the destination S3 bucket.

2. Does it work cross region?   
> No, please use S3 Cross Region Replication feature.