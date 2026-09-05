-- Remove seeded bot users and their direct messages (V20)

DELETE FROM public.message
WHERE sender_id IN (SELECT id FROM public.users WHERE email LIKE '%@shadowvibe.bot')
   OR receiver_id IN (SELECT id FROM public.users WHERE email LIKE '%@shadowvibe.bot');

DELETE FROM public.users
WHERE email LIKE '%@shadowvibe.bot';