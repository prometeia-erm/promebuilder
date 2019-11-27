import os
from promebuilder.pytestreporter import TestReporter

TEST_REPORTER = TestReporter(os.getenv("PYTEST_SLACK_URL"))


def pytest_report_teststatus(report):
    TEST_REPORTER.add_report(report)


def pytest_runtest_teardown(item, nextitem):
    if nextitem is None:
        TEST_REPORTER.send()
