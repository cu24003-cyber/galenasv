#!/usr/bin/env python3
"""Comprueba la carga literal y la precedencia sin arrancar Maven, Liberty ni PostgreSQL."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = ('PG_USER', 'PG_PASS', 'PG_DB', 'PG_HOST', 'PG_PORT', 'LTPA_KEYS_PASSWORD')


class LibertyEnvTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        (self.root / 'scripts').mkdir()
        shutil.copy(ROOT / 'scripts/liberty.sh', self.root / 'scripts/liberty.sh')
        fake = self.root / 'mvn'
        fake.write_text('#!/usr/bin/env python3\nimport json, os, sys\n'
                        f'print(json.dumps({{"args": sys.argv[1:], "vars": {{k: os.getenv(k) for k in {REQUIRED!r}}}}}))\n')
        fake.chmod(0o700)
        self.env = {k: v for k, v in os.environ.items() if k not in REQUIRED}
        self.env['PATH'] = str(self.root) + os.pathsep + self.env['PATH']
        self.values = dict(zip(REQUIRED, ('demo', 'secret', 'demo', 'localhost', '5432', 'test-key')))

    def write_env(self):
        (self.root / '.env').write_bytes(('\r\n'.join(
            ['# comentario', ''] + [f'{k}={v}' for k, v in self.values.items()])).encode())

    def run_loader(self, *args):
        return subprocess.run(['bash', str(self.root / 'scripts/liberty.sh'), *args],
                              cwd='/', env=self.env, text=True, capture_output=True)

    def test_literal_values_crlf_and_default_command(self):
        marker = self.root / 'executed'
        self.values['PG_PASS'] = f' $HOME # = "quoted" \'single\' $(touch {marker}) `touch {marker}` '
        self.write_env()
        result = self.run_loader()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(json.loads(result.stdout), {'args': ['liberty:run'], 'vars': self.values})
        self.assertFalse(marker.exists())

    def test_process_overrides_file_and_arguments_pass_through(self):
        self.write_env()
        self.env['PG_HOST'] = 'database-container'
        result = self.run_loader('liberty:dev', '-DskipTests')
        self.assertEqual(result.returncode, 0, result.stderr)
        parsed = json.loads(result.stdout)
        self.assertEqual(parsed['vars']['PG_HOST'], 'database-container')
        self.assertEqual(parsed['args'], ['liberty:dev', '-DskipTests'])

    def test_environment_without_file(self):
        self.env.update(self.values)
        self.assertEqual(self.run_loader().returncode, 0)

    def test_missing_or_empty_required_variables_fail_without_secrets(self):
        for key in REQUIRED:
            with self.subTest(key=key):
                self.env.update(self.values)
                self.env[key] = ''
                self.write_env()
                result = self.run_loader()
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(key, result.stderr)
                self.assertNotIn('secret', result.stderr)
                self.assertEqual(result.stdout, '')

    def test_invalid_line_is_rejected_without_echoing_it(self):
        self.write_env()
        with (self.root / '.env').open('a') as stream:
            stream.write('\nexport SECRET=never-print-this\n')
        result = self.run_loader()
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn('never-print-this', result.stderr)
        self.assertEqual(result.stdout, '')

    def test_template_uses_environment_for_all_secrets_and_database_fields(self):
        root = ET.parse(ROOT / 'src/main/liberty/config/server.xml.ejemplo').getroot()
        props = root.find('dataSource/properties.postgresql').attrib
        self.assertEqual(props, dict(zip(
            ('user', 'password', 'databaseName', 'serverName', 'portNumber'),
            ('${env.PG_USER}', '${env.PG_PASS}', '${env.PG_DB}', '${env.PG_HOST}', '${env.PG_PORT}'))))
        self.assertEqual(root.find('ltpa').get('keysPassword'), '${env.LTPA_KEYS_PASSWORD}')
        defaults = {v.get('name'): v.get('defaultValue') for v in root.findall('variable')}
        self.assertEqual(defaults, {'HTTP_PORT': '9080', 'HTTPS_PORT': '9443', 'HTTP_HOST': 'localhost'})


if __name__ == '__main__':
    unittest.main()
