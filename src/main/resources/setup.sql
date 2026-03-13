-- =============================================
-- 1. РОЛИ
-- =============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'es_admin') THEN
        CREATE ROLE es_admin LOGIN PASSWORD 'admin123';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'es_guest') THEN
        CREATE ROLE es_guest LOGIN PASSWORD 'guest123';
    END IF;
END $$;
 
-- =============================================
-- 2. ТАБЛИЦЫ
-- =============================================
CREATE TABLE IF NOT EXISTS app_users (
    id         SERIAL PRIMARY KEY,
    username   VARCHAR(64) UNIQUE NOT NULL,
    pass_hash  TEXT NOT NULL,
    role       VARCHAR(16) NOT NULL DEFAULT 'guest',
    created_at TIMESTAMPTZ DEFAULT NOW()
);
 
CREATE TABLE IF NOT EXISTS matches (
    id             SERIAL PRIMARY KEY,
    match_date     DATE         NOT NULL,
    match_duration VARCHAR(16)  NOT NULL,
    tournament     VARCHAR(128) NOT NULL,
    game           VARCHAR(64)  NOT NULL,
    format         VARCHAR(16)  NOT NULL,
    team1          VARCHAR(64)  NOT NULL,
    team2          VARCHAR(64)  NOT NULL,
    winner         VARCHAR(64),
    created_at     TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT chk_teams_different CHECK (team1 <> team2),
    CONSTRAINT chk_winner CHECK (winner IS NULL OR winner = team1 OR winner = team2),
    CONSTRAINT chk_format CHECK (format IN ('Bo1','Bo2','Bo3','Bo5'))
);
 
-- =============================================
-- 3. ФУНКЦИИ И ПРОЦЕДУРЫ
-- =============================================
CREATE OR REPLACE FUNCTION fn_app_login(p_username TEXT, p_password TEXT)
RETURNS TEXT LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_role TEXT; v_hash TEXT;
BEGIN
    SELECT pass_hash, role INTO v_hash, v_role FROM app_users WHERE username = p_username;
    IF NOT FOUND THEN RAISE EXCEPTION 'Неверный логин или пароль.'; END IF;
    IF v_hash <> encode(sha256(p_password::bytea), 'hex') THEN RAISE EXCEPTION 'Неверный логин или пароль.'; END IF;
    RETURN v_role;
END; $$;
 
CREATE OR REPLACE FUNCTION fn_get_all()
RETURNS TABLE (id INT, match_date TEXT, match_duration TEXT, tournament TEXT, game TEXT, format TEXT, team1 TEXT, team2 TEXT, winner TEXT)
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    RETURN QUERY SELECT m.id, m.match_date::TEXT, m.match_duration::TEXT, m.tournament::TEXT, m.game::TEXT, m.format::TEXT, m.team1::TEXT, m.team2::TEXT, COALESCE(m.winner,'')::TEXT
    FROM matches m ORDER BY m.match_date DESC, m.id DESC;
END; $$;
 
CREATE OR REPLACE FUNCTION fn_get_by_id(p_id INT)
RETURNS TABLE (id INT, match_date TEXT, match_duration TEXT, tournament TEXT, game TEXT, format TEXT, team1 TEXT, team2 TEXT, winner TEXT)
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    RETURN QUERY SELECT m.id, m.match_date::TEXT, m.match_duration::TEXT, m.tournament::TEXT, m.game::TEXT, m.format::TEXT, m.team1::TEXT, m.team2::TEXT, COALESCE(m.winner,'')::TEXT
    FROM matches m WHERE m.id = p_id;
END; $$;
 
CREATE OR REPLACE FUNCTION fn_search_by_tournament(p_query TEXT)
RETURNS TABLE (id INT, match_date TEXT, match_duration TEXT, tournament TEXT, game TEXT, format TEXT, team1 TEXT, team2 TEXT, winner TEXT)
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    RETURN QUERY SELECT m.id, m.match_date::TEXT, m.match_duration::TEXT, m.tournament::TEXT, m.game::TEXT, m.format::TEXT, m.team1::TEXT, m.team2::TEXT, COALESCE(m.winner,'')::TEXT
    FROM matches m WHERE m.tournament ILIKE '%' || p_query || '%' ORDER BY m.match_date DESC, m.id DESC;
END; $$;
 
CREATE OR REPLACE PROCEDURE sp_add_match(p_date TEXT, p_duration TEXT, p_tourn TEXT, p_game TEXT, p_format TEXT, p_team1 TEXT, p_team2 TEXT, p_winner TEXT)
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    IF p_date IS NULL OR trim(p_date) = '' THEN RAISE EXCEPTION 'Дата обязательна.'; END IF;
    IF p_tourn IS NULL OR trim(p_tourn) = '' THEN RAISE EXCEPTION 'Название турнира обязательно.'; END IF;
    IF p_team1 IS NULL OR trim(p_team1) = '' THEN RAISE EXCEPTION 'Команда 1 обязательна.'; END IF;
    IF p_team2 IS NULL OR trim(p_team2) = '' THEN RAISE EXCEPTION 'Команда 2 обязательна.'; END IF;
    IF p_team1 = p_team2 THEN RAISE EXCEPTION 'Команды не могут совпадать.'; END IF;
    IF p_winner IS NOT NULL AND trim(p_winner) <> '' AND p_winner <> p_team1 AND p_winner <> p_team2 THEN
        RAISE EXCEPTION 'Победитель должен совпадать с одной из команд.';
    END IF;
    INSERT INTO matches (match_date, match_duration, tournament, game, format, team1, team2, winner)
    VALUES (p_date::DATE, trim(p_duration), trim(p_tourn), trim(p_game), trim(p_format), trim(p_team1), trim(p_team2),
            CASE WHEN p_winner IS NULL OR trim(p_winner) = '' THEN NULL ELSE trim(p_winner) END);
