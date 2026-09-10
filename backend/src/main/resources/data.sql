INSERT INTO resource (id, title, authors, description, metadata_source)
SELECT 1, 'Clean Code', 'Robert C. Martin', 'A handbook of agile software craftsmanship.', 'MANUAL'
WHERE NOT EXISTS (SELECT 1 FROM resource WHERE id = 1);

INSERT INTO physical_item (id, resource_id, barcode, location, inventory_status, circulation_status)
SELECT 101, 1, 'LIB-101', 'UNASSIGNED', 'ACTIVE', 'AVAILABLE'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 101);

INSERT INTO physical_item (id, resource_id, barcode, location, inventory_status, circulation_status)
SELECT 102, 1, 'LIB-102', 'UNASSIGNED', 'ACTIVE', 'AVAILABLE'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 102);

INSERT INTO physical_item (id, resource_id, barcode, location, inventory_status, circulation_status)
SELECT 103, 1, 'LIB-103', 'UNASSIGNED', 'ACTIVE', 'BORROWED'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 103);

INSERT INTO physical_item (id, resource_id, barcode, location, inventory_status, circulation_status)
SELECT 104, 1, 'LIB-104', 'UNASSIGNED', 'ACTIVE', 'BORROWED'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 104);

INSERT INTO physical_item (id, resource_id, barcode, location, inventory_status, circulation_status)
SELECT 105, 1, 'LIB-105', 'UNASSIGNED', 'ACTIVE', 'BORROWED'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 105);

INSERT INTO digital_item (id, resource_id)
SELECT 301, 1
WHERE NOT EXISTS (SELECT 1 FROM digital_item WHERE id = 301);

INSERT INTO resource (id, title, authors, description, metadata_source)
SELECT 2, 'Refactoring', 'Martin Fowler', 'Improving the design of existing code.', 'MANUAL'
WHERE NOT EXISTS (SELECT 1 FROM resource WHERE id = 2);

INSERT INTO physical_item (id, resource_id, barcode, location, inventory_status, circulation_status)
SELECT 201, 2, 'LIB-201', 'UNASSIGNED', 'ACTIVE', 'BORROWED'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 201);

INSERT INTO physical_item (id, resource_id, barcode, location, inventory_status, circulation_status)
SELECT 202, 2, 'LIB-202', 'UNASSIGNED', 'ACTIVE', 'BORROWED'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 202);

INSERT INTO physical_item (id, resource_id, barcode, location, inventory_status, circulation_status)
SELECT 203, 2, 'LIB-203', 'UNASSIGNED', 'ACTIVE', 'BORROWED'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 203);

INSERT INTO resource (id, title, authors, description, metadata_source)
SELECT 3, 'Designing Data-Intensive Applications', 'Martin Kleppmann', 'The big ideas behind reliable, scalable, and maintainable systems.', 'MANUAL'
WHERE NOT EXISTS (SELECT 1 FROM resource WHERE id = 3);

INSERT INTO digital_item (id, resource_id)
SELECT 302, 3
WHERE NOT EXISTS (SELECT 1 FROM digital_item WHERE id = 302);

INSERT INTO resource (id, title, authors, description, metadata_source)
SELECT 4, 'Structure and Interpretation of Computer Programs', 'Harold Abelson, Gerald Jay Sussman', 'SICP - Fundamental principles of computer programming.', 'MANUAL'
WHERE NOT EXISTS (SELECT 1 FROM resource WHERE id = 4);

INSERT INTO physical_item (id, resource_id, barcode, location, inventory_status, circulation_status)
SELECT 401, 4, 'LIB-401', 'UNASSIGNED', 'ACTIVE', 'AVAILABLE'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 401);

INSERT INTO physical_item (id, resource_id, barcode, location, inventory_status, circulation_status)
SELECT 402, 4, 'LIB-402', 'UNASSIGNED', 'ACTIVE', 'BORROWED'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 402);

INSERT INTO digital_item (id, resource_id)
SELECT 303, 4
WHERE NOT EXISTS (SELECT 1 FROM digital_item WHERE id = 303);
-- Seed Membership Plans
INSERT INTO membership_plan (code, name, duration_months, price_amount, currency, monthly_borrow_quota, active) 
SELECT 'MONTHLY', 'Monthly Plan', 1, 10.00, 'USD', 5, true
WHERE NOT EXISTS (SELECT 1 FROM membership_plan WHERE code = 'MONTHLY');

INSERT INTO membership_plan (code, name, duration_months, price_amount, currency, monthly_borrow_quota, active) 
SELECT 'YEARLY', 'Yearly Plan', 12, 100.00, 'USD', 50, true
WHERE NOT EXISTS (SELECT 1 FROM membership_plan WHERE code = 'YEARLY');
