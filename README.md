# Extra Folder Context Menus

This project provides extra Folder Context Menu items such as 'Copy folder...' and 'Move folder...'
for BloomReach XM with extensible base implementations for developers.

# Documentation (Local)

The documentation can generated locally by this command:

 > mvn clean site

The output is in the ```target/site/``` directory by default. You can open ```target/site/index.html``` in a browser.

# Documentation (GitHub Pages)

Documentation is available at [bloomreach-forge.github.io/folder-context-menus](https://bloomreach-forge.github.io/folder-context-menus).

You can generate the GitHub pages only from ```master``` branch by this command:

 > mvn -Pgithub.pages clean site

The output is in the ```docs/``` directory by default. You can open ```docs/index.html``` in a browser.

You can push it and GitHub Pages will be served for the site automatically.

# Releasing

The release workflow is triggered by pushing a tag that matches the version in both `pom.xml` and `demo/pom.xml`.

1. Set the release version and commit
```bash
mvn versions:set -DgenerateBackupPoms=false -DnewVersion="x.y.z"
mvn -f demo versions:set -DgenerateBackupPoms=false -DnewVersion="x.y.z"
git commit -a -m "<ISSUE_ID> releasing x.y.z: set version"
```

2. Create and push the release tag
```bash
git tag x.y.z
git push origin x.y.z
```

3. Set the next snapshot version and push your branch
```bash
mvn versions:set -DgenerateBackupPoms=false -DnewVersion="x.y.z+1-SNAPSHOT"
mvn -f demo versions:set -DgenerateBackupPoms=false -DnewVersion="x.y.z+1-SNAPSHOT"
git commit -a -m "<ISSUE_ID> releasing x.y.z: set next development version"
git push origin <branch>
```

The release workflow automatically:
- Verifies the root and demo versions match the tag
- Builds and tests the project and demo
- Deploys to the Forge Maven repository
- Creates a GitHub Release
- Regenerates and publishes the docs to `master`
