@Library('promebuilder')_

pipeline {
  agent any
  environment {
      CONDAENV = "${env.JOB_NAME}_${env.BUILD_NUMBER}".replace('%2F','_').replace('/', '_')
  }
  stages {
    stage('Bootstrap') {
      steps {
        writeFile file: 'buildnum', text: "${env.BUILD_NUMBER}"
        writeFile file: 'commit', text: "${env.GIT_COMMIT}"
        writeFile file: 'branch', text: "${env.GIT_BRANCH}"
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
    stage("CleanUp") {
        steps {
            deleteDir()
        }
    }
  }
}
