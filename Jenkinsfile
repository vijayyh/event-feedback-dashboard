// Event Feedback Dashboard – CI/CD pipeline (Windows agent: bat / powershell steps)
// Checkout -> Build -> Unit & API tests -> UI tests (Selenium) -> Package -> Docker image (build + push)
//   -> Deploy to Tomcat -> Smoke test -> Deploy container (fresh container from the registry + smoke test)
//
// Jenkins prerequisites (Manage Jenkins):
//   Tools:        JDK "JDK-21" (C:\DevTools\jdk-21) and Maven "Maven-3.9.16"
//   Credentials:  Secret text "efd-organiser-password" – organiser sign-in password of the deployed app
//   Tomcat 11:    installed in TOMCAT_HOME. The pipeline runs its own Tomcat instance (CATALINA_BASE = TOMCAT_BASE)
//                 on JDK 21, so the Windows Tomcat service and its settings are not touched.
//   Docker:       Docker Desktop running; local registry on REGISTRY
//                 (docker run -d --name efd-registry --restart always -p 5000:5000 -v efd-registry-data:/var/lib/registry registry:2)
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
               description: 'Tomcat 11 installation (CATALINA_HOME): binaries and default configuration.')
        string(name: 'TOMCAT_BASE', defaultValue: 'C:\\ProgramData\\event-feedback-dashboard\\tomcat',
               description: 'Tomcat instance folder (CATALINA_BASE) managed by this pipeline: conf, webapps, logs.')
        string(name: 'TOMCAT_URL', defaultValue: 'http://localhost:8081',
               description: 'Base URL of the Tomcat instance; its port is the HTTP port the instance listens on.')
        string(name: 'REGISTRY', defaultValue: 'localhost:5000',
               description: 'Docker registry that receives the versioned image.')
        string(name: 'CONTAINER_PORT', defaultValue: '8085',
               description: 'Host port of the application container deployed by the pipeline.')
    }

    environment {
        // production -> /event-feedback-dashboard, staging -> /event-feedback-dashboard-staging
        APP_CONTEXT = "${params.DEPLOY_ENV == 'production' ? 'event-feedback-dashboard' : 'event-feedback-dashboard-' + params.DEPLOY_ENV}"
        DOCKER      = 'C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe'
        // parameters as explicit variables: Jenkins does not export a newly added parameter on its first run
        DOCKER_REGISTRY = "${params.REGISTRY}"
        APP_PORT        = "${params.CONTAINER_PORT}"
        IMAGE_NAME  = "${params.REGISTRY}/event-feedback-dashboard"
        CONTAINER   = 'efd-app'
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

        stage('Docker image') {
            when {
                expression { currentBuild.resultIsBetterOrEqualTo('SUCCESS') }
            }
            steps {
                // Versioned image of the tested WAR: <maven version>-<build number>-<commit>, e.g. 1.0.0-SNAPSHOT-12-7672792
                powershell '''
                    $env:PATH = "$(Split-Path $env:DOCKER);$env:PATH"   # docker.exe runs its credential helpers from the same folder
                    function Docker { & $env:DOCKER @args 2>&1 | ForEach-Object { "$_" }; if ($LASTEXITCODE -ne 0) { throw "docker $($args -join ' ') failed with exit code $LASTEXITCODE" } }
                    $line = Select-String -Path 'target\\classes\\META-INF\\build-info.properties' -Pattern '^build\\.version=' | Select-Object -First 1
                    $version = $line.Line.Substring('build.version='.Length)
                    $commit = (git rev-parse --short=7 HEAD).Trim()
                    $tag = "$version-$env:BUILD_NUMBER-$commit"
                    $image = "${env:IMAGE_NAME}:${tag}"

                    Docker build --progress=plain --build-arg "VERSION=$version" --build-arg "REVISION=$commit" `
                        --label "jenkins.build=$env:BUILD_URL" -t $image -t "${env:IMAGE_NAME}:latest" .
                    Docker push $image
                    Docker push "${env:IMAGE_NAME}:latest"

                    $digest = & $env:DOCKER inspect --format '{{index .RepoDigests 0}}' $image
                    Write-Host "Published $image"
                    Write-Host "Digest    $digest"
                    $tags = Invoke-RestMethod "http://$env:DOCKER_REGISTRY/v2/event-feedback-dashboard/tags/list" -UseBasicParsing
                    Write-Host "Registry $env:DOCKER_REGISTRY now holds: $($tags.tags -join ', ')"

                    # hand the exact image to the deploy stage and keep it with the build
                    Set-Content -Path 'target\\docker-image.txt' -Value $image -Encoding ascii
                    Set-Content -Path 'target\\docker-image-digest.txt' -Value $digest -Encoding ascii
                '''
                archiveArtifacts artifacts: 'target/docker-image*.txt'
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
                        $catalinaHome = $env:TOMCAT_HOME
                        $base    = $env:TOMCAT_BASE
                        $context = $env:APP_CONTEXT
                        $port    = ([uri]$env:TOMCAT_URL).Port
                        Write-Host "Deploying $context ($env:DEPLOY_ENV) to the Tomcat 11 instance in $base, port $port"

                        # 1. First deployment: create the Tomcat instance (CATALINA_BASE) from the installation's configuration
                        if (-not (Test-Path (Join-Path $base 'conf\\server.xml'))) {
                            foreach ($d in 'conf', 'logs', 'temp', 'webapps', 'work') {
                                New-Item -ItemType Directory -Force (Join-Path $base $d) | Out-Null
                            }
                            Copy-Item (Join-Path $catalinaHome 'conf\\*') (Join-Path $base 'conf') -Recurse -Force
                            $serverXml = Join-Path $base 'conf\\server.xml'
                            $text = [IO.File]::ReadAllText($serverXml)
                            $text = $text -replace 'Connector port="[0-9]+" protocol="HTTP/1.1"', "Connector port=`"$port`" protocol=`"HTTP/1.1`""
                            $text = $text -replace '<Server port="-1"', '<Server port="8015"'
                            [IO.File]::WriteAllText($serverXml, $text)
                            Write-Host "Created Tomcat instance in $base"
                        }

                        # 2. Environment settings for this deployment: Tomcat passes <Parameter> values to the app
                        #    as servlet context parameters, which Spring Boot reads like application.properties.
                        $esc = [System.Security.SecurityElement]
                        $seed = $env:SEED_DEMO_DATA.ToLower()
                        $xml = @"
<?xml version="1.0" encoding="UTF-8"?>
<!-- Written by the Jenkins pipeline EFD-Pipeline - do not edit by hand -->
<Context>
    <Parameter name="app.environment" value="$($esc::Escape($env:DEPLOY_ENV))" override="false"/>
    <Parameter name="app.seed-demo-data" value="$seed" override="false"/>
    <Parameter name="app.data-dir" value="`${user.home}/.event-feedback-dashboard/$($env:DEPLOY_ENV)" override="false"/>
    <Parameter name="spring.security.user.password" value="$($esc::Escape($env:ORGANISER_PASSWORD))" override="false"/>
</Context>
"@
                        $ctxDir = Join-Path $base 'conf\\Catalina\\localhost'
                        New-Item -ItemType Directory -Force $ctxDir | Out-Null
                        # Tomcat redeploys the app whenever this file changes, and the WAR copy below already
                        # triggers a reload, so the file is only rewritten when a setting really changed
                        $ctxFile = Join-Path $ctxDir "$context.xml"
                        if (-not (Test-Path $ctxFile) -or [IO.File]::ReadAllText($ctxFile) -ne $xml) {
                            [IO.File]::WriteAllText($ctxFile, $xml)
                            Write-Host "Context settings written to $ctxFile"
                        } else {
                            Write-Host 'Context settings unchanged'
                        }

                        # 3. Hot deploy: Tomcat (autoDeploy=true) picks up the new WAR
                        Copy-Item 'target\\event-feedback-dashboard.war' (Join-Path $base "webapps\\$context.war") -Force

                        # 4. Start the instance if nothing listens on the port yet. JENKINS_NODE_COOKIE=dontKillMe keeps
                        #    Tomcat running after this build ends (Jenkins otherwise stops processes a build started).
                        if (-not (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)) {
                            $env:CATALINA_HOME = $catalinaHome
                            $env:CATALINA_BASE = $base
                            $env:CATALINA_OPTS = '-Xms256m -Xmx1024m'
                            $env:JENKINS_NODE_COOKIE = 'dontKillMe'
                            $env:BUILD_ID = 'dontKillMe'
                            Start-Process -FilePath (Join-Path $catalinaHome 'bin\\catalina.bat') -ArgumentList 'start' -WorkingDirectory $base -WindowStyle Hidden
                            Write-Host "Started Tomcat 11 on port $port with JAVA_HOME=$env:JAVA_HOME"
                        } else {
                            Write-Host "Tomcat is already running on port $port - the new WAR is redeployed automatically"
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

                    # Tomcat picks up the new WAR a few seconds after the copy, so the previous version may still
                    # answer. Wait until the server reports the build time of the WAR built in this run.
                    $line = Select-String -Path 'target\\classes\\META-INF\\build-info.properties' -Pattern '^build\\.time=' | Select-Object -First 1
                    $expected = [DateTimeOffset]$line.Line.Substring('build.time='.Length).Replace('\\:', ':')
                    Write-Host "Waiting for build $($expected.ToString('o')) at $base"
                    $deadline = (Get-Date).AddMinutes(3)
                    do {
                        Start-Sleep -Seconds 5
                        try { $deployed = [DateTimeOffset](Invoke-RestMethod "$base/actuator/info" -TimeoutSec 5 -UseBasicParsing).build.time } catch { $deployed = $null }
                        Write-Host ("deployed build: " + $(if ($deployed) { $deployed.ToString('o') } else { 'not reachable yet' }))
                    } until ($deployed -eq $expected -or (Get-Date) -gt $deadline)
                    if ($deployed -ne $expected) { throw "The new build did not come up at $base" }

                    $health = Invoke-RestMethod "$base/actuator/health" -TimeoutSec 10 -UseBasicParsing
                    Write-Host "health: $($health.status)"
                    if ($health.status -ne 'UP') { throw "Application is not healthy at $base" }

                    $info = Invoke-RestMethod "$base/api/info" -UseBasicParsing
                    Write-Host "Running version $($info.version) in environment '$($info.environment)'"
                    if ($info.environment -ne $env:DEPLOY_ENV) { throw "Expected environment $env:DEPLOY_ENV but got $($info.environment)" }
                    $page = Invoke-WebRequest "$base/" -UseBasicParsing
                    $type = $page.Headers['Content-Type']
                    Write-Host "Home page Content-Type: $type"
                    if ($type -notmatch 'charset=UTF-8') { throw "Home page is not served as UTF-8 ($type)" }
                    $events = Invoke-RestMethod "$base/api/events" -UseBasicParsing
                    Write-Host "Smoke test passed: $($events.Count) events served at $base/"
                '''
            }
        }

        stage('Deploy container') {
            when {
                expression { currentBuild.resultIsBetterOrEqualTo('SUCCESS') }
            }
            steps {
                withCredentials([string(credentialsId: 'efd-organiser-password', variable: 'SPRING_SECURITY_USER_PASSWORD')]) {
                    powershell '''
                        $env:PATH = "$(Split-Path $env:DOCKER);$env:PATH"   # docker.exe runs its credential helpers from the same folder
                        function Docker { & $env:DOCKER @args 2>&1 | ForEach-Object { "$_" }; if ($LASTEXITCODE -ne 0) { throw "docker $($args -join ' ') failed with exit code $LASTEXITCODE" } }
                        $image = (Get-Content 'target\\docker-image.txt').Trim()
                        $previous = & $env:DOCKER inspect --format '{{.Config.Image}}' $env:CONTAINER 2>$null
                        Write-Host "Running before: $(if ($previous) { $previous } else { 'no container' })"

                        # Deploy what the registry serves (not the local build cache), as a fresh container.
                        # The database lives in the volume, so data survives the replacement.
                        Docker pull $image
                        & $env:DOCKER rm -f $env:CONTAINER 2>$null | Out-Null
                        Docker run -d --name $env:CONTAINER --restart unless-stopped -p "$($env:APP_PORT):8080" --memory 512m `
                            -e APP_ENVIRONMENT=docker -e SPRING_SECURITY_USER_PASSWORD -v efd-app-data:/data `
                            --label "jenkins.build=$env:BUILD_NUMBER" $image

                        # Smoke test: Docker health check, then the build time and environment the app reports
                        $deadline = (Get-Date).AddMinutes(3)
                        do {
                            Start-Sleep -Seconds 5
                            $health = & $env:DOCKER inspect --format '{{.State.Health.Status}}' $env:CONTAINER
                            Write-Host "container health: $health"
                        } until ($health -eq 'healthy' -or $health -eq 'unhealthy' -or (Get-Date) -gt $deadline)
                        if ($health -ne 'healthy') { & $env:DOCKER logs --tail 40 $env:CONTAINER; throw "Container $env:CONTAINER is $health" }

                        $base = "http://localhost:$env:APP_PORT"
                        $line = Select-String -Path 'target\\classes\\META-INF\\build-info.properties' -Pattern '^build\\.time=' | Select-Object -First 1
                        $expected = [DateTimeOffset]$line.Line.Substring('build.time='.Length).Replace('\\:', ':')
                        $deployed = [DateTimeOffset](Invoke-RestMethod "$base/actuator/info" -UseBasicParsing).build.time
                        if ($deployed -ne $expected) { throw "Container serves build $deployed, expected $expected" }
                        $info = Invoke-RestMethod "$base/api/info" -UseBasicParsing
                        if ($info.environment -ne 'docker') { throw "Expected environment docker but got $($info.environment)" }
                        & $env:DOCKER ps --filter "name=$env:CONTAINER" --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'
                        Write-Host "Container smoke test passed: build $($expected.ToString('o')), version $($info.version) at $base/"

                        # Housekeeping: keep the five newest image tags locally (the registry keeps all of them)
                        $old = & $env:DOCKER images $env:IMAGE_NAME --format '{{.Tag}}' | Where-Object { $_ -match '-(\\d+)-[0-9a-f]{7}$' } |
                            Sort-Object { [int]($_ -replace '^.*-(\\d+)-[0-9a-f]{7}$', '$1') } -Descending | Select-Object -Skip 5
                        foreach ($t in $old) { & $env:DOCKER rmi "${env:IMAGE_NAME}:${t}" | Out-Null }
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "Deployed ${env.APP_CONTEXT} (${params.DEPLOY_ENV}): ${params.TOMCAT_URL}/${env.APP_CONTEXT}/"
            echo "Container ${env.CONTAINER}: http://localhost:${params.CONTAINER_PORT}/ (image in ${params.REGISTRY})"
        }
        failure {
            echo 'Pipeline failed - deployment was skipped. See the test reports and archived Selenium screenshots.'
        }
        unstable {
            echo 'Tests reported failures - deployment was skipped.'
        }
    }
}
