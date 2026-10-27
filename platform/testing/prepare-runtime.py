#!/usr/bin/env python3
"""Generate a fresh isolated operator, never read or change David's credentials."""
import getpass
import runpy
import secrets
from pathlib import Path
# Reuse the same runtime provisioning and hidden hash pipeline with random synthetic input.
getpass.getpass = lambda prompt: secrets.token_urlsafe(32)
runpy.run_path(str(Path(__file__).resolve().parents[1] / 'runtime' / 'prepare.py'), run_name='__main__')
