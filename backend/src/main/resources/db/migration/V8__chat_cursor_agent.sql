-- Persist Cursor Cloud Agent ID per chat session for multi-turn follow-ups

ALTER TABLE chat_session ADD cursor_agent_id VARCHAR(64) NULL;
