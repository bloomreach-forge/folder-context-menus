# Folder Context Menus - Permission Configuration

## Overview

The folder-context-menus plugin provides granular, split permissions for folder copy and move operations. This allows administrators to give users the ability to copy folders without being able to move them, or vice versa.

**Related JIRA Ticket:** FORGE-575

## Permission Model

### Privileges

The plugin defines three custom Hippo privileges:

| Privilege | Role Name | Purpose |
|-----------|-----------|---------|
| `folderctxmenus:copy` | `folderctxmenus-copy` | Grants permission to copy folders |
| `folderctxmenus:move` | `folderctxmenus-move` | Grants permission to move folders |
| `folderctxmenus:editor` | `folderctxmenus-editor` | **Deprecated** - Grants both copy and move permissions (backward compatibility) |

### Permission Checking

Permissions are checked at **two levels**:

#### 1. Frontend Level (Menu Visibility)
- When a user opens the folder context menu, the plugin checks individual privileges
- Copy menu item only appears if user has `folderctxmenus:copy` privilege
- Move menu item only appears if user has `folderctxmenus:move` privilege
- **Location:** `CopyFolderWorkflowMenuItemPlugin.userHasCopyFolderPrivilege()` and `MoveFolderWorkflowMenuItemPlugin.userHasMoveFolderPrivilege()`

#### 2. Backend Level (Operation Authorization)
- When user executes copy/move operation, the backend verifies they have `HIPPO_AUTHOR` permission on destination folder
- This provides additional security and prevents unauthorized operations
- **Location:** `ExtendedFolderWorkflowImpl.copyFolder()` and `moveFolder()`

## Configuration Examples

### Scenario 1: Copy-Only Access (FORGE-575 Use Case)

Grant users the ability to copy folders but not move them:

```yaml
definitions:
  config:
    /hippo:configuration/hippo:domains/extra-folder-options:
      jcr:primaryType: hipposys:domain
      /content-domain:
        jcr:primaryType: hipposys:domainrule
        hipposys:equals: true
        hipposys:facet: jcr:path
        hipposys:type: Reference
        hipposys:value: /content
      /copy-only-users:
        jcr:primaryType: hipposys:authrole
        hipposys:role: folderctxmenus-copy    # Only copy privilege
        hipposys:userrole: xm.content.author   # Assign to author users
        hipposys:users:
          .meta:category: system
          .meta:add-new-system-values: true
          type: string
          value: []
```

**Result:** Users assigned to `xm.content.author` role can copy folders but will not see the move menu item.

### Scenario 2: Move-Only Access

Grant users the ability to move folders but not copy them:

```yaml
definitions:
  config:
    /hippo:configuration/hippo:domains/extra-folder-options:
      jcr:primaryType: hipposys:domain
      /content-domain:
        jcr:primaryType: hipposys:domainrule
        hipposys:equals: true
        hipposys:facet: jcr:path
        hipposys:type: Reference
        hipposys:value: /content
      /move-only-users:
        jcr:primaryType: hipposys:authrole
        hipposys:role: folderctxmenus-move     # Only move privilege
        hipposys:userrole: xm.content.admin    # Assign to admin users
        hipposys:users:
          .meta:category: system
          .meta:add-new-system-values: true
          type: string
          value: []
```

**Result:** Users assigned to `xm.content.admin` role can move folders but will not see the copy menu item.

### Scenario 3: Both Permissions (Backward Compatible)

Grant users both copy and move permissions (original behavior):

```yaml
definitions:
  config:
    /hippo:configuration/hippo:domains/extra-folder-options:
      jcr:primaryType: hipposys:domain
      /content-domain:
        jcr:primaryType: hipposys:domainrule
        hipposys:equals: true
        hipposys:facet: jcr:path
        hipposys:type: Reference
        hipposys:value: /content
      /admin:
        jcr:primaryType: hipposys:authrole
        hipposys:role: folderctxmenus-editor   # Both copy and move
        hipposys:userrole: xm.content.admin
        hipposys:users:
          .meta:category: system
          .meta:add-new-system-values: true
          type: string
          value: []
```

**Result:** Users assigned to `xm.content.admin` role can both copy and move folders (original behavior).

## Default Configuration

The plugin ships with the default configuration that assigns the `folderctxmenus-editor` role (granting both privileges) to the `xm.content.admin` user role.

**File:** `repository/src/main/resources/hcm-config/configuration/domains/extra-folder-options.yaml`

## Upgrade Path

### From Version 6.0.x to 7.1.2+ (with FORGE-575)

**Backward Compatibility:** Fully maintained. Existing configurations using `folderctxmenus-editor` continue to work without any changes.

**Migration Steps:** None required for backward compatibility. To use new granular permissions:

