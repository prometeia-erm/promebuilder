import pytest
from promebuilder.utils import gen_metadata, gen_ver_build


def test_gen_metadata():
    mtd = gen_metadata("nome", "descrizione", "e@ma.il")
    assert mtd["name"] == "nome"
    assert mtd["description"] == "descrizione"
    assert mtd["license"] == "Proprietary"
    with open('version') as verfile:
        assert mtd["version"].startswith(verfile.read().strip())


def test_gen_ver_build():
    # Invalid VER
    assert ('1.2.3.zuppone', 99, '') == gen_ver_build('1.2.3.zuppone', 'master', 99)
    # No branch: dev0
    assert '1.2.3a0' == gen_ver_build('1.2.3', '', 99)[0]
    # Branch master, support
    assert ('1.2.3', 0, 'main') == gen_ver_build('1.2.3', 'master', 99)
    assert ('1.2.3', 0, 'main') == gen_ver_build('1.2.3', 'support/XXX', 99)
    # Branch develop
    assert ('1.2.3a4', 99, 'develop') == gen_ver_build('1.2.3', 'develop', 99)
    # Branch develop_XXX
    assert ('1.2.3a5', 99, 'refactor') == gen_ver_build('1.2.3', 'develop_refactor', 99)
    # Branch develop-XXX are normal branch, no channel
    assert ('1.2.3a0', 99, '') == gen_ver_build('1.2.3', 'develop-refactor', 99)
    # Branch feature/XXX
    assert ('1.2.3a2', 99, 'feature') == gen_ver_build('1.2.3', 'feature/h725', 99)
    # Branch backporting/XXX
    assert ('1.2.3a3', 99, 'backporting') == gen_ver_build('1.2.3', 'backporting/h725', 99)
    # unknown branches
    assert ('1.2.3a0', 99, '') == gen_ver_build('1.2.3', 'unknown/h725', 99)
    assert ('1.2.3a0', 99, '') == gen_ver_build('1.2.3', 'unknown2', 99)
    # Branch release/XXX
    assert ('1.2.3rc99', 0, 'release') == gen_ver_build('1.2.3', 'release/ciccio', 99)
    # Branch hotfix/XXX
    assert ('1.2.3rc99', 0, 'hotfix') == gen_ver_build('1.2.3', 'hotfix/pluto', 99)
    # Invalid subname
    with pytest.raises(AssertionError):
        gen_ver_build('1.2.3', 'hotfix/develop', 99)
    with pytest.raises(AssertionError):
        gen_ver_build('1.2.3', 'develop/master', 99)

