// Jenkins 声明式流水线 —— 适配自建 Jenkins（CentOS7）
// 前置：Jenkins 安装 JDK21、Maven、NodeJS 工具，或节点已装好；docker 可选。
pipeline {
  agent any

  environment {
    MAVEN_CLI = 'mvn -B -ntp'
  }

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  stages {
    stage('后端构建+测试') {
      steps {
        dir('server/license-platform') { sh "${MAVEN_CLI} verify" }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'server/license-platform/target/surefire-reports/TEST-*.xml'
          archiveArtifacts artifacts: 'server/license-platform/target/site/jacoco/**', allowEmptyArchive: true
        }
      }
    }

    stage('SDK 构建+测试') {
      steps {
        dir('sdk/license-sdk') { sh "${MAVEN_CLI} install" }
        dir('examples/protected-app') { sh "${MAVEN_CLI} -DskipTests compile" }
      }
      post {
        always { junit allowEmptyResults: true, testResults: 'sdk/license-sdk/target/surefire-reports/TEST-*.xml' }
      }
    }

    stage('签名链路冒烟 (Ed25519/SM2)') {
      matrix {
        axes { axis { name 'ALGO'; values 'ed25519', 'sm2' } }
        stages {
          stage('smoke') {
            steps {
              dir('sdk/license-sdk') { sh "${MAVEN_CLI} -DskipTests package" }
              dir('server/license-platform') { sh "${MAVEN_CLI} -DskipTests package" }
              sh '''
                cd server/license-platform
                LICENSE_SIGN_ALGO=$ALGO nohup java -jar target/license-platform-1.0.1.jar > /tmp/be-$ALGO.log 2>&1 &
                echo $! > /tmp/be-$ALGO.pid
                for i in $(seq 1 60); do curl -sf http://localhost:8080/actuator/health && break || sleep 2; done
                cd ../..
                TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"8888"}' | sed -E 's/.*"token":"([^"]+)".*/\\1/')
                EXP=$([ "$ALGO" = "sm2" ] && echo SM2 || echo Ed25519)
                ALG=$(curl -s http://localhost:8080/api/licenses/public-key -H "Authorization: Bearer $TOKEN" | sed -E 's/.*"algorithm":"([^"]+)".*/\\1/')
                test "$ALG" = "$EXP"
                curl -s -X POST http://localhost:8080/api/licenses/issue -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"tenantCode":"T-CI","customer":"ci","edition":"ENTERPRISE","modules":["RISK","REPORT"],"features":{"REPORT.EXPORT":true},"appVersionRange":">=2.0.0 <3.0.0","notBefore":"2026-01-01","notAfter":"2099-01-01","concurrency":5}' >/dev/null
                LID=$(curl -s http://localhost:8080/api/licenses -H "Authorization: Bearer $TOKEN" | grep -oE '"licenseId":"LIC-[0-9-]+"' | head -1 | cut -d\\" -f4)
                PUB=$(curl -s http://localhost:8080/api/licenses/public-key -H "Authorization: Bearer $TOKEN" | sed -E 's/.*"publicKeyBase64":"([^"]+)".*/\\1/')
                curl -s "http://localhost:8080/api/licenses/$LID/download" -H "Authorization: Bearer $TOKEN" -o ci.lic
                java -jar sdk/license-sdk/target/license-sdk-demo.jar check "$PUB" ci.lic REPORT.EXPORT 2.4.0
                kill $(cat /tmp/be-$ALGO.pid) || true
              '''
            }
          }
        }
      }
    }

    stage('前端构建') {
      steps { dir('web/console') { sh 'npm install && npm run build' } }
      post { always { archiveArtifacts artifacts: 'web/console/dist/**', allowEmptyArchive: true } }
    }

    stage('端到端 E2E') {
      steps {
        sh '''
          cd server/license-platform
          nohup java -jar target/license-platform-1.0.1.jar > /tmp/be-e2e.log 2>&1 &
          echo $! > /tmp/be-e2e.pid
          for i in $(seq 1 60); do curl -sf http://localhost:8080/actuator/health && break || sleep 2; done
          cd ../../web/console
          npm install
          npx playwright install --with-deps chromium
          npx playwright test
          kill $(cat /tmp/be-e2e.pid) || true
        '''
      }
      post { always { archiveArtifacts artifacts: 'web/console/playwright-report/**', allowEmptyArchive: true } }
    }

    stage('代码加固 + 镜像') {
      steps {
        dir('examples/protected-app') { sh "${MAVEN_CLI} -Pharden -DskipTests package" }
        // 可选：docker build -t registry/codeman-backend:${BUILD_NUMBER} ./server/license-platform
      }
      post { always { archiveArtifacts artifacts: 'examples/protected-app/target/*-obf.jar', allowEmptyArchive: true } }
    }
  }

  post {
    success { echo 'CI 通过：后端/SDK/签名冒烟/前端/E2E/加固 全部成功' }
    always  { sh 'pkill -f license-platform-1.0.1.jar || true' }
  }
}
