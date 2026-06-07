-- AI Counselling Assistant schema

CREATE TABLE chat_user (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    visitor_token   VARCHAR(64)  NOT NULL UNIQUE,
    ip_address      VARCHAR(64)  NULL,
    user_agent      VARCHAR(512) NULL,
    created_at      DATETIME2    NOT NULL CONSTRAINT DF_chat_user_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2    NOT NULL CONSTRAINT DF_chat_user_updated DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_chat_user_token ON chat_user (visitor_token);

CREATE TABLE student_profile (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    chat_user_id        BIGINT       NOT NULL UNIQUE REFERENCES chat_user (id) ON DELETE CASCADE,
    [rank]              INT          NULL,
    category            VARCHAR(16)  NULL,
    gender              VARCHAR(8)   NULL,
    preferred_branches  VARCHAR(512) NULL,
    preferred_location  VARCHAR(128) NULL,
    budget              VARCHAR(64)  NULL,
    created_at          DATETIME2    NOT NULL CONSTRAINT DF_student_profile_created DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2    NOT NULL CONSTRAINT DF_student_profile_updated DEFAULT SYSUTCDATETIME()
);

CREATE TABLE chat_session (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    chat_user_id    BIGINT       NOT NULL REFERENCES chat_user (id) ON DELETE CASCADE,
    title           VARCHAR(255) NULL,
    message_count   INT          NOT NULL CONSTRAINT DF_chat_session_msg_count DEFAULT 0,
    lead_cta_shown  BIT          NOT NULL CONSTRAINT DF_chat_session_lead_cta DEFAULT 0,
    active          BIT          NOT NULL CONSTRAINT DF_chat_session_active DEFAULT 1,
    created_at      DATETIME2    NOT NULL CONSTRAINT DF_chat_session_created DEFAULT SYSUTCDATETIME(),
    updated_at      DATETIME2    NOT NULL CONSTRAINT DF_chat_session_updated DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_chat_session_user ON chat_session (chat_user_id);
CREATE INDEX idx_chat_session_created ON chat_session (created_at DESC);

CREATE TABLE chat_message (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_id      BIGINT        NOT NULL REFERENCES chat_session (id) ON DELETE CASCADE,
    role            VARCHAR(16)   NOT NULL,
    content         NVARCHAR(MAX) NOT NULL,
    structured_json NVARCHAR(MAX) NULL,
    model           VARCHAR(64)   NULL,
    prompt_tokens   INT           NULL,
    completion_tokens INT         NULL,
    created_at      DATETIME2     NOT NULL CONSTRAINT DF_chat_message_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT chk_chat_message_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE INDEX idx_chat_message_session ON chat_message (session_id, created_at);

CREATE TABLE faq_article (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    slug        VARCHAR(128)  NOT NULL UNIQUE,
    title       VARCHAR(255)  NOT NULL,
    category    VARCHAR(64)   NOT NULL,
    content     NVARCHAR(MAX) NOT NULL,
    tags        VARCHAR(512)  NULL,
    active      BIT           NOT NULL CONSTRAINT DF_faq_active DEFAULT 1,
    created_at  DATETIME2     NOT NULL CONSTRAINT DF_faq_created DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2     NOT NULL CONSTRAINT DF_faq_updated DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_faq_category ON faq_article (category);
CREATE INDEX idx_faq_active ON faq_article (active);

CREATE TABLE chat_analytics_event (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_id  BIGINT        NULL REFERENCES chat_session (id) ON DELETE NO ACTION,
    chat_user_id BIGINT       NULL REFERENCES chat_user (id) ON DELETE NO ACTION,
    event_type  VARCHAR(64)   NOT NULL,
    metadata_json NVARCHAR(1024) NULL,
    created_at  DATETIME2     NOT NULL CONSTRAINT DF_chat_event_created DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_chat_event_type ON chat_analytics_event (event_type);
CREATE INDEX idx_chat_event_created ON chat_analytics_event (created_at DESC);
