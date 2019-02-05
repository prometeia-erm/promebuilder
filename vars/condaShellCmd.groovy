#!/usr/bin/env groovy

def call(command, condaenv="base", returnStdout=false) {
    lock(label: 'conda_lock_${env.NODE_NAME}') {
        if (isUnix()) {
            return sh(script: "source /home/jenkins/miniconda2/bin/activate ${condaenv}; ${command}", returnStdout: returnStdout)
        } else {
            return bat(script: "conda activate ${condaenv} && ${command} && conda deactivate", returnStdout: returnStdout)
        }
    }
}
