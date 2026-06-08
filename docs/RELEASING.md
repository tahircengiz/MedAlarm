# Releasing

Releases are produced by CI from a git tag. You never build or upload an APK by hand.

## How to cut a release

1. Bump the version in `app/build.gradle.kts`:
   - `versionName` — semver, e.g. `0.1.0-beta2` (a hyphen marks it a pre-release).
   - `versionCode` — increment by 1 (monotonic; Android requires it for upgrades).
2. Commit + push that bump to `main` and let the normal CI build go green.
3. Tag and push:
   ```sh
   git tag v0.1.0-beta2
   git push origin v0.1.0-beta2
   ```
4. The **Release** workflow (`.github/workflows/release.yml`) runs automatically:
   - rebuilds with the manifesto guard,
   - decodes the signing keystore from secrets,
   - `gradle assembleRelease` → signed APK,
   - creates a GitHub Release named `MedAlarm vX.Y.Z`, marks it pre-release if the
     tag contains a hyphen, and attaches `MedAlarm-vX.Y.Z.apk`.

Tag naming: `v` + the `versionName` (e.g. `v0.1.0-beta2`).

## Signing

The app is signed with a PKCS12 keystore that is **never** committed. The key
material lives in two places:

- **GitHub Actions secrets** (used by CI):
  `SIGNING_KEYSTORE_BASE64`, `SIGNING_KEYSTORE_PASSWORD`,
  `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.
- **Local backup**: `~/medalarm-signing-backup/` on the maintainer's machine
  (`medalarm-release.p12` + `CREDENTIALS.txt`).

> ⚠️ If the keystore is lost, you can no longer ship signed upgrades that install
> over existing copies — users would have to uninstall and reinstall. Keep the
> backup safe (and ideally mirror it to a password manager / encrypted storage).

### Building a release APK locally (optional)

You don't need the real keystore for a local smoke build — `assembleRelease`
falls back to the debug signing identity when no release signing is configured,
producing an installable (but **not distributable**) APK.

To sign locally with the real key, create `keystore.properties` at the repo root
(it's git-ignored):

```properties
storeFile=/absolute/path/to/medalarm-release.p12
storePassword=...
keyAlias=medalarm
keyPassword=...
```

## Rotating the signing key

Only do this before a final 1.0 with no installed beta base, or you'll break
in-place upgrades. Generate a new PKCS12 keystore, update the four GitHub secrets,
and refresh the local backup.
