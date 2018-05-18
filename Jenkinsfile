pipeline {
  agent any
  environment {
      CONDAENV = calc_conda_env()
  }
  stages {
    stage('SetUp') {
      steps {
        writeFile file: 'buildnum', text: "${env.BUILD_NUMBER}"
        writeFile file: 'commit', text: "${env.GIT_COMMIT}"
        writeFile file: 'branch', text: "${env.GIT_BRANCH}"
        stash(name: "source", useDefaultExcludes: true)
      }
    }
    stage("MultiBuild") {
      steps {
        parallel(
          "Build on Windows": {
            builder("windows")
          },
          "Build on Linux": {
            builder("linux")
          }
        )
      }
    }
  }
}


def calc_conda_env() {
    return "${env.JOB_NAME}_${env.BUILD_NUMBER}".replace('%2F','_').replace('/', '_')
}


def builder(envlabel, condaenvb="base") {
  node(envlabel) {
    pipeline {
      stage('Unstash') {
        unstash "source"
      }
      stage('Test') {
        condashellcmd("conda create -y -n test_${CONDAENV} python=2.7", condaenvb)
        condashellcmd("conda install -y --file requirements.txt", "test_${CONDAENV}")
        condashellcmd("python setup.py pytest", "test_${CONDAENV}")
        condashellcmd("conda env remove -y -n test_${CONDAENV}", condaenvb)
      }
     stage('Build') {
        echo "Building on ${envlabel}, conda environment ${condaenvb}"
        unstash "source"
        script {
          writeFile file: 'buildoutput', text: condashellcmd("python setup.py bdist_conda", condaenvb, true).trim()
          writeFile file: 'packagename', text: readFile('buildoutput').split(" ")[-1]
        }
        echo "BUILD OUTPUT: \n\n ================ \n" + readFile('buildoutput') + "\n ================ \n"
        echo "PACKAGENAME: " + readFile('packagename')
      }
      stage('Install') {
        echo "Installing on indipendent test environment"
        condashellcmd("conda create -y -n ${CONDAENV} python=2.7", condaenvb)
        condashellcmd("conda install -y " + readFile('packagename'), CONDAENV)
        condashellcmd("conda env remove -y -n ${CONDAENV}", condaenvb)
      }
      stage('Upload') {
        if (readFile('channel')) {
          echo "Uploading " + readFile('packagename') + " to label " + readFile('channel')
          condashellcmd("anaconda upload " + readFile('packagename') + " --label " + readFile('channel'), condaenvb)
        }
      }
      stage('TearDown') {
        deleteDir()
      }
    }
  }
}


void shellcmd(command, returnStdout=false, inPlace=False) {
  if (isUnix() && inPlace) {
    return sh(script: "source ${command}", returnStdout: returnStdout)
  } else if (isUnix()) {
    return sh(script: command, returnStdout: returnStdout)
  } else {
    return bat(script:command, returnStdout: returnStdout)
  }
}


void condashellcmd(command, condaenv, returnStdout=false) {
  if (isUnix()) {
    return sh(script: "source /home/jenkins/miniconda2/bin/activate ${condaenv}; ${command}", returnStdout: returnStdout)
  } else {
    return bat(script: "activate ${condaenv} && ${command} && deactivate", returnStdout: returnStdout)
  }
}