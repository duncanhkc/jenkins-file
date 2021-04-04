### Introduction:

This project contains 
1. The pipeline file run by a groovy file (vars/runPipeline.groovy)
2. Dockerfile needed for the pipeline (files/Dockerfile)
3. K8s deployment file for the pipeline (files/deployment.yaml)
4. Other files should be configured of the app. (For example, log setting/pom.xml setting)

Each branch contains specific pipeline file needed by CD of a kind of app. 
For example, the files in branch "mvn-springboot" can only be used by applications using maven and springboot.  

---
### Usage:

1. Create a pipeline job

2. Set the environment variables. 

3. Put the following scripts in the Jenkinsfile of the job.
```shell script
library identifier: "fetch-jenkins-file@${PIPELINE_BRANCH}", retriever: modernSCM(
  [$class: 'GitSCMSource',
   remote: "${PIPELINE_GIT_URL}",
   credentialsId: "${GIT_SSH_ID}"])

runPipeline([:])
```