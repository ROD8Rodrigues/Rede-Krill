INSERT INTO palletization_type (id, code, name, capacity_multiplier) VALUES
    (1, 'PALETIZADO', 'Paletizado', 1),
    (2, 'NAO_PALETIZADO', 'Não Paletizado', 2);

SELECT setval('palletization_type_id_seq', (SELECT MAX(id) FROM palletization_type));

INSERT INTO time_slot (id, start_time, end_time) VALUES
    (1, TIME '08:00', TIME '10:00'),
    (2, TIME '10:00', TIME '12:00'),
    (3, TIME '13:00', TIME '15:00'),
    (4, TIME '15:00', TIME '17:00');

SELECT setval('time_slot_id_seq', (SELECT MAX(id) FROM time_slot));

INSERT INTO truck_type (id, name, description, created_at, updated_at) VALUES
    (1, 'Carreta', 'Carreta padrão', NOW(), NOW()),
    (2, 'Truck', 'Caminhão truck', NOW(), NOW()),
    (3, 'VUC', 'Veículo urbano de carga', NOW(), NOW());

SELECT setval('truck_type_id_seq', (SELECT MAX(id) FROM truck_type));
