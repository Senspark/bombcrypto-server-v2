--
-- PostgreSQL database dump
--

-- Dumped from database version 17.6 (Debian 17.6-2.pgdg13+1)
-- Dumped by pg_dump version 17.7 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: bomberland; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA bomberland;


--
-- Name: fn_select_or_insert_new_user(character varying, character varying, character varying, character varying, character varying, character varying); Type: FUNCTION; Schema: bomberland; Owner: -
--

CREATE FUNCTION bomberland.fn_select_or_insert_new_user(_user_name character varying, _email character varying, _address character varying, _nick_name character varying, _type_account character varying, _telegram_id character varying) RETURNS TABLE(uid integer, user_name character varying, nick_name character varying, email character varying, type_account character varying, create_at timestamp with time zone, telegram_id character varying)
    LANGUAGE plpgsql
    AS $$
DECLARE
    _existed boolean := FALSE;
BEGIN

    SELECT EXISTS(SELECT 1 FROM bomberland.users WHERE bomberland.users.username = _user_name) INTO _existed;

    IF _existed THEN
        UPDATE bomberland.users
        SET last_login  = NOW(),
            telegram_id = CASE
                  WHEN bomberland.users.telegram_id IS NULL OR bomberland.users.telegram_id = '' THEN _telegram_id
                  ELSE bomberland.users.telegram_id
                END
        WHERE bomberland.users.username = _user_name
        RETURNING bomberland.users.id, bomberland.users.username, bomberland.users.nickname, bomberland.users.email, bomberland.users.type_account, bomberland.users.create_at, bomberland.users.telegram_id
            INTO uid, user_name, nick_name, email, type_account, create_at, telegram_id;
        RETURN QUERY
            SELECT uid, user_name, nick_name, email, type_account, create_at, telegram_id;
    ELSE
        INSERT INTO bomberland.users(username, email, address, nickname, create_at, update_at, last_login, type_account,
                                     telegram_id)
        VALUES (_user_name, _email, _address, _nick_name, NOW(), NOW(), NOW(), _type_account, _telegram_id)
        RETURNING bomberland.users.id, bomberland.users.username, bomberland.users.nickname, bomberland.users.email, bomberland.users.type_account, bomberland.users.create_at, bomberland.users.telegram_id
            INTO uid, user_name, nick_name, email, type_account, create_at, telegram_id;
        RETURN QUERY
            SELECT uid, user_name, nick_name, email, type_account, create_at, telegram_id;
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END ;
$$;


--
-- Name: fn_select_or_insert_new_user_sol(character varying); Type: FUNCTION; Schema: bomberland; Owner: -
--

CREATE FUNCTION bomberland.fn_select_or_insert_new_user_sol(_wallet_address character varying) RETURNS TABLE(uid integer, user_name character varying, create_at timestamp with time zone)
    LANGUAGE plpgsql
    AS $$
BEGIN

    IF EXISTS(SELECT 1 FROM bomberland.users WHERE bomberland.users.username = _wallet_address) THEN
        UPDATE bomberland.users
        SET last_login = NOW()
        WHERE bomberland.users.username = _wallet_address
        RETURNING bomberland.users.id, bomberland.users.username, bomberland.users.create_at
            INTO uid, user_name, create_at;
        RETURN QUERY
            SELECT uid, user_name, create_at;
    ELSE
        INSERT INTO bomberland.users(username, address, create_at, update_at, last_login, type_account)
        VALUES (_wallet_address, _wallet_address, NOW(), NOW(), NOW(), 'SOL')
        RETURNING bomberland.users.id, bomberland.users.username, bomberland.users.create_at
            INTO uid, user_name, create_at;
        RETURN QUERY
            SELECT uid, user_name, create_at;
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END ;
$$;


--
-- Name: fn_select_or_insert_new_user_web(character varying); Type: FUNCTION; Schema: bomberland; Owner: -
--

CREATE FUNCTION bomberland.fn_select_or_insert_new_user_web(_wallet_address character varying) RETURNS TABLE(uid integer, user_name character varying, address character varying, nick_name character varying, create_at timestamp with time zone)
    LANGUAGE plpgsql
    AS $$
BEGIN

    IF EXISTS(SELECT 1 FROM bomberland.users WHERE bomberland.users.username = _wallet_address
                                             OR  bomberland.users.address = _wallet_address) THEN
        UPDATE bomberland.users
        SET last_login = NOW()
        WHERE bomberland.users.username = _wallet_address OR bomberland.users.address = _wallet_address
        RETURNING
            bomberland.users.id,
            bomberland.users.username,
            bomberland.users.address,
            bomberland.users.nickname,
            bomberland.users.create_at

            INTO uid, user_name, address, nick_name, create_at;
        RETURN QUERY
            SELECT uid, user_name, address, nick_name, create_at;
    ELSE
        INSERT INTO bomberland.users(username, address, create_at, update_at, last_login, type_account)
        VALUES (_wallet_address, _wallet_address, NOW(), NOW(), NOW(), 'FI')
        RETURNING
            bomberland.users.id,
            bomberland.users.username,
            bomberland.users.address,
            bomberland.users.nickname,
            bomberland.users.create_at

            INTO uid, user_name, address, nick_name, create_at;
        RETURN QUERY
            SELECT uid, user_name, address, nick_name, create_at;
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END;
$$;


