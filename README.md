[![Build Status](https://travis-ci.org/bloomreach-forge/folder-context-menus.svg?branch=develop)](https://travis-ci.org/bloomreach-forge/folder-context-menus)

# Hippo Folder Context Menus

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
