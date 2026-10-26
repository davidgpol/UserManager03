"""Install checksum-pinned project tools inside the disposable build image only."""
import base64
import hashlib
import json
import pathlib
import platform
import subprocess
import tarfile
import tempfile

if platform.machine() not in ("aarch64", "arm64"):
    raise SystemExit("This toolchain is restricted to Linux ARM64")

manifest = json.loads(pathlib.Path(__file__).with_name("distributions.json").read_text())
with tempfile.TemporaryDirectory(prefix="usermanager03-toolchain-") as temporary:
    for tool in manifest["distributions"]:
        archive = pathlib.Path(temporary) / tool["name"]
        subprocess.run([
            "curl", "--fail", "--silent", "--show-error", "--location",
            "--proto", "=https", "--proto-redir", "=https", "--retry", "2",
            "--output", str(archive), tool["url"],
        ], check=True)
        actual = hashlib.file_digest(archive.open("rb"), tool["algorithm"]).digest()
        expected = (base64.b64decode(tool["digest"]) if tool.get("encoding") == "base64"
                    else bytes.fromhex(tool["digest"]))
        if actual != expected:
            raise SystemExit(f"Checksum mismatch: {tool['name']}")
        destination = pathlib.Path(tool["destination"])
        if tool.get("archive", True):
            destination.mkdir(parents=True, exist_ok=True)
            # Distribution tarballs all have one enclosing directory.
            with tarfile.open(archive) as source:
                members = source.getmembers()
                roots = {m.name.split('/')[0] for m in members}
                if len(roots) != 1:
                    raise SystemExit(f"Unexpected archive layout: {tool['name']}")
                unpacked = pathlib.Path(temporary) / (tool["name"] + "-extracted")
                source.extractall(unpacked, filter="data")
                root = unpacked / roots.pop()
                for child in root.iterdir():
                    child.rename(destination / child.name)
        else:
            archive.rename(destination)
        print(f"Verified and installed {tool['name']}")

# Use the separately verified npm distribution rather than the bundled version.
for name, target in (("npm", "npm-cli.js"), ("npx", "npx-cli.js")):
    executable = pathlib.Path("/opt/node/bin") / name
    executable.unlink(missing_ok=True)
    executable.symlink_to(pathlib.Path("/opt/npm/bin") / target)
