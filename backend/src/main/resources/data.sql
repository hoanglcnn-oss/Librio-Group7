
INSERT INTO resource (id, title, authors, description)
VALUES (
           1,
           'Clean Code',
           'Robert C. Martin',
           'A handbook of agile software craftsmanship.'
       );

INSERT INTO physical_item (id, resource_id, status)
VALUES
    (101, 1, 'AVAILABLE'),
    (102, 1, 'AVAILABLE'),
    (103, 1, 'BORROWED'),
    (104, 1, 'BORROWED'),
    (105, 1, 'BORROWED');

INSERT INTO digital_item (id, resource_id)
VALUES (301, 1);


INSERT INTO resource (id, title, authors, description)
VALUES (
           2,
           'Refactoring',
           'Martin Fowler',
           'Improving the design of existing code.'
       );

INSERT INTO physical_item (id, resource_id, status)
VALUES
    (201, 2, 'BORROWED'),
    (202, 2, 'BORROWED'),
    (203, 2, 'BORROWED');


-- Resource 3: Designing Data-Intensive Applications
-- Digital Only
INSERT INTO resource (id, title, authors, description)
VALUES (
           3,
           'Designing Data-Intensive Applications',
           'Martin Kleppmann',
           'The big ideas behind reliable, scalable, and maintainable systems.'
       );

INSERT INTO digital_item (id, resource_id)
VALUES (302, 3);


-- Resource 4: Structure and Interpretation of Computer Programs
-- Physical + Digital
INSERT INTO resource (id, title, authors, description)
VALUES (
           4,
           'Structure and Interpretation of Computer Programs',
           'Harold Abelson, Gerald Jay Sussman',
           'SICP - Fundamental principles of computer programming.'
       );

INSERT INTO physical_item (id, resource_id, status)
VALUES
    (401, 4, 'AVAILABLE'),
    (402, 4, 'BORROWED');

INSERT INTO digital_item (id, resource_id)
VALUES (303, 4);