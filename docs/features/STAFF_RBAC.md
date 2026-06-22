# Staff, Roles y Permisos

## Objetivo

Administrar empleados por cuenta y restaurante usando roles globales rigidos definidos por el software. Un empleado pertenece a un solo owner, puede trabajar en varios restaurantes de ese owner y tiene exactamente un rol por restaurante.

## Reglas Funcionales

- El owner conserva acceso total por `ALL:ALL`.
- Los roles viven globalmente en `roles` con `restaurant_id IS NULL`.
- Los roles operativos disponibles son `MANAGER`, `CASHIER`, `WAITER` y `KITCHEN`.
- Los permisos no son personalizables desde la UI tenant.
- El dueno del software podra modificar presets mediante migraciones, scripts internos o una futura consola superadmin.
- `user_restaurant_roles` sigue siendo la relacion operativa: usuario + restaurante + rol.
- Un rol global asignable puede usarse en cualquier restaurante.
- `OWNER` no es asignable por invitacion ni por edicion de staff.
- `MODULE:MANAGE` concede todas las acciones de ese modulo.
- `MODULE:ALL` y `ALL:ALL` conservan compatibilidad como acceso total.
- Las asignaciones `SUSPENDED` o `REMOVED` no aportan permisos.
- La suspension global del usuario bloquea login y acceso al cargar contexto.
- Remover acceso no elimina usuario ni historial.

## Roles Globales

| Preset | Asignable | Funcion |
|---|---:|---|
| `OWNER` | No | Acceso total por `ALL:ALL`. |
| `MANAGER` | Si | Operacion completa sin billing/subscription. |
| `CASHIER` | Si | Caja, pagos, movimientos y operacion de ordenes. |
| `WAITER` | Si | Mesas, creacion/edicion ordinaria de ordenes y consulta de cocina. |
| `KITCHEN` | Si | KDS y actualizacion de tickets de cocina. |

## Estados

### Usuario

- `ACTIVE`: puede autenticarse.
- `SUSPENDED`: no puede autenticarse ni operar.

### Asignacion Restaurante

- `ACTIVE`: aporta permisos.
- `SUSPENDED`: conserva vinculo, pero no aporta permisos en ese restaurante.
- `REMOVED`: acceso retirado, preservando auditoria.

### Invitacion

- `PENDING`: token vigente y no usado.
- `EXPIRED`: vencida.
- `ACCEPTED`: aceptada.
- `REVOKED`: revocada manualmente.

## Permisos

Modulo nuevo: `STAFF`.

Acciones:

- `VIEW`: consultar empleados, roles read-only e invitaciones.
- `CREATE`: crear invitaciones.
- `EDIT`: cambiar rol o estado por restaurante.
- `DELETE`: retirar acceso o revocar invitaciones.
- `MANAGE`: ejecutar todas las acciones del modulo.

Acciones sensibles agregadas:

- `ORDERS:CANCEL`.
- `ORDERS:COMPLETE`.
- `PAYMENTS:REFUND`.

## Invitaciones

- Se genera un token de un solo uso.
- La base de datos guarda solo SHA-256 del token.
- El enlace vence en 7 dias.
- Regenerar invalida el token anterior.
- Aceptar dos veces falla.
- El email del token no se puede cambiar durante aceptacion.
- Si el email ya pertenece a un empleado del mismo owner, se asigna directamente al restaurante.
- Si el email pertenece a otro owner o a un owner existente, se rechaza.
- La invitacion solo permite roles globales activos, asignables y con `restaurant_id IS NULL`.

## API

### Restaurante

```text
GET    /restaurants/{restaurantId}/staff/permission-catalog
GET    /restaurants/{restaurantId}/staff/roles
GET    /restaurants/{restaurantId}/staff
GET    /restaurants/{restaurantId}/staff/{userId}
PATCH  /restaurants/{restaurantId}/staff/{userId}
DELETE /restaurants/{restaurantId}/staff/{userId}
GET    /restaurants/{restaurantId}/staff/invitations
POST   /restaurants/{restaurantId}/staff/invitations
POST   /restaurants/{restaurantId}/staff/invitations/{invitationId}/regenerate
POST   /restaurants/{restaurantId}/staff/invitations/{invitationId}/revoke
```

No existen endpoints publicos para crear, editar, activar, desactivar o eliminar roles.

### Publico

```text
GET  /staff-invitations/{token}
POST /staff-invitations/{token}/accept
```

### Account

```text
GET   /account/staff
GET   /account/staff/{userId}
PATCH /account/staff/{userId}/status
```

## Frontend

- `/restaurants/[id]/staff`: tabs Employees, Roles e Invitations.
- `Roles` es read-only y muestra el catalogo de roles del sistema.
- No hay matriz editable de permisos.
- No hay acciones de crear, editar, activar/desactivar ni eliminar roles.
- `/account/staff`: vista consolidada del owner.
- `/invitations/[token]`: aceptacion publica.
- Navegacion movida a Configuration y Account.
- Acciones protegidas con `STAFF` y semantica `MANAGE`.
- Permisos del usuario refrescan al recuperar foco y luego de mutaciones de staff.

## Limites

- Se respeta `multiUserAccess` y `maxEmployees` del plan.
- Empleados existentes e invitaciones pendientes cuentan para el limite.

## Fuera de Alcance

- Envio SMTP.
- Empleados compartidos entre owners.
- Billing o cambio de plan desde staff.
- Roles personalizados por restaurante.
- Edicion tenant de permisos.
- Eliminacion fisica de usuarios, roles o asignaciones historicas.
