// Event Feedback Dashboard – CI/CD pipeline (Windows agent: bat / powershell steps)
// Checkout -> Build -> Unit & API tests -> UI tests (Selenium) -> Package -> Deploy to Tomcat -> Smoke test
//
// Jenkins prerequisites (Manage Jenkins):
//   Tools:        JDK "JDK-21" (C:\DevTools\jdk-21) and Maven "Maven-3.9.16"
//   Credentials:  Secret text "efd-organiser-password" – organiser sign-in password of the deployed app
//   Tomcat 11:    Windows service running on Java 17+ (Java 21 here), HTTP port 8081
pipeline {
    agent any

    tools {
        jdk 'JDK-21'
        maven 'Maven-3.9.16'
    }

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['staging', 'production'],
               description: 'Target environment. Sets the Tomcat context path, the data folder and the environment name the app reports.')
        booleanParam(name: 'SEED_DEMO_DATA', defaultValue: true,
               description: 'Fill an empty database with sample events and feedback on first start.')
        string(name: 'TOMCAT_HOME', defaultValue: 'C:\\Program Files\\Apache Software Foundation\\Tomcat 11.0',
               description: 'Tomcat 11 installation folder (webapps and conf are written here).')
        string(name: 'TOMCAT_SERVICE', defaultValue: 'Tomcat11',
               description: 'Windows service name of Tomcat; started if it is not running.')
        string(name: 'TOMCAT_URL', defaultValue: 'http://localhost:8081',
               description: 'Base URL of Tomcat, used by the smoke test.')
    }

    environment {
        // production -> /event-feedback-dashboard, staging -> /event-feedback-dashboard-staging
        APP_CONTEXT = "${params.DEPLOY_ENV == 'production' ? 'event-feedback-dashboard' : 'event-feedback-dashboard-' + params.DEPLOY_ENV}"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
    }

    triggers {
        pollSCM('H/2 * * * *')   // GitHub cannot reach localhost, so Jenkins polls for new commits
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                bat 'git log -1 --format="%%h %%s (%%an)"'
            }
        }

        stage('Build') {
            steps {
                bat 'mvn -B -ntp clean compile'
            }
        }

        stage('Unit & API tests') {
            steps {
                bat 'mvn -B -ntp test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('UI tests (Selenium)') {
            steps {
                // starts the app on a random port and drives headless Chrome through the user journeys;
                // also builds the WAR and enforces the coverage check
                bat 'mvn -B -ntp verify -DskipUTs=true'
            }
            post {
                always {
                    junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: 'target/selenium-screenshots/**', allowEmptyArchive: true
                }
            }
        }

        stage('Package') {
            steps {
                archiveArtifacts artifacts: 'target/*.war', fingerprint: true
                archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
            }
        }

        stage('Deploy to Tomcat') {
            when {
                // failed tests stop deployment: only a fully green build may be deployed
                expression { currentBuild.resultIsBetterOrEqualTo('SUCCESS') }
            }
            steps {
                withCredentials([string(credentialsId: 'efd-organiser-password', variable: 'ORGANISER_PASSWORD')]) {
                    powershell '''
                        $ErrorActionPreference = 'Stop'
                        $tomcat  = $env:TOMCAT_HOME
                        $context = $env:APP_CONTEXT
                        Write-Host "Deploying $context ($env:DEPLOY_ENV) to $tomcat"

                        # 1. Environment settings for this deployment: Tomcat passes <Parameter> values to the app
                        #    as servlet context parameters, which Spring Boot reads like application.properties.
                        $esc = [System.Security.SecurityElement]
                        $seed = $env:SEED_DEMO_DATA.ToLower()
                        $xml = @"
<?xml version="1.0" encoding="UTF-8"?>
<!-- Written by Jenkins build $env:BUILD_NUMBER - do not edit by hand -->
<Context>
    <Parameter name="app.environment" value="$($esc::Escape($env:DEPLOY_ENV))" override="false"/>
    <Parameter name="app.seed-demo-data" value="$seed" override="false"/>
    <Parameter name="app.data-dir" value="`${user.home}/.event-feedback-dashboard/$($env:DEPLOY_ENV)" override="false"/>
    <Parameter name="spring.security.user.password" value="$($esc::Escape($env:ORGANISER_PASSWORD))" override="false"/>
</Context>
"@
                        $ctxDir = Join-Path $tomcat 'conf\\Catalina\\localhost'
                        New-Item -ItemType Directory -Force $ctxDir | Out-Null
                        [IO.File]::WriteAllText((Join-Path $ctxDir "$context.xml"), $xml)

                        # 2. Hot deploy: Tomcat (autoDeploy=true) picks up the new WAR
                        Copy-Item 'target\\event-feedback-dashboard.war' (Join-Path $tomcat "webapps\\$context.war") -Force

                        # 3. Make sure Tomcat is running
                        $svc = Get-Service -Name $env:TOMCAT_SERVICE
                        if ($svc.Status -ne 'Running') {
                            Write-Host "Starting service $($env:TOMCAT_SERVICE)"
                            Start-Service -Name $env:TOMCAT_SERVICE
                        }
                        Write-Host "Deployed to $env:TOMCAT_URL/$context/"
                    '''
                }
            }
        }

        stage('Smoke test') {
            when {
                expression { currentBuild.resultIsBetterOrEqualTo('SUCCESS') }
            }
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'
                    $base = "$env:TOMCAT_URL/$env:APP_CONTEXT"
                    $deadline = (Get-Date).AddMinutes(3)
                    do {
                        Start-Sleep -Seconds 5
                        try { $health = Invoke-RestMethod "$base/actuator/health" -TimeoutSec 5 -UseBasicParsing } catch { $health = $null }
                        Write-Host ("health: " + $(if ($health) { $health.status } else { 'not reachable yet' }))
                    } until (($health -and $health.status -eq 'UP') -or (Get-Date) -gt $deadline)
                    if (-not $health -or $health.status -ne 'UP') { throw "Application did not become healthy at $base" }

                    $info = Invoke-RestMethod "$base/api/info" -UseBasicParsing
                    Write-Host "Running version $($info.version) in environment '$($info.environment)'"
                    if ($info.environment -ne $env:DEPLOY_ENV) { throw "Expected environment $env:DEPLOY_ENV but got $($info.environment)" }
                    $events = Invoke-RestMethod "$base/api/events" -UseBasicParsing
                    Write-Host "Smoke test passed: $($events.Count) events served at $base/"
                '''
            }
        }
    }

    post {
        success {
            echo "Deployed ${env.APP_CONTEXT} (${params.DEPLOY_ENV}): ${params.TOMCAT_URL}/${env.APP_CONTEXT}/"
        }
        failure {
            echo 'Pipeline failed - deployment was skipped. See the test reports and archived Selenium screenshots.'
        }
        unstable {
            echo 'Tests reported failures - deployment was skipped.'
        }
    }
}
