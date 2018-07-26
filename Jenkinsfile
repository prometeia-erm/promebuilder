pipeline {
  agent any
  environment {
      CONDAENV = calc_conda_env()
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
      stage('SetUp') {
        echo "Setup on ${envlabel}, conda environment ${CONDAENV}"
        unstash "source"
        condashellcmd("conda create -y -n ${CONDAENV} python=2.7", condaenvb)
        condashellcmd("conda install -y --file build-requirements.txt", CONDAENV)
        echo "Checking package and channel names"
        condashellcmd("python setup.py --name", CONDAENV)
        if (readFile('channel')) {
          echo "Adding to environment channel " + readFile('channel')
          condashellcmd("conda config --env --add channels t/${env.ANACONDA_TOKEN}/prometeia/channel/" + readFile('channel'), CONDAENV)
        } else {
          echo "WARNING: Build without channel!"
        }
        condashellcmd("conda config --show channels", CONDAENV)
        echo "Installing requiremets on conda environment ${CONDAENV}"
        condashellcmd("conda install -y --file requirements.txt", CONDAENV)
      }
      stage('Test') {
        condashellcmd("python setup.py pytest", CONDAENV)
      }
      stage('Build') {
        script {
          writeFile file: 'buildoutput', text: condashellcmd("python setup.py bdist_conda", CONDAENV, true).trim()
          writeFile file: 'packagename', text: readFile('buildoutput').split(" ")[-1]
        }
        echo "BUILD OUTPUT: \n\n ================ \n" + readFile('buildoutput') + "\n ================ \n"
        echo "PACKAGENAME: " + readFile('packagename')
      }
      stage('Install') {
        echo "Creating indipendent test environment test_${CONDAENV}"
        condashellcmd("conda create -y -n test_${CONDAENV} python=2.7", condaenvb)
        if (readFile('channel')) {
          condashellcmd("conda config --env --add channels t/${env.ANACONDA_TOKEN}/prometeia/channel/" + readFile('channel'), "test_${CONDAENV}")
        }
        echo "Installing package on test environment test_${CONDAENV}"
        condashellcmd("conda install -y " + readFile('packagename'), "final_${CONDAENV}")
        condashellcmd("conda env remove -y -n test_${CONDAENV}", condaenvb)
      }
      stage('Upload') {
        if (readFile('channel')) {
          echo "Uploading " + readFile('packagename') + " to label " + readFile('channel')
          condashellcmd("anaconda upload " + readFile('packagename') + " --label " + readFile('channel'), condaenvb)
        }
      }
     stage('ConvertUpload32bit') {
        if (!isUnix()) {
          echo "Converting and Uploading package for win32"
          condashellcmd("conda convert " + readFile('packagename') + " -p win-32 && anaconda upload win-32\\* --label " + readFile('channel') + " && del win-32\\* /Q", condaenvb)
        }
      }
      stage('TearDown') {
        condashellcmd("conda env remove -y -n build_${CONDAENV}", condaenvb)
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