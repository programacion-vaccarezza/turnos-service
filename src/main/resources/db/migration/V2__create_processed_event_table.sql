CREATE TABLE processed_event (
    event_id      VARCHAR(100)             PRIMARY KEY,
    event_type    VARCHAR(60)              NOT NULL,
    processed_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
