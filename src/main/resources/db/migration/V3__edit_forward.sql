ALTER TABLE public.message
    ADD COLUMN edited BOOLEAN DEFAULT FALSE,
    ADD COLUMN edited_at TIMESTAMP,
    ADD COLUMN forwarded_from VARCHAR(80);

ALTER TABLE public.group_messages
    ADD COLUMN edited BOOLEAN DEFAULT FALSE,
    ADD COLUMN edited_at TIMESTAMP,
    ADD COLUMN forwarded_from VARCHAR(80);
