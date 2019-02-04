@Library('promebuilder@develop')_

pipeline {
  agent any
  parameters {
    booleanParam(
      name: 'skip_tests',
      defaultValue: false,
      description: 'Skip unit tests'
    )
    booleanParam(
      name: 'deep_tests',
      defaultValue: false,
      description: 'Do deep testing (regression, sonarqube, install, etc..)'
    )
    booleanParam(
      name: 'force_upload',
      defaultValue: false,
      description: 'Force Anaconda upload, overwriting the same build.'
    )
    booleanParam(
      name: 'keep_on_fail',
      defaultValue: false,
      description: 'Keep job environment on failed build.'
    )
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
  }
  environment {
    CONDAENV = "${env.JOB_NAME}_${env.BUILD_NUMBER}".replace('%2F','_').replace('/', '_')
    DEEPTESTS = ${env.GIT_BRANCH} == 'master' || params.deep_tests
  }
  stages {
    stage('Bootstrap') {
      steps {
        writeFile file: 'buildnum', text: "${env.BUILD_NUMBER}"
        writeFile file: 'commit', text: "${env.GIT_COMMIT}"
        // env.GIT_BRANCH is wrong when the included library is the same project is builded!
        // writeFile file: 'branch', text: "${env.GIT_BRANCH}"
        writeFile file: 'branch', text: bat(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).split(" ")[-1].trim()
        stash(name: "source", useDefaultExcludes: true)
      }
    }
    stage("MultiBuild") {
      parallel {
        stage("Build on Linux") {
          steps {
            doubleArchictecture('linux')
          }
        }
        stage("Build on Windows") {
          steps {
            doubleArchictecture('windows', 'base', true)
          }
        }
      }
    }
  }
  post {
    always {
      deleteDir()
    }
    success {
      slackSend color: "good", message: "Builded ${env.JOB_NAME} (<${env.BUILD_URL}|Open>)"
    }
    failure {
      slackSend color: "warning", message: "Failed ${env.JOB_NAME} (<${env.BUILD_URL}|Open>)"
    }
  }
}
