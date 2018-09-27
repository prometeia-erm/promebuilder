import pytest
from promebuilder.scanner import read_config, COVERAGE_FILENAME, scan_here


def test_read_config():
    cfg = read_config()
    assert cfg
    assert cfg.get("sonar.projectKey")
    assert cfg.get(COVERAGE_FILENAME) == "coverage.xml"


def test_read_missing_config():
    with pytest.raises(IOError):
        read_config("missingconfig.xxx")
