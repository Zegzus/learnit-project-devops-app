pipeline {
    agent { label 'docker-agent' }

    // Declares the webhook trigger in code so the job config is reproducible.
    // Still requires a GitHub webhook pointing at this Jenkins job, and the
    // "GitHub hook trigger for GITScm polling" checkbox enabled once in the
    // job configuration (or use a Multibranch Pipeline, which wires this up
    // automatically for every branch).
    triggers {
        githubPush()
    }

    environment {
        DOCKERHUB_REPO = 'zegzus/learnit-app'
        APP_SERVER_IP  = "${env.APP_SERVER_IP}"
        // Secret text credential holding a Discord webhook URL.
        // Swap this stage for email/Slack/Telegram the same way if you prefer.
        DISCORD_WEBHOOK = credentials('discord-webhook-url')
    }

    stages {
        stage('Build & Test App') {
            steps {
                // Using the plain Maven CLI to match the Dockerfile's maven
                // image. Switch back to ./mvnw only if the Maven Wrapper
                // (mvnw, mvnw.cmd, .mvn/) is actually committed to the repo.
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
            // Only commits to master/main trigger a deployment, as required.
            // Every branch still gets Build & Test + Build & Push above.
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
                            docker pull ${DOCKERHUB_REPO}:latest &&
                            docker stop learnit-app || true &&
                            docker rm learnit-app || true &&
                            docker run -d --name learnit-app -p 8080:8080 --restart unless-stopped ${DOCKERHUB_REPO}:latest
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
