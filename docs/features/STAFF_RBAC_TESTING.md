# Staff RBAC - Pruebas Manuales

## Consultas Base

```sql
SELECT id, email, account_status, owner_id, last_login_at
FROM users
ORDER BY created_at DESC;

SELECT id, restaurant_id, name, preset_key, is_active, is_assignable, permissions
FROM roles
ORDER BY preset_key NULLS LAST, name;

SELECT user_id, restaurant_id, role_id, assignment_status, deleted_at
FROM user_restaurant_roles
ORDER BY updated_at DESC;

SELECT id, restaurant_id, role_id, email, accepted_at, revoked_at, expires_at, deleted_at
FROM staff_invitations
ORDER BY created_at DESC;
```

## 1. Roles Globales

Accion: validar V24 o iniciar backend con V24 aplicada.

Esperado en DB:

- Existe un solo set global con `OWNER`, `MANAGER`, `CASHIER`, `WAITER` y `KITCHEN`.
- Todos tienen `restaurant_id IS NULL`.
- `OWNER` tiene `is_assignable = false` y `permissions = {"ALL":["ALL"]}`.
- Roles operativos tienen `is_assignable = true`.
- Crear un restaurante nuevo no crea roles locales.

SQL:

```sql
SELECT restaurant_id, preset_key, is_assignable, COUNT(*)
FROM roles
WHERE preset_key IN ('OWNER', 'MANAGER', 'CASHIER', 'WAITER', 'KITCHEN')
GROUP BY restaurant_id, preset_key, is_assignable
ORDER BY preset_key;
```

## 2. Listar Roles En Staff

Accion frontend:

1. Ir a `/restaurants/{id}/staff`.
2. Abrir tab `Roles`.

Endpoint esperado:

```text
GET /restaurants/{restaurantId}/staff/roles
```

Esperado:

- Se muestran solo roles globales activos y asignables.
- No aparece `OWNER`.
- No existen botones para crear, editar, activar/desactivar o eliminar roles.

## 3. Crear Invitacion

Accion frontend:

1. Ir a `/restaurants/{id}/staff`.
2. Abrir `Invite`.
3. Seleccionar un rol global asignable y email nuevo.
4. Crear invitacion.

Endpoint esperado:

```text
POST /restaurants/{restaurantId}/staff/invitations
```

Esperado en UI:

- Toast de invitacion creada.
- Enlace copiado.
- Fila en tab Invitations con estado `PENDING`.

Esperado en DB:

- `staff_invitations.email` normalizado en minusculas.
- `role_id` apunta a un rol global con `restaurant_id IS NULL` e `is_assignable = true`.
- `token_hash` poblado.
- No existe token plano.
- `accepted_at` y `revoked_at` son `NULL`.

## 4. Aceptar Invitacion

Accion frontend:

1. Abrir `/invitations/{token}`.
2. Completar nombre, apellido, telefono opcional y contrasena.
3. Aceptar.

Endpoints esperados:

```text
GET  /staff-invitations/{token}
POST /staff-invitations/{token}/accept
```

Esperado en DB:

- Nuevo usuario con `owner_id` del owner del restaurante.
- Usuario con `account_status = ACTIVE`.
- Una fila `user_restaurant_roles` con `assignment_status = ACTIVE`.
- `role_id` apunta al rol global elegido.
- Invitacion con `accepted_at` poblado.

## 5. Reutilizar Token

Accion: intentar aceptar el mismo link otra vez.

Esperado:

- Backend rechaza la aceptacion.
- No se crea segundo usuario ni segunda asignacion.

## 6. Regenerar Invitacion

Accion frontend:

1. En tab Invitations, presionar `Regenerate`.
2. Copiar el nuevo link.

Endpoint esperado:

```text
POST /restaurants/{restaurantId}/staff/invitations/{invitationId}/regenerate
```

Esperado en DB:

- Cambia `token_hash`.
- `expires_at` se actualiza a 7 dias desde la regeneracion.
- El token anterior deja de resolver.

## 7. Revocar Invitacion

Accion frontend:

1. En tab Invitations, presionar `Revoke`.
2. Confirmar.

Endpoint esperado:

```text
POST /restaurants/{restaurantId}/staff/invitations/{invitationId}/revoke
```

Esperado en DB:

- `revoked_at` y `revoked_by` poblados.
- Estado calculado `REVOKED`.

## 8. Intentar Asignar OWNER

Accion tecnica: enviar una invitacion o patch usando el `role_id` de `OWNER`.

Esperado:

- Backend rechaza con error de rol no asignable.
- No se crea invitacion ni asignacion operativa.

SQL:

```sql
SELECT id FROM roles WHERE preset_key = 'OWNER';
```

## 9. Cambiar Rol De Empleado

Accion frontend:

1. Tab Employees.
2. Editar empleado.
3. Seleccionar otro rol global asignable.
4. Guardar.

Endpoint esperado:

```text
PATCH /restaurants/{restaurantId}/staff/{userId}
```

Esperado en DB:

- Misma fila `user_restaurant_roles` cambia `role_id`.
- Sigue existiendo una sola asignacion por `(user_id, restaurant_id)`.
- El nuevo `role_id` apunta a un rol global.

## 10. Suspender Por Restaurante

Accion frontend:

1. Editar empleado.
2. Estado `Suspended for this restaurant`.
3. Guardar.

Esperado:

- `assignment_status = SUSPENDED`.
- Permisos de ese restaurante desaparecen en `/auth/my-permissions`.
- Si tiene otro restaurante activo, conserva acceso alli.

## 11. Remover Acceso

Accion frontend:

1. Tab Employees.
2. Accion Remove access.
3. Confirmar.

Esperado en DB:

- `assignment_status = REMOVED`.
- `deleted_at` y `deleted_by` poblados.
- Usuario permanece en `users`.

## 12. Suspension Global

Accion frontend:

1. Ir a `/account/staff`.
2. Suspender cuenta.

Endpoint esperado:

```text
PATCH /account/staff/{userId}/status
```

Esperado en DB:

- `users.account_status = SUSPENDED`.

Esperado en auth:

- Login falla.
- JWT existente deja de operar cuando backend carga el usuario.

## 13. Rol Diferente Por Restaurante

Accion:

1. Invitar o asignar el mismo empleado a dos restaurantes del mismo owner.
2. Usar roles distintos.

Esperado:

- Hay una fila por restaurante en `user_restaurant_roles`.
- Cada fila puede apuntar a un rol global distinto.
- Los permisos se aplican por restaurante.

## 14. Aislamiento Multitenant

Accion:

- Intentar consultar o modificar staff con otro `restaurantId`.
- Intentar asignar un usuario que pertenece a otro owner.

Esperado:

- Backend rechaza por validacion.
- No se crean filas cruzadas.

## 15. Limites De Plan

Accion:

- Configurar `maxEmployees` bajo en `subscription_plans.features`.
- Crear empleados/invitaciones hasta el limite.

Esperado:

- Empleados existentes e invitaciones pendientes cuentan.
- La siguiente invitacion falla.
