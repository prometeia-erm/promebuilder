# -*- coding: utf-8 -*-
import setuptools
import os
import re
import time
import sys

VERSIONFILE = 'version'
BRANCHFILE = 'branch'
REQUIREMENTSFILE = 'requirements.txt'
BUILDNUMFILES = ['buildnum', 'build_trigger_number']
CHANNELFILE = 'channel'
VALIDVER = re.compile(r'^(\d+)\.(\d+)\.(\d+)$')


def gen_ver_build(rawversion, branch, build):
    """Returns <version>, <buildnum>, <channel>"""
    if not VALIDVER.match(rawversion):
        return rawversion, build, ''
    if not branch:
        return rawversion + '.dev0', int(time.time() - 1514764800), ''
    if branch == 'master':
        return rawversion, 0, 'main'
    if branch == 'develop':
        return rawversion + '.dev2', build, 'develop'
    if branch.startswith('release/'):
        return rawversion + '.b1', build, 'release'
    if branch.startswith('hotfix/'):
        return rawversion + '.b2', build, 'release'
    return rawversion + '.dev1', build, ''


def _readfiles(names, default=None):
    if not isinstance(names, list):
        names = [names]
    for name in names:
        try:
            with open(name) as thefile:
                data = thefile.read().strip()
                if data:
                    return data
        except IOError:
            print "[not found file %s]" % name
            pass
    if default is None:
        raise IOError("Missing or empty all of the files " + ', '.join(names))
    else:
        print "[returning default '%s']" % default
    return default


def gen_metadata(name, description, email, url="http://www.prometeia.com", keywords=None, packages=None,
                 entry_points=None, package_data=None, data_files=None, zip_safe=False):
    version, buildnum, channel = gen_ver_build(
        _readfiles(VERSIONFILE),
        _readfiles(BRANCHFILE, ''),
        int(_readfiles(BUILDNUMFILES, '0')))
    print('Building version "%s" build "%d" for channel "%s"' % (version, buildnum, channel or None))

    with open(CHANNELFILE, 'w') as channelfile:
        print("Writeing channel '%s' on %s" % (channel, os.path.abspath(CHANNELFILE)))
        channelfile.write(channel)

    if 'bdist_conda' in sys.argv:
        print("bdist_conda mode: requirements from file become setup requires")
        requires = _readfiles(REQUIREMENTSFILE).splitlines()
    else:
        # Quando si installa in sviluppo, tanto al setup quanto all'esecuzione del wrapper viene verificato
        # che i package indicati siano effettivamente presenti. I package sono però gli effettivi moduli,
        # mentre nel requirements.txt ci sono i nomi dei pacchetti!
        # Qui andrebbero elencati tutti i moduli indispensabili davvero voluti, non i pacchetti, che a loro volta
        # dovrebbero avere analogamente ordinati install_requires. Qui un sottoinsieme solo di verifica.
        requires = []

    metadata = dict(
        name=name,
        description=description,
        author="Prometeia",
        url=url,
        author_email=email,
        keywords=keywords or [],
        packages=sorted(packages or set(setuptools.find_packages()) - {'tests'}),
        license="Proprietary",
        include_package_data=True,
        zip_safe=zip_safe,
        classifiers=['License :: Other/Proprietary License'],
        install_requires=requires or [],
        package_data=package_data or {},
        entry_points=entry_points or {},
        version=version,
        data_files=data_files or []
    )

    try:
        import distutils.command.bdist_conda
        print("Conda distribution")
        metadata.update(dict(
            distclass=distutils.command.bdist_conda.CondaDistribution,
            conda_import_tests=True,
            conda_command_tests=True,
            conda_buildnum=buildnum
        ))
    except ImportError:
        print("Standard distribution")

    return metadata


def setup(metadata):
    setuptools.setup(**metadata)
