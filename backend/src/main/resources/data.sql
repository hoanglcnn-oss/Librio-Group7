-- Seed Users
INSERT INTO user_account (id, username) VALUES (1, 'reader1');
INSERT INTO user_account (id, username) VALUES (2, 'reader2');

-- Seed Resource 1: Clean Code (Physical Available + Digital Available)
INSERT INTO resource (id, title, authors, description) VALUES 
(1, 'Clean Code', 'Robert C. Martin', 'A handbook of agile software craftsmanship.');

INSERT INTO physical_item (id, resource_id, status) VALUES 
(101, 1, 'AVAILABLE'),
(102, 1, 'AVAILABLE'),
(103, 1, 'BORROWED'),
(104, 1, 'BORROWED'),
(105, 1, 'BORROWED');

INSERT INTO digital_item (id, resource_id) VALUES (301, 1);

INSERT INTO borrowing (id, physical_item_id, user_id, borrowed_at, due_at, returned_at) VALUES
(501, 103, 1, '2026-08-01 10:00:00', '2026-08-15 10:00:00', NULL),
(502, 104, 2, '2026-08-05 14:00:00', '2026-08-19 14:00:00', NULL),
(503, 105, 1, '2026-08-10 09:00:00', '2026-08-24 09:00:00', NULL);


-- Seed Resource 2: Refactoring (Physical Only - Out of Stock)
INSERT INTO resource (id, title, authors, description) VALUES 
(2, 'Refactoring', 'Martin Fowler', 'Improving the design of existing code.');

INSERT INTO physical_item (id, resource_id, status) VALUES 
(201, 2, 'BORROWED'),
(202, 2, 'BORROWED'),
(203, 2, 'BORROWED');

INSERT INTO borrowing (id, physical_item_id, user_id, borrowed_at, due_at, returned_at) VALUES
(504, 201, 1, '2026-08-02 11:00:00', '2026-08-16 11:00:00', NULL),
(505, 202, 2, '2026-08-04 15:00:00', '2026-08-18 15:00:00', NULL),
(506, 203, 1, '2026-08-06 16:00:00', '2026-08-20 16:00:00', NULL);


-- Seed Resource 3: Designing Data-Intensive Applications (Digital Only)
INSERT INTO resource (id, title, authors, description) VALUES 
(3, 'Designing Data-Intensive Applications', 'Martin Kleppmann', 'The big ideas behind reliable, scalable, and maintainable systems.');

INSERT INTO digital_item (id, resource_id) VALUES (302, 3);


-- Seed Resource 4: Structure and Interpretation of Computer Programs (Mixed Physical & Digital)
INSERT INTO resource (id, title, authors, description) VALUES 
(4, 'Structure and Interpretation of Computer Programs', 'Harold Abelson, Gerald Jay Sussman', 'SICP - Fundamental principles of computer programming.');

INSERT INTO physical_item (id, resource_id, status) VALUES 
(401, 4, 'AVAILABLE'),
(402, 4, 'BORROWED');

INSERT INTO digital_item (id, resource_id) VALUES (303, 4);

INSERT INTO borrowing (id, physical_item_id, user_id, borrowed_at, due_at, returned_at) VALUES
(507, 402, 2, '2026-08-12 13:00:00', '2026-08-26 13:00:00', NULL);
