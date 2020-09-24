#!/usr/bin/env groovy

def call(envlabel, condaenvb="base", convert32=false, pythonver="2.7", condaenvbuild=CONDAENV, extrachannel="", scanme=true) {
  node(envlabel) {
    pipeline {
      stage('SetUp') {
        echo "Working on ${env.NODE_NAME}"
        echo "Existing conda envs"
        condaShellCmd("conda info --envs", condaenvb)
        echo "Setup on ${envlabel}, conda environment ${condaenvbuild}"
        unstash "source"
        condaShellCmd("conda create -q -y -n ${condaenvbuild} python=${pythonver}", condaenvb)
        if (extrachannel) {
          condaShellCmd("conda config --env --append channels ${extrachannel}", condaenvbuild)
        }
        retry(3) {
          condaShellCmd("conda install --copy -q -y --file build-requirements.txt", condaenvbuild)
        }
        echo "Checking package and channel names"
        condaShellCmd("python setup.py --name", condaenvbuild)
        condaShellCmd("conda config --env --add channels t/${env.ANACONDA_TOKEN}/prometeia", condaenvbuild)
        if (readFile('channel')) {
          echo "Adding to environment channel " + readFile('channel')
          condaShellCmd("conda config --env --add channels t/${env.ANACONDA_TOKEN}/prometeia/channel/" + readFile('channel'), condaenvbuild)
        } else {
          echo "WARNING: Build without channel, adding 'develop' as default fallback channel:"
          condaShellCmd("conda config --env --add channels t/${env.ANACONDA_TOKEN}/prometeia/channel/develop", condaenvbuild)
        }
        condaShellCmd("conda config --show channels", condaenvbuild)
        echo "INFO: Installing requirements on conda environment ${condaenvbuild}"
        retry(3) {
          condaShellCmd("conda install --copy -q -y --file requirements.txt", condaenvbuild)
        }
      }
      stage('UnitTests') {
        if (! params?.skip_tests) {
          // Forced reinstall to avoid annoying wrong setuptools usage
          condaShellCmd("conda update -q setuptools --force", condaenvbuild)
          try {
            condaShellCmd("python setup.py develop", condaenvbuild)
          } catch (err) {
            echo "Removing conda environment after error"
            condaShellCmd("conda env remove -y -n ${condaenvbuild}", condaenvb)
            error "Failed SETUP for UT"
          }
          try {
            if ((env.GIT_BRANCH == 'master' || params?.test_markers == "") && isUnix() && scanme){
              condaShellCmdNoLock("pytest --cache-clear --cov-report html --cov-report xml --junitxml=junit.xml", condaenvbuild)
              archiveArtifacts('htmlcov/**')
              junit(testResults: 'junit.xml')
            } else {
              condaShellCmdNoLock("pytest --cache-clear --junitxml=junit.xml --no-cov -m '" + params?.test_markers + "'", condaenvbuild)
              junit(testResults: 'junit.xml')
            }
          } catch (err) {
            echo "Removing conda environment after error"
            condaShellCmd("conda env remove -y -n ${condaenvbuild}", condaenvb)
            junit(allowEmptyResults: true, testResults: 'junit.xml')
            error "Failed UT"
          }
        }
      }
      stage('SonarScanner') {
        if (! params?.skip_tests && (env.GIT_BRANCH == 'master' || params?.deep_tests) && isUnix() && pythonver == "2.7") {
          try   {
            condaShellCmdNoLock("sonar-scanner -D sonar.projectVersion=" + readFile('version') , condaenvbuild)
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
          if (extrachannel) {
            condaShellCmd("conda config --env --append channels ${extrachannel}", "test_${condaenvbuild}")
          }
          if (readFile('channel')) {
            condaShellCmd("conda config --env --add channels t/${env.ANACONDA_TOKEN}/prometeia/channel/" + readFile('channel'), "test_${condaenvbuild}")
          }
          echo "Installing package on test environment test_${condaenvbuild}"
          retry(3) {
            condaShellCmd("conda install --copy -q -y " + readFile('packagename'), "test_${condaenvbuild}")
          }
          condaShellCmd("conda env remove -y -n test_${condaenvbuild}", condaenvb)
        }
      }
      stage('Upload') {
        if (readFile('channel')) {
          writeFile file: 'labels', text: " --label " + readFile('channel')
          if (fileExists("htmlcov/index.html")) {
            writeFile file: 'labels', text: " --label deeptested" + readFile('labels')
          }
          if (params?.force_upload) {
            writeFile file: 'labels', text: " --force " + readFile('labels')
          }
          // echo "Archiving " + readFile('packagename')
          // archiveArtifacts(artifacts:readFile('packagename'))
          echo "Uploading " + readFile('packagename') + " with options:" + readFile('labels')
          retry(3) {
            condaShellCmdNoLock("anaconda upload " + readFile('packagename') + readFile('labels'), condaenvb)
          }
        }
      }
      stage('ArchiveDoc') {
        if (isUnix() && fileExists("dist/doc")) {
          archiveArtifacts(artifacts:'dist/doc/**', allowEmptyArchive:true, onlyIfSuccessful: true)
        }
      }
      stage('ConvertUpload32bit') {
        if (convert32 && !isUnix() && readFile('channel')) {
          echo "Converting and Uploading package for win32"
          condaShellCmdNoLock("conda convert " + readFile('packagename') + " -p win-32", condaenvb)
          if (! params?.force_upload) {
            condaShellCmdNoLock("anaconda upload win-32\\* --force --label " + readFile('channel'), condaenvb)
          } else {
            condaShellCmdNoLock("anaconda upload win-32\\* --label " + readFile('channel'), condaenvb)
          }
        }
      }
      stage('Teardown') {
        condaCleaner(true, condaenvbuild, condaenvb)
      }
    }
  }
}
