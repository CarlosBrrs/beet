/* =========================================================================
   V12 - Enforce tenant integrity across cash registers, sessions and orders.
   ========================================================================= */

ALTER TABLE cash_registers ADD CONSTRAINT uq_cash_registers_id_restaurant
    UNIQUE (id, restaurant_id);

ALTER TABLE cash_sessions DROP CONSTRAINT fk_cash_sessions_register;
ALTER TABLE cash_sessions ADD CONSTRAINT fk_cash_sessions_register_restaurant
    FOREIGN KEY (cash_register_id, restaurant_id)
    REFERENCES cash_registers(id, restaurant_id)
    ON DELETE RESTRICT;

ALTER TABLE cash_sessions ADD CONSTRAINT uq_cash_sessions_id_restaurant
    UNIQUE (id, restaurant_id);

ALTER TABLE orders DROP CONSTRAINT fk_orders_cash_session;
ALTER TABLE orders ADD CONSTRAINT fk_orders_cash_session_restaurant
    FOREIGN KEY (cash_session_id, restaurant_id)
    REFERENCES cash_sessions(id, restaurant_id);
