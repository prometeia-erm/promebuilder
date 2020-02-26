import os
from argparse import ArgumentParser
from subprocess import check_call
from .utils import read_version

COVERAGE_FILENAME = "sonar.python.coverage.reportPaths"


def read_config(filename="sonar-project.properties"):
    out = dict()
    with open(filename) as cfile:
        for row in cfile:
            if not row.strip():
                continue
            key, val = row.split('=', 1)
            out[key.strip()] = val.strip()
    return out


def scan_here():
    parser = ArgumentParser(description="Launch sonar-scanner on current project.")
    parser.add_argument('-upload', action='store_true', help="Upload analysis",  default=False)
    args = parser.parse_args()
    if 'help' in args:
        return
    config = read_config()
    if config.get(COVERAGE_FILENAME) and not os.path.exists(config[COVERAGE_FILENAME]):
            raise RuntimeError("Missing coverage file %s: have you run your unit tests?")
    command = "sonar-scanner -Dsonar.projectVersion=%s" % read_version()
    if not args.upload:
        command += " -Dsonar.dryRun=true"
    print("> %s" % command)
    check_call(command, shell=True)
