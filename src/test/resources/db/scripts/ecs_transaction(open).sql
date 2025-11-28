INSERT INTO test_tenant_mod_dcb.transactions (id, item_id, item_title, item_barcode, service_point_id,
                                              service_point_name, pickup_library_code, material_type,
                                              lending_library_code, patron_id, patron_group, patron_barcode, status,
                                              role, created_by, created_date, updated_by, updated_date, request_id,
                                              self_borrowing, item_location_code)
VALUES ('571b0a2c-8883-40b5-a449-d41fe6017083', '5b95877d-86c0-4cb7-a0cd-7660b348ae5a', 'ITEM', 'DCB_ITEM',
        '9d1b77e8-f02e-4b7f-b296-3f2042ddac55', 'TestServicePointCode', 'TestLibraryCode', 'book', 'EXC',
        '18c1741d-e678-4c8e-9fe7-cfaeefab5eea', 'patron', 'DCB_PATRON', 'OPEN', 'BORROWER',
        '08d51c7a-0f36-4f3d-9e35-d285612a23df', '2025-10-30 14:19:18.980689 +00:00', null, null,
        '571b0a2c-8883-40b5-a449-d41fe6017083', false, null);

