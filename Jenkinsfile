@Library('promebuilder@pipeline')_

pipeline {
  agent any
  parameters {
    booleanParam(
      name: 'skip_tests',
      defaultValue: false,
      description: 'Skip unit tests'
    )
    string(
      name: 'test_markers',
      defaultValue: "not slow and not benchmark",
      description: 'Markers to run'
    )
    booleanParam(
      name: 'windows',
      defaultValue: false,
      description: 'Building also for Windows'
    )
    booleanParam(
      name: 'python3',
      defaultValue: true,
      description: 'Building also for Pytho3'
    )
    booleanParam(
      name: 'force_upload',
      defaultValue: false,
      description: 'Force Anaconda upload, overwriting the same build.'
    )
    string(
      name: 'failure_to',
      defaultValue: "denis.brandolini@prometeia.com",
      description: 'Failed build report'
    )
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
    disableConcurrentBuilds()
  }
  environment {
    PYVER = "2.7"
    PYVER3 = "3.7"
    CONDAENV = "${env.JOB_NAME}_${env.BUILD_NUMBER}_PY2".replace('%2F','_').replace('/', '_')
    CONDAENV3 = "${env.JOB_NAME}_${env.BUILD_NUMBER}_PY3".replace('%2F','_').replace('/', '_')
  }
  stages {
    stage('Bootstrap') {
      steps {
        writeFile file: 'buildnum', text: "${env.BUILD_NUMBER}"
        writeFile file: 'commit', text: "${env.GIT_COMMIT}"
        // env.GIT_BRANCH is wrong when the included library is the same project is builded!
        // writeFile file: 'branch', text: "${env.GIT_BRANCH}"
        script {
          if (isUnix()) {
            writeFile file: 'branch', text: sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).split(" ")[-1].trim()
          } else {
            writeFile file: 'branch', text: bat(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).split(" ")[-1].trim()
          }
          env.GIT_BRANCH = readFile("branch")
        }
        stash(name: "source", useDefaultExcludes: true)
      }
    }
    stage("MultiBuild") {
      parallel {
        stage("Build on Linux - Legacy Python") {
          steps {
            doubleArchictecture('linux', 'base', false, PYVER, CONDAENV, env.GIT_BRANCH.startsWith("develop") ? "conda-forge" : "")
          }
        }
        stage("Build on Windows - Legacy Python") {
          when { expression { return env.GIT_BRANCH == 'master' || params.test_markers == '' || params.windows } }
          steps {
            script {
              try {
                doubleArchictecture('windows', 'base', true, PYVER, CONDAENV, env.GIT_BRANCH.startsWith("develop") ? "conda-forge" : "")
              } catch (exc) {
                echo 'Build failed on Windows Legacy Python'
                echo 'Current build result is' + currentBuild.result
                if (!currentBuild.result || currentBuild.result == 'SUCCESS') {
                  currentBuild.result = 'UNSTABLE'
                } else (
                  currentBuild.result = 'FAILED'
                )
              }
            }
          }
        }
        stage("Build on Linux - Python3") {
          when { expression { return params.python3 } }
          steps {
            doubleArchictecture('linux', 'base', false, PYVER3, CONDAENV3, env.GIT_BRANCH.startsWith("develop") ? "conda-forge" : "")
          }
        }
        stage("Build on Windows - Python3") {
          when { expression { return params.python3 && (env.GIT_BRANCH == 'master' || params.test_markers == '' || params.windows) } }
          steps {
            doubleArchictecture('windows', 'base', true, PYVER3, CONDAENV3, env.GIT_BRANCH.startsWith("develop") ? "conda-forge" : "")
          }
        }
      }
    }
  }
  post {
    always {
      deleteDir()
    }
    failure {
        emailext body: 'Check console output at $BUILD_URL to view the results. \n\n ${CHANGES} \n\n -------------------------------------------------- \n${BUILD_LOG, maxLines=100, escapeHtml=false}',
                to: "${params.failure_to}",
                subject: 'Failed CI Build ${JOB_NAME}'
    }
  }
}
