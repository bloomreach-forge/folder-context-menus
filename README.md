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



# Upgrade 14: actions required due to new security model

The security model has changed in the BrXM V14. Therefore the authorization model has slightly changed. 
Authors and editors do not have access to the extra folder options by default. To grant users access to those 
extra actions, the users should be granted a newly introduced privilege on the folders they should have 
the additional folder options.

The new version makes use of an override of the folder workflow. This means that when the project uses custom classes 
for the threepane workflow, specifically the folder-extended and folder-permissions categories, those classes need to 
be aligned with the new ExtendedFolderWorkflow implementation. 


# Permissions / privilege for extra folder options

For users to have access to the extra folder options, it is required that they are granted an additional 
role folderctxmenus-extra (privilege 'folderctxmenus:extra') on the folders they should have those extra actions on.   

By default only the admin has privileges to use the extra folder options (for all content), through 
auth rule 'extra-folder-options' in the domain rule 'content'.

By adding the editor group to the auth role, all members of the editor group will also have access to the extra 
folder options.

```
definitions:
  config:
    /hippo:configuration/hippo:domains/content/extra-folder-options:
      hipposys:groups:
        operation: add
        type: string
        value: [editor]
```
