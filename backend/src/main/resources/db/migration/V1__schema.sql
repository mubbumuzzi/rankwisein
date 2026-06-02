-- ============================================================
-- RankWise schema (SQL Server)
-- ============================================================

CREATE TABLE college (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    location    VARCHAR(255) NULL,
    district    VARCHAR(128) NULL,
    autonomous  BIT          NOT NULL CONSTRAINT DF_college_autonomous DEFAULT 0,
    website     VARCHAR(255) NULL,
    created_at  DATETIME2    NOT NULL CONSTRAINT DF_college_created DEFAULT SYSUTCDATETIME()
);

CREATE TABLE branch (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    created_at  DATETIME2    NOT NULL CONSTRAINT DF_branch_created DEFAULT SYSUTCDATETIME()
);

CREATE TABLE college_branch (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    college_id  BIGINT NOT NULL REFERENCES college (id) ON DELETE CASCADE,
    branch_id   BIGINT NOT NULL REFERENCES branch (id) ON DELETE CASCADE,
    intake      INT NULL,
    created_at  DATETIME2 NOT NULL CONSTRAINT DF_college_branch_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_college_branch UNIQUE (college_id, branch_id)
);

CREATE TABLE cutoff (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    [year]        INT         NOT NULL,
    phase         VARCHAR(32) NOT NULL,
    college_id    BIGINT      NOT NULL REFERENCES college (id) ON DELETE CASCADE,
    branch_id     BIGINT      NOT NULL REFERENCES branch (id) ON DELETE CASCADE,
    category      VARCHAR(16) NOT NULL,
    gender        VARCHAR(8)  NOT NULL,
    closing_rank  INT         NOT NULL,
    created_at    DATETIME2   NOT NULL CONSTRAINT DF_cutoff_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_cutoff UNIQUE ([year], phase, college_id, branch_id, category, gender),
    CONSTRAINT chk_closing_rank CHECK (closing_rank > 0)
);

CREATE INDEX idx_cutoff_category     ON cutoff (category);
CREATE INDEX idx_cutoff_gender       ON cutoff (gender);
CREATE INDEX idx_cutoff_branch_id    ON cutoff (branch_id);
CREATE INDEX idx_cutoff_closing_rank ON cutoff (closing_rank);
CREATE INDEX idx_cutoff_year         ON cutoff ([year]);
CREATE INDEX idx_cutoff_college_id   ON cutoff (college_id);
CREATE INDEX idx_cutoff_predict      ON cutoff (category, gender, closing_rank);

CREATE TABLE student_search (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    [rank]              INT         NOT NULL,
    category            VARCHAR(16) NOT NULL,
    gender              VARCHAR(8)  NOT NULL,
    preferred_branches  VARCHAR(512) NULL,
    ip_address          VARCHAR(64) NULL,
    created_at          DATETIME2   NOT NULL CONSTRAINT DF_student_search_created DEFAULT SYSUTCDATETIME()
);

CREATE TABLE admin_user (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    email       VARCHAR(255) NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(32)  NOT NULL CONSTRAINT DF_admin_role DEFAULT 'ADMIN',
    created_at  DATETIME2    NOT NULL CONSTRAINT DF_admin_created DEFAULT SYSUTCDATETIME()
);

CREATE TABLE import_file (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    file_name         VARCHAR(255) NOT NULL,
    file_path         VARCHAR(512) NULL,
    [year]            INT          NOT NULL,
    phase             VARCHAR(32)  NOT NULL,
    status            VARCHAR(32)  NOT NULL,
    records_imported  INT          NOT NULL CONSTRAINT DF_import_records DEFAULT 0,
    import_duration   BIGINT NULL,
    uploaded_at       DATETIME2    NOT NULL CONSTRAINT DF_import_uploaded DEFAULT SYSUTCDATETIME()
);

CREATE TABLE import_log (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    import_file_id  BIGINT NOT NULL REFERENCES import_file (id) ON DELETE CASCADE,
    message         NVARCHAR(MAX) NULL,
    created_at      DATETIME2 NOT NULL CONSTRAINT DF_import_log_created DEFAULT SYSUTCDATETIME()
);

CREATE TABLE import_staging_row (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    import_file_id  BIGINT NOT NULL REFERENCES import_file (id) ON DELETE CASCADE,
    college_code    VARCHAR(32) NULL,
    college_name    VARCHAR(255) NULL,
    branch_code     VARCHAR(32) NULL,
    branch_name     VARCHAR(255) NULL,
    category        VARCHAR(16) NULL,
    gender          VARCHAR(8) NULL,
    closing_rank    INT NULL,
    valid           BIT NOT NULL CONSTRAINT DF_staging_valid DEFAULT 1,
    is_duplicate    BIT NOT NULL CONSTRAINT DF_staging_dup DEFAULT 0,
    error_message   VARCHAR(512) NULL,
    created_at      DATETIME2 NOT NULL CONSTRAINT DF_staging_created DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_staging_import_file ON import_staging_row (import_file_id);
