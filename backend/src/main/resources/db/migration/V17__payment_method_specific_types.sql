/* =========================================================================
   V17 - Payment method specific types.

   V16 originally created broad payment method categories. The frontend and
   backend now expose concrete configurable methods so restaurants can enable
   or disable debit, credit and wallet rails independently.
   ========================================================================= */

ALTER TYPE payment_method_type ADD VALUE IF NOT EXISTS 'DEBIT_CARD';
ALTER TYPE payment_method_type ADD VALUE IF NOT EXISTS 'CREDIT_CARD';
ALTER TYPE payment_method_type ADD VALUE IF NOT EXISTS 'NEQUI';
ALTER TYPE payment_method_type ADD VALUE IF NOT EXISTS 'DAVIPLATA';
ALTER TYPE payment_method_type ADD VALUE IF NOT EXISTS 'INTERNAL_CREDIT';
