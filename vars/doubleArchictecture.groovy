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
        if (env.GIT_BRANCH == 'master' || params?.deep_tests) {
            echo "Activating NRT"
            condaShellCmd("activatenrt --doit", CONDAENV)
        }
      }
      stage('UnitTests') {
        if (! params?.skip_tests) {
          // Forced reinstall to avoid annoying wrong setuptools usage
          condaShellCmd("conda update -q setuptools --force", CONDAENV)
          condaShellCmd("python setup.py develop", CONDAENV)
          if (env.GIT_BRANCH == 'master' || params?.deep_tests) {
            condaShellCmd("pytest --cache-clear", CONDAENV)
            archiveArtifacts('htmlcov/**')
          } else {
            condaShellCmd("pytest --cache-clear --no-cov", CONDAENV)
          }
        }
      }
      stage('SonarScanner') {
        if (! params?.skip_tests && (env.GIT_BRANCH == 'master' || params?.deep_tests) && isUnix() ) {
          try   {
            condaShellCmd("sonar-scanner -D sonar.projectVersion=" + readFile('version') , CONDAENV)
          } catch (err) {
            echo "Failed sonarqube scanning"
          }
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
        if (env.GIT_BRANCH == 'master' || params?.deep_tests) {
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
          writeFile: 'labels', text: " --label" + readFile('channel')
          if (fileExists("htmlcov/index.html") ) {
            writeFile: 'labels', text: " --label deeptested" + readFile('labels')
          }
          if (! params?.force_upload) {
            writeFile: 'labels', text: " --force " + readFile('labels')
          }
          echo "Uploading " + readFile('packagename') + " with options" + readFile('labels')
          condaShellCmd("anaconda upload " + readFile('packagename') + readFile('labels'), condaenvb)
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
      stage('Teardown') {
        condaCleaner(true, CONDAENV, condaenvb)
      }
    }
  }
}