--
-- Name: fn_select_user_account_web(character varying, character varying); Type: FUNCTION; Schema: bomberland; Owner: -
--

CREATE FUNCTION bomberland.fn_select_user_account_web(_username character varying, _password character varying) RETURNS TABLE(uid integer, user_name character varying, address character varying, nickname character varying, create_at timestamp with time zone, type_account character varying)
    LANGUAGE plpgsql
    AS $$
BEGIN

    IF EXISTS(SELECT 1 FROM bomberland.users WHERE bomberland.users.username = _username AND bomberland.users.password = _password) THEN
        UPDATE bomberland.users
        SET last_login = NOW()
        WHERE bomberland.users.username = _username AND bomberland.users.password = _password
        RETURNING
            bomberland.users.id,
            bomberland.users.username,
            bomberland.users.address,
            bomberland.users.nickname,
            bomberland.users.create_at,
            bomberland.users.type_account
            INTO uid, user_name, address, nickname, create_at, type_account;
        RETURN QUERY
            SELECT uid, user_name, address, nickname, create_at, type_account;
    ELSE
        RAISE EXCEPTION 'User not found';
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END;
$$;


--
-- Name: fn_select_user_account_web_no_password(character varying); Type: FUNCTION; Schema: bomberland; Owner: -
--

CREATE FUNCTION bomberland.fn_select_user_account_web_no_password(_username character varying) RETURNS TABLE(uid integer, user_name character varying, address character varying, nickname character varying, create_at timestamp with time zone, type_account character varying)
    LANGUAGE plpgsql
    AS $$
BEGIN

    IF EXISTS(SELECT 1 FROM bomberland.users WHERE bomberland.users.username = _username) THEN
        UPDATE bomberland.users
        SET last_login = NOW()
        WHERE bomberland.users.username = _username
        RETURNING
            bomberland.users.id,
            bomberland.users.username,
            bomberland.users.address,
            bomberland.users.nickname,
            bomberland.users.create_at,
            bomberland.users.type_account
            INTO uid, user_name, address, nickname, create_at, type_account;
        RETURN QUERY
            SELECT uid, user_name, address, nickname, create_at, type_account;
    ELSE
        RAISE EXCEPTION 'User not found';
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION '%,%',SQLERRM,SQLSTATE;
END ;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: users; Type: TABLE; Schema: bomberland; Owner: -
--

CREATE TABLE bomberland.users (
    id integer NOT NULL,
    username character varying(255),
    email text,
    address character varying(255),
    nickname text,
    password text,
    is_deleted boolean DEFAULT false NOT NULL,
    create_at timestamp without time zone NOT NULL,
    update_at timestamp without time zone NOT NULL,
    passcode text,
    avatar character varying(10),
    last_login timestamp without time zone,
    type_account character varying(255),
    telegram_id character varying(20)
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: bomberland; Owner: -
--

CREATE SEQUENCE bomberland.users_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: bomberland; Owner: -
--

ALTER SEQUENCE bomberland.users_id_seq OWNED BY bomberland.users.id;


--
-- Name: users id; Type: DEFAULT; Schema: bomberland; Owner: -
--

ALTER TABLE ONLY bomberland.users ALTER COLUMN id SET DEFAULT nextval('bomberland.users_id_seq'::regclass);


--
-- Name: users PK_a3ffb1c0c8416b9fc6f907b7433; Type: CONSTRAINT; Schema: bomberland; Owner: -
--

ALTER TABLE ONLY bomberland.users
    ADD CONSTRAINT "PK_a3ffb1c0c8416b9fc6f907b7433" PRIMARY KEY (id);


--
-- Name: users UQ_97672ac88f789774dd47f7c8be3; Type: CONSTRAINT; Schema: bomberland; Owner: -
--

ALTER TABLE ONLY bomberland.users
    ADD CONSTRAINT "UQ_97672ac88f789774dd47f7c8be3" UNIQUE (email);


--
-- Name: users UQ_b0ec0293d53a1385955f9834d5c; Type: CONSTRAINT; Schema: bomberland; Owner: -
--

ALTER TABLE ONLY bomberland.users
    ADD CONSTRAINT "UQ_b0ec0293d53a1385955f9834d5c" UNIQUE (address);


--
-- Name: users UQ_fe0bb3f6520ee0469504521e710; Type: CONSTRAINT; Schema: bomberland; Owner: -
--

ALTER TABLE ONLY bomberland.users
    ADD CONSTRAINT "UQ_fe0bb3f6520ee0469504521e710" UNIQUE (username);


--
-- Name: users_telegram_id_index; Type: INDEX; Schema: bomberland; Owner: -
--

CREATE INDEX users_telegram_id_index ON bomberland.users USING btree (telegram_id);


--
-- PostgreSQL database dump complete
--
