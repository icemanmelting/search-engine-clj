--:name get-document-by-id :query :one
SELECT * FROM documents WHERE id=:id AND owner_id=:owner;

--:name get-all-documents-by-user :query :many
SELECT * FROM documents WHERE owner_id=:user;

--:name insert-document :execute :affected
INSERT INTO documents (id, owner_id, content, created_at)
VALUES (:id, :owner, :content, NOW())
ON CONFLICT (id, owner_id) DO UPDATE SET
  content=:content,
  updated_at=NOW();

--:name delete-document-by-id :execute :affected
DELETE FROM documents WHERE id=:id AND owner_id=:owner;
