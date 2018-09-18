#!/usr/bin/env groovy

def call(envlabel, condaenvb="base", convert32=false) {
  node(envlabel) {
    pipeline {
      stage('SetUp') {
        echo "Existing conda envs"
        condaShellCmd("conda info --envs", condaenvb)
        echo "Setup on ${envlabel}, conda environment ${CONDAENV}"
        unstash "source"
        condaShellCmd("conda create -y -n ${CONDAENV} python=2.7", condaenvb)
        condaShellCmd("conda install -q -y --file build-requirements.txt --force", CONDAENV)
        echo "Checking package and channel names"
        condaShellCmd("python setup.py --name", CONDAENV)
        if (readFile('channel')) {
          echo "Adding to environment channel " + readFile('channel')
          condaShellCmd("conda config --env --add channels t/${env.ANACONDA_TOKEN}/prometeia/channel/" + readFile('channel'), CONDAENV)
        } else {
          echo "WARNING: Build without channel, adding 'develop' as default fallback channel:"
          condaShellCmd("conda config --env --add channels t/${env.ANACONDA_TOKEN}/prometeia/channel/develop", CONDAENV)
        }
        condaShellCmd("conda config --show channels", CONDAENV)
        echo "INFO: Installing requiremets on conda environment ${CONDAENV}"
        condaShellCmd("conda install -q -y --file requirements.txt", CONDAENV)
      }
      stage('UnitTests') {
        if (! params?.skip_tests) {
          // Forced reinstall to avoid annoying wrong setuptools usage
          condaShellCmd("conda update -q setuptools --force", CONDAENV)
          condaShellCmd("python setup.py develop", CONDAENV)
          condaShellCmd("pytest", CONDAENV)
        }
      }
      stage('SonarScanner') {
        if (! params?.skip_tests) {
          condaShellCmd("sonar-scanner -D sonar.projectVersion=" + readFile('version') , CONDAENV)
        }
      }
      stage('Build') {
        script {
          writeFile file: 'buildoutput', text: condaShellCmd("python setup.py bdist_conda", CONDAENV, true).trim()
          writeFile file: 'packagename', text: readFile('buildoutput').split(" ")[-1]
        }
        echo "BUILD OUTPUT: \n\n ================ \n" + readFile('buildoutput') + "\n ================ \n"
        echo "PACKAGENAME: " + readFile('packagename')
      }
      stage('Install') {
        if (! params?.skip_tests) {
          echo "Creating indipendent test environment test_${CONDAENV}"
          condaShellCmd("conda create -q -y -n test_${CONDAENV} python=2.7", condaenvb)
          if (readFile('channel')) {
            condaShellCmd("conda config --env --add channels t/${env.ANACONDA_TOKEN}/prometeia/channel/" + readFile('channel'), "test_${CONDAENV}")
          }
          echo "Installing package on test environment test_${CONDAENV}"
          condaShellCmd("conda install -q -y " + readFile('packagename'), "test_${CONDAENV}")
          condaShellCmd("conda env remove -y -n test_${CONDAENV}", condaenvb)
        }
      }
      stage('Upload') {
        if (readFile('channel')) {
          echo "Uploading " + readFile('packagename') + " to label " + readFile('channel')
          if (! params?.force_upload) {
            condaShellCmd("anaconda upload " + readFile('packagename') + " --force --label " + readFile('channel'), condaenvb)
          } else {
            condaShellCmd("anaconda upload " + readFile('packagename') + " --label " + readFile('channel'), condaenvb)
          }
        }
      }
      stage('ConvertUpload32bit') {
        if (convert32 && !isUnix() && readFile('channel')) {
          echo "Converting and Uploading package for win32"
          condaShellCmd("conda convert " + readFile('packagename') + " -p win-32", condaenvb)
          if (! params?.force_upload) {
            condaShellCmd("anaconda upload win-32\\* --force --label " + readFile('channel'), condaenvb)
          } else {
            condaShellCmd("anaconda upload win-32\\* --label " + readFile('channel'), condaenvb)
          }
        }
      }
      stage('ArtifactTearDown') {
        if (! params?.skip_tests) {
          archiveArtifacts('htmlcov/**')
        }
        condaShellCmd("conda env remove -y -n ${CONDAENV}", condaenvb)
        deleteDir()
      }
    }
  }
}
