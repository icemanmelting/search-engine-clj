INSERT INTO users(login, password, salt) VALUES ('fabio@dias.com', 'be7075073959f02a3a60b76fe1fd621d64804b6e71ead52731e74e0f559714c2', 'salt');

INSERT INTO documents(id, owner_id, content, created_at) VALUES ('doc1', 'fabio@dias.com','I really like bananas, apples not so much', NOW());
INSERT INTO documents(id, owner_id, content, created_at) VALUES ('doc2', 'fabio@dias.com','I dont like bananas, apples so and so', NOW());
INSERT INTO documents(id, owner_id, content, created_at) VALUES ('doc3', 'fabio@dias.com','I really like oranges', NOW());

