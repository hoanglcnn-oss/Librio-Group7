INSERT INTO resource (id, title, authors, description)
SELECT 1, 'Clean Code', 'Robert C. Martin', 'A handbook of agile software craftsmanship.'
WHERE NOT EXISTS (SELECT 1 FROM resource WHERE id = 1);

INSERT INTO physical_item (id, resource_id, status)
SELECT 101, 1, 'AVAILABLE'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 101);

INSERT INTO physical_item (id, resource_id, status)
SELECT 102, 1, 'AVAILABLE'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 102);

INSERT INTO physical_item (id, resource_id, status)
SELECT 103, 1, 'BORROWED'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 103);

INSERT INTO physical_item (id, resource_id, status)
SELECT 104, 1, 'BORROWED'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 104);

INSERT INTO physical_item (id, resource_id, status)
SELECT 105, 1, 'BORROWED'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 105);

INSERT INTO digital_item (id, resource_id)
SELECT 301, 1
WHERE NOT EXISTS (SELECT 1 FROM digital_item WHERE id = 301);

INSERT INTO resource (id, title, authors, description)
SELECT 2, 'Refactoring', 'Martin Fowler', 'Improving the design of existing code.'
WHERE NOT EXISTS (SELECT 1 FROM resource WHERE id = 2);

INSERT INTO physical_item (id, resource_id, status)
SELECT 201, 2, 'BORROWED'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 201);

INSERT INTO physical_item (id, resource_id, status)
SELECT 202, 2, 'BORROWED'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 202);

INSERT INTO physical_item (id, resource_id, status)
SELECT 203, 2, 'BORROWED'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 203);

INSERT INTO resource (id, title, authors, description)
SELECT 3, 'Designing Data-Intensive Applications', 'Martin Kleppmann', 'The big ideas behind reliable, scalable, and maintainable systems.'
WHERE NOT EXISTS (SELECT 1 FROM resource WHERE id = 3);

INSERT INTO digital_item (id, resource_id)
SELECT 302, 3
WHERE NOT EXISTS (SELECT 1 FROM digital_item WHERE id = 302);

INSERT INTO resource (id, title, authors, description)
SELECT 4, 'Structure and Interpretation of Computer Programs', 'Harold Abelson, Gerald Jay Sussman', 'SICP - Fundamental principles of computer programming.'
WHERE NOT EXISTS (SELECT 1 FROM resource WHERE id = 4);

INSERT INTO physical_item (id, resource_id, status)
SELECT 401, 4, 'AVAILABLE'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 401);

INSERT INTO physical_item (id, resource_id, status)
SELECT 402, 4, 'BORROWED'
WHERE NOT EXISTS (SELECT 1 FROM physical_item WHERE id = 402);

INSERT INTO digital_item (id, resource_id)
SELECT 303, 4
WHERE NOT EXISTS (SELECT 1 FROM digital_item WHERE id = 303);
