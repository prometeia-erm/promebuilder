# -*- coding: utf-8 -*-
import setuptools
import os
import re
import time
import sys
import warnings
from setuptools.extern.packaging.version import Version, InvalidVersion

VERSIONFILE = 'version'
BRANCHFILE = 'branch'
REQUIREMENTSFILE = 'requirements.txt'
BUILDNUMFILES = ['buildnum', 'build_trigger_number']
CHANNELFILE = 'channel'
VALIDVER = re.compile(r'^(\d+)\.(\d+)\.(\d+)$')
COVERAGEFILE = "htmlcov/index.html"
DYNBUILDNUM = int(time.time() - 1514764800)
LONGDESCFILE = "README.md"


def gen_ver_build(rawversion, branch, build, masterlabel='main', masterbuild=0):
    """Returns <version>, <buildnum>, <channel>"""
    def calc():
        if not VALIDVER.match(rawversion):
            return rawversion, build, ''
        if not branch:
            return rawversion + 'a0', DYNBUILDNUM, ''
        if branch == 'develop':
            return rawversion + 'a4', build or DYNBUILDNUM, 'develop'
        try:
            btype, bname = branch.split('/')
        except ValueError:
            btype, bname = '', ''
        assert bname not in ('master', 'support', 'hotfix', 'release', 'develop')
        if branch == 'master' or btype == 'support':
            return rawversion, masterbuild, masterlabel
        if build and btype in ('release', 'hotfix'):
            return '{}rc{}'.format(rawversion, build), masterbuild, btype
        return '{}a{}'.format(rawversion, 1 + int(bool(btype)) + int(btype == 'feature')), \
               build or DYNBUILDNUM, \
               bname if btype == 'develop' else ''

    tver, tbuild, tlab = calc()
    # Version normalization
    try:
        parsedver = Version(tver)
        tver = str(parsedver)
    except InvalidVersion:
        warnings.warn("Invalid version %s" % tver)
    return tver, tbuild, tlab


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
            print("[not found file %s]" % name)
    if default is None:
        raise IOError("Missing or empty all of the files " + ', '.join(names))
    else:
        print("[returning default '%s']" % default)
    return default


def read_version():
    return _readfiles(VERSIONFILE)


def has_coverage_report():
    hascoverage = os.path.isfile(COVERAGEFILE)
    if hascoverage:
        print("[found a coverage report in %s]" % COVERAGEFILE)
    else:
        print("[no coverage was calculated]")
    return hascoverage


def gen_metadata(name, description, email, url="http://www.prometeia.com", keywords=None, packages=None,
                 entry_points=None, package_data=None, data_files=None, zip_safe=False,
                 masterlabel='main', masterbuild=0):
    branch = _readfiles(BRANCHFILE, '')
    version, buildnum, channel = gen_ver_build(read_version(), branch, int(_readfiles(BUILDNUMFILES, '0')),
                                               masterlabel, masterbuild)
    print('Building version "%s" build "%d" from branch "%s" for channel "%s"' % (
        version, buildnum, branch, channel or ''))
    with open(CHANNELFILE, 'w') as channelfile:
        print("Writing channel '%s' on %s" % (channel, os.path.abspath(CHANNELFILE)))
        channelfile.write(channel)

    if 'bdist_conda' in sys.argv:
        print("bdist_conda mode: requirements from file become setup requires")
        requires = _readfiles(REQUIREMENTSFILE, default="").splitlines()
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
        classifiers=['License :: Other/Proprietary License', 'Framework :: Pytest',
                     'Intended Audience :: Financial and Insurance Industry',
                     'Operating System :: Microsoft :: Windows', 'Operating System :: POSIX :: Linux',
                     'Programming Language :: Python :: 2.7'],
        platforms=['Microsoft Windows, Linux'],
        install_requires=requires or [],
        package_data=package_data or {},
        entry_points=entry_points or {},
        version=version,
        data_files=data_files or []
    )

    try:
        with open(LONGDESCFILE, "r") as fh:
            long_description = fh.read()
        metadata['long_description'] = long_description
        metadata['long_description_content_type'] = "text/markdown"
    except OSError:
        metadata['long_description'] = description
        metadata['long_description_content_type'] = "text/plain"

    try:
        import distutils.command.bdist_conda
        print("Conda distribution")
        docondatests = has_coverage_report()
        if not docondatests:
            print("[skipping conda tests]")
        metadata.update(dict(
            distclass=distutils.command.bdist_conda.CondaDistribution,
            conda_import_tests=docondatests,
            conda_command_tests=docondatests,
            conda_buildnum=buildnum
        ))
    except ImportError:
        print("Standard distribution")

    return metadata


def setup(metadata):
    try:
        from conda import CondaError
    except ImportError:
        setuptools.setup(**metadata)
    else:
        tnum = 3
        while tnum:
            tnum -= 1
            try:
                setuptools.setup(**metadata)
            except CondaError as cerr:
                if not tnum:
                    print("-- TOO MANY CONDA ERROR--")
                    raise
                print("-- CONDA ERROR --")
                print(str(cerr))
                print("-- RETRY --")
            else:
                break
