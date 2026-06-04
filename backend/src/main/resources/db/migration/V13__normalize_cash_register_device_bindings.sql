/* =========================================================================
   V13 - Cash register bindings exist only while a cash session is open.
   ========================================================================= */

UPDATE cash_registers
   SET device_id = NULL,
       updated_at = NOW()
 WHERE device_id IS NOT NULL;

UPDATE cash_registers cr
   SET device_id = cs.opened_device_id,
       updated_at = NOW()
  FROM cash_sessions cs
 WHERE cs.cash_register_id = cr.id
   AND cs.restaurant_id = cr.restaurant_id
   AND cs.status = 'OPEN';