1. Create additional `hipposys:authrole` entries for specific user roles
2. Assign `folderctxmenus-copy` or `folderctxmenus-move` roles as needed
3. The existing `folderctxmenus-editor` role continues to grant both permissions

## Testing Permission Changes

### Verify Copy-Only Access

1. Create a test user with `folderctxmenus-copy` privilege
2. Log in as that user
3. Right-click on a folder in the CMS
4. **Expected:** Copy menu item visible, Move menu item not visible
5. Click Copy and verify it works

### Verify Move-Only Access

1. Create a test user with `folderctxmenus-move` privilege
2. Log in as that user
3. Right-click on a folder in the CMS
4. **Expected:** Move menu item visible, Copy menu item not visible
5. Click Move and verify it works

### Verify Both Permissions (Original Behavior)

1. Create a test user with both `folderctxmenus-copy` and `folderctxmenus-move` privileges (or use `folderctxmenus-editor`)
2. Log in as that user
3. Right-click on a folder in the CMS
4. **Expected:** Both Copy and Move menu items visible
5. Verify both operations work

### Verify No Permissions

1. Create a test user without any folder context menu privileges
2. Log in as that user
3. Right-click on a folder in the CMS
4. **Expected:** Neither Copy nor Move menu items visible

## Advanced: Custom Permission Model

To implement a completely different permission model:

1. Extend `AbstractFolderActionWorkflowMenuItemPlugin` in your own plugin
2. Override the constructor to implement custom permission checking logic
3. Use `checkPrivilege()` method to verify privileges based on custom logic
4. Define custom privilege names in your plugin's configuration

Example:
```java
public class CustomCopyPlugin extends AbstractFolderActionWorkflowMenuItemPlugin {
    public CustomCopyPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        // Your custom permission logic here
    }
}
```

## Security Considerations

1. **Server-Side Enforcement:** All copy/move operations are validated server-side against `HIPPO_AUTHOR` permission
2. **Menu Visibility:** Frontend permission checks hide menu items but don't prevent operation execution
3. **User Isolation:** Privileges are assigned per user role and domain path
4. **Audit Trail:** All copy/move operations are logged with user information

## Troubleshooting

### Copy/Move Menu Items Not Appearing

**Symptoms:** Menu items don't appear even for admin users

**Causes:**
1. User doesn't have `folderctxmenus:copy` or `folderctxmenus:move` privilege
2. Domain configuration not correctly mapping role to user role
3. Plugin not properly deployed

**Solution:**
1. Verify user has correct role via Hippo Repository
2. Check domain configuration in `extra-folder-options.yaml`
3. Verify plugin JAR is in classpath
4. Check server logs for errors

### Operation Fails with "User Lacks Permission"

**Symptoms:** Copy/Move menu item visible but operation fails with "User lacks author permission in destination folder"

**Causes:**
1. User doesn't have `HIPPO_AUTHOR` permission on destination folder
2. Destination folder permissions restricted

**Solution:**
1. Verify user has `HIPPO_AUTHOR` permission on destination folder
2. Administrators can grant folder permissions in Hippo CMS
3. Check folder permission configuration

### Backward Compatibility Issues

**Symptoms:** Existing configurations fail after upgrade

**Causes:**
1. Using very old configuration files that relied on hard-coded privilege names
2. Custom extensions relying on old `userHasAdvancedFolderPrivileges()` method

**Solution:**
1. The old method `userHasAdvancedFolderPrivileges()` is deprecated but still functional
2. It returns true only if BOTH copy and move privileges are granted
3. Update custom extensions to use new `userHasCopyFolderPrivilege()` and `userHasMoveFolderPrivilege()` methods

## References

- **Configuration Files:**
  - Role Definition: `repository/src/main/resources/hcm-config/configuration/roles/folderctxmenus-extra.yaml`
  - Domain Mapping: `repository/src/main/resources/hcm-config/configuration/domains/extra-folder-options.yaml`

- **Source Code:**
  - Base Plugin: `frontend/src/main/java/org/onehippo/forge/folderctxmenus/cms/plugin/AbstractFolderActionWorkflowMenuItemPlugin.java`
  - Copy Plugin: `frontend/src/main/java/org/onehippo/forge/folderctxmenus/cms/plugin/CopyFolderWorkflowMenuItemPlugin.java`
  - Move Plugin: `frontend/src/main/java/org/onehippo/forge/folderctxmenus/cms/plugin/MoveFolderWorkflowMenuItemPlugin.java`
  - Workflow: `common/src/main/java/org/onehippo/forge/folderctxmenus/common/ExtendedFolderWorkflowImpl.java`

- **Related JIRA:** FORGE-575 - Split the copy and move permissions
