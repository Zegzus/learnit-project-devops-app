pipeline {
    agent { label 'docker-agent' }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    triggers {
        githubPush()
    }

    environment {
        DOCKERHUB_REPO  = 'zegzus/learnit-app'
        APP_SERVER_IP   = "${env.APP_SERVER_IP}"
    }

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                script {
                    def isMain  = (env.BRANCH_NAME in ['main', 'master'])
                    def extraTag = isMain ? 'latest' : "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"

                    withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                        sh "docker build -t ${DOCKERHUB_REPO}:${env.BUILD_NUMBER} -t ${DOCKERHUB_REPO}:${extraTag} ."
                        sh 'echo $PASS | docker login -u $USER --password-stdin'
                        sh "docker push ${DOCKERHUB_REPO}:${env.BUILD_NUMBER}"
                        sh "docker push ${DOCKERHUB_REPO}:${extraTag}"
                    }
                }
            }
        }

        stage('Deploy to AWS EC2') {
            when {
                anyOf {
                    branch 'main'
                    branch 'master'
                }
            }
            steps {
                sshagent(credentials: ['aws-ec2-ssh-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ubuntu@${APP_SERVER_IP} '
                            cd /opt/learnit-app &&
                            docker compose pull app &&
                            docker compose up -d app
                        '
                    """
                }
            }
        }
    }

    post {
        success {
            script {
                def branchNote = (env.BRANCH_NAME in ['main', 'master']) ? " and deployed" : ""
                def payload = "{\"content\": \"✅ Build #${env.BUILD_NUMBER} on ${env.BRANCH_NAME} succeeded (built, tested, pushed${branchNote}).\"}"
                withCredentials([string(credentialsId: 'discord-webhook-url', variable: 'WEBHOOK')]) {
                    sh """
                        curl -H "Content-Type: application/json" -d '${payload}' "\$WEBHOOK"
                    """
                }
            }
        }
        failure {
            script {
                def payload = "{\"content\": \"❌ Build #${env.BUILD_NUMBER} on ${env.BRANCH_NAME} failed. Check Jenkins: ${env.BUILD_URL}\"}"
                withCredentials([string(credentialsId: 'discord-webhook-url', variable: 'WEBHOOK')]) {
                    sh """
                        curl -H "Content-Type: application/json" -d '${payload}' "\$WEBHOOK"
                    """
                }
            }
        }
    }
}