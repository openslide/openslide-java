#!/usr/bin/python3

import os
from pathlib import Path
import shutil
import subprocess

base = Path(os.getenv('MESON_DIST_ROOT'))

subprocess.run(['autoreconf', '-i'], cwd=base, check=True)
shutil.rmtree(base / 'autom4te.cache')
