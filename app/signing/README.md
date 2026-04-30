KeiOS CI debug signing
======================

`keios-ci-debug.keystore` is a shared non-release signing key for debug and
benchmark APKs published by CI. It is intentionally public and reproducible so
debug/benchmark artifacts from GitHub Actions can be verified across machines.

Store password: `android`
Key alias: `androiddebugkey`
Key password: `android`
Certificate SHA-256:
`0E:F1:13:32:DC:81:07:B7:85:35:BA:C6:D3:50:7C:6C:52:C2:22:46:30:C8:4E:FD:8D:44:C3:D9:21:FF:4F:D8`

Release builds use a separate release signing flow with a private release key.
The CI workflows verify this certificate before uploading debug and benchmark
artifacts.
