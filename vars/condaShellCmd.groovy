#!/usr/bin/env groovy

def call(command, condaenv="base", returnStdout=false) {
    lock(label: "conda_lock_${env.NODE_NAME}") {
        if (isUnix()) {
            return sh(script: "source {$env.LINUX_CONDA_BIN}/activate ${condaenv}; ${command}", returnStdout: returnStdout)
        } else {
            return bat(script: "{$env.WIN_CONDA_BIN}\activate ${condaenv} && ${command} && conda deactivate", returnStdout: returnStdout)
        }
    }
}
