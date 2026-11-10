#!/usr/bin/env python3
"""Collect exact verifier findings without treating compatibility as API approval."""

import argparse
import hashlib
import json
from pathlib import Path
import re

PLUGIN_ID = 'dev.sort.datafusion-spice-intellij-plugin'
TARGETS = ('DB-262.10315.132', 'IU-262.10968.63', 'IU-263.4732.28', 'IU-263.5701.42')
CATEGORIES = {'internal': 'internal-api-usages.txt',
              'experimental': 'experimental-api-usages.txt',
              'deprecated': 'deprecated-usages.txt'}


def owner(finding):
    # Inspect the call site, not the JetBrains API named at the start of the row.
    site = finding.split('. This ', 1)[0].rsplit(' in ', 1)[-1]
    site = re.sub(r'^class ', '', site)
    if site.startswith('dev.sort.datafusionspice.'):
        return 'consumer'
    if site.startswith('dev.brikk.house.'):
        return 'released-brikk-support'
    return 'unclassified'


def collect(reports, version, targets=TARGETS):
    targets = tuple(targets)
    if not targets or len(targets) != len(set(targets)):
        raise ValueError('Expected a nonempty, unique target list')
    result = {'pluginId': PLUGIN_ID, 'pluginVersion': version, 'targets': {},
              'publicationAuthorized': False, 'apiApprovalGranted': False}
    for target in targets:
        directory = reports / target / 'plugins' / PLUGIN_ID / version.replace('+', '_')
        verdict_file = directory / 'verification-verdict.txt'
        verdict = verdict_file.read_text().strip()
        if not verdict.startswith('Compatible.'):
            raise ValueError(f'{target}: not compatible: {verdict}')
        entry = {'verdict': verdict, 'findings': {}, 'reportSha256': {}}
        for category, filename in CATEGORIES.items():
            path = directory / filename
            # Missing reports are not silently interpreted as an approval or a clean run.
            data = path.read_bytes()
            rows = [line.strip() for line in data.decode().splitlines() if line.strip()]
            prefix = {'internal': 'Internal ', 'experimental': 'Experimental API ',
                      'deprecated': 'Deprecated '}[category]
            if any(not row.startswith(prefix) for row in rows):
                raise ValueError(f'Unrecognized {category} report format: {path}')
            entry['findings'][category] = [dict(text=row, owner=owner(row)) for row in rows]
            entry['reportSha256'][filename] = hashlib.sha256(data).hexdigest()
        result['targets'][target] = entry
    result['requiresOwnerApiDecision'] = any(
        entry['findings']['internal'] or entry['findings']['experimental']
        for entry in result['targets'].values())
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reports', type=Path, default=Path('build/reports/pluginVerifier'))
    parser.add_argument('--version', required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--require-api-clean', action='store_true',
                        help='Fail if internal or experimental findings remain; never grants exemptions')
    args = parser.parse_args()
    try:
        result = collect(args.reports, args.version)
    except (OSError, ValueError) as failure:
        parser.exit(1, f'{failure}\n')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    for target, entry in result['targets'].items():
        print(target, ', '.join(f'{len(rows)} {category}' for category, rows in entry['findings'].items()))
    print('API findings recorded; no approval or publication authorization granted.')
    if args.require_api_clean and result['requiresOwnerApiDecision']:
        parser.exit(1, 'Internal/experimental API usages remain. Owner decision required.\n')


if __name__ == '__main__':
    main()
