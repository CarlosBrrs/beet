/* =========================================================================
   V4: Normalize Owner role name to UPPERCASE and set wildcard permissions.
   ========================================================================= */

-- 1. Normalize role name to UPPERCASE to match Java PermissionModule/PermissionAction enums
UPDATE roles SET name = 'OWNER' WHERE name = 'Owner';

-- 2. Set the Owner role permissions to the ALL:ALL wildcard.
--    This means the OWNER has unrestricted access to everything.
--    The backend/frontend both check for { "ALL": ["ALL"] } as the superuser signal.
UPDATE roles
SET permissions = '{"ALL": ["ALL"]}'
WHERE name = 'OWNER';
