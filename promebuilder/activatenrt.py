import os
from argparse import ArgumentParser
import ConfigParser

COVERAGE_FILENAME = "sonar.python.coverage.reportPath"
NRT_FOLDER = "tests-nrt"
PYTESTINI = "pytest.ini"
PYTESTSECTION = "pytest"
TESTPATHSKEY = "testpaths"


def activate_nrt():
    parser = ArgumentParser(description="Add NRT folder to %s." % PYTESTINI)
    parser.add_argument('--doit', action='store_true', help="Actually add it",  default=False)
    args = parser.parse_args()
    if 'help' in args:
        return
    if not os.path.isdir(NRT_FOLDER):
        print("Not found NRT folder %s" % NRT_FOLDER)
        return
    config = ConfigParser.ConfigParser()
    config.read(PYTESTINI)
    tests = (config.get(PYTESTSECTION, TESTPATHSKEY) or '').split()
    if NRT_FOLDER in tests:
        print("NRT folder %s already in tests" % TESTPATHSKEY)
        return
    tests.append(NRT_FOLDER)
    value = ' '.join(tests)
    config.set(PYTESTSECTION, TESTPATHSKEY, value)
    if not args.doit:
        print("Dryrun, not really written new configuration << %s = %s >>" % (TESTPATHSKEY, value))
    else:
        with open(PYTESTINI, 'w') as configfile:
            config.write(configfile)
        print("Added %s folder to %s" % (NRT_FOLDER, PYTESTINI))
