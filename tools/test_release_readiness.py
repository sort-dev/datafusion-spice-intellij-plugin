import importlib.util
from pathlib import Path
import tempfile
import unittest
import re

spec = importlib.util.spec_from_file_location('release_audit', Path(__file__).with_name('audit-release-readiness.py'))
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


class ReleaseReadinessTests(unittest.TestCase):
    def fixture(self, root):
        directory = root / 'IU-test/plugins' / audit.PLUGIN_ID / '1.0'
        directory.mkdir(parents=True)
        (directory / 'verification-verdict.txt').write_text('Compatible.\n')
        rows = {
            'internal-api-usages.txt': 'Internal interface com.intellij.Factory is referenced in dev.brikk.house.Gate. This interface is internal.',
            'experimental-api-usages.txt': 'Experimental API method com.intellij.Proto.getProject() is invoked in dev.sort.datafusionspice.catalog.ProductHarvest.handle() : Object. This method can change.',
            'deprecated-usages.txt': 'Deprecated method com.intellij.Model.type() is overridden in class dev.brikk.house.Helper',
        }
        for name, row in rows.items():
            (directory / name).write_text(row + '\n')
        return directory

    def test_compatibility_does_not_grant_approval(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.fixture(root)
            result = audit.collect(root, '1.0', ['IU-test'])
            self.assertTrue(result['requiresOwnerApiDecision'])
            self.assertFalse(result['apiApprovalGranted'])
            self.assertFalse(result['publicationAuthorized'])
            findings = result['targets']['IU-test']['findings']
            self.assertEqual('consumer', findings['experimental'][0]['owner'])
            self.assertEqual('released-brikk-support', findings['internal'][0]['owner'])
            self.assertEqual('released-brikk-support', findings['deprecated'][0]['owner'])

    def test_missing_report_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            report = self.fixture(root)
            (report / 'experimental-api-usages.txt').unlink()
            with self.assertRaises(FileNotFoundError):
                audit.collect(root, '1.0', ['IU-test'])

    def test_unknown_finding_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            report = self.fixture(root)
            (report / 'internal-api-usages.txt').write_text('Unrecognized output\n')
            with self.assertRaises(ValueError):
                audit.collect(root, '1.0', ['IU-test'])

    def test_wrong_version_or_incompatible_verdict_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            report = self.fixture(root)
            with self.assertRaises(FileNotFoundError):
                audit.collect(root, '2.0', ['IU-test'])
            (report / 'verification-verdict.txt').write_text('Incompatible.\n')
            with self.assertRaises(ValueError):
                audit.collect(root, '1.0', ['IU-test'])

    def test_owner_is_call_site_not_referenced_api(self):
        self.assertEqual('consumer', audit.owner(
            'Experimental API class dev.brikk.house.Api is referenced in dev.sort.datafusionspice.Consumer'))
        self.assertEqual('unclassified', audit.owner('Internal interface com.intellij.Factory is referenced in foreign.Gate'))

    def test_release_preparation_preserves_manual_publication_guards(self):
        root = Path(__file__).resolve().parent.parent
        workflow = (root / '.github/workflows/release.yml').read_text()
        self.assertIn('workflow_dispatch:', workflow)
        self.assertNotRegex(workflow, r'(?m)^  (push|pull_request|schedule):', msg='Publication must be manual-only')
        self.assertIn("startsWith(github.ref, 'refs/heads/release/')", workflow)
        self.assertIn("inputs.authorization == 'publish-github-release'", workflow)
        self.assertIn('@387c9f39bf66142cf9f07074bc340f44ae1d3801', workflow)
        self.assertNotIn('publishPlugin', workflow)
        build = (root / 'build.gradle.kts').read_text()
        versions = re.findall(r'^version = "([^"]+)"$', build, re.MULTILINE)
        self.assertEqual(1, len(versions))
        self.assertRegex(versions[0], r'^\d+(?:\.\d+)+$')
        self.assertIn('if (supportCandidate.isPresent) version = "$version-qualification"', build)


if __name__ == '__main__':
    unittest.main()
