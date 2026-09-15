-- Tin nhắn trỏ tới conversation không tồn tại (trước đây service không kiểm tra) phải xoá trước khi thêm FK
DELETE FROM chat_messages m
WHERE NOT EXISTS (SELECT 1 FROM conversations c WHERE c.id = m.conversation_id);

ALTER TABLE chat_messages
    ADD CONSTRAINT fk_chat_messages_conversation
    FOREIGN KEY (conversation_id) REFERENCES conversations (id);
