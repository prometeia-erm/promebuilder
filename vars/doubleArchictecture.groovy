#!/usr/bin/env groovy

def call(envlabel, condaenvb="base", convert32=false, pythonver="2.7", condaenvbuild=CONDAENV) {
  node(envlabel) {
    pipeline {
      stage('SetUp') {
        echo "Existing conda envs"
        condaShellCmd("conda info --envs", condaenvb)
        echo "Setup on ${envlabel}, conda environment ${condaenvbuild}"
        unstash "source"
        condaShellCmd("conda create -q -y -n ${condaenvbuild} python=${pythonver}, condaenvb)
        retry(3) {
          condaShellCmd("conda install -q -y --file build-requirements.txt", condaenvbuild)
        }
        echo "Checking package and channel names"
        condaShellCmd("python setup.py --name", condaenvbuild)
        if (readFile('channel')) {
          echo "Adding to environment channel " + readFile('channel')
          condaShellCmd("conda config --env --add channels t/${env.ANACONDA_TOKEN}/prometeia/channel/" + readFile('channel'), condaenvbuild)
        } else {
          echo "WARNING: Build without channel, adding 'develop' as default fallback channel:"
          condaShellCmd("conda config --env --add channels t/${env.ANACONDA_TOKEN}/prometeia/channel/develop", condaenvbuild)
        }
        condaShellCmd("conda config --show channels", condaenvbuild)
        echo "INFO: Installing requiremets on conda environment ${condaenvbuild}"
        condaShellCmd("conda install -q -y --file requirements.txt", condaenvbuild)
        if (env.GIT_BRANCH == 'master' || params?.deep_tests) {
            echo "Activating NRT"
            condaShellCmd("activatenrt --doit", condaenvbuild)
        }
      }
      stage('UnitTests') {
        if (! params?.skip_tests) {
          // Forced reinstall to avoid annoying wrong setuptools usage
          condaShellCmd("conda update -q setuptools --force", condaenvbuild)
          condaShellCmd("python setup.py develop", condaenvbuild)
          if (env.GIT_BRANCH == 'master' || params?.deep_tests) {
            condaShellCmd("pytest --cache-clear", condaenvbuild)
            archiveArtifacts('htmlcov/**')
          } else {
            condaShellCmd("pytest --cache-clear --no-cov", condaenvbuild)
          }
        }
      }
      stage('SonarScanner') {
        if (! params?.skip_tests && (env.GIT_BRANCH == 'master' || params?.deep_tests) && isUnix() && pythonver == "2.7") {
          try   {
            condaShellCmd("sonar-scanner -D sonar.projectVersion=" + readFile('version') , condaenvbuild)
          } catch (err) {
            echo "Failed sonarqube scanning"
          }
        }
      }
      stage('Build') {
        script {
          writeFile file: 'buildoutput', text: condaShellCmd("python setup.py bdist_conda", condaenvbuild, true).trim()
          writeFile file: 'packagename', text: readFile('buildoutput').split(" ")[-1]
        }
        echo "BUILD OUTPUT: \n\n ================ \n" + readFile('buildoutput') + "\n ================ \n"
        echo "PACKAGENAME: " + readFile('packagename')
      }
      stage('Install') {
        if (env.GIT_BRANCH == 'master' || params?.deep_tests) {
          echo "Creating indipendent test environment test_${condaenvbuild}"
          condaShellCmd("conda create -q -y -n test_${condaenvbuild} python=${pythonver}", condaenvb)
          if (readFile('channel')) {
            condaShellCmd("conda config --env --add channels t/${env.ANACONDA_TOKEN}/prometeia/channel/" + readFile('channel'), "test_${condaenvbuild}")
          }
          echo "Installing package on test environment test_${condaenvbuild}"
          condaShellCmd("conda install -q -y " + readFile('packagename'), "test_${condaenvbuild}")
          condaShellCmd("conda env remove -y -n test_${condaenvbuild}", condaenvb)
        }
      }
      stage('Upload') {
        if (readFile('channel')) {
          writeFile file: 'labels', text: " --label " + readFile('channel')
          if (fileExists("htmlcov/index.html") ) {
            writeFile file: 'labels', text: " --label deeptested" + readFile('labels')
          }
          if (params?.force_upload) {
            writeFile file: 'labels', text: " --force " + readFile('labels')
          }
          echo "Uploading " + readFile('packagename') + " with options:" + readFile('labels')
          retry(3) {
            condaShellCmd("anaconda upload " + readFile('packagename') + readFile('labels'), condaenvb)
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
      stage('Teardown') {
        condaCleaner(true, condaenvbuild, condaenvb)
      }
    }
  }
}
