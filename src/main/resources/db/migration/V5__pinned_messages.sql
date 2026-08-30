-- ShadowVibe pinned messages (V5)
-- Single pinned message per direct conversation / group.

ALTER TABLE public.message ADD COLUMN pinned_at timestamp(6) without time zone;
ALTER TABLE public.message ADD COLUMN pinned_by character varying(255);

ALTER TABLE public.group_messages ADD COLUMN pinned_at timestamp(6) without time zone;
ALTER TABLE public.group_messages ADD COLUMN pinned_by character varying(255);
