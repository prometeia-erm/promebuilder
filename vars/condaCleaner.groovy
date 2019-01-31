#!/usr/bin/env groovy

def call(doclean, condaenv, condaenvbase) {
    if (doclean) {
       deleteDir()
       condaShellCmd("conda env remove -y -n ${condaenv}", condaenvbase)
    }
}