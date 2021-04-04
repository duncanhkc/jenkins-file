def call(Map pipelineParams) {

    env.JENKINS_FILES_DIR = "jenkins-files"

    pipeline {

        options {
            skipStagesAfterUnstable()
        }

        agent {
            kubernetes {
                yaml """\
        apiVersion: v1
        kind: Pod
        metadata:
          labels:
            jenkins-agent: "true"
        spec:
          volumes:
            - name: maven-volume
              nfs:
                server: 192.168.0.13
                path: /nfs_share/infra/jenkins/.m2
            - name: docker-socket-volume
              hostPath:
                path: /var/run/docker.sock
            - name: kubectl-volume
              nfs:
                server: 192.168.0.13
                path: /nfs_share/infra/jenkins/.kube
          containers:
            - name: maven
              volumeMounts:
                - name: maven-volume
                  mountPath: /root/.m2
              image: maven:3-jdk-11
              command:
                - cat
              tty: true
            - name: docker
              volumeMounts:
              - mountPath: /var/run/docker.sock
                name: docker-socket-volume
              image: docker:19.03.13
              command:
                - cat
              securityContext:
                privileged: true
              tty: true
            - name: kubectl
              image: "dtzar/helm-kubectl:3.3.1"
              command:
                - cat
              volumeMounts:
                - name: kubectl-volume
                  mountPath: /root/.kube/config
                  subPath: kube-config-demo
              tty: true
        """.stripIndent()
            }
        }

        stages {

            stage('Checkout Scm') {

                steps {
                    sh "env"
                    git(branch: "${GIT_BRANCH}", url: "${GIT_URL}")

                    script {
                        dir("${JENKINS_FILES_DIR}") {
                            git(branch: "${PIPELINE_BRANCH}", url: "${PIPELINE_GIT_URL}")
                        }
                    }

                    sh "ls -l"
                    sh "ls -l ${JENKINS_FILES_DIR}"
                }


            }

            stage('Maven Build') {
                steps {
                    container('maven') {
                        sh "cat pom.xml"
                        sh "mvn package -Dapp.name=${APP_NAME} \
                           -Djava.version=${APP_JAVA_VERSION} \
                           -Dmaven.compiler.source=${APP_JAVA_VERSION} \
                           -Dmaven.compiler.target=${APP_JAVA_VERSION} \
                           -Dmaven.compiler.release=${APP_JAVA_VERSION} \
                           -Dmaven.test.skip"
                        sh "ls -l target/*.jar"
                    }
                }
            }

            stage('Docker Image') {
                steps {
                    container('docker') {
                        sh "ls -l target/*.jar"
                        sh "test -f ./Dockerfile || cp ./${JENKINS_FILES_DIR}/files/Dockerfile ./Dockerfile"
                        sh '''eval "$(sed 's/^/echo "/; s/$/";/' ./Dockerfile)" > ./parsed-Dockerfile '''
                        sh "cat ./parsed-Dockerfile"
                        sh """
             docker --version
             docker build -t ${DOCKER_REGISTRY}/${APP_NAME}-${PROFILE}:${BUILD_NUMBER} -f ./parsed-Dockerfile .
             docker push ${DOCKER_REGISTRY}/${APP_NAME}-${PROFILE}:${BUILD_NUMBER}
             """
                    }
                }
            }

            stage('K8s Deployment') {
                steps {
                    container('kubectl') {

                        // We don't have dns right now, so add ip in /etc/hosts
                        sh "echo '192.168.0.12   rancher-server.demo.com' >> /etc/hosts"

                        sh "test -f ./deployment.yaml || cp ./${JENKINS_FILES_DIR}/files/deployment.yaml ./deployment.yaml"
                        sh '''eval "$(sed 's/^/echo "/; s/$/";/' ./deployment.yaml)" > ./parsed-deployement.yaml '''
                        sh "cat ./parsed-deployement.yaml"
                        sh "kubectl apply -f ./parsed-deployement.yaml -n=${NAMESPACE}"
                    }
                }
            }

        }
    }
}