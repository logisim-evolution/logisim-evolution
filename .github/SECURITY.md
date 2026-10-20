# Security

We take the security of our software seriously, which includes all source code repositories managed through our GitHub
organizations, which include [Logisim-evolution](https://github.com/logisim-evolution).

If you believe you have found a security vulnerability in any of our own repository that poses serious threat to the end users and
you feel that creating regular bug ticket is not enough, please report it to us as described below.

## Reporting Security Issues

**Please do not report security vulnerabilities through public GitHub issues in unencrypted form.**

Instead, please use the GPG key attached to the end of the file to encrypt your message, then export it as ASCII armour and create
the regular ticket.

```text
-----BEGIN PGP PUBLIC KEY BLOCK-----

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
Z9iZimT6H+eeNqCGo5wqXxPIq3w5c5hb
=8Tnv
-----END PGP PUBLIC KEY BLOCK-----
```

Please include the requested information listed below (as much as you can provide) to help us better understand the nature and scope
of the possible issue:

* Type of issue (e.g. buffer overflow, SQL injection, cross-site scripting, etc.)
* Full paths of source file(s) related to the manifestation of the issue
* The location of the affected source code (tag/branch/commit or direct URL)
* Any special configuration required to reproduce the issue
* Step-by-step instructions to reproduce the issue
* Proof-of-concept or exploit code (if possible)
* Impact of the issue, including how an attacker might exploit the issue

This information will help us triage your report more quickly.

## Preferred Languages

We prefer all communications to be in English.
