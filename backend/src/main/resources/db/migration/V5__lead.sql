CREATE TABLE lead (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    name        VARCHAR(128) NULL,
    mobile      VARCHAR(16)  NULL,
    [rank]      INT          NOT NULL,
    category    VARCHAR(16)  NOT NULL,
    gender      VARCHAR(8)   NOT NULL,
    branch      VARCHAR(256) NOT NULL,
    created_at  DATETIME2    NOT NULL CONSTRAINT DF_lead_created DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_lead_created_at ON lead (created_at DESC);
CREATE INDEX idx_lead_category ON lead (category);
CREATE INDEX idx_lead_rank ON lead ([rank]);
