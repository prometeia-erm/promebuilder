#!/usr/bin/env groovy

def call(command, condaenv="base", returnStdout=false) {
    if (isUnix()) {
        return sh(script: "source ${env.CONDA_PREFIX}/bin/activate ${condaenv}; ${command}; conda deactivate", returnStdout: returnStdout)
    } else {
        return bat(script: "${env.CONDA_PREFIX}\\scripts\\activate ${condaenv} && ${command} && conda deactivate", returnStdout: returnStdout)
    }
}
