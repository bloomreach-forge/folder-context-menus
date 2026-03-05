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

# Release Process

Releases are fully automated via GitHub Actions. No local version bumping or tagging required.

## Steps

1. Merge `develop` into `master`
2. Go to **Actions → Release → Run workflow** (from `master`)
3. Enter the release version (e.g. `5.2.1`) and the next SNAPSHOT (e.g. `5.2.2-SNAPSHOT`)
4. Click **Run workflow**

The [Release](.github/workflows/release.yml) workflow will:

1. Set the version in `pom.xml` and `demo/pom.xml` to the release version
2. Build and test the project and demo
3. Deploy the artifact to the Bloomreach Forge Maven repository
4. Generate `forge-addon.yaml` from `.forge/addon-config.yaml`
5. Commit the release files (`pom.xml` + `forge-addon.yaml`) to `master` and create the `x.y.z` tag — the tag points to this commit, so `forge-addon.yaml` is readable via the GitHub Contents API at that ref
6. Create a GitHub Release with `forge-addon.yaml` also attached as a downloadable asset
7. Bump `pom.xml` and `demo/pom.xml` to the next SNAPSHOT and commit

Once the GitHub Release is published, the [Deploy Docs](.github/workflows/deploy-docs.yml) workflow runs automatically and publishes the updated site to `gh-pages`.

The workflow also automatically pushes the next SNAPSHOT version to `develop`.

### Branch model

| Branch | Purpose |
|---|---|
| `develop` | Active development |
| `master` | Release branch — the release workflow runs here |
| `gh-pages` | Published documentation (managed by CI, do not edit manually) |