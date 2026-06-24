pipeline {
    agent any

    environment {
        // ─── AWS Configuration ───────────────────────────────────────────────
        AWS_REGION          = "ap-south-1"
        AWS_ACCOUNT_ID      = "469935552565"
        ECR_REGISTRY        = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        ECS_CLUSTER         = "cryptovault-dev-cluster"
        S3_BUCKET           = "cryptovault-dev-frontend-assets"

        // ─── Build Configuration ─────────────────────────────────────────────
        MAVEN_OPTS          = "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
        SONAR_HOST_URL      = "http://sonarqube:9000"

        // ─── DB / JWT (used by integration tests only) ────────────────────────
        DB_HOST             = "postgres-db"
        DB_PORT             = "5432"
        DB_USER             = "postgres"
        DB_PASSWORD         = credentials('cryptovault-db-password')   // Jenkins secret
        DB_NAME             = "cryptovault"
        JWT_SECRET_KEY      = credentials('cryptovault-jwt-secret')     // Jenkins secret

        // ─── Services list ────────────────────────────────────────────────────
        BACKEND_SERVICES    = "auth-service wallet-service transaction-service notification-service risk-service audit-service kyc-service api-gateway"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        ansiColor('xterm')
    }

    stages {

        // ─────────────────────────────────────────────────────────────────────
        stage('Checkout') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                echo '\033[36m=== Stage 1: Checkout Source Code ===\033[0m'
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                    env.GIT_BRANCH_SLUG  = env.GIT_BRANCH?.replaceAll('[^a-zA-Z0-9]', '-')?.toLowerCase() ?: 'main'
                    echo "Branch: ${env.GIT_BRANCH_SLUG} | Commit: ${env.GIT_COMMIT_SHORT}"
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Build Common Library') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                echo '\033[36m=== Stage 2: Build Shared Common Library ===\033[0m'
                dir('backend/common-lib') {
                    sh 'chmod +x mvnw && ./mvnw clean install -DskipTests -q'
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Build & Test Backend') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                echo '\033[36m=== Stage 3: Compile + Unit Test All Microservices ===\033[0m'
                script {
                    def services = env.BACKEND_SERVICES.trim().split(' ')
                    def parallelStages = [:]

                    for (svc in services) {
                        def service = svc   // local capture for closure
                        parallelStages[service] = {
                            dir("backend/${service}") {
                                sh "chmod +x mvnw && ./mvnw clean package -Dmaven.test.failure.ignore=true -q"
                            }
                        }
                    }
                    // Run all service builds in parallel
                    parallel parallelStages
                }
            }
            post {
                always {
                    // Publish JUnit results
                    junit testResults: 'backend/*/target/surefire-reports/*.xml',
                          allowEmptyResults: true
                    // Publish JaCoCo coverage
                    jacoco execPattern:   'backend/*/target/jacoco.exec',
                           classPattern:  'backend/*/target/classes',
                           sourcePattern: 'backend/*/src/main/java',
                           exclusionPattern: '**/entity/**,**/dto/**,**/config/**'
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('SonarQube Analysis') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                echo '\033[36m=== Stage 4: Static Code Analysis — SonarQube ===\033[0m'
                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                    script {
                        def services = env.BACKEND_SERVICES.trim().split(' ')
                        for (svc in services) {
                            def service = svc
                            dir("backend/${service}") {
                                sh """
                                    ./mvnw sonar:sonar \
                                        -Dsonar.host.url=${env.SONAR_HOST_URL} \
                                        -Dsonar.token=${SONAR_TOKEN} \
                                        -Dsonar.projectKey=cryptovault-${service} \
                                        -Dsonar.projectName=${service} -q
                                """
                            }
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Quality Gate') {
        // ─────────────────────────────────────────────────────────────────────
            options { timeout(time: 10, unit: 'MINUTES') }
            steps {
                echo '\033[36m=== Stage 5: Enforcing SonarQube Quality Gate ===\033[0m'
                script {
                    def qg = waitForQualityGate()
                    if (qg.status != 'OK') {
                        error "Pipeline aborted — SonarQube Quality Gate FAILED! Status: ${qg.status}"
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Build Frontend') {
        // ─────────────────────────────────────────────────────────────────────
            agent {
                docker {
                    image 'node:20-alpine'
                    reuseNode true
                    args '--network cryptovault-network'
                }
            }
            steps {
                echo '\033[36m=== Stage 6: Build React Frontend ===\033[0m'
                dir('frontend/web-app') {
                    sh '''
                        npm ci --prefer-offline
                        npm run build
                    '''
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'frontend/web-app/dist/**',
                                     allowEmptyArchive: true,
                                     fingerprint: true
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('ECR Login') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                echo '\033[36m=== Stage 7: Authenticate Docker → AWS ECR ===\033[0m'
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                                  credentialsId: 'aws-credentials']]) {
                    sh """
                        aws ecr get-login-password --region ${AWS_REGION} \
                            | docker login --username AWS \
                              --password-stdin ${ECR_REGISTRY}
                    """
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Build & Push Docker Images') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                echo '\033[36m=== Stage 8: Build & Push Microservice Images to ECR ===\033[0m'
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                                  credentialsId: 'aws-credentials']]) {
                    script {
                        def services = env.BACKEND_SERVICES.trim().split(' ')
                        def parallelBuilds = [:]

                        for (svc in services) {
                            def service = svc
                            parallelBuilds[service] = {
                                def imageLatest = "${ECR_REGISTRY}/cryptovault-${service}:latest"
                                def imageTagged = "${ECR_REGISTRY}/cryptovault-${service}:${env.GIT_COMMIT_SHORT}"

                                sh """
                                    echo "Building ${service}..."
                                    docker build \
                                        -t ${imageLatest} \
                                        -t ${imageTagged} \
                                        -f backend/${service}/Dockerfile \
                                        ./backend

                                    echo "Pushing ${service}..."
                                    docker push ${imageLatest}
                                    docker push ${imageTagged}
                                """
                            }
                        }
                        parallel parallelBuilds
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Deploy to ECS') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                echo '\033[36m=== Stage 9: Rolling Deploy to ECS Fargate ===\033[0m'
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                                  credentialsId: 'aws-credentials'],
                                 string(credentialsId: 'cryptovault-db-password',
                                        variable: 'RDS_PASSWORD')]) {
                    script {
                        def services = env.BACKEND_SERVICES.trim().split(' ')
                        def DB_HOST_RDS = "cryptovault-dev-db.cd2myqieownu.ap-south-1.rds.amazonaws.com"
                        def JWT_SECRET  = "Y29uZmlndXJhYmxlc2VjcmV0a2V5Zm9yY3J5cHRvdmF1bHRhdXRoc2VydmljZWJhc2U2NA=="

                        // Map service name → port
                        def portMap = [
                            'api-gateway'          : 8080,
                            'auth-service'         : 8083,
                            'wallet-service'       : 8081,
                            'transaction-service'  : 8082,
                            'notification-service' : 8084,
                            'risk-service'         : 8085,
                            'audit-service'        : 8086,
                            'kyc-service'          : 8087
                        ]

                        // Fetch IAM roles from existing task definition
                        def tdJson = sh(
                            script: """aws ecs describe-task-definition \
                                          --task-definition cryptovault-dev-api-gateway \
                                          --region ${AWS_REGION} --output json""",
                            returnStdout: true
                        ).trim()
                        def td = readJSON text: tdJson
                        def execRole = td.taskDefinition.executionRoleArn
                        def taskRole = td.taskDefinition.taskRoleArn

                        for (svc in services) {
                            def service  = svc
                            def port     = portMap[service]
                            def family   = "cryptovault-dev-${service}"
                            def ecsName  = "cryptovault-dev-${service}"
                            def imageUri = "${ECR_REGISTRY}/cryptovault-${service}:${env.GIT_COMMIT_SHORT}"
                            def logGroup = "/ecs/${family}"

                            // Build container definition JSON inline
                            def containerDef = [
                                name      : service,
                                image     : imageUri,
                                essential : true,
                                portMappings: [[containerPort: port, hostPort: port]],
                                environment: [
                                    [name: 'DB_HOST',              value: DB_HOST_RDS],
                                    [name: 'DB_PORT',              value: '5432'],
                                    [name: 'DB_USER',              value: 'postgres'],
                                    [name: 'DB_PASSWORD',          value: RDS_PASSWORD],
                                    [name: 'DB_NAME',              value: 'cryptovault'],
                                    [name: 'SPRING_PROFILES_ACTIVE', value: 'dev'],
                                    [name: 'JWT_SECRET_KEY',       value: JWT_SECRET],
                                    [name: 'AUTH_SERVICE_HOST',         value: 'auth-service.cryptovault.local'],
                                    [name: 'WALLET_SERVICE_HOST',        value: 'wallet-service.cryptovault.local'],
                                    [name: 'TRANSACTION_SERVICE_HOST',   value: 'transaction-service.cryptovault.local'],
                                    [name: 'NOTIFICATION_SERVICE_HOST',  value: 'notification-service.cryptovault.local'],
                                    [name: 'RISK_SERVICE_HOST',          value: 'risk-service.cryptovault.local'],
                                    [name: 'AUDIT_SERVICE_HOST',         value: 'audit-service.cryptovault.local'],
                                    [name: 'KYC_SERVICE_HOST',           value: 'kyc-service.cryptovault.local']
                                ],
                                logConfiguration: [
                                    logDriver: 'awslogs',
                                    options  : [
                                        'awslogs-group'         : logGroup,
                                        'awslogs-region'        : AWS_REGION,
                                        'awslogs-stream-prefix' : 'ecs'
                                    ]
                                ]
                            ]

                            def taskDefPayload = [
                                family                  : family,
                                networkMode             : 'awsvpc',
                                requiresCompatibilities : ['FARGATE'],
                                cpu                     : '256',
                                memory                  : '512',
                                executionRoleArn        : execRole,
                                taskRoleArn             : taskRole,
                                containerDefinitions    : [containerDef]
                            ]

                            def tmpFile = "/tmp/td-${service}.json"
                            writeJSON file: tmpFile, json: taskDefPayload, pretty: 2

                            def newTdArn = sh(
                                script: """aws ecs register-task-definition \
                                              --cli-input-json file://${tmpFile} \
                                              --region ${AWS_REGION} \
                                              --query 'taskDefinition.taskDefinitionArn' \
                                              --output text""",
                                returnStdout: true
                            ).trim()

                            echo "Registered: ${newTdArn}"

                            sh """
                                aws ecs update-service \
                                    --cluster ${ECS_CLUSTER} \
                                    --service ${ecsName} \
                                    --task-definition ${newTdArn} \
                                    --region ${AWS_REGION} \
                                    --query 'service.{Status:status,Running:runningCount}' \
                                    --output table
                            """
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Deploy Frontend to S3') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                echo '\033[36m=== Stage 10: Upload React Build → S3 + Invalidate CloudFront ===\033[0m'
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                                  credentialsId: 'aws-credentials']]) {
                    dir('frontend/web-app') {
                        sh """
                            aws s3 sync dist/ s3://${S3_BUCKET}/ \
                                --delete \
                                --cache-control 'max-age=31536000,public' \
                                --exclude 'index.html' \
                                --region ${AWS_REGION}

                            aws s3 cp dist/index.html s3://${S3_BUCKET}/index.html \
                                --cache-control 'no-cache,no-store,must-revalidate' \
                                --region ${AWS_REGION}
                        """
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        stage('Verify Deployment') {
        // ─────────────────────────────────────────────────────────────────────
            steps {
                echo '\033[36m=== Stage 11: Verify ECS Service Stability ===\033[0m'
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                                  credentialsId: 'aws-credentials']]) {
                    script {
                        // Wait up to 5 min for services to stabilise
                        def services = env.BACKEND_SERVICES.trim().split(' ')
                        def allHealthy = false
                        def retries    = 0
                        def maxRetries = 15     // 15 × 20s = 5 min

                        while (!allHealthy && retries < maxRetries) {
                            sleep(time: 20, unit: 'SECONDS')
                            retries++

                            def runningCounts = [:]
                            for (svc in services) {
                                def running = sh(
                                    script: """aws ecs describe-services \
                                                  --cluster ${ECS_CLUSTER} \
                                                  --services cryptovault-dev-${svc} \
                                                  --region ${AWS_REGION} \
                                                  --query 'services[0].runningCount' \
                                                  --output text""",
                                    returnStdout: true
                                ).trim().toInteger()
                                runningCounts[svc] = running
                            }

                            def allUp = runningCounts.every { k, v -> v >= 1 }
                            echo "Attempt ${retries}/${maxRetries} — ${runningCounts}"

                            if (allUp) {
                                allHealthy = true
                                echo "\033[32m✓ All services are RUNNING!\033[0m"
                            }
                        }

                        if (!allHealthy) {
                            error "Deployment verification FAILED — not all services reached running state within timeout."
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    post {
    // ─────────────────────────────────────────────────────────────────────────
        success {
            echo '''
╔══════════════════════════════════════════════════════╗
║   ✅ CryptoVault Pipeline SUCCESS                    ║
║   All microservices deployed to ECS Fargate          ║
║   Frontend synced to S3 / CloudFront                 ║
╚══════════════════════════════════════════════════════╝'''
        }
        failure {
            echo '''
╔══════════════════════════════════════════════════════╗
║   ❌ CryptoVault Pipeline FAILED                     ║
║   Check stage logs above for details                 ║
╚══════════════════════════════════════════════════════╝'''
        }
        always {
            // Clean up dangling images to free disk space on the Jenkins agent
            sh 'docker image prune -f || true'
            cleanWs()
        }
    }
}
