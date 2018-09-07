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
    assert '1.2.3.dev0' == gen_ver_build('1.2.3', '', 99)[0]
    # Branch master
    assert ('1.2.3', 0, 'main') == gen_ver_build('1.2.3', 'master', 99)
    # Branch develop
    assert ('1.2.3.dev3', 99, 'develop') == gen_ver_build('1.2.3', 'develop', 99)
    # Branch develop_refactor
    assert ('1.2.3.dev2', 99, 'develop_refactor') == gen_ver_build('1.2.3', 'develop_refactor', 99)
    # Branch release/ciccio
    assert ('1.2.3rc1', 99, 'release') == gen_ver_build('1.2.3', 'release/ciccio', 99)
    # Branch hotfix/pluto
    assert ('1.2.3rc2', 99, 'release') == gen_ver_build('1.2.3', 'hotfix/pluto', 99)
