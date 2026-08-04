pipeline {
    agent { label 'docker-agent' }
    
    triggers {
        githubPush()
    }

    environment {
        DOCKERHUB_REPO = 'zegzus/learnit-app'
        APP_SERVER_IP  = "${env.APP_SERVER_IP}"
        DISCORD_WEBHOOK = credentials('discord-webhook-url')
    }

    stages {
        stage('Build & Test App') {
            steps {
                sh 'mvn clean package -DskipTests=false'
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh "docker build -t ${DOCKERHUB_REPO}:latest -t ${DOCKERHUB_REPO}:${env.BUILD_NUMBER} ."
                    sh "echo $PASS | docker login -u $USER --password-stdin"
                    sh "docker push ${DOCKERHUB_REPO}:latest"
                    sh "docker push ${DOCKERHUB_REPO}:${env.BUILD_NUMBER}"
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
                        ssh -o StrictHostKeyChecking=no ubuntu@${APP_SERVER_IP} '
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
                sh """
                    curl -H "Content-Type: application/json" \
                         -d '{"content": "✅ Build #${env.BUILD_NUMBER} on ${env.BRANCH_NAME} succeeded (built, tested, pushed${branchNote})."}' \
                         ${DISCORD_WEBHOOK}
                """
            }
        }
        failure {
            sh """
                curl -H "Content-Type: application/json" \
                     -d '{"content": "❌ Build #${env.BUILD_NUMBER} on ${env.BRANCH_NAME} failed. Check Jenkins: ${env.BUILD_URL}"}' \
                     ${DISCORD_WEBHOOK}
            """
        }
    }
}