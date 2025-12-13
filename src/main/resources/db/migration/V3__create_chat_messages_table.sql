CREATE TABLE Chat_Messages (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    role VARCHAR(10) NOT NULL CHECK (role IN ('user', 'assistant')),
    content TEXT NOT NULL,
    template_used VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat FOREIGN KEY (chat_id) REFERENCES Chats(id) ON DELETE CASCADE
);

CREATE INDEX idx_message_chat_id ON Chat_Messages(chat_id);