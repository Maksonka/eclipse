-- Premium themes (AURORA/SUNSET) removed: existing values fall back to DARK
UPDATE users SET theme_preference = 'DARK' WHERE theme_preference IN ('AURORA', 'SUNSET');