END; $$;
 
CREATE OR REPLACE PROCEDURE sp_update_match(p_id INT, p_date TEXT, p_duration TEXT, p_tourn TEXT, p_game TEXT, p_format TEXT, p_team1 TEXT, p_team2 TEXT, p_winner TEXT)
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM matches WHERE id = p_id) THEN RAISE EXCEPTION 'Запись с ID % не найдена.', p_id; END IF;
    IF p_team1 = p_team2 THEN RAISE EXCEPTION 'Команды не могут совпадать.'; END IF;
    IF p_winner IS NOT NULL AND trim(p_winner) <> '' AND p_winner <> p_team1 AND p_winner <> p_team2 THEN
        RAISE EXCEPTION 'Победитель должен совпадать с одной из команд.';
    END IF;
    UPDATE matches SET
        match_date     = p_date::DATE,
        match_duration = trim(p_duration),
        tournament     = trim(p_tourn),
        game           = trim(p_game),
        format         = trim(p_format),
        team1          = trim(p_team1),
        team2          = trim(p_team2),
        winner         = CASE WHEN p_winner IS NULL OR trim(p_winner) = '' THEN NULL ELSE trim(p_winner) END
    WHERE id = p_id;
END; $$;
 
CREATE OR REPLACE PROCEDURE sp_delete_by_id(p_id INT)
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    DELETE FROM matches WHERE id = p_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Запись с ID % не найдена.', p_id; END IF;
END; $$;
 
CREATE OR REPLACE PROCEDURE sp_delete_by_tournament(p_tournament TEXT)
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    DELETE FROM matches WHERE tournament = trim(p_tournament);
END; $$;
 
CREATE OR REPLACE PROCEDURE sp_clear_table()
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    TRUNCATE TABLE matches RESTART IDENTITY;
END; $$;
 
CREATE OR REPLACE PROCEDURE sp_app_register(p_username TEXT, p_password TEXT, p_role TEXT, p_admin_code TEXT)
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    IF length(trim(p_username)) < 3 THEN RAISE EXCEPTION 'Логин должен быть не менее 3 символов.'; END IF;
    IF length(p_password) < 4 THEN RAISE EXCEPTION 'Пароль должен быть не менее 4 символов.'; END IF;
    IF EXISTS (SELECT 1 FROM app_users WHERE username = p_username) THEN RAISE EXCEPTION 'Пользователь «%» уже существует.', p_username; END IF;
    IF p_role = 'admin' AND p_admin_code <> 'admin' THEN RAISE EXCEPTION 'Неверный код администратора.'; END IF;
    IF p_role NOT IN ('admin','guest') THEN RAISE EXCEPTION 'Недопустимая роль: %.', p_role; END IF;
    INSERT INTO app_users (username, pass_hash, role)
    VALUES (trim(p_username), encode(sha256(p_password::bytea), 'hex'), p_role);
END; $$;
 
CREATE OR REPLACE PROCEDURE sp_create_user(p_username TEXT, p_password TEXT, p_role TEXT)
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    IF length(trim(p_username)) < 3 THEN RAISE EXCEPTION 'Логин должен быть не менее 3 символов.'; END IF;
    IF length(p_password) < 4 THEN RAISE EXCEPTION 'Пароль должен быть не менее 4 символов.'; END IF;
    IF EXISTS (SELECT 1 FROM app_users WHERE username = p_username) THEN RAISE EXCEPTION 'Пользователь «%» уже существует.', p_username; END IF;
    IF p_role NOT IN ('admin','guest') THEN RAISE EXCEPTION 'Недопустимая роль: %.', p_role; END IF;
    INSERT INTO app_users (username, pass_hash, role)
    VALUES (trim(p_username), encode(sha256(p_password::bytea), 'hex'), p_role);
END; $$;
 
-- =============================================
-- 4. НАЧАЛЬНЫЕ ДАННЫЕ
-- =============================================
INSERT INTO app_users (username, pass_hash, role) VALUES
    ('admin', encode(sha256('adminpass'::bytea), 'hex'), 'admin'),
    ('guest', encode(sha256('guestpass'::bytea), 'hex'), 'guest')
ON CONFLICT (username) DO NOTHING;
 
-- =============================================
-- 5. ПРАВА (всегда последними, после создания объектов!)
-- =============================================
GRANT CONNECT ON DATABASE esports_db TO es_admin, es_guest;
GRANT USAGE ON SCHEMA public TO es_admin, es_guest;
 
-- Админ — полный доступ
GRANT ALL ON ALL TABLES    IN SCHEMA public TO es_admin;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO es_admin;
GRANT ALL ON ALL ROUTINES  IN SCHEMA public TO es_admin;
 
-- Гость — только чтение таблиц
GRANT SELECT ON matches, app_users TO es_guest;
 
-- Гость — только разрешённые функции/процедуры
GRANT EXECUTE ON FUNCTION  fn_app_login(TEXT, TEXT)             TO es_guest;
GRANT EXECUTE ON FUNCTION  fn_get_all()                         TO es_guest;
GRANT EXECUTE ON FUNCTION  fn_get_by_id(INT)                    TO es_guest;
GRANT EXECUTE ON FUNCTION  fn_search_by_tournament(TEXT)        TO es_guest;
GRANT EXECUTE ON PROCEDURE sp_app_register(TEXT,TEXT,TEXT,TEXT) TO es_guest;
-- sp_add_match, sp_update_match, sp_delete_*, sp_clear_table, sp_create_user — гостю НЕ выдаём