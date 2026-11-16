-- Shell « Aide & guides » › Gérer les questions (daf360-portal, entity PortalHelpFaq).
-- Database DAF360_HR. Idempotent. In local, `ddl-auto: update` creates it on startup too.
IF OBJECT_ID('dbo.portal_help_faq', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.portal_help_faq (
        id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        page_key     NVARCHAR(80)   NOT NULL,
        question_fr  NVARCHAR(300)  NOT NULL,
        answer_fr    NVARCHAR(2000) NOT NULL,
        question_en  NVARCHAR(300)  NULL,
        answer_en    NVARCHAR(2000) NULL,
        question_ar  NVARCHAR(300)  NULL,
        answer_ar    NVARCHAR(2000) NULL,
        order_index  INT            NOT NULL CONSTRAINT DF_portal_help_faq_order DEFAULT 0,
        is_active    BIT            NOT NULL CONSTRAINT DF_portal_help_faq_active DEFAULT 1,
        created_at   DATETIMEOFFSET(6) NOT NULL,
        created_by   BIGINT NULL,
        updated_at   DATETIMEOFFSET(6) NULL,
        updated_by   BIGINT NULL
    );
    CREATE INDEX IX_portal_help_faq_page ON dbo.portal_help_faq (page_key, order_index);
END
