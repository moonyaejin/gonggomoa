CREATE TABLE institution (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(255) NOT NULL,
    inst_type      VARCHAR(30)  NOT NULL,
    inst_clsf      VARCHAR(100),
    homepage_url   VARCHAR(500),
    external_code  VARCHAR(50)  NOT NULL,
    synced_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_institution_external_code (external_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recruitment (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    institution_id            BIGINT        NOT NULL,
    title                     VARCHAR(500)  NOT NULL,
    posted_at                 DATETIME,
    apply_start_at            DATE,
    apply_end_at              DATE,
    written_exam_at           DATE,
    source_url                VARCHAR(1000) NOT NULL,
    external_id               VARCHAR(100)  NOT NULL,
    content_hash              VARCHAR(64)   NOT NULL,
    employment_type           VARCHAR(30)   NOT NULL,
    screening_procedure_text  LONGTEXT,
    source_type               VARCHAR(30)   NOT NULL,
    status                    VARCHAR(30)   NOT NULL,
    collected_at              DATETIME      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recruitment_external_id (external_id),
    UNIQUE KEY uk_recruitment_content_hash (content_hash),
    KEY idx_recruitment_apply_end_status (apply_end_at, status),
    KEY idx_recruitment_apply_range (apply_start_at, apply_end_at),
    KEY idx_recruitment_source_collected (source_type, collected_at),
    CONSTRAINT fk_recruitment_institution FOREIGN KEY (institution_id) REFERENCES institution (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attachment (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    recruitment_id  BIGINT        NOT NULL,
    file_name       VARCHAR(255)  NOT NULL,
    file_url        VARCHAR(1000) NOT NULL,
    attachment_type VARCHAR(30)   NOT NULL,
    mime_type       VARCHAR(100),
    extracted_text  LONGTEXT,
    extract_status  VARCHAR(30)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_attachment_recruitment (recruitment_id),
    CONSTRAINT fk_attachment_recruitment FOREIGN KEY (recruitment_id) REFERENCES recruitment (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE position (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    recruitment_id  BIGINT       NOT NULL,
    job_category    VARCHAR(30)  NOT NULL,
    headcount       INT,
    work_region     VARCHAR(255),
    PRIMARY KEY (id),
    KEY idx_position_job_category (job_category, recruitment_id),
    CONSTRAINT fk_position_recruitment FOREIGN KEY (recruitment_id) REFERENCES recruitment (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exam_plan (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    position_id           BIGINT       NOT NULL,
    has_written_exam      BOOLEAN,
    written_exam_date     DATE,
    exam_type             VARCHAR(30),
    major_subjects        VARCHAR(500),
    raw_position_name     VARCHAR(255),
    total_questions       INT,
    time_limit_minutes    INT,
    ncs_areas_confirmed   BOOLEAN      NOT NULL DEFAULT FALSE,
    verify_status         VARCHAR(30)  NOT NULL,
    confidence            DECIMAL(3, 2),
    verified_at           DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exam_plan_position (position_id),
    KEY idx_exam_plan_type_status (exam_type, verify_status),
    CONSTRAINT fk_exam_plan_position FOREIGN KEY (position_id) REFERENCES position (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ncs_area_item (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    exam_plan_id   BIGINT      NOT NULL,
    ncs_area       VARCHAR(30) NOT NULL,
    question_count INT,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ncs_area_item_plan_area (exam_plan_id, ncs_area),
    KEY idx_ncs_area_item_area (ncs_area, exam_plan_id),
    CONSTRAINT fk_ncs_area_item_exam_plan FOREIGN KEY (exam_plan_id) REFERENCES exam_plan (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE extraction_log (
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    recruitment_id         BIGINT      NOT NULL,
    attempted_at           DATETIME    NOT NULL,
    duration_ms            INT,
    confidence             DECIMAL(3, 2),
    auto_approved          BOOLEAN     NOT NULL DEFAULT FALSE,
    corrected_field_count  INT,
    model_id               VARCHAR(100),
    failure_reason         VARCHAR(500),
    PRIMARY KEY (id),
    KEY idx_extraction_log_attempted_at (attempted_at),
    CONSTRAINT fk_extraction_log_recruitment FOREIGN KEY (recruitment_id) REFERENCES recruitment (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE collection_batch_log (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    started_at     DATETIME NOT NULL,
    duration_ms    INT,
    fetched_count  INT,
    new_count      INT,
    updated_count  INT,
    success        BOOLEAN  NOT NULL DEFAULT FALSE,
    error_message  VARCHAR(1000),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE click_event (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    recruitment_id BIGINT      NOT NULL,
    event_type     VARCHAR(30) NOT NULL,
    occurred_on    DATE        NOT NULL,
    count          INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_click_event_recruitment_type_day (recruitment_id, event_type, occurred_on),
    CONSTRAINT fk_click_event_recruitment FOREIGN KEY (recruitment_id) REFERENCES recruitment (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
