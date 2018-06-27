DROP TABLE IF EXISTS users CASCADE;
CREATE TABLE users (
  login VARCHAR(128),
  password CHAR(64),
  salt VARCHAR(64),

  PRIMARY KEY (login)
);

DROP TABLE IF EXISTS sessions CASCADE;
CREATE TABLE sessions (
  id UUID,
  login VARCHAR(128) NOT NULL,
  seen TIMESTAMP NOT NULL,

  PRIMARY KEY (id),
  FOREIGN KEY (login) REFERENCES users (login)
);

DROP TABLE IF EXISTS documents CASCADE;
CREATE TABLE documents (
  id VARCHAR,
  owner_id VARCHAR,
  content VARCHAR,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,

  PRIMARY KEY (id, owner_id),
  FOREIGN KEY (owner_id) REFERENCES users (login)
);
