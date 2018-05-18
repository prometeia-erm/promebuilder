# -*- coding: utf-8 -*-
from promebuilder import gen_metadata, setup

METADATA = gen_metadata(
    name="promebuilder",
    description="Prometeia Package Builder",
    email="pytho_support@prometeia.com",
    url="https://github.com/prometeia/promebuilder",
    keywords="setup build pipeline ci",
)


if __name__ == '__main__':
    setup(METADATA)
