import promebuilder


def test_gen_metadata():
    mtd = promebuilder.gen_metadata("nome", "descrizione", "e@ma.il")
    assert mtd["name"] == "nome"
    assert mtd["description"] == "descrizione"
    assert mtd["license"] == "Proprietary"
    with open('version') as verfile:
        assert mtd["version"] == verfile.read().strip() + '.dev0'
