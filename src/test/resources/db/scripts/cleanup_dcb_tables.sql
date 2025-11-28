-- Conditionally truncate tables only if schema exists
CREATE OR REPLACE FUNCTION truncate_dcb_tables() RETURNS void AS '
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = ''test_tenant_mod_dcb'') THEN
    EXECUTE ''TRUNCATE TABLE test_tenant_mod_dcb.transactions CASCADE'';
    EXECUTE ''TRUNCATE TABLE test_tenant_mod_dcb.transactions_audit CASCADE'';
    EXECUTE ''TRUNCATE TABLE test_tenant_mod_dcb.service_point_expiration_period CASCADE'';
  END IF;
END;
' LANGUAGE plpgsql;

SELECT truncate_dcb_tables();
DROP FUNCTION IF EXISTS truncate_dcb_tables();
