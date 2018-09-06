#!/usr/bin/env groovy

def call(command, condaenv="base", returnStdout=false) {
    if (isUnix()) {
        return sh(script: "source /home/jenkins/miniconda2/bin/activate ${condaenv}; ${command}", returnStdout: returnStdout)
    } else {
        return bat(script: "activate ${condaenv} && ${command} && deactivate", returnStdout: returnStdout)
    }
}