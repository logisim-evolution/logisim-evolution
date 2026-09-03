Contributing rules

* Open issue/FR ticket **first** and discuss your intent,
* Fork the repository,
* You **MUST** put your work either in the **main** branch or a branch of **main**,
* Once done, create a pull request against the **main** branch,
* Ensure your change is also listed in the topmost (`@dev`) section of `CHANGES.md`,
  crediting yourself with your GitHub handle, i.e. `* Fixed the frobnicator (@nick).`
  This is enforced by CI for pull requests touching the code, and can be opted out of
  by putting either of these markers in the pull request description:
  * `NO_CHANGELOG_ENTRY` if the change really needs no entry at all (typo fix,
    internal cleanup, etc.),
  * `NO_CHANGELOG_AUTHOR_CREDIT` if you want the entry, but prefer not to be credited.
