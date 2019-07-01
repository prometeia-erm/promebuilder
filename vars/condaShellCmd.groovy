#!/usr/bin/env groovy

def call(command, condaenv="base", returnStdout=false) {
    lock(label: "conda_lock_${env.NODE_NAME}") {
        return condaShellCmdNoLock(command, condaenv, returnStdout)
    }
}
