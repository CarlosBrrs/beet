# Cash Operations V22 - Manual Testing

## Preparacion

Usar un restaurante con owner `ALL:ALL`, una caja activa y productos publicados.

```sql
SELECT id, name, settings FROM restaurants WHERE id = '<restaurant-id>';
```

Debe existir `settings.cashCountMode`, con `BLIND` por defecto.

## Metodos de pago

Al crear un restaurante deben aparecer Efectivo, Debito, Credito, Nequi y Daviplata sin acciones manuales.

```sql
SELECT code, name, type, is_active, requires_reference, sort_order
FROM payment_methods
WHERE restaurant_id = '<restaurant-id>'
ORDER BY sort_order;
```

Crear un metodo personalizado, editarlo y desactivarlo. El codigo queda normalizado en mayusculas. Desactivar el ultimo
metodo activo debe fallar sin modificar la fila.

## Abrir dia operativo

En Cash Registers, pulsar `Abrir dia`.

```sql
SELECT * FROM restaurant_business_days
WHERE restaurant_id = '<restaurant-id>' ORDER BY business_date DESC;

SELECT * FROM business_day_events
WHERE restaurant_id = '<restaurant-id>' ORDER BY occurred_at DESC;
```

Debe existir un dia `OPEN` y un evento `OPENED`. Sin dia abierto deben fallar apertura de caja, orden, pago y movimiento.

## Abrir cajas

```sql
SELECT id, business_day_id, cash_register_id, status, opening_amount, opened_device_id
FROM cash_sessions
WHERE business_day_id = '<business-day-id>';
```

Todas las cajas del ciclo deben apuntar al mismo `business_day_id`.

## Entradas y salidas

Registrar fondo de cambio `IN` y retiro `OUT`.

```sql
SELECT direction, reason, amount, status, notes, created_device_id
FROM cash_movements
WHERE cash_session_id = '<session-id>' ORDER BY created_at;
```

`OTHER` y `CORRECTION` sin nota deben fallar. Otro dispositivo o una sesion cerrada tambien deben fallar.

## Anular movimiento

```sql
SELECT status, voided_at, voided_by, voided_device_id, void_reason
FROM cash_movements WHERE id = '<movement-id>';
```

Debe quedar `VOIDED`; la fila no se elimina y deja de afectar el arqueo.

## Arqueo visible y ciego

Configurar `settings.cashCountMode` como `VISIBLE` o `BLIND`. En visible se muestra el efectivo esperado antes del
conteo. En ciego el endpoint oculta esperado y totales hasta cerrar. Una diferencia no cero exige explicacion.

```sql
SELECT * FROM cash_session_closures WHERE cash_session_id = '<session-id>';
SELECT * FROM cash_session_payment_totals
WHERE closure_id = '<closure-id>' ORDER BY method_name_snapshot;
```

Validar:

```text
expectedCash =
openingAmount
+ pagos CASH
+ propinas CASH
- devoluciones del pago CASH
+ movimientos IN
- movimientos OUT
```

`cash_sessions.closing_amount` debe coincidir con `cash_session_closures.counted_cash`.

## Cierre diario

Intentar cerrar con cajas abiertas, ordenes `DRAFT/AWAITING_PAYMENT/OPEN`, devoluciones pendientes o arqueos faltantes.
Cada caso debe fallar. Luego resolver bloqueos y cerrar.

```sql
SELECT * FROM business_day_closures
WHERE business_day_id = '<business-day-id>' ORDER BY closure_sequence;

SELECT * FROM business_day_payment_totals
WHERE closure_id = '<business-day-closure-id>' ORDER BY method_name_snapshot;
```

El dia queda `CLOSED`, se crea evento `CLOSED` y se consolidan todas las cajas.

## Reapertura

Reabrir con motivo, abrir una caja nueva y cerrar otra vez.

```sql
SELECT event_type, reason, occurred_at
FROM business_day_events
WHERE business_day_id = '<business-day-id>' ORDER BY occurred_at;

SELECT closure_sequence, closed_at, expected_cash_total, counted_cash_total, difference_total
FROM business_day_closures
WHERE business_day_id = '<business-day-id>' ORDER BY closure_sequence;
```

Deben conservarse ambos cierres y el evento `REOPENED`.

## Multitenancy y concurrencia

- IDs de otro restaurante deben rechazarse.
- Pago, movimiento y cierre sobre una sesion deben serializarse.
- No se registra pago o movimiento despues del arqueo.
- Solo existe un dia `OPEN` por restaurante.

## Devoluciones diferidas

V22 atribuye devoluciones al metodo original. Quedan pendientes el metodo real de devolucion, reversos de pasarela,
autorizaciones, limites y conciliacion externa.
