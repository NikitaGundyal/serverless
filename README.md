# serverless

Technology Stack

This application is a maven project which will be deployed as the lambda function. This lambda function will be triggered by SNS topic which would publish the list of bills that ae due in "x" days. The details of the bills would be sent to the user's email which would be sent by Lambda Function. Dynamodb is used to store the TTL property.

Build Instructions

To trigger lambda function to send email, clone the above git repository 

Deploy Instructions

We can trigger the Code Deployment using curl command to call the circleci API
CICD

For CircleCi to read the config.yml we need to set inputs in CircleCI environment variables Setup your circleci user credentials in circle ci environment which is created in AWS console Setup code deploy bucket name which is the bucket created in AWS console for code deploy to upload the s3 artifact Setup the region in circle ci environment variables where the code deloy should take place Specify the branch name in circle ci for which build needs to be triggered Command to trigger CICD 

curl -u ${CIRCLE_API_USER_TOKEN} \
     -d build_parameters[CIRCLE_JOB]=build \
     https://circleci.com/api/v1.1/project/<vcs-type>/<org>/<repo>/tree/<branch>


