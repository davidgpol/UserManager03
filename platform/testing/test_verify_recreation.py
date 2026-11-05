#!/usr/bin/env python3
"""Synthetic, isolated checks for the optional operator password copy."""
import importlib.util
import tempfile
import unittest
from pathlib import Path

SCRIPT=Path(__file__).resolve().parents[1]/'runtime/verify-recreation.py'
spec=importlib.util.spec_from_file_location('verify_recreation',SCRIPT)
verify=importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify)

class PasswordCopyTest(unittest.TestCase):
    def record(self, copy, db='db-before', app='app-before'):
        return dict(data=b'synthetic-data-digest',count=1,credentials=[b'synthetic-user',b'synthetic-hash'],
                    passwordCopy=copy,db=db,app=app,volume='synthetic-volume')

    def test_present_copy_is_preserved(self):
        with tempfile.TemporaryDirectory(prefix='um03-f2-fixture-') as name:
            path=Path(name)/'access/operator-password'
            path.parent.mkdir()
            path.write_bytes(b'synthetic-password-a')
            before=self.record(verify.password_copy(Path(name)))
            after=self.record(verify.password_copy(Path(name)),db='db-after',app='app-after')
            result=verify.compare(before,after)
            self.assertTrue(result['passwordCopy'])
            self.assertTrue(result['passwordCopyPresent'])
            self.assertTrue(result['containersRecreated'])

    def test_absent_copy_is_allowed(self):
        with tempfile.TemporaryDirectory(prefix='um03-f2-fixture-') as name:
            before=self.record(verify.password_copy(Path(name)))
            after=self.record(verify.password_copy(Path(name)),db='db-after',app='app-after')
            result=verify.compare(before,after)
            self.assertTrue(result['passwordCopy'])
            self.assertFalse(result['passwordCopyPresent'])

    def test_changed_copy_is_detected(self):
        with tempfile.TemporaryDirectory(prefix='um03-f2-fixture-') as name:
            path=Path(name)/'access/operator-password'
            path.parent.mkdir()
            path.write_bytes(b'synthetic-password-a')
            before=self.record(verify.password_copy(Path(name)))
            path.write_bytes(b'synthetic-password-b')
            after=self.record(verify.password_copy(Path(name)),db='db-after',app='app-after')
            result=verify.compare(before,after)
            self.assertFalse(result['passwordCopy'])
            self.assertNotIn('synthetic-password',str(result))

if __name__=='__main__':unittest.main()
