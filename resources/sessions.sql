-- :name find-session :query :one
SELECT s.* FROM sessions AS s
  JOIN users AS u ON (u.login=s.login)
WHERE s.id=:id;

-- :name touch-session :execute :affected
UPDATE sessions SET seen=:seen WHERE id=:id;

-- :name create-session :execute :affected
INSERT INTO sessions (id, login, seen) VALUES (:id, :login, NOW());

-- :name find-user :query :one
SELECT * FROM users WHERE login=:login;
