#!/usr/bin/python3

import os
from pathlib import Path
import shutil
import subprocess

base = Path(os.getenv('MESON_DIST_ROOT'))

base.joinpath('.gitattributes').unlink()
base.joinpath('.gitignore').unlink()
base.joinpath('m4', '.gitignore').unlink()
shutil.rmtree(base / '.github')

# fixtures are currently only used by GitHub Actions
shutil.rmtree(base / 'fixtures')

subprocess.run(['autoreconf', '-i'], cwd=base, check=True)
shutil.rmtree(base / 'autom4te.cache')
