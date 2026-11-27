"""Build a source release from a committed tree and reviewed dependency archives."""

import hashlib
import io
import re
import subprocess
import sys
from pathlib import Path, PurePosixPath
from zipfile import ZIP_STORED, ZipFile, ZipInfo


def git(*args):
    return subprocess.check_output(["git", *args])


def package(commit, version):
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("Use the full release commit SHA")
    if not re.fullmatch(r"[0-9]+(?:\.[0-9]+)+", version):
        raise ValueError("Invalid version name")
    listing = git("ls-tree", "-r", "-z", "--name-only", commit).decode().rstrip("\0").split("\0")
    manifest = "docs/licenses/CORRESPONDING-SOURCE-SHA256SUMS.txt"
    if manifest not in listing:
        raise ValueError("The reviewed corresponding-source manifest is missing")
    vendor = "third-party-source/"
    approved = {}
    for line in git("show", f"{commit}:{manifest}").decode().splitlines():
        if not line.strip():
            continue
        checksum, name = line.split()
        if not re.fullmatch(r"[0-9a-fA-F]{64}", checksum):
            raise ValueError("Invalid source archive checksum")
        if not re.fullmatch(r"[A-Za-z0-9_.-]+\.(?:zip|jar)", name) or name in approved:
            raise ValueError("Invalid or duplicate source archive name")
        approved[name] = checksum.lower()
    if not approved or set(vendor + name for name in approved) != {
        path for path in listing if path.startswith(vendor)
    }:
        raise ValueError("The reviewed source manifest and committed archives must match")

    with ZipFile(io.BytesIO(git("archive", "--format=zip", commit))) as source:
        entries = [entry for entry in source.infolist() if not entry.is_dir()]
        if set(listing) != {entry.filename for entry in entries}:
            raise ValueError("The archive must contain the complete committed source tree")
        for entry in entries:
            path = PurePosixPath(entry.filename)
            if (
                path.suffix.lower() in {".jks", ".keystore", ".p12", ".pfx", ".key", ".pem", ".apk", ".aab"}
                or path.name.lower() in {"local.properties", "keystore.properties", "google-services.json", ".env"}
                or path.name.lower().startswith(".env.")
                or re.fullmatch(r"Corresponding-Source-v.*\.zip", path.name)
            ):
                raise ValueError(f"Excluded local or sensitive file in release tree: {path}")
        for name, expected in approved.items():
            data = source.read(vendor + name)
            if hashlib.sha256(data).hexdigest() != expected:
                raise ValueError(f"Source archive checksum mismatch: {name}")
            with ZipFile(io.BytesIO(data)) as dependency:
                if not dependency.namelist() or dependency.testzip() is not None:
                    raise ValueError(f"Invalid source archive: {name}")

        output = Path(f"Corresponding-Source-v{version}.zip")
        with ZipFile(output, "w", compression=ZIP_STORED) as archive:
            archive.comment = commit.encode("ascii")
            for entry in sorted(entries, key=lambda item: item.filename):
                target = ZipInfo(f"GANASUNO-v{version}/{entry.filename}")
                target.create_system = 3
                target.external_attr = entry.external_attr
                archive.writestr(target, source.read(entry))
    print(output)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("Usage: package_release_source.py COMMIT VERSION")
    try:
        package(*sys.argv[1:])
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        sys.exit(str(error))
