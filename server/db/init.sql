--
-- PostgreSQL database dump
--

\restrict 9VBpFg0Y70wGCAPokc0VyIE0PU5jpFfBJPA7QNom1zcWnMeSRr6cXys4bZ1RtST

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
-- Data for Name: bombcrypto_hero_traditional_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.bombcrypto_hero_traditional_config (item_id, skin, color, name, speed, range, bomb, max_speed, max_range, max_bomb) FROM stdin;
8	18	2	Frog	3	1	2	8	6	7
9	1	3	Knight	2	3	1	7	8	6
10	2	3	Man	1	2	3	6	7	8
11	3	3	Vampire	1	1	4	6	6	9
12	4	3	Witch	1	4	1	6	9	6
13	5	1	Doge	2	2	2	7	7	7
14	6	1	Pepe	3	2	1	8	7	6
15	7	1	Ninja	1	3	2	6	8	7
16	8	1	King	4	1	1	9	6	6
17	9	1	Rabbit	2	3	2	7	8	7
\.


--
-- Data for Name: config_adventure_mode_entity_creator; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_adventure_mode_entity_creator (id, entity_id, health, damage, speed_move, follow, through, gold_reward_first_time, gold_reward_other_time, range) FROM stdin;
1	1	1	1	1	0	0	0	0	0
2	2	1	1	1	0	0	0	0	0
3	3	1	1	1	0	0	0	0	0
5	5	2	1	1	1	0	0	0	0
6	6	2	1	1	0	1	0	0	0
7	7	2	1	2	1	0	0	0	0
9	9	3	2	2	1	0	0	0	0
10	10	3	2	2	1	0	0	0	0
11	11	3	2	2	1	0	0	0	0
13	13	3	2	3	0	0	0	0	0
14	14	3	2	2	1	1	0	0	0
15	15	3	2	3	1	0	0	0	0
17	17	4	3	3	0	0	0	0	0
18	18	4	3	3	2	0	0	0	0
19	19	4	3	3	1	1	0	0	0
21	21	1	3	2	0	0	0	0	0
22	22	1	5	2	1	0	0	0	0
23	23	2	5	2	1	1	0	0	0
24	24	35	8	2	1	0	100	30	0
25	25	1	3	2	0	0	0	0	0
26	26	1	5	2	0	0	0	0	0
27	27	2	5	2	1	0	0	0	0
28	28	35	8	2	1	0	100	30	0
29	29	1	3	2	0	0	0	0	0
30	30	1	5	2	0	0	0	0	0
31	31	2	5	2	1	0	0	0	0
32	32	35	8	2	1	0	100	30	0
33	33	1	2	2	1	0	0	0	0
34	34	1	2	3	1	0	0	0	0
35	35	1	5	2	0	0	0	0	0
36	36	2	5	2	0	0	100	30	0
37	37	35	8	2	1	0	0	0	0
38	38	1	3	2	1	1	0	0	0
39	39	3	3	3	1	0	0	0	0
40	40	30	1	3	1	1	100	30	0
4	4	20	1	1	0	1	100	20	2
8	8	22	2	2	1	0	125	20	2
12	12	25	2	2	1	0	150	20	2
16	16	27	2	1	1	0	175	30	3
20	20	34	2	3	1	0	200	30	3
\.


--
-- Data for Name: config_adventure_mode_items; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_adventure_mode_items (id, type, drop_rate, reward_min, reward_max, reward_type) FROM stdin;
1	SkullHead	714	1	1	\N
2	BombUp	714	1	1	\N
3	Boots	714	1	1	\N
4	Chest	250	1	1	BRONZE_CHEST
5	FireUp	714	1	1	\N
6	GoldX1	714	1	3	GOLD
7	GoldX5	714	4	6	GOLD
8	Shield	714	1	1	\N
9	SilverChest	175	1	1	SILVER_CHEST
10	GoldChest	50	1	1	GOLD_CHEST
11	PlatinumChest	25	1	1	PLATINUM_CHEST
\.


--
-- Data for Name: config_adventure_mode_level_strategy; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_adventure_mode_level_strategy (id, stage, level, enemies, enemies_num, "row", col, row_v1, col_v1, density, enemies_door, enemies_door_first_num, enemies_door_then_num, blocks, player_spawn, door, enemies_v2, is_free_revive_hero, max_gold_reward) FROM stdin;
34	7	4	[22,23,25,26,27]	[2,2,5,5,4]	13	29	13	29	0.3	[22,23,25,26,27]	4	8	\N	\N	\N	\N	f	20
5	1	5	[4]	[1]	9	13	9	13	0.5	[1,2,3]	2	3	\N	\N	\N	\N	f	20
44	9	4	[30,31,33,34,35]	[3,4,5,5,5]	15	31	15	31	0.3	[30,31,33,34,35]	4	8	\N	\N	\N	\N	f	20
49	10	4	[34,35,37,38,39]	[3,4,5,5,5]	15	29	15	29	0.3	[34,35,37,38,39]	4	8	\N	\N	\N	\N	f	20
13	3	3	[6,7,9,10]	[1,2,2,2]	9	21	9	21	0.4	[6,7,9,10,11]	2	2	\N	\N	\N	\N	f	20
16	4	1	[10,11,13]	[2,2,2]	9	23	9	23	0.4	[10,11,13,14,15]	4	8	\N	\N	\N	\N	f	20
36	8	1	[26,27,29]	[5,5,4]	15	31	15	31	0.5	[26,27,29,30,31]	4	8	\N	\N	\N	\N	f	20
46	10	1	[34,35,37]	[7,6,5]	15	29	15	29	0.5	[34,35,37,38,39]	4	8	\N	\N	\N	\N	f	20
9	2	4	[5,6,7]	[2,2,2]	7	19	7	19	0.5	[2,3,5,6,7]	2	2	\N	\N	\N	\N	f	20
40	8	5	[32]	[1]	11	31	11	31	0.2	[26,27,29,30,31]	4	8	\N	\N	\N	\N	f	20
45	9	5	[36]	[1]	11	31	11	31	0.2	[30,31,33,34,35]	4	8	\N	\N	\N	\N	f	20
30	6	5	[24]	[1]	11	27	11	27	0.2	[18,19,21,22,23]	4	8	\N	\N	\N	\N	f	20
28	6	3	[18,19,21,22]	[1,4,5,2]	11	27	11	27	0.3	[18,19,21,22,23]	4	8	\N	\N	\N	\N	f	20
19	4	4	[13,14,15]	[3,3,3]	9	23	9	23	0.4	[10,11,13,14,15]	4	8	\N	\N	\N	\N	f	20
39	8	4	[26,27,29,30,31]	[2,3,4,6,5]	15	31	15	31	0.3	[26,27,29,30,31]	4	8	\N	\N	\N	\N	f	20
24	5	4	[14,15,17,18,19]	[1,2,4,3,2]	11	25	11	25	0.4	[14,15,17,18,19]	4	8	\N	\N	\N	\N	f	20
27	6	2	[18,19,21,22]	[2,3,5,1]	11	27	11	27	0.4	[18,19,21,22,23]	4	8	\N	\N	\N	\N	f	20
32	7	2	[22,23,25,26]	[2,3,6,3]	13	29	13	29	0.4	[22,23,25,26,27]	4	8	\N	\N	\N	\N	f	20
42	9	2	[30,31,33,34]	[5,5,5,5]	15	31	15	31	0.4	[30,31,33,34,35]	4	8	\N	\N	\N	\N	f	20
7	2	2	[3,5,6]	[2,1,1]	7	19	7	19	0.5	[2,3,5,6,7]	1	1	\N	\N	\N	\N	f	20
20	4	5	[16]	[1]	11	27	11	27	0.35	[10,11,13,14,15]	4	8	\N	\N	\N	\N	f	20
21	5	1	[14,15,17]	[2,3,2]	11	25	11	25	0.4	[14,15,17,18,19]	4	8	\N	\N	\N	\N	f	20
31	7	1	[22,23,25]	[4,4,4]	13	29	13	29	0.5	[22,23,25,26,27]	4	8	\N	\N	\N	\N	f	20
47	10	2	[34,35,37,38]	[5,5,5,5]	15	29	15	29	0.4	[34,35,37,38,39]	4	8	\N	\N	\N	\N	f	20
48	10	3	[34,35,37,38]	[5,5,6,6]	15	29	15	29	0.3	[34,35,37,38,39]	4	8	\N	\N	\N	\N	f	20
6	2	1	[2,3,5]	[1,2,1]	7	19	7	19	0.5	[1,2,3]	0	0	\N	\N	\N	\N	f	20
33	7	3	[22,23,25,26,27]	[2,2,5,5,2]	13	29	13	29	0.3	[22,23,25,26,27]	4	8	\N	\N	\N	\N	f	20
50	10	5	[40]	[1]	11	29	11	29	0.2	[34,35,37,38,39]	4	8	\N	\N	\N	\N	f	20
15	3	5	[12]	[1]	11	25	11	25	0.35	[6,7,9,10,11]	4	4	\N	\N	\N	\N	f	20
29	6	4	[18,19,21,22,23]	[1,2,5,3,2]	11	27	11	27	0.3	[18,19,21,22,23]	4	8	\N	\N	\N	\N	f	20
23	5	3	[14,15,17,18]	[2,2,3,2]	11	25	11	25	0.4	[14,15,17,18,19]	4	8	\N	\N	\N	\N	f	20
1	1	1	[1]	[3]	5	9	5	9	0.5	[1,2,3]	0	0	\N	\N	\N	\N	f	20
4	1	4	[1,2,3]	[2,2,1]	5	9	5	9	0.5	[1,2,3]	2	2	\N	\N	\N	\N	f	20
25	5	5	[20]	[1]	11	13	11	13	0.35	[14,15,17,18,19]	4	8	\N	\N	\N	\N	f	20
17	4	2	[10,11,13,14]	[1,2,3,1]	9	23	9	23	0.4	[10,11,13,14,15]	4	8	\N	\N	\N	\N	f	20
8	2	3	[5,6,7]	[2,2,1]	7	19	7	19	0.5	[2,3,5,6,7]	1	2	\N	\N	\N	\N	f	20
43	9	3	[30,31,33,34]	[5,5,6,6]	15	31	15	31	0.3	[30,31,33,34,35]	4	8	\N	\N	\N	\N	f	20
26	6	1	[18,19,21]	[3,3,4]	11	27	11	27	0.5	[18,19,21,22,23]	4	8	\N	\N	\N	\N	f	20
35	7	5	[28]	[1]	11	29	11	29	0.2	[22,23,25,26,27]	4	8	\N	\N	\N	\N	f	20
37	8	2	[26,27,29,30]	[3,4,4,5]	15	31	15	31	0.4	[26,27,29,30,31]	4	8	\N	\N	\N	\N	f	20
38	8	3	[26,27,29,30]	[4,4,5,5]	15	31	15	31	0.3	[26,27,29,30,31]	4	8	\N	\N	\N	\N	f	20
11	3	1	[6,7,9]	[1,2,2]	9	21	9	21	0.4	[6,7,9,10,11]	1	2	\N	\N	\N	\N	f	20
18	4	3	[11,13,14]	[2,4,2]	9	23	9	23	0.4	[10,11,13,14,15]	4	8	\N	\N	\N	\N	f	20
2	1	2	[1,2]	[3,1]	5	9	5	9	0.5	[1,2,3]	0	1	\N	\N	\N	\N	f	20
3	1	3	[1,2]	[2,2]	5	9	5	9	0.5	[1,2,3]	1	2	\N	\N	\N	\N	f	20
14	3	4	[7,9,10,11]	[1,2,3,2]	9	21	9	21	0.4	[6,7,9,10,11]	3	3	\N	\N	\N	\N	f	20
22	5	2	[15,17,18]	[3,3,2]	11	25	11	25	0.4	[14,15,17,18,19]	4	8	\N	\N	\N	\N	f	20
10	2	5	[8]	[1]	11	29	11	29	0.4	[2,3,5,6,7]	3	3	\N	\N	\N	\N	f	20
41	9	1	[30,31,33]	[7,6,5]	15	31	15	31	0.5	[30,31,33,34,35]	4	8	\N	\N	\N	\N	f	20
12	3	2	[7,9,10]	[1,3,2]	9	21	9	21	0.4	[6,7,9,10,11]	1	2	\N	\N	\N	\N	f	20
\.


--
-- Data for Name: config_bid_club_package; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_bid_club_package (package_id, bid_quantity) FROM stdin;
1	10
2	9
3	8
4	7
5	6
6	5
7	4
8	3
9	2
10	1
\.


--
-- Data for Name: config_block; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_block (id, type, hp, id_reward) FROM stdin;
1	BSC	1	1
1	POLYGON	1	1
4	POLYGON	190	1
3	POLYGON	100	1
6	BSC	1500	1
8	BSC	0	1
7	BSC	0	1
7	POLYGON	1500	1
2	POLYGON	5500	1
6	POLYGON	1500	1
5	POLYGON	900	1
8	POLYGON	0	1
3	BSC	100	1
5	BSC	900	1
4	BSC	190	1
2	BSC	5500	1
2	TON	5500	1
1	TON	1	1
4	TON	190	1
3	TON	100	1
6	TON	1500	1
5	TON	900	1
8	TON	0	1
7	TON	1500	1
1	SOL	1	1
2	SOL	5500	1
3	SOL	100	1
4	SOL	190	1
5	SOL	900	1
6	SOL	1500	1
7	SOL	1500	1
8	SOL	0	1
2	RON	5500	1
1	RON	1	1
4	RON	190	1
3	RON	100	1
6	RON	1500	1
5	RON	900	1
8	RON	0	1
7	RON	1500	1
2	BAS	5500	1
1	BAS	1	1
4	BAS	190	1
3	BAS	100	1
6	BAS	1500	1
5	BAS	900	1
8	BAS	0	1
7	BAS	1500	1
2	VIC	5500	1
1	VIC	1	1
4	VIC	190	1
3	VIC	100	1
6	VIC	1500	1
5	VIC	900	1
8	VIC	0	1
7	VIC	1500	1
\.


--
-- Data for Name: config_block_drop_by_day; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_block_drop_by_day (id, type, days, datas) FROM stdin;
3	TR	1	[0,0,0,0,0,0,0,0]
2	POLYGON	1	[80459,1,13400,4940,800,400,0,0]
1	BSC	1	[80459,1,13400,4940,800,400,0,0]
4	TON	1	[80459,1,13400,4940,800,400,0,0]
5	SOL	1	[80459,1,13400,4940,800,400,0,0]
6	RON	1	[80459,1,13400,4940,800,400,0,0]
7	BAS	1	[80459,1,13400,4940,800,400,0,0]
8	VIC	1	[80459,1,13400,4940,800,400,0,0]
\.


--
-- Data for Name: config_block_reward; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_block_reward (data_type, block_id, type, min_reward, max_reward, reward_weight) FROM stdin;
BSC	1	COIN	0	0	0
BSC	7	COIN	0	0	0
BSC	8	COIN	0	0	0
POLYGON	1	COIN	0	0	0
POLYGON	7	COIN	0	0	0
BSC	2	BOMBERMAN	0	0	0
BSC	3	SENSPARK	0.001498	0.001797	17600
POLYGON	3	SENSPARK	0.001498	0.001797	17600
POLYGON	4	SENSPARK	0.004494	0.005392	147400
POLYGON	2	BOMBERMAN	1	1	1000000
BSC	4	SENSPARK	0.004494	0.005392	147400
BSC	5	SENSPARK	0.023966	0.028759	173800
POLYGON	5	SENSPARK	0.023966	0.028759	173800
BSC	6	SENSPARK	0.047932	0.057518	180400
POLYGON	6	SENSPARK	0.047932	0.057518	180400
BSC	3	COIN	0.032	0.0384	902400
POLYGON	3	COIN	0.032	0.0384	902400
POLYGON	4	COIN	0.096	0.1152	182600
BSC	4	COIN	0.096	0.1152	182600
BSC	5	COIN	0.512	0.6144	36200
POLYGON	5	COIN	0.512	0.6144	36200
POLYGON	8	COIN	0	0	0
BSC	6	COIN	1.024	1.2288	0
POLYGON	6	COIN	1.024	1.2288	0
TON	8	COIN	0	0	0
TON	3	COIN	0.0256	0.03072	902400
TON	4	COIN	0.0768	0.09216	182600
TON	5	COIN	0.4096	0.49152	36200
TON	6	COIN	0.8192	0.98304	0
TON	7	COIN	0	0	0
TON	1	COIN	0	0	0
TON	2	BOMBERMAN	1	1	1000000
SOL	1	COIN	0	0	0
SOL	2	BOMBERMAN	1	1	1000000
SOL	3	COIN	0.0256	0.03072	902400
SOL	4	COIN	0.0768	0.09216	182600
SOL	5	COIN	0.4096	0.49152	36200
SOL	6	COIN	0.8192	0.98304	2000
SOL	7	COIN	0	0	0
SOL	8	COIN	0	0	0
POLYGON	5	BCOIN	0.04915	0.0944	790000
BSC	4	BCOIN	0.00922	0.0177	670000
POLYGON	3	BCOIN	0.00307	0.0059	80000
BSC	6	BCOIN	0.0983	0.1888	820000
BSC	5	BCOIN	0.04915	0.0944	790000
POLYGON	6	BCOIN	0.0983	0.1888	820000
POLYGON	4	BCOIN	0.00922	0.0177	670000
BSC	3	BCOIN	0.00307	0.0059	80000
RON	1	COIN	0	0	0
RON	2	BOMBERMAN	1	1	1000000
RON	3	COIN	0.0256	0.03072	902400
RON	4	COIN	0.0768	0.09216	182600
RON	5	COIN	0.4096	0.49152	36200
RON	6	COIN	0.8192	0.98304	2000
RON	7	COIN	0	0	0
RON	8	COIN	0	0	0
BAS	1	COIN	0	0	0
BAS	2	BOMBERMAN	1	1	1000000
BAS	3	COIN	0.0256	0.03072	902400
BAS	4	COIN	0.0768	0.09216	182600
BAS	5	COIN	0.4096	0.49152	36200
BAS	6	COIN	0.8192	0.98304	2000
BAS	7	COIN	0	0	0
BAS	8	COIN	0	0	0
VIC	1	COIN	0	0	0
VIC	2	BOMBERMAN	1	1	1000000
VIC	3	COIN	0.0256	0.03072	902400
VIC	4	COIN	0.0768	0.09216	182600
VIC	5	COIN	0.4096	0.49152	36200
VIC	6	COIN	0.8192	0.98304	2000
VIC	7	COIN	0	0	0
VIC	8	COIN	0	0	0
\.


--
-- Data for Name: config_bomber_ability; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_bomber_ability (ability, "values") FROM stdin;
1	1
2	1
3	1
4	1
5	1
6	1
7	1
\.


--
-- Data for Name: config_burn_hero; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_burn_hero (rarity, hero_s_rock, hero_l_rock) FROM stdin;
0	5	1
1	10	2
2	20	4
3	35	7
4	55	11
5	80	16
\.


--
-- Data for Name: config_coin_leaderboard; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_coin_leaderboard (id, name, up_rank_point_user, up_rank_point_club) FROM stdin;
1	Bronze	1000	200000
2	Silver	5000	4000000
3	Gold	10000	30000000
4	Platinum	50000	600000000
5	Diamond	150000	8000000000
6	Mega	2147483647	9223372036854775806
\.


--
-- Data for Name: config_coin_ranking_season; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_coin_ranking_season (id, start_date, end_date, modify_date) FROM stdin;
0	2024-02-01 07:00:00+07	2024-02-27 22:52:05+07	\N
1	2024-03-01 07:00:00+07	2024-03-28 01:20:40+07	2024-03-28 01:20:46+07
2	2024-04-01 07:00:00+07	2024-04-27 22:51:17+07	\N
3	2024-05-01 07:00:00+07	2024-05-27 07:00:00+07	\N
4	2024-06-01 07:00:00+07	2024-06-27 07:00:00+07	\N
5	2024-07-01 07:00:00+07	2024-07-27 07:00:00+07	\N
6	2024-08-01 07:00:00+07	2024-08-27 07:00:00+07	\N
7	2024-09-01 07:00:00+07	2024-09-27 07:00:00+07	\N
8	2024-10-01 07:00:00+07	2024-10-27 07:00:00+07	\N
9	2024-11-01 07:00:00+07	2024-11-27 07:00:00+07	\N
10	2024-12-01 07:00:00+07	2024-12-27 07:00:00+07	\N
11	2025-01-01 07:00:00+07	2025-01-27 07:00:00+07	\N
12	2025-02-01 07:00:00+07	2025-02-27 07:00:00+07	\N
13	2025-03-01 07:00:00+07	2025-03-27 07:00:00+07	\N
14	2025-04-01 07:00:00+07	2025-04-27 07:00:00+07	\N
15	2025-05-01 07:00:00+07	2025-05-27 07:00:00+07	\N
16	2025-06-01 07:00:00+07	2025-06-27 07:00:00+07	\N
17	2025-07-01 07:00:00+07	2025-07-27 07:00:00+07	\N
20	2025-10-01 07:00:00+07	2025-10-27 07:00:00+07	\N
21	2025-11-01 07:00:00+07	2025-11-27 07:00:00+07	\N
22	2025-12-01 07:00:00+07	2025-12-27 07:00:00+07	\N
23	2026-01-01 07:00:00+07	2026-01-27 07:00:00+07	\N
24	2026-02-01 07:00:00+07	2026-02-27 07:00:00+07	\N
25	2026-03-01 07:00:00+07	2026-03-27 07:00:00+07	\N
18	2025-07-29 07:00:00+07	2025-08-27 07:00:00+07	\N
19	2025-08-27 07:00:00+07	2025-09-27 07:00:00+07	\N
\.


--
-- Data for Name: config_daily_check_in; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_daily_check_in (day, reward, data_type) FROM stdin;
5	5	VIC
4	8	VIC
15	12	VIC
7	10	VIC
27	18	VIC
8	8	VIC
28	36	VIC
16	12	VIC
22	18	VIC
12	8	VIC
30	61	VIC
23	18	VIC
14	16	VIC
26	18	VIC
24	18	VIC
17	12	VIC
21	24	VIC
9	8	VIC
20	12	VIC
13	8	VIC
29	46	VIC
6	5	VIC
1	5	VIC
10	8	VIC
19	12	VIC
25	27	VIC
18	18	VIC
2	5	VIC
11	12	VIC
3	5	VIC
\.


--
-- Data for Name: config_drop_rate; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_drop_rate (name, drop_rate) FROM stdin;
PVP_ITEM	[1,1,1,1,1,1,1]
\.


--
-- Data for Name: config_free_reward_by_ads; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_free_reward_by_ads (reward_type, quantity_per_view, interval_in_minutes) FROM stdin;
GOLD	15	240
GEM_LOCKED	0	240
\.


--
-- Data for Name: config_gacha_chest; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_gacha_chest (type, open_time_in_minute, items_quantity, skip_open_time_gem_require, gold_price, gem_price, is_sellable, coin_price, is_has_discount, "desc") FROM stdin;
1	30	2	1	\N	\N	f	\N	f	\N
2	60	3	2	\N	\N	f	\N	f	\N
3	120	4	4	\N	\N	f	\N	f	\N
4	240	5	8	\N	\N	f	\N	f	\N
5	\N	2	\N	\N	\N	t	47	f	\N
6	\N	3	\N	\N	\N	t	95	f	\N
7	\N	3	\N	1212	\N	t	\N	f	\N
8	\N	4	\N	\N	127	t	\N	f	\N
9	\N	5	\N	\N	285	t	\N	f	\N
10	\N	6	\N	\N	1195	t	\N	f	\N
11	\N	4	\N	\N	\N	t	174	f	\N
12	\N	3	\N	\N	\N	f	\N	f	\N
\.


--
-- Data for Name: config_gacha_chest_items; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_gacha_chest_items (item_id, chest_type, min, max, drop_rate, no, is_lock, expiration_after) FROM stdin;
77	11	1	1	2	\N	f	604800000
92	11	1	1	2	\N	f	604800000
83	11	1	1	2	\N	f	604800000
76	11	1	1	2	\N	f	604800000
84	11	1	1	2	\N	f	604800000
125	11	1	1	9	\N	f	604800000
72	6	1	1	5	\N	f	604800000
46	6	1	1	5	\N	f	604800000
86	6	1	1	6	\N	f	604800000
87	6	1	1	6	\N	f	604800000
91	6	1	1	6	\N	f	604800000
88	5	1	1	20	\N	f	604800000
55	5	1	1	15	\N	f	604800000
64	5	1	1	15	\N	f	604800000
15	2	1	1	3	\N	t	\N
15	3	1	1	3	\N	t	\N
15	4	1	1	2	\N	t	\N
10	12	1	1	0.25	\N	t	\N
127	12	1	1	0.25	\N	t	\N
4	12	1	1	0.75	\N	t	604800000
28	12	1	1	9.5	\N	t	\N
21	12	1	1	9.5	\N	t	\N
90	12	1	1	0.5	\N	t	604800000
6	12	1	1	0.75	\N	t	604800000
27	12	1	1	9.5	\N	t	\N
43	12	1	1	1.5	\N	t	604800000
42	12	1	1	1.5	\N	t	604800000
20	12	1	1	9.5	\N	t	\N
40	12	1	1	1.5	\N	t	604800000
75	12	1	1	0.5	\N	t	604800000
22	12	1	1	9.5	\N	t	\N
18	12	1	1	9.5	\N	t	\N
78	12	1	1	0.5	\N	t	604800000
63	12	1	1	0.75	\N	t	604800000
79	12	1	1	0.5	\N	t	604800000
23	12	1	1	9.5	\N	t	\N
103	12	3	10	3	\N	t	\N
41	12	1	1	1.5	\N	t	604800000
62	12	1	1	0.75	\N	t	604800000
26	12	1	1	9.5	\N	t	\N
19	12	1	1	9.5	\N	t	\N
145	11	1	1	14	\N	f	\N
11	11	1	1	14	\N	f	\N
129	11	1	1	14	\N	f	\N
17	11	1	1	14	\N	f	\N
131	11	1	1	14	\N	f	\N
145	10	1	1	6	\N	t	\N
19	10	1	1	1	\N	t	\N
18	10	1	1	1	\N	t	\N
21	10	1	1	1	\N	t	\N
23	10	1	1	1	\N	t	\N
104	10	50	150	3	\N	t	\N
129	10	1	1	8	\N	t	\N
125	10	1	1	7	\N	t	2592000000
89	10	1	1	4	\N	t	2592000000
77	10	1	1	4	\N	t	2592000000
86	10	1	1	3	\N	t	2592000000
88	10	1	1	3	\N	t	2592000000
84	10	1	1	3	\N	t	2592000000
81	10	1	1	3	\N	t	2592000000
91	10	1	1	3	\N	t	2592000000
92	10	1	1	3	\N	t	2592000000
70	10	1	1	3	\N	t	2592000000
64	10	1	1	3	\N	t	2592000000
71	10	1	1	3	\N	t	2592000000
44	10	1	1	3	\N	t	2592000000
47	10	1	1	3	\N	t	2592000000
48	10	1	1	3	\N	t	2592000000
50	10	1	1	3	\N	t	2592000000
49	10	1	1	3	\N	t	2592000000
131	10	1	1	8	\N	t	\N
11	10	1	1	8	\N	t	\N
143	10	1	1	6	\N	t	\N
68	9	1	1	5	\N	t	2592000000
61	9	1	1	5	\N	t	2592000000
51	9	1	1	5	\N	t	2592000000
56	9	1	1	5	\N	t	2592000000
104	9	50	100	21	\N	t	\N
23	9	1	1	10	\N	t	\N
21	9	1	1	10	\N	t	\N
18	9	1	1	10	\N	t	\N
19	9	1	1	10	\N	t	\N
17	9	1	1	3	\N	t	\N
46	9	1	1	5	\N	t	2592000000
144	9	1	1	3	\N	t	\N
87	9	1	1	4	\N	t	2592000000
83	9	1	1	4	\N	t	2592000000
82	8	1	1	7	\N	t	604800000
21	8	1	1	6	\N	t	\N
23	8	1	1	6	\N	t	\N
104	8	50	50	44	\N	t	\N
19	8	1	1	6	\N	t	\N
14	8	1	1	6	\N	t	\N
55	8	1	1	5	\N	t	2592000000
72	8	1	1	7	\N	t	604800000
18	8	1	1	6	\N	t	\N
76	8	1	1	7	\N	t	604800000
103	7	5	5	38	\N	t	\N
23	7	1	1	8	\N	t	\N
21	7	1	1	8	\N	t	\N
18	7	1	1	8	\N	t	\N
19	7	1	1	8	\N	t	\N
101	7	1	1	10	\N	t	\N
65	7	1	1	10	\N	t	604800000
45	7	1	1	10	\N	t	604800000
26	6	1	1	1	\N	f	\N
0	6	10	150	2	\N	t	\N
103	6	5	10	3	\N	t	\N
23	6	1	1	1	\N	f	\N
21	6	1	1	1	\N	f	\N
18	6	1	1	1	\N	f	\N
19	6	1	1	1	\N	f	\N
28	6	1	1	1	\N	f	\N
27	6	1	1	1	\N	f	\N
144	6	1	1	17	\N	f	\N
14	6	1	1	17	\N	f	\N
143	6	1	1	17	\N	f	\N
71	6	1	1	3	\N	f	604800000
45	6	1	1	3	\N	f	604800000
39	6	1	1	3	\N	f	604800000
101	5	1	1	26	\N	f	\N
27	5	1	1	1	\N	f	\N
26	5	1	1	1	\N	f	\N
28	5	1	1	1	\N	f	\N
19	5	1	1	1	\N	f	\N
18	5	1	1	1	\N	f	\N
21	5	1	1	1	\N	f	\N
23	5	1	1	1	\N	f	\N
20	5	1	1	1	\N	f	\N
22	5	1	1	1	\N	f	\N
103	5	5	5	3	\N	f	\N
0	5	10	100	2	\N	f	\N
82	5	1	1	5	\N	f	604800000
48	5	1	1	5	\N	f	604800000
82	4	1	1	6	\N	t	604800000
72	4	1	1	5	\N	t	604800000
71	4	1	1	5	\N	t	604800000
64	4	1	1	5	\N	t	604800000
45	4	1	1	5	\N	t	604800000
48	4	1	1	5	\N	t	604800000
55	4	1	1	5	\N	t	604800000
27	4	1	1	1	\N	t	\N
21	4	1	1	1	\N	t	\N
22	4	1	1	1	\N	t	\N
18	4	1	1	1	\N	t	\N
0	4	10	80	29	\N	t	\N
23	4	1	1	1	\N	t	\N
19	4	1	1	1	\N	t	\N
12	4	1	1	2	\N	t	\N
9	4	1	1	2	\N	t	\N
26	4	1	1	1	\N	t	\N
28	4	1	1	1	\N	t	\N
20	4	1	1	1	\N	t	\N
125	4	1	1	8	\N	t	604800000
87	4	1	1	6	\N	t	604800000
88	4	1	1	6	\N	t	604800000
28	3	1	1	2	\N	t	\N
27	3	1	1	2	\N	t	\N
21	3	1	1	2	\N	t	\N
22	3	1	1	2	\N	t	\N
18	3	1	1	2	\N	t	\N
23	3	1	1	2	\N	t	\N
0	3	10	60	49	\N	t	\N
19	3	1	1	2	\N	t	\N
12	3	1	1	3	\N	t	\N
9	3	1	1	3	\N	t	\N
26	3	1	1	2	\N	t	\N
20	3	1	1	2	\N	t	\N
125	3	1	1	7	\N	t	604800000
82	3	1	1	5	\N	t	604800000
71	3	1	1	4	\N	t	604800000
64	3	1	1	4	\N	t	604800000
48	3	1	1	4	\N	t	604800000
9	2	1	1	3	\N	t	\N
26	2	1	1	2	\N	t	\N
28	2	1	1	2	\N	t	\N
20	2	1	1	2	\N	t	\N
82	2	1	1	14	\N	t	604800000
64	2	1	1	6	\N	t	604800000
23	2	1	1	2	\N	t	\N
19	2	1	1	2	\N	t	\N
48	2	1	1	6	\N	t	604800000
27	2	1	1	2	\N	t	\N
21	2	1	1	2	\N	t	\N
12	2	1	1	3	\N	t	\N
22	2	1	1	2	\N	t	\N
18	2	1	1	2	\N	t	\N
0	2	10	40	47	\N	t	\N
0	1	10	20	70	\N	t	\N
18	1	1	1	1	\N	t	\N
22	1	1	1	1	\N	t	\N
27	1	1	1	1	\N	t	\N
20	1	1	1	1	\N	t	\N
26	1	1	1	1	\N	t	\N
19	1	1	1	1	\N	t	\N
48	1	1	1	11	\N	t	604800000
23	1	1	1	0.5	\N	t	\N
21	1	1	1	0.5	\N	t	\N
28	1	1	1	1	\N	t	\N
64	1	1	1	11	\N	t	604800000
70	11	1	1	1	\N	f	604800000
61	11	1	1	1	\N	f	604800000
68	11	1	1	1	\N	f	604800000
65	11	1	1	1	\N	f	604800000
47	11	1	1	1	\N	f	604800000
50	11	1	1	1	\N	f	604800000
56	11	1	1	1	\N	f	604800000
51	11	1	1	1	\N	f	604800000
49	11	1	1	1	\N	f	604800000
89	11	1	1	2	\N	f	604800000
\.


--
-- Data for Name: config_gacha_chest_slot; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_gacha_chest_slot (id, slot, type, price) FROM stdin;
1	1	FREE	0
2	2	FREE	0
3	3	FREE	0
4	4	BUY	50
5	5	BUY	150
6	6	VIP	0
\.


--
-- Data for Name: config_grind_hero; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_grind_hero (item_kind, drop_items, price) FROM stdin;
MVP	[{"itemId":107,"quantity":10,"weight":7000},{"itemId":108,"quantity":1,"weight":2900},{"itemId":109,"quantity":1,"weight":100},{"itemId":110,"quantity":1,"weight":0},{"itemId":111,"quantity":1,"weight":0}]	200
NORMAL	[{"itemId":107,"quantity":10,"weight":5000},{"itemId":108,"quantity":1,"weight":4550},{"itemId":109,"quantity":1,"weight":300},{"itemId":110,"quantity":1,"weight":100},{"itemId":111,"quantity":1,"weight":50}]	200
PREMIUM	[{"itemId":107,"quantity":10,"weight":1000},{"itemId":108,"quantity":1,"weight":3000},{"itemId":109,"quantity":1,"weight":3150},{"itemId":110,"quantity":1,"weight":1850},{"itemId":111,"quantity":1,"weight":1000}]	200
\.


--
-- Data for Name: config_hero_repair_shield; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_hero_repair_shield (id, rarity, shield_level, price, price_rock) FROM stdin;
21	5	0	50	10
19	4	2	40	24
24	5	3	50	40
23	5	2	50	30
13	3	0	30	6
12	2	3	20	16
14	3	1	30	12
8	1	3	10	8
3	0	2	10	3
7	1	2	10	6
20	4	3	40	32
10	2	1	20	8
11	2	2	20	12
17	4	0	40	8
16	3	3	30	24
15	3	2	30	18
22	5	1	50	20
9	2	0	20	4
1	0	0	10	1
6	1	1	10	4
5	1	0	10	2
2	0	1	10	2
4	0	3	10	4
18	4	1	40	16
\.


--
-- Data for Name: config_hero_traditional_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_hero_traditional_config (item_id, name, skin, color, speed, range, bomb, hp, dmg, max_speed, max_range, max_bomb, max_dmg, max_hp, tutorial, can_be_bot, max_upgrade_speed, max_upgrade_range, max_upgrade_bomb, max_upgrade_hp, max_upgrade_dmg) FROM stdin;
141	Pinky Neko	30	1	4	3	1	3	3	6	6	6	10	10	0	1	10	10	10	10	10
117	Kuroneko	20	1	4	2	2	2	3	5	5	5	8	8	0	1	8	8	8	8	8
136	Pinky Bear	29	1	2	2	1	6	3	6	6	6	10	10	0	1	10	10	10	10	10
128	Doge	25	1	2	2	2	3	3	5	5	5	8	8	0	1	8	8	8	8	8
129	King	26	1	4	3	2	2	3	6	6	6	10	10	0	1	10	10	10	10	10
11	G. Ku	12	1	3	2	2	4	3	6	6	6	10	10	0	1	10	10	10	10	10
13	Pinky Toon	13	1	3	2	1	4	3	5	5	5	8	8	0	1	8	8	8	8	8
14	Stickman	14	1	2	2	2	5	3	6	6	6	10	10	0	1	10	10	10	10	10
131	B-Guy	28	1	2	2	2	3	5	6	6	6	10	10	0	1	10	10	10	10	10
116	Calico	19	1	4	2	2	2	3	5	5	5	8	8	0	1	8	8	8	8	8
12	Witch	5	1	2	3	2	2	3	5	5	5	8	8	1	1	8	8	8	8	8
127	Frog	24	1	2	1	2	5	3	5	5	5	8	8	0	1	8	8	8	8	8
9	Knight	2	1	2	1	1	5	3	5	5	5	8	8	1	1	8	8	8	8	8
8	Mr. Poo	11	1	3	2	2	3	3	5	5	5	8	8	0	1	8	8	8	8	8
17	Dragon	16	1	3	2	1	5	3	6	6	6	10	10	0	1	10	10	10	10	10
130	Cupid	27	1	4	1	2	3	4	6	6	6	10	10	0	1	10	10	10	10	10
119	Mr. Dear	22	1	3	1	1	5	3	5	6	6	8	8	0	1	8	8	8	8	8
15	Ninja	8	1	4	2	1	2	3	5	5	5	8	8	1	1	8	8	8	8	8
10	Cowboy	3	1	3	2	2	3	3	5	5	5	8	8	0	1	8	8	8	8	8
120	T. Lion	23	1	3	1	3	4	3	6	6	6	10	10	0	1	10	10	10	10	10
118	Golden Kat	21	1	4	1	2	4	3	6	6	6	10	10	0	1	10	10	10	10	10
100	Santa	17	1	3	1	3	3	3	5	5	5	8	8	0	1	8	8	8	8	8
16	Monitor	15	1	2	3	2	3	3	5	5	5	8	8	0	1	8	8	8	8	8
101	Miner	18	1	3	2	2	4	3	6	6	6	10	10	0	1	10	10	10	10	10
143	Dragoon	31	1	3	4	1	3	3	6	6	6	10	10	0	1	10	10	10	10	10
145	Hesman	33	1	3	3	2	4	3	6	6	6	10	10	0	1	10	10	10	10	10
144	Fat Tiger	32	1	1	2	2	5	3	6	6	6	10	10	0	1	10	10	10	10	10
\.


--
-- Data for Name: config_hero_upgrade_power; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_hero_upgrade_power (rare, datas) FROM stdin;
0	[0,1,2,3,5]
1	[0,1,2,3,5]
2	[0,1,2,3,5]
3	[0,1,2,3,5]
4	[0,1,2,3,5]
5	[0,1,2,3,5]
\.


--
-- Data for Name: config_hero_upgrade_shield; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_hero_upgrade_shield (rarity, data, price) FROM stdin;
2	[1250, 2500, 3750, 5000]	[0,4,4,4]
3	[1500, 3000, 4500, 6000]	[0,6,6,6]
1	[1125, 2250, 3375, 4500]	[0,2,2,2]
5	[2000, 4000, 6000, 8000]	[0,10,10,10]
0	[1000, 2000, 3000, 4000]	[0,1,1,1]
4	[1750, 3500, 5250, 7000]	[0,8,8,8]
\.


--
-- Data for Name: config_iap_gold_shop; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_iap_gold_shop (item_id, name, gem_price, golds_receive, bonus_first_time) FROM stdin;
1	Tiny pack	109	1500	0
2	Regular pack	259	3750	0
3	Pro pack	509	7500	0
4	Big pack	1259	18750	0
5	Massive pack	2009	30000	0
\.


--
-- Data for Name: config_iap_shop; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_iap_shop (product_id, type, name, items, limit_per_user, bonus_type, items_bonus, is_stater_pack, is_remove_ads, buy_step, purchase_time_limit) FROM stdin;
deluxe_pack	GEM	Deluxe pack	[{"item_id":103,"quantity":1250}]	\N	1	[{"item_id":103,"quantity":625}]	f	f	1	\N
giant_pack	GEM	Giant Pack	[{"item_id":103,"quantity":8000}]	\N	1	[{"item_id":103,"quantity":8000}]	f	f	1	\N
huge_pack	GEM	Huge Pack	[{"item_id":103,"quantity":4000}]	\N	1	[{"item_id":103,"quantity":4000}]	f	f	1	\N
pro_pack	GEM	Pro pack	[{"item_id":103,"quantity":500}]	\N	1	[{"item_id":103,"quantity":150}]	f	f	1	\N
regular_pack	GEM	Regular pack	[{"item_id":103,"quantity":250}]	\N	1	[{"item_id":103,"quantity":50}]	f	f	1	\N
super_deluxe_pack	GEM	Super deluxe pack	[{"item_id":103,"quantity":2000}]	\N	1	[{"item_id":103,"quantity":2000}]	f	f	1	\N
tiny_pack	GEM	Tiny pack	[{"item_id":103,"quantity":100}]	\N	1	[{"item_id":103,"quantity":10}]	f	f	1	\N
premium_offer	PACK	Premium pack	[{"item_id":104,"quantity":5000},{"item_id":103,"quantity":100},{"item_id":73,"quantity":1},{"item_id":82,"quantity":1},{"item_id":43,"quantity":1},{"item_id":18,"quantity":12},{"item_id":19,"quantity":12},{"item_id":16,"quantity":1},{"item_id":3,"quantity":1}]	0	0	[]	f	t	2	172800
hero_offer	PACK	Hero pack	[{"item_id":14,"quantity":1},{"item_id":10,"quantity":1},{"item_id":17,"quantity":1},{"item_id":127,"quantity":1},{"item_id":129,"quantity":1},{"item_id":131,"quantity":1}]	0	0	[]	f	f	1	172800
starter_offer	PACK	Starter pack	[{"item_id":103,"quantity":100,"expiration_after":0},{"item_id":64,"quantity":1,"expiration_after":604800000},{"item_id":48,"quantity":1,"expiration_after":604800000},{"item_id":77,"quantity":1,"expiration_after":2592000000},{"item_id":18,"quantity":20,"expiration_after":0},{"item_id":19,"quantity":20,"expiration_after":0}]	0	0	[]	t	f	1	172800
\.


--
-- Data for Name: config_item; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_item (id, type, name, ability, modify_date, description_en, kind, gold_price_7_days, gem_price_7_days, gem_price_30_days, gold_price, gem_price, active, is_sellable, is_default, tag, sale_start_date, sale_end_date) FROM stdin;
84	8	Purple	[1]	2024-01-30 06:56:31.637788	Purple Fire	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
156	11	Pepe Cry	[]	2024-07-12 16:34:52	Pepe Cry Emoji	PREMIUM	\N	\N	\N	\N	75	t	t	f	NEW	\N	\N
155	11	Doge Bonk	[]	2024-07-12 16:34:52	Doge Bonk Emoji	PREMIUM	\N	\N	\N	\N	75	t	t	f	NEW	\N	\N
154	11	SEN	[]	2024-07-12 16:34:52	SEN Emoji	PREMIUM	\N	\N	\N	\N	40	t	t	f	NEW	\N	\N
152	11	Doge	[]	2024-07-12 16:34:52	Doge Emoji	PREMIUM	\N	\N	\N	\N	75	t	t	f	NEW	\N	\N
153	11	BCoin	[]	2024-07-12 16:34:52	BCoin Emoji	PREMIUM	\N	\N	\N	\N	40	t	t	f	NEW	\N	\N
157	11	Pepe Shock	[]	2024-07-12 16:34:52	Pepe Shock Emoji	PREMIUM	\N	\N	\N	\N	75	t	t	f	NEW	\N	\N
147	12	Joyful Spell	[]	2024-07-19 11:36:07	Joyful Spell Avatar	MVP	\N	\N	\N	\N	\N	t	t	t	NEW	\N	\N
146	12	Ninja Fury	[]	2024-07-19 11:36:07	Ninja Fury Avatar	MVP	\N	\N	\N	\N	\N	t	t	t	NEW	\N	\N
148	12	Mighty Fist	[]	2024-07-19 11:36:07	Mighty Fist Avatar	NORMAL	\N	\N	\N	800	\N	t	t	f	NEW	\N	\N
149	12	Red Hot	[]	2024-07-19 11:36:07	Red Hot Avatar	NORMAL	\N	\N	\N	800	\N	t	t	f	NEW	\N	\N
151	12	Reckoning	[]	2024-07-19 11:36:07	Reckoning Avatar	PREMIUM	\N	\N	\N	\N	120	t	t	f	NEW	\N	\N
150	12	Final Form	[]	2024-07-19 11:36:07	Final Form Avatar	PREMIUM	\N	\N	\N	\N	120	t	t	f	NEW	\N	\N
56	1	Dragon	[2]	2022-11-28 09:03:28.824018	Skin Bomb Dragon	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
61	3	Valkyrie	[4]	2022-11-28 09:08:18.685946	Skin Wing Valkyrie	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
64	3	Asura	[4]	2022-11-28 09:08:18.685946	Skin Wing Asura	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
65	3	Moonblader	[5]	2022-11-28 09:08:18.685946	Skin Wing Moonblader	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
71	3	Shuriken	[2]	2022-11-28 09:08:18.685946	Skin Wing Shuriken	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
77	8	Pepe	[1]	2022-11-28 09:11:22.367312	Skin Fire Pepe	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
83	8	Melon	[1]	2022-11-28 09:11:22.367312	Skin Fire Melon	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
87	8	Dog Bite	[1]	2022-11-28 09:11:22.367312	Skin Fire Dog Bite	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
46	1	BlackCat	[3]	2024-01-30 06:56:31.637788	Black Cat Bomb	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
68	3	Lucifer	[3]	2024-01-30 06:56:31.637788	Lucifer Wing	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
72	3	Shuriken Ori	[3]	2024-01-30 06:56:31.637788	Shuriken Ori Wing	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
86	8	Dark Cat	[1]	2024-01-30 06:56:31.637788	Dark Cat Fire	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
89	8	Green Star	[1]	2024-01-30 06:56:31.637788	Green Star Fire	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
82	8	Thunder	[1]	2024-01-30 06:56:31.637788	Thunder Fire	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
81	8	Star Beam	[1]	2024-01-30 06:56:31.637788	Skin Fire Star Beam	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
88	8	Monitor	[1]	2024-01-30 06:56:31.637788	Monitor Fire	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
76	8	Wukong	[1]	2024-01-30 06:56:31.637788	Skin Fire Wukong	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
11	4	G. Ku	[]	2024-01-30 06:56:31.637788	G. Ku	PREMIUM	\N	\N	\N	\N	360	t	f	f	\N	\N	\N
17	4	Dragon	[]	2024-01-30 06:56:31.637788	Dragon	PREMIUM	\N	\N	\N	\N	360	t	f	f	\N	\N	\N
55	1	Sword	[2]	2022-11-28 09:03:28.824018	Sword Bomb	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
14	4	Gladiator	[]	2024-01-30 06:56:31.637788	Gladiator	PREMIUM	\N	\N	\N	\N	360	t	f	f	\N	\N	\N
96	8	Candy	[1]	2022-11-28 09:11:22.367312	Skin Fire Candy	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
137	1	Sakura	[2]	2024-01-30 06:56:31.637788	Sakura Bomb	PREMIUM	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
113	1	Apricot	[5]	2024-01-30 06:56:31.637788	Apricot Bomb	PREMIUM	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
60	1	Ghost	[2]	2024-01-30 06:56:31.637788	Ghost Bomb	PREMIUM	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
51	1	Dragoon	[3]	2024-01-30 06:56:31.637788	Dragoon Bomb	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
49	1	Monitor	[1]	2024-01-30 06:56:31.637788	Monitor Bomb	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
50	1	Mr Poo	[4]	2024-01-30 06:56:31.637788	Mr Poo Bomb	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
44	1	Watermelon	[1]	2024-01-30 06:56:31.637788	Watermelon Bomb	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
112	1	Firework	[1]	2024-01-30 06:56:31.637788	Firework Bomb	PREMIUM	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
48	1	PugDog	[2]	2024-01-30 06:56:31.637788	Pug Dog Bomb	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
45	1	Witch Purple	[2]	2024-01-30 06:56:31.637788	Witch Purple Bomb	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
47	1	Tiger	[4]	2024-01-30 06:56:31.637788	Tiger Bomb	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
121	1	Greenmelon	[3]	2024-01-30 06:56:31.637788	Greenmelon	PREMIUM	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
59	1	Werewolves	[2]	2024-01-30 06:56:31.637788	Werewolves Bomb	PREMIUM	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
57	1	Santa Claus	[2]	2022-11-28 09:03:28.824018	Skin Bomb Santa Claus	PREMIUM	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
58	1	Pumpkin	[2]	2024-01-30 06:56:31.637788	Pumpkin Bomb	PREMIUM	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
122	3	Phoenix	[5]	2024-01-30 06:56:31.637788	Phoenix Wing	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
115	8	Apricot	[3]	2024-01-30 06:56:31.637788	Apricot	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
92	8	Pink Force	[1]	2024-01-30 06:56:31.637788	Pink Force Fire	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
39	1	Wukong	[2]	2022-11-28 09:03:28.824018	Skin Bomb Wukong	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
0	7	Gold	[]	2024-01-30 06:56:31.637788	Gold	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
1	1	Rainbow	[]	2024-01-30 06:56:31.637788	Rainbow Bomb	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
2	1	Bomb Cool Puffy	[]	2024-01-30 06:56:31.637788	Bomb Cool Puffy	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
4	3	Angel	[]	2022-10-07 10:18:45.363249	Skin Wing Angel	NORMAL	850	\N	80	\N	\N	t	t	f	\N	\N	\N
6	3	Devil	[]	2022-10-07 10:18:45.363249	Skin Wing Devil	NORMAL	850	\N	80	\N	\N	t	t	f	\N	\N	\N
7	1	Candy Ball	[]	2022-10-07 10:18:45.363249	Skin Bomb Candy Ball	NORMAL	600	\N	40	\N	\N	t	t	f	\N	\N	\N
8	4	Mr. Poo	[]	2024-01-30 06:56:31.637788	Mr. Poo	NORMAL	\N	\N	\N	\N	100	t	t	f	\N	\N	\N
10	4	Cowboy	[]	2024-01-30 06:56:31.637788	Cowboy	NORMAL	\N	\N	\N	\N	100	t	t	f	\N	\N	\N
13	4	Pinky Toon	[]	2024-01-30 06:56:31.637788	Pinky Toon	NORMAL	\N	\N	\N	\N	100	t	t	f	\N	\N	\N
16	4	Monitor	[]	2024-01-30 06:56:31.637788	Monitor	NORMAL	\N	\N	\N	\N	100	t	t	f	\N	\N	\N
18	5	Key	[]	2024-01-23 08:34:22.989828	Key	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
19	5	Shield	[]	2024-01-23 08:34:22.989828	Shield	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
20	5	Rank Guardian	[]	2024-01-30 06:56:31.637788	Rank Guardian	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
21	5	Full Rank Guardian	[]	2024-01-30 06:56:31.637788	Full Rank Guardian	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
22	5	Conquest Card	[]	2024-01-30 06:56:31.637788	Conquest Card	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
23	5	Full Conquest Card	[]	2024-01-30 06:56:31.637788	Full Conquest Card	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
24	6	Loudspeaker	[]	2024-01-30 06:56:31.637788	Loudspeaker	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
25	1	Starbeam	[]	2022-10-07 10:18:45.363249	Skin Bomb Starbeam	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
26	5	+1 Bomb	[]	2022-10-19 10:30:20.137719	+1 Bomb	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
27	5	+1 Speed	[]	2022-10-19 10:30:20.137719	+1 Speed	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
28	5	+1 Range	[]	2022-10-19 10:30:20.137	+1 Range	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
37	1	Puffer Fish	[]	2024-01-30 06:56:31.637788	Puffer Fish Bomb	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
38	1	Doge Coin	[]	2024-01-30 06:56:31.637788	Doge Coin Bomb	NORMAL	600	\N	40	\N	\N	t	t	f	\N	\N	\N
40	1	Pepe	[]	2024-01-30 06:56:31.637788	Pepe Bomb	NORMAL	600	\N	40	\N	\N	t	t	f	\N	\N	\N
41	1	Pilot Rabbit	[]	2024-01-30 06:56:31.637788	Pilot Rabbit Bomb	NORMAL	600	\N	40	\N	\N	t	t	f	\N	\N	\N
42	1	Snowman	[]	2022-11-28 09:03:28.824018	Skin Bomb Snowman	NORMAL	600	\N	40	\N	\N	t	t	f	\N	\N	\N
43	1	Thunder	[]	2024-01-30 06:56:31.637788	Thunder Bomb	NORMAL	600	\N	40	\N	\N	t	t	f	\N	\N	\N
52	1	Pinkytoon	[]	2024-01-30 06:56:31.637788	Pinky Toon Bomb	NORMAL	600	\N	40	\N	\N	t	t	f	\N	\N	\N
53	1	GB Pink	[]	2022-11-28 09:03:28.824018	Skin Bomb GB Pink	NORMAL	600	\N	40	\N	\N	t	t	f	\N	\N	\N
54	1	Gold Miner	[]	2024-01-30 06:56:31.637788	Gold Miner Bomb	NORMAL	600	\N	40	\N	\N	t	t	f	\N	\N	\N
62	3	Savior	[]	2022-11-28 09:08:18.685946	Skin Wing Savior	NORMAL	850	\N	80	\N	\N	t	t	f	\N	\N	\N
63	3	GearX	[]	2024-01-30 06:56:31.637788	GearX Wing	NORMAL	850	\N	80	\N	\N	t	t	f	\N	\N	\N
66	3	Trigrams	[]	2024-01-30 06:56:31.637788	Trigrams Wing	NORMAL	850	\N	80	\N	\N	t	t	f	\N	\N	\N
67	3	Rainbow	[]	2022-11-28 09:08:18.685946	Skin Wing Rainbow	NORMAL	850	\N	80	\N	\N	t	t	f	\N	\N	\N
69	3	Yamato	[]	2024-01-30 06:56:31.637788	Yamato Wing	NORMAL	850	\N	80	\N	\N	t	t	f	\N	\N	\N
73	3	Mijonir	[]	2022-11-28 09:08:18.685946	Skin Wing Mijonir	NORMAL	850	\N	80	\N	\N	t	t	f	\N	\N	\N
74	3	Casper Angel	[]	2024-01-30 06:56:31.637788	Casper Angel Wing	NORMAL	850	\N	80	\N	\N	t	f	f	\N	\N	\N
75	8	Doge	[]	2022-11-28 09:11:22.367312	Skin Fire Doge	NORMAL	1000	\N	200	\N	\N	t	t	f	\N	\N	\N
78	8	Rabbit	[]	2024-01-30 06:56:31.637788	Skin Fire Rabbit	NORMAL	1000	\N	200	\N	\N	t	t	f	\N	\N	\N
79	8	Water	[]	2022-11-28 09:11:22.367312	Skin Fire Water	NORMAL	1000	\N	200	\N	\N	t	t	f	\N	\N	\N
80	8	Snow	[]	2022-11-28 09:11:22.367312	Skin Fire Snow	NORMAL	1000	\N	200	\N	\N	t	t	f	\N	\N	\N
85	8	Tiger	[]	2024-01-30 06:56:31.637788	Tiger Fire	NORMAL	1000	\N	200	\N	\N	t	t	f	\N	\N	\N
15	4	Ninja	[]	2024-01-23 08:34:22.989828	Ninja	MVP	\N	\N	\N	600	\N	t	t	f	\N	\N	\N
3	2	Rainbow	[]	2024-01-30 06:56:31.637788	Trail Rainbow	NORMAL	\N	50	200	\N	\N	t	t	f	\N	\N	\N
90	8	Black Magic	[]	2022-11-28 09:11:22.367312	Skin Fire Black Magic	NORMAL	1000	\N	200	\N	\N	t	t	f	\N	\N	\N
93	8	Miner	[]	2022-11-28 09:11:22.367312	Skin Fire Miner	NORMAL	1000	\N	200	\N	\N	t	t	f	\N	\N	\N
94	8	Sword	[]	2024-01-30 06:56:31.637788	Sword Fire	NORMAL	1000	\N	200	\N	\N	t	t	f	\N	\N	\N
95	8	Dragon Burn	[]	2022-11-28 09:11:22.367312	Skin Fire Dragon Burn	NORMAL	1000	\N	200	\N	\N	t	t	f	\N	\N	\N
98	8	Craw Wolf	[]	2024-01-30 06:56:31.637788	Craw Wolf Fire	NORMAL	1000	\N	200	\N	\N	t	t	f	\N	\N	\N
99	8	Ghost	[]	2024-01-30 06:56:31.637788	Ghost Fire	NORMAL	1000	\N	200	\N	\N	t	t	f	\N	\N	\N
102	7	GEM	[]	2024-01-30 06:56:31.637788	Gem	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
104	7	GOLD	[]	2022-11-28 09:11:22.367312	Gold	PREMIUM	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
105	7	COIN	[]	2024-01-30 06:56:31.637788	Coin	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
107	10	Scrap	[]	2024-01-30 06:56:31.637788	Scrap	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
109	10	Rough Crystal	[]	2024-01-30 06:56:31.637788	Rough Crystal	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
110	10	Pure Crystal	[]	2024-01-30 06:56:31.637788	Pure Crystal	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
111	10	Perfect Crystal	[]	2024-01-30 06:56:31.637788	Perfect Crystal	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
116	4	Calico	[]	2024-01-30 06:56:31.637788	Calico	NORMAL	\N	\N	\N	\N	100	t	f	f	\N	\N	\N
117	4	Kuroneko	[]	2024-01-30 06:56:31.637788	Kuroneko	NORMAL	\N	\N	\N	\N	100	t	f	f	\N	\N	\N
119	4	Mr. Dear	[]	2024-01-30 06:56:31.637788	Mr. Dear	NORMAL	\N	\N	\N	\N	100	t	f	f	\N	\N	\N
124	2	Multi Shadow	[]	2024-01-30 06:56:31.637788	Multi Shadow Trail	NORMAL	\N	50	200	\N	\N	t	t	f	\N	\N	\N
126	2	Nitro	[]	2024-01-30 06:56:31.637788	Nitro Trail	NORMAL	\N	50	200	\N	\N	t	t	f	\N	\N	\N
127	4	Frog	[]	2024-01-30 06:56:31.637788	Frog	NORMAL	\N	\N	\N	\N	100	t	t	f	\N	\N	\N
132	11	Smirk Emoji	[]	2024-01-30 06:56:31.637788	Smirk Emoji	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
133	11	Angry Emoji	[]	2024-01-30 06:56:31.637788	Angry Emoji	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
134	11	Sad Emoji	[]	2024-01-30 06:56:31.637788	Sad Emoji	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
135	11	Happy Emoji	[]	2024-01-30 06:56:31.637788	Happy Emoji	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
9	4	Knight	[]	2024-01-23 08:34:22.989828	Knight	MVP	\N	\N	\N	600	\N	t	t	f	\N	\N	\N
12	4	Witch	[]	2024-01-23 08:34:22.989828	Witch	MVP	\N	\N	\N	600	\N	t	t	f	\N	\N	\N
70	3	Venom	[5]	2024-01-30 06:56:31.637788	Venom Wing	PREMIUM	\N	30	120	\N	\N	t	f	f	\N	\N	\N
108	10	Lesser Crystal	[]	2024-02-23 02:23:01.934516	Lesser Crystal	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
100	4	Santa	[]	2024-01-30 06:56:31.637788	Santa	NORMAL	\N	\N	\N	\N	100	t	f	f	\N	\N	\N
97	8	Pumpkin	[]	2024-01-30 06:56:31.637788	Pumpkin Fire	NORMAL	1000	\N	200	\N	\N	t	t	f	\N	\N	\N
106	9	Mystery Box	[]	2024-03-05 11:00:24.622679	Lucky Wheel Mystery Box	NORMAL	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
128	4	Doge	[]	2024-01-30 06:56:31.637788	Doge	NORMAL	\N	\N	\N	\N	100	t	t	f	\N	\N	\N
91	8	Pinky Toon	[1]	2024-01-30 06:56:31.637788	Pinky Toon Fire	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
138	8	Sakura Vigor	[1]	2024-01-30 06:56:31.637788	Sakura Vigor	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
114	8	Firework	[1]	2024-01-30 06:56:31.637788	Firework	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
123	8	Greenmelon	[5]	2024-01-30 06:56:31.637788	Greenmelon	PREMIUM	\N	70	280	\N	\N	t	f	f	\N	\N	\N
139	2	Sakura Petal	[3]	2024-01-30 06:56:31.637788	Sakura Petal Trail	PREMIUM	\N	100	400	\N	\N	t	f	f	\N	\N	\N
125	2	Happy Color	[3]	2024-01-30 06:56:31.637788	Happy Color Trail	PREMIUM	\N	100	400	\N	\N	t	f	f	\N	\N	\N
142	2	Sky Spark	[3]	2024-01-30 06:56:31.637788	Sky Spark Trail	PREMIUM	\N	100	400	\N	\N	t	f	f	\N	\N	\N
140	2	Red Heart	[3]	2024-01-30 06:56:31.637788	Red Heart Trail	PREMIUM	\N	100	400	\N	\N	t	f	f	\N	\N	\N
141	4	Pink Neko	[]	2024-01-30 06:56:31.637788	Pink Neko	PREMIUM	\N	\N	\N	\N	360	t	f	f	\N	\N	\N
120	4	T. Lion	[]	2024-01-30 06:56:31.637788	T. Lion	PREMIUM	\N	\N	\N	\N	360	t	f	f	\N	\N	\N
118	4	Golden Kat	[]	2024-01-30 06:56:31.637788	Golden Kat	PREMIUM	\N	\N	\N	\N	360	t	f	f	\N	\N	\N
136	4	Pinky Bear	[]	2024-01-30 06:56:31.637788	Pinky Bear	PREMIUM	\N	\N	\N	\N	360	t	f	f	\N	\N	\N
101	4	Miner	[]	2024-01-30 06:56:31.637788	Miner	PREMIUM	\N	\N	\N	\N	360	t	f	f	\N	\N	\N
131	4	B-Guy	[]	2024-01-30 06:56:31.637788	B-Guy	PREMIUM	\N	\N	\N	\N	360	t	f	f	\N	\N	\N
129	4	King	[]	2024-01-30 06:56:31.637788	King	PREMIUM	\N	\N	\N	\N	360	t	f	f	\N	\N	\N
130	4	Cupid	[]	2024-01-30 06:56:31.637788	Cupid	PREMIUM	\N	\N	\N	\N	360	t	f	f	\N	\N	\N
143	4	Dragoon	[]	2024-03-28 08:25:02.022266	Dragoon	PREMIUM	\N	\N	\N	\N	360	t	f	f	\N	\N	\N
144	4	Fat Tiger	[]	2024-03-28 08:25:02.184899	Fat Tiger	PREMIUM	\N	\N	\N	\N	360	t	f	f	\N	\N	\N
145	4	Hesman	[]	2024-03-28 08:25:02.345141	Hesman	PREMIUM	\N	\N	\N	\N	360	t	f	f	NEW	2024-05-01	\N
103	7	Gem Locked	[]	2022-11-28 09:11:22.367312	Gem Locked	PREMIUM	\N	\N	\N	\N	\N	t	f	f	\N	\N	\N
\.


--
-- Data for Name: config_item_market_v3; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_item_market_v3 (id, type, name, kind, min_price, max_price, is_have_expiration, max_price_7_days, max_price_30_days, active, fixed_amount, reward_type, created_at) FROM stdin;
3	2	Rainbow	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
4	3	Angel	NORMAL	1	\N	1	60	240	1	30	GEM	2025-05-06 07:17:09.672588
6	3	Devil	NORMAL	1	\N	1	60	240	1	30	GEM	2025-05-06 07:17:09.672588
7	1	Candy Ball	NORMAL	1	\N	1	30	120	1	30	GEM	2025-05-06 07:17:09.672588
8	4	Mr. Poo	NORMAL	1	300	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
9	4	Knight	MVP	1	132	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
10	4	Cowboy	NORMAL	1	300	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
11	4	G. Ku	PREMIUM	1	1080	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
12	4	Witch	MVP	1	132	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
13	4	Pinky Toon	NORMAL	1	300	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
14	4	Gladiator	PREMIUM	1	1080	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
15	4	Ninja	MVP	1	132	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
16	4	Monitor	NORMAL	1	300	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
17	4	Dragon	PREMIUM	1	1080	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
18	5	Key	NORMAL	1	25	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
19	5	Shield	NORMAL	1	25	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
20	5	Rank Guardian	NORMAL	1	25	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
21	5	Full Rank Guardian	NORMAL	1	50	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
22	5	Conquest Card	NORMAL	1	25	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
23	5	Full Conquest Card	NORMAL	1	50	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
25	1	Starbeam	NORMAL	1	\N	1	30	120	1	30	GEM	2025-05-06 07:17:09.672588
26	5	+1 Bomb	NORMAL	1	25	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
27	5	+1 Speed	NORMAL	1	25	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
28	5	+1 Range	NORMAL	1	25	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
37	1	Puffer Fish	NORMAL	1	\N	1	30	120	1	30	GEM	2025-05-06 07:17:09.672588
38	1	Doge Coin	NORMAL	1	\N	1	30	120	1	30	GEM	2025-05-06 07:17:09.672588
39	1	Wukong	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
40	1	Pepe	NORMAL	1	\N	1	30	120	1	30	GEM	2025-05-06 07:17:09.672588
41	1	Pilot Rabbit	NORMAL	1	\N	1	30	120	1	30	GEM	2025-05-06 07:17:09.672588
42	1	Snowman	NORMAL	1	\N	1	30	120	1	30	GEM	2025-05-06 07:17:09.672588
43	1	Thunder	NORMAL	1	\N	1	30	120	1	30	GEM	2025-05-06 07:17:09.672588
44	1	Watermelon	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
45	1	Witch Purple	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
46	1	BlackCat	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
47	1	Tiger	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
48	1	PugDog	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
49	1	Monitor	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
50	1	Mr Poo	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
51	1	Dragoon	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
52	1	Pinkytoon	NORMAL	1	\N	1	30	120	1	30	GEM	2025-05-06 07:17:09.672588
53	1	GB Pink	NORMAL	1	\N	1	30	120	1	30	GEM	2025-05-06 07:17:09.672588
54	1	Gold Miner	NORMAL	1	\N	1	30	120	1	30	GEM	2025-05-06 07:17:09.672588
55	1	Sword	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
56	1	Dragon	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
61	3	Valkyrie	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
62	3	Savior	NORMAL	1	\N	1	60	240	1	30	GEM	2025-05-06 07:17:09.672588
63	3	GearX	NORMAL	1	\N	1	60	240	1	30	GEM	2025-05-06 07:17:09.672588
64	3	Asura	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
65	3	Moonblader	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
66	3	Trigrams	NORMAL	1	\N	1	60	240	1	30	GEM	2025-05-06 07:17:09.672588
67	3	Rainbow	NORMAL	1	\N	1	60	240	1	30	GEM	2025-05-06 07:17:09.672588
68	3	Lucifer	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
69	3	Yamato	NORMAL	1	\N	1	60	240	1	30	GEM	2025-05-06 07:17:09.672588
70	3	Venom	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
71	3	Shuriken	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
72	3	Shuriken Ori	PREMIUM	1	\N	1	90	360	1	30	GEM	2025-05-06 07:17:09.672588
73	3	Mijonir	NORMAL	1	\N	1	60	240	1	30	GEM	2025-05-06 07:17:09.672588
75	8	Doge	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
76	8	Wukong	PREMIUM	1	\N	1	210	840	1	30	GEM	2025-05-06 07:17:09.672588
77	8	Pepe	PREMIUM	1	\N	1	210	840	1	30	GEM	2025-05-06 07:17:09.672588
78	8	Rabbit	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
79	8	Water	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
80	8	Snow	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
81	8	Star Beam	PREMIUM	1	\N	1	210	840	1	30	GEM	2025-05-06 07:17:09.672588
82	8	Thunder	PREMIUM	1	\N	1	210	840	1	30	GEM	2025-05-06 07:17:09.672588
83	8	Melon	PREMIUM	1	\N	1	210	840	1	30	GEM	2025-05-06 07:17:09.672588
84	8	Purple	PREMIUM	1	\N	1	210	840	1	30	GEM	2025-05-06 07:17:09.672588
85	8	Tiger	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
86	8	Dark Cat	PREMIUM	1	\N	1	210	840	1	30	GEM	2025-05-06 07:17:09.672588
87	8	Dog Bite	PREMIUM	1	\N	1	210	840	1	30	GEM	2025-05-06 07:17:09.672588
88	8	Monitor	PREMIUM	1	\N	1	210	840	1	30	GEM	2025-05-06 07:17:09.672588
89	8	Green Star	PREMIUM	1	\N	1	210	840	1	30	GEM	2025-05-06 07:17:09.672588
90	8	Black Magic	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
91	8	Pinky Toon	PREMIUM	1	\N	1	210	840	1	30	GEM	2025-05-06 07:17:09.672588
92	8	Pink Force	PREMIUM	1	\N	1	210	840	1	30	GEM	2025-05-06 07:17:09.672588
93	8	Miner	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
94	8	Sword	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
95	8	Dragon Burn	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
96	8	Candy	PREMIUM	1	\N	1	210	840	1	30	GEM	2025-05-06 07:17:09.672588
97	8	Pumpkin	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
98	8	Craw Wolf	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
99	8	Ghost	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
101	4	Miner	PREMIUM	1	1080	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
124	2	Multi Shadow	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
125	2	Happy Color	PREMIUM	1	\N	1	300	1200	1	30	GEM	2025-05-06 07:17:09.672588
126	2	Nitro	NORMAL	1	\N	1	150	600	1	30	GEM	2025-05-06 07:17:09.672588
127	4	Frog	NORMAL	1	300	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
128	4	Doge	NORMAL	1	300	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
129	4	King	PREMIUM	1	1080	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
131	4	B-Guy	PREMIUM	1	1080	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
136	4	Pinky Bear	PREMIUM	1	1080	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
143	4	Dragoon	PREMIUM	1	1080	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
144	4	Fat Tiger	PREMIUM	1	1080	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
145	4	Hesman	PREMIUM	1	1080	0	\N	\N	1	30	GEM	2025-05-06 07:17:09.672588
\.


--
-- Data for Name: config_lucky_wheel_reward; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_lucky_wheel_reward (code, item_id, quantity, active, sort, weight) FROM stdin;
BOOSTER_BOMB	26	1	t	2	1
BOOSTER_SPEED	27	1	t	4	1
BOOSTER_RANGE	28	1	t	3	1
GEM_1	103	1	t	6	1
GOLD_30	104	30	t	5	1
MYSTERY_BOX	106	1	t	1	1
\.


--
-- Data for Name: config_message; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_message (code, vn, en) FROM stdin;
DAILY_MISSION_WATCH_ADDS	Xem quảng cáo	Watch ads
\.


--
-- Data for Name: config_min_stake_hero; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_min_stake_hero (rarity, min_stake_amount) FROM stdin;
0	60
1	486
2	971
3	1942
4	4854
5	9709
\.


--
-- Data for Name: config_mystery_box_item; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_mystery_box_item (item_id, weight, expiration_after, quantity) FROM stdin;
25	1200	86400000	1
39	500	86400000	1
42	1100	86400000	1
53	1200	86400000	1
55	500	86400000	1
56	500	86400000	1
4	700	86400000	1
6	600	86400000	1
61	100	86400000	1
62	600	86400000	1
64	100	86400000	1
65	100	86400000	1
67	600	86400000	1
71	100	86400000	1
73	600	86400000	1
75	100	86400000	1
77	100	86400000	1
79	120	86400000	1
80	120	86400000	1
83	100	86400000	1
87	100	86400000	1
90	120	86400000	1
93	120	86400000	1
95	120	86400000	1
9	75	\N	1
101	75	\N	1
15	75	\N	1
8	50	\N	1
13	75	\N	1
10	75	\N	1
12	75	\N	1
\.


--
-- Data for Name: config_new_user_gift; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_new_user_gift (item_id, quantity, item_name, active, expiration_after, step) FROM stdin;
9	1	Knight	t	0	0
12	1	Witch	t	0	0
15	1	Ninja	t	0	0
18	3	Key	t	0	0
19	3	Shield	t	0	0
104	100	GOLD	t	0	0
132	1	Smirk	t	0	0
133	1	Angry	t	0	0
134	1	Sad	t	0	0
135	1	Happy	t	0	0
64	1	Asura Wing	t	86400000	0
76	1	Wukong Fire	t	86400000	0
125	1	Happy Color Trail	t	86400000	0
49	1	Monitor Bomb	t	86400000	0
\.


--
-- Data for Name: config_offline_reward_th_mode; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_offline_reward_th_mode (key, value, network) FROM stdin;
block_hp	1365	TON
block_value	0.08	TON
no_auto_mine	60	TON
heroes_dame	[12,40,90,160,250,360]	TON
energy_used	[4,7,10,13,17,20]	TON
\.


--
-- Data for Name: config_on_boarding; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_on_boarding (step, reward, description) FROM stdin;
1	10	BUY A BHERO
2	10	STAKE BHERO
3	10	REPAIR SHIELD
\.


--
-- Data for Name: config_package_auto_mine; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_package_auto_mine (id, package_name, num_day, min_price, price_percent, data_type) FROM stdin;
6	Package_30_Days	30	0.3	0	TON
5	Package_7_Days	7	0.075	0	TON
1	PACKAGE_7_DAYS	7	10	12	BSC
2	PACKAGE_7_DAYS	7	10	12	POLYGON
4	PACKAGE_30_DAYS	30	30	35	POLYGON
3	PACKAGE_30_DAYS	30	30	35	BSC
8	PACKAGE_30_DAYS	30	0.0092	0	SOL
7	PACKAGE_7_DAYS	7	0.0023	0	SOL
10	Package_30_Days	30	3.1	0	RON
9	Package_7_Days	7	0.8	0	RON
12	Package_30_Days	30	0.00086	0	BAS
11	Package_7_Days	7	0.000214	0	BAS
14	Package_30_Days	30	2.3	0	VIC
13	Package_7_Days	7	9.2	0	VIC
\.


--
-- Data for Name: config_package_rent_house_v2; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_package_rent_house_v2 (rarity, num_days, price, data_type) FROM stdin;
3	7	193	TON
4	7	242	TON
2	7	145	TON
5	7	290	TON
0	7	48	TON
1	7	97	TON
3	14	348	TON
0	14	87	TON
2	14	261	TON
1	14	174	TON
5	14	522	TON
4	14	435	TON
0	30	166	TON
5	30	994	TON
1	30	331	TON
3	30	662	TON
4	30	828	TON
2	30	497	TON
0	7	37	SOL
1	7	74	SOL
2	7	110	SOL
3	7	147	SOL
4	7	184	SOL
5	7	221	SOL
0	14	66	SOL
1	14	132	SOL
2	14	198	SOL
3	14	265	SOL
4	14	331	SOL
5	14	397	SOL
0	30	126	SOL
1	30	252	SOL
2	30	378	SOL
3	30	504	SOL
4	30	630	SOL
5	30	756	SOL
0	7	3.5	RON
1	7	7	RON
2	7	10.5	RON
3	7	14	RON
4	7	17.5	RON
5	7	21	RON
0	14	6.3	RON
1	14	12.6	RON
2	14	18.9	RON
3	14	25.2	RON
4	14	31.5	RON
5	14	37.8	RON
0	30	12	RON
1	30	24	RON
2	30	36	RON
3	30	48	RON
4	30	60	RON
5	30	72	RON
0	7	0.00065	BAS
1	7	0.0013	BAS
2	7	0.00195	BAS
3	7	0.0026	BAS
4	7	0.00326	BAS
5	7	0.00391	BAS
0	14	0.00117	BAS
1	14	0.00234	BAS
2	14	0.00352	BAS
3	14	0.00469	BAS
4	14	0.00586	BAS
5	14	0.00703	BAS
0	30	0.00223	BAS
1	30	0.00446	BAS
2	30	0.0067	BAS
3	30	0.00893	BAS
4	30	0.01116	BAS
5	30	0.01339	BAS
3	30	96	VIC
1	14	25	VIC
5	7	42	VIC
4	7	35	VIC
0	30	24	VIC
1	7	14	VIC
4	30	120	VIC
4	14	63	VIC
3	7	28	VIC
1	30	48	VIC
5	30	144	VIC
5	14	76	VIC
2	30	72	VIC
0	7	7	VIC
0	14	13	VIC
2	14	38	VIC
2	7	21	VIC
3	14	50	VIC
\.


--
-- Data for Name: config_pvp_ranking; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_pvp_ranking (bomb_rank, start_point, end_point, name, win_point, loose_point, min_matches, decay_point) FROM stdin;
1	0	100	Iron 1	20	0	0	0
2	100	200	Iron 2	20	0	0	0
9	1500	2000	Platinum 1	20	-40	2	80
5	450	600	Silver 1	20	-20	1	20
14	4600	2147483647	Diamond 2	20	-60	5	300
8	1000	1500	Gold 2	20	-30	1	40
12	3200	3900	Emerald 2	20	-50	3	150
11	2600	3200	Emerald 1	20	-50	3	150
7	800	1000	Gold 1	20	-30	1	40
6	600	800	Silver 2	20	-20	1	20
4	300	450	Copper 2	20	-10	1	10
13	3900	4600	Diamond 1	20	-60	5	300
10	2000	2600	Platinum 2	20	-40	2	80
3	200	300	Copper 1	20	-10	1	10
\.


--
-- Data for Name: config_pvp_ranking_reward; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_pvp_ranking_reward (rank_min, rank_max, reward) FROM stdin;
11	50	{"gem":600}
101	200	{"gem":200}
51	100	{"gem":400}
4	10	{"gem":1000}
1	1	{"gem":10000}
3	3	{"gem":2000}
2	2	{"gem":5000}
\.


--
-- Data for Name: config_ranking_season; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_ranking_season (id, start_date, end_date, modify_date, is_calculated_reward) FROM stdin;
1	2026-03-01 00:00:00	2026-03-27 23:59:59	\N	f
\.


--
-- Data for Name: config_reset_shield_balancer; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_reset_shield_balancer (rare, final_damage) FROM stdin;
2	200000
4	280000
0	160000
3	240000
1	180000
5	320000
\.


--
-- Data for Name: config_revive_hero_cost; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_revive_hero_cost (times, allow_ads, gem_amount, modify_date) FROM stdin;
1	1	3	2022-11-14 10:26:54.629878+07
2	0	5	2022-11-14 10:26:54.629878+07
\.


--
-- Data for Name: config_reward_level_th_v2; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_reward_level_th_v2 (level, num_users, bcoin, sen, coin) FROM stdin;
10	59049	2.9396e-06	5.8792e-06	1.17584e-05
6	729	0.0003792534	0.0007585019	0.0015170038
1	3	0.13507909	0.27015644	0.5403129
2	9	0.042152137	0.08430373	0.16860746
7	2187	0.0001146558	0.0002293102	0.0004586204
5	243	0.0012436176	0.0024872194	0.0049744383
3	27	0.013097994	0.02619582	0.05239164
9	19683	1.01258e-05	2.02514e-05	4.05028e-05
8	6561	3.4298e-05	6.85955e-05	0.000137191
4	81	0.0040484252	0.008096799	0.016193599
\.


--
-- Data for Name: config_reward_pool_th_v2; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_reward_pool_th_v2 (pool_id, remaining_reward, type, max_reward) FROM stdin;
0	1250	BCOIN	1250
1	2000	BCOIN	2000
2	3000	BCOIN	3000
3	3750	BCOIN	3750
4	6250	BCOIN	6250
5	8750	BCOIN	8750
0	8333	SENSPARK	8333
1	8333	SENSPARK	8333
2	8333	SENSPARK	8333
3	8333	SENSPARK	8333
4	8333	SENSPARK	8333
5	8333	SENSPARK	8333
0	500000	COIN	500000
1	500000	COIN	500000
2	500000	COIN	500000
3	500000	COIN	500000
4	500000	COIN	500000
5	500000	COIN	500000
\.


--
-- Data for Name: config_rock_pack; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_rock_pack (pack_name, rock_amount, sen_price, bcoin_price) FROM stdin;
ULTRA_PACK	1000	25000	2000
GIANT_PACK	100	2500	200
TINY_PACK	5	125	10
MEGA_PACK	500	12500	1000
MEDIUM_PACK	10	250	20
PRO_PACK	50	1250	100
\.


--
-- Data for Name: config_skin_chest_drop_rate; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_skin_chest_drop_rate (value) FROM stdin;
[35,10,15,35,0,5]
\.


--
-- Data for Name: config_swap_token; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_swap_token (from_type, from_network, to_type, to_network, ratio) FROM stdin;
GEM	TR	SENSPARK	BSC	5
GEM	TR	SENSPARK	POLYGON	5
\.


--
-- Data for Name: config_swap_token_realtime; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_swap_token_realtime (key, value) FROM stdin;
gem_price_dollar	0.01
times_swap_each_day	1
max_amount_dollar_each_time	10
total_dollar_swap_each_day	1000
time_minute_update_price	60
min_gem_swap	50
remaining_total_dollar_swap	1000
\.


--
-- Data for Name: config_th_mode; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_th_mode (key, value, network) FROM stdin;
hero_limit	500	BSC
hero_limit	500	POLYGON
house_mint_limits	[2500,1250,750,250,200,50]	BSC
house_mint_limits	[2500,1250,750,250,200,50]	POLYGON
hero_upgrade_cost	[[1,2,4,7],[2,4,5,9],[2,4,5,10],[3,7,11,22],[7,18,40,146],[9,25,56,199]]	POLYGON
house_stats	[{"recovery":120,"capacity":4},{"recovery":300,"capacity":6},{"recovery":480,"capacity":8},{"recovery":660,"capacity":10},{"recovery":840,"capacity":12},{"recovery":1020,"capacity":14}]	POLYGON
hero_ability_designs	[{"min_cost":2,"max_cost":2,"incremental_cost":0},{"min_cost":5,"max_cost":10,"incremental_cost":1},{"min_cost":10,"max_cost":20,"incremental_cost":2},{"min_cost":20,"max_cost":40,"incremental_cost":4},{"min_cost":35,"max_cost":60,"incremental_cost":5},{"min_cost":50,"max_cost":80,"incremental_cost":6}]	BSC
house_prices	[720,2400,5400,9600,15000,21600]	BSC
house_limit	5	POLYGON
house_stats	[{"recovery":120,"capacity":4},{"recovery":300,"capacity":6},{"recovery":480,"capacity":8},{"recovery":660,"capacity":10},{"recovery":840,"capacity":12},{"recovery":1020,"capacity":14}]	BSC
house_limit	5	BSC
hero_ability_designs	[{"min_cost":2,"max_cost":2,"incremental_cost":0},{"min_cost":5,"max_cost":10,"incremental_cost":1},{"min_cost":10,"max_cost":20,"incremental_cost":2},{"min_cost":20,"max_cost":40,"incremental_cost":4},{"min_cost":35,"max_cost":60,"incremental_cost":5},{"min_cost":50,"max_cost":80,"incremental_cost":6}]	POLYGON
hero_upgrade_cost	[[1,2,4,7],[2,4,5,9],[2,4,5,10],[3,7,11,22],[7,18,40,146],[9,25,56,199]]	BSC
house_prices	[720,2400,5400,9600,15000,21600]	POLYGON
house_stats	[{"recovery":120,"capacity":4},{"recovery":300,"capacity":6},{"recovery":480,"capacity":8},{"recovery":660,"capacity":10},{"recovery":840,"capacity":12},{"recovery":1020,"capacity":14}]	TON
house_stats	[{"recovery":120,"capacity":4},{"recovery":300,"capacity":6},{"recovery":480,"capacity":8},{"recovery":660,"capacity":10},{"recovery":840,"capacity":12},{"recovery":1020,"capacity":14}]	SOL
house_prices	[422,1405,3162,5621,8783,12648]	TON
fusion_fee	[0,8,11,14,23,38,56,74,95,116]	TON
house_prices_token_network	[4,13,30,53,83,120]	TON
hero_price	[{"bcoin_deposited":6},{"ton":0.04}]	TON
hero_limit	1000	TON
disable_buy_with_token_network	1744909199000	TON
hero_price	{"Coin":50}	POLYGON
hero_price	{"Coin":50}	BSC
house_prices	[321,1405,3162,5621,8783,12648]	SOL
hero_price	[{"sol":0.0013},{"bcoin_deposited":5}]	SOL
fusion_fee	[0,6,9,10,17,29,42,56,72,88]	SOL
house_prices_token_network	[0.09,0.3,0.68,1.2,1.88,2.7]	SOL
disable_buy_with_token_network	1751066969000	SOL
hero_limit	1000	RON
house_stats	[{"recovery":120,"capacity":4},{"recovery":300,"capacity":6},{"recovery":480,"capacity":8},{"recovery":660,"capacity":10},{"recovery":840,"capacity":12},{"recovery":1020,"capacity":14}]	RON
house_prices_token_network	[34,102,231,410,637,922]	RON
house_prices	[0,0,0,0,0,0]	RON
hero_price	[{"ron":0.4},{"star_core":11}]	RON
fusion_fee	[0,0.38,0.56,0.68,1.13,1.88,2.79,3.69,4.74,5.8]	RON
hero_limit	1000	BAS
fusion_fee	[0,0.0001,0.00015,0.00018,0.00031,0.00051,0.00076,0.001,0.00129,0.00158]	BAS
house_prices	[0,0,0,0,0,0]	BAS
house_prices_token_network	[0.0059,0.019,0.0432,0.0762,0.1194,0.1722]	BAS
house_stats	[{"recovery":120,"capacity":4},{"recovery":300,"capacity":6},{"recovery":480,"capacity":8},{"recovery":660,"capacity":10},{"recovery":840,"capacity":12},{"recovery":1020,"capacity":14}]	BAS
hero_price	[{"bas":0.000094},{"star_core":11}]	BAS
hero_limit	1000	VIC
hero_price	[{"vic":1},{"star_core":11}]	VIC
house_prices	[0,0,0,0,0,0]	VIC
house_prices_token_network	[63,204,463,817,1279,1846]	VIC
house_stats	[{"recovery":120,"capacity":4},{"recovery":300,"capacity":6},{"recovery":480,"capacity":8},{"recovery":660,"capacity":10},{"recovery":840,"capacity":12},{"recovery":1020,"capacity":14}]	VIC
fusion_fee	[0,1.1,1.6,2,3.3,5.5,8.1,10.8,13.8,16.9]	VIC
hero_limit	3000	SOL
\.


--
-- Data for Name: config_th_mode_v2; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_th_mode_v2 (key, value, date_updated, type) FROM stdin;
period	60	2024-05-17 03:05:20	BCOIN
reward_pool	{"0":0.2468750001,"1":499985.7675,"2":499996.4575,"3":499989.0175,"4":499997.06,"5":499995.2675}	2024-05-17 03:05:20	BCOIN
reward_pool	{"0":0.29937500024,"1":499985.7675,"2":499996.4575,"3":499989.0175,"4":499997.06,"5":499995.2675}	2024-05-17 07:05:08	SENSPARK
reward_pool	{"0":0.29937500024,"1":499985.7675,"2":499996.4575,"3":499989.0175,"4":499997.06,"5":499995.2675}	2024-05-17 14:04:57	COIN
min_stake	[0,0,0,0,0,0]	2024-07-12 08:32:42	SENSPARK
min_stake	[0,0,0,0,0,0]	2024-07-12 08:32:42	BCOIN
max_pool	8333	2024-05-23 04:45:21	SENSPARK
max_pool	8333	2024-05-23 04:45:21	BCOIN
\.


--
-- Data for Name: config_ton_tasks; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_ton_tasks (id, name, reward, type, deleted) FROM stdin;
1	Buy 1 Hero	10	0	0
2	Buy 5 Heroes	15	0	0
3	Buy 15 Heroes	20	0	0
4	Buy a House	30	1	0
5	Follow X page Bomb Crypto	3	2	0
6	Join Discord Bomb Crypto	3	2	0
7	Join Telegram Bomb Crypto	3	2	0
8	Follow Substack Bomb Crypto	3	2	0
9	Follow Tiktok Bomb Crypto	3	2	0
10	Play Easy Cake	5	2	0
11	Join Telegram Easy Cake	3	2	0
12	Subscribe Youtube Easy Cake	3	2	0
13	Play Psyduck	5	2	0
14	Follow X page Psyduck	3	2	0
15	Join Telegram Psyduck	3	2	0
16	Subscribe Youtube Psyduck	3	2	0
17	Play MemeTD	5	2	0
18	Join Telegram MemeTD	3	2	0
19	Follow X page MemeTD	3	2	0
20	Play Snap Fly	5	2	0
21	Join Telegram Snap Fly	3	2	0
22	Follow X page Snap Fly	3	2	0
23	Play Money Garden AI	5	2	0
24	Join Telegram Money Garden AI	3	2	0
25	Follow X page Money Garden AI	3	2	0
26	Play Shark Attack	5	2	0
27	Join Telegram Shark Attack 	3	2	0
28	Play BFB Sport	5	2	0
29	Join Telagram BFB Sport	3	2	0
30	Play Shiok	5	2	0
31	Join Telegram Shiok	3	2	0
32	Follow X page Shiok	3	2	0
33	Play TON Pirate Kings	5	2	0
34	Play FishWar	5	2	0
35	Join Telegram FishWar	3	2	0
36	Follow X page FishWar	3	2	0
37	Play Slimewifhat	5	2	0
38	Join Telegram Slimewifhat	3	2	0
39	Follow X page Slimewifhat	3	2	0
40	Play Cowtopia	5	2	0
41	Play Outa	5	2	0
42	Join Telegram Outa	3	2	0
43	Follow X page Outa	3	2	0
44	Play TON Cook	5	2	0
45	Join Telegram TON Cook	3	2	0
46	Play Royal Pets	5	2	0
47	Join Telegram Royal Pets	3	2	0
48	Follow X page Royal Pets	3	2	0
49	Play xOffer Play2Earn bot	5	2	0
50	Join Telegram xOffer Play2Earn	3	2	0
51	Follow X page xOffer Play2Earn	3	2	0
52	Play DealTON	5	2	0
53	Join Telegram DealTON	3	2	0
54	Subscribe Youtube DealTON	3	2	0
55	Play game POP Launch	5	2	0
56	Join Telegram POP Launch	3	2	0
57	Follow X page POP Launch	3	2	0
58	Play CoinCrypto	5	2	0
59	Play Mobiverse	5	2	0
63	Play game Wepunk	5	2	0
64	Join Telegram Wepunk	3	2	0
65	Follow X page Wepunk	3	2	0
66	Play game Clockie Chaos	5	2	0
67	Join Telegram Clockie Chaos	3	2	0
68	Play game TON Def	5	2	0
69	Play game Greedy Ball	5	2	0
70	Play Piloton	5	2	0
71	Join Telegram	3	2	0
72	Follow X page	3	2	0
73	Play GemGame	5	2	0
74	Join Telegram	3	2	0
75	Play TON Flash	5	2	0
76	Play Play HangarX	5	2	0
77	Join Telegram HangarX	3	2	0
78	Follow X page HangarX	3	2	0
79	Play Run Tap Tap	5	2	0
80	Join Telegram Run Tap Tap	3	2	0
81	Follow X Run Tap Tap	3	2	0
82	Play Cat Gold Miner	5	2	0
83	Join Telegram Cat Gold Miner	3	2	0
84	Subscribe Youtube Cat Gold Miner	3	2	0
85	Play Bear Fi	5	2	0
86	Join Telegram Bear Fi	3	2	0
87	Follow X Bear Fi	3	2	0
88	Play Purr	5	2	0
89	Join Telegram Purr	3	2	0
90	Follow X Purr	3	2	0
91	Play Grand Journey	5	2	0
92	Join Telegram Grand Journey	3	2	0
93	Play DogX	5	2	0
94	Join Telegram DogX	3	2	0
95	Follow X DogX	3	2	0
96	Play PokeTON	5	2	0
97	Join Telegram PokeTON	3	2	0
98	Play Plant Harvest	5	2	0
99	Join Telegram Plant Harvest	3	2	0
100	Follow X Plant Harvest	3	2	0
101	Play LOL Happy Mining	5	2	0
102	Join Telegram LOL Happy Mining	3	2	0
103	Join Telegram LOL Happy Mining	3	2	0
104	Play Kokomo	5	2	0
105	Follow X Kokomo	3	2	0
106	Play USDspin	5	2	0
107	Play Uang Gratis	5	2	0
108	Follow X WcoinGame	3	2	0
109	Play Habit Tap	5	2	0
110	Join Telegram Habit Tap	3	2	0
111	Play TFarm	5	2	0
112	Join Telegram TFarm	3	2	0
113	Follow X TFarm	3	2	0
114	Play Musgard	5	2	0
115	Join Telegram Musgard	3	2	0
116	Follow X Musgard	3	2	0
117	Play Stability World AI	5	2	0
118	Join Telegram Stability World AI	3	2	0
119	Follow X Stability World AI	3	2	0
120	Play Rabbits	5	2	0
121	Join Telegram Rabbits	3	2	0
122	Follow X Rabbits	3	2	0
123	Play Ragdoll	5	2	0
124	Play Butterfly's	5	2	0
125	Join Telegram Butterfly's	3	2	0
126	Follow X Butterfly's	3	2	0
127	Play Scroo-G	5	2	0
128	Follow X Scroo-G	3	2	0
129	Join Telegram Scroo-G	3	2	0
130	Play Kaboom	5	2	0
131	Join Telegram Kaboom	3	2	0
132	Play Totemancer	5	2	0
133	Join Telegram Totemancer	3	2	0
134	Follow X Totemancer	3	2	0
136	Join Telegram Unicorn Galaxy	3	2	0
60	Play game Moon Token Mining	5	2	1
62	Follow X page Moon Token Mining	3	2	1
61	Join Telegram Moon Token Mining	3	2	1
137	Follow X Unicorn Galaxy	3	2	0
135	Play Unicorn Galaxy	5	2	0
138	Play Fruicy Blast	5	2	0
139	Play My Corp	5	2	0
140	Play Coco Park	5	2	0
141	Join Telegram Coco Park	3	2	0
142	Play Fortune Boss	5	2	0
143	Play Bine	5	2	0
144	Join Telegram Bine	3	2	0
145	Follow X Bine	3	2	0
146	Play Monkey Paw	5	2	0
147	Join Telegram Monkey Paw	3	2	0
169	Play Clarnium	5	2	0
170	Join Telegram Clarnium	3	2	0
171	Follow X Clarnium	3	2	0
172	Play TonOS	5	2	0
173	Play Happy Farmer	5	2	0
174	Play Coin Gate Pad	5	2	0
175	Join Telegram Coin Gate Pad	3	2	0
176	Follow X Coin Gate Pad	3	2	0
177	Play XStar Fleet	5	2	0
178	Join Telegram XStar Fleet	3	2	0
179	Join Telegram UQUID	3	2	0
180	Follow X UQUID	3	2	0
181	Play Smash Quest	5	2	0
182	Join Telegram Smash Quest	3	2	0
183	Follow X Smash Quest	3	2	0
184	Play Pecks	5	2	0
185	Follow X Pecks	3	2	0
186	Play Agentz	5	2	0
187	Join Telegram Agentz	3	2	0
149	Join Telegram TON Milk	3	2	1
148	Play TON Milk	5	2	1
150	Follow X TON Milk	3	2	1
206	Play Ton Realm	5	2	0
207	Join Telegram on Realm	3	2	0
208	Play Fishtopia	5	2	0
209	Join Telegram Fishtopia	3	2	0
210	Follow X Fishtopia	3	2	0
211	Play Fortune Cats	5	2	0
212	Join Telegram Fortune Cats	3	2	0
213	Follow X Fortune Cats	3	2	0
214	Play Capybara Meme	5	2	0
215	Play Lamaz	5	2	0
\.


--
-- Data for Name: config_upgrade_crystal; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_upgrade_crystal (source_item_id, target_item_id, gold_fee, gem_fee) FROM stdin;
108	109	100	0
109	110	200	5
110	111	300	10
\.


--
-- Data for Name: config_upgrade_hero_tr; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.config_upgrade_hero_tr (index, type, gold_fee, gem_fee, items) FROM stdin;
1	SPEED_FIRE_BOMB	200	0	[{"item_id":108,"quantity":1}]
4	DMG_HP	1000	5	[{"item_id":109,"quantity":1}]
5	DMG_HP	1500	10	[{"item_id":109,"quantity":2}]
6	DMG_HP	2000	15	[{"item_id":110,"quantity":1}]
7	DMG_HP	2500	20	[{"item_id":110,"quantity":2}]
8	DMG_HP	3000	25	[{"item_id":111,"quantity":1}]
9	DMG_HP	3500	30	[{"item_id":111,"quantity":2}]
10	DMG_HP	4000	60	[{"item_id":111,"quantity":4}]
3	DMG_HP	500	0	[{"item_id":108,"quantity":1}]
4	SPEED_FIRE_BOMB	800	0	[{"item_id":110,"quantity":3},{"item_id":111,"quantity":3}]
2	SPEED_FIRE_BOMB	400	0	[{"item_id":108,"quantity":3},{"item_id":109,"quantity":1}]
3	SPEED_FIRE_BOMB	600	0	[{"item_id":109,"quantity":3},{"item_id":110,"quantity":1}]
\.


--
-- Data for Name: daily_task_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.daily_task_config (id, completed, reward, is_random, is_default, expired, is_deleted, description) FROM stdin;
1	1	[18:1],[19:1]	t	t	\N	f	Chơi thắng 1 level bất kì ở adventure mode
2	3	[1],[2],[7],[25],[37],[38],[40],[41],[42],[43],[52],[53],[54],[75],[78],[79],[80],[85],[90],[93],[94],[95],[97],[98],[99]	t	t	86400000	f	Chơi thắng 3 level bất kì ở adventure mode
3	1	[26:1],[27:1],[28:1]	t	t	\N	f	Chơi thắng 1 trận đấu PvP mode
6	1	[3],[4],[6],[62],[63],[66],[67],[69],[73],[74],[124],[126]	t	f	86400000	f	Hoàn thành Grind 1 Hero
7	1	[18:1],[19:1]	t	f	\N	f	Hoàn thành Upgrade 1 chỉ số bất kì của 1 Hero
8	3	[108:1]	f	f	\N	f	Sử dụng Shield 3 lần khi chơi PvP mode
9	3	[108:1]	f	f	\N	f	Sử dụng Key 3 lần khi chơi PvP mode
10	1	[26:1],[27:1],[28:1]	t	f	\N	f	Chiến thắng 1 level boss ở Adventure mode
5	1	[18:1],[19:1]	t	f	\N	f	Mua 1 Hero trong P2P Market
4	3	[18:1],[19:1]	t	f	\N	f	Mua 3 items trong P2P Market 
\.


--
-- Data for Name: game_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.game_config (key, value, date_updated) FROM stdin;
min_sell	50	2023-11-09 03:04:01
fee_sell	0.2	2023-11-09 03:04:01
claim_bcoin_limit	[[0,5],[40,-1]]	2023-11-10 02:45:47
min_version_can_play	1	2023-11-10 02:45:47
pvp_match_reward	30	2023-11-10 02:45:47
is_server_maintenance	0	2023-11-10 02:45:47
is_server_game_test	1	2024-04-04 10:47:22
new_user_gift_skin	[49,64,76,125]	2024-04-06 06:50:56
size_ranking_leaderboard	200	2024-04-25 07:34:18
chanel_slack_id	null	2024-05-06 08:29:36
token_bot_slack	null	2024-05-06 08:29:36
is_explode_v2_handler	1	2024-05-28 09:49:31
is_explode_v3_handler	1	2024-06-11 06:37:20
min_stake_bcoin_th_v1	[60,194,388,777,1942,3883]	2024-06-21 03:57:46
min_stake_sen_th_v1	[300,971,1942,3884,9709,19417]	2024-07-12 08:32:26
enable_claim_token	1	2024-08-09 10:40:14
ios_hero_loaded	100	2024-10-14 09:06:18
min_claim_referral	50	2024-10-14 09:55:37
enable_claim_referral	1	2024-10-14 09:55:37
time_pay_out_referral	24	2024-10-14 09:55:37
bid_unit_price	0.4	2024-10-21 11:05:54
hero_special_color	6	2025-03-05 11:17:27
total_task_in_day	5	2025-03-28 09:12:55
new_user_ton_gift_hero	[0,0]	2024-08-09 11:22:31
time_out_market	60	2025-04-22 06:49:09
refresh_min_price_market	30	2025-04-22 06:49:09
refresh_min_price_client	60	2025-04-28 04:17:13
enable_claim_token_deposited	1	2024-08-09 10:40:26
enable_claim_hero	1	2024-08-09 10:40:26
url_config_tasks	https://game.bombcrypto.io/tasks_data/data/data_v42.json	2024-09-23 02:32:26
daily_task_config_url	https://game.bombcrypto.io/daily_tasks_data/data/data_v2.json	2025-03-28 09:12:55
coin_ranking_season_day	27	2025-07-04 03:09:01
pvp_ranking_season_day	27	2025-07-04 03:41:49
\.


--
-- Name: config_adventure_mode_entity_creator_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.config_adventure_mode_entity_creator_id_seq', 1, false);


--
-- Name: config_adventure_mode_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.config_adventure_mode_items_id_seq', 9, true);


--
-- Name: config_adventure_mode_level_strategy_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.config_adventure_mode_level_strategy_id_seq', 3, true);


--
-- Name: config_block_drop_by_day_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.config_block_drop_by_day_id_seq', 1, false);


--
-- Name: config_block_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.config_block_id_seq', 1, false);


--
-- Name: config_gacha_chest_slot_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.config_gacha_chest_slot_id_seq', 6, true);


--
-- Name: config_hero_repair_shield_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.config_hero_repair_shield_id_seq', 1, false);


--
-- Name: config_item_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.config_item_id_seq', 1, false);


--
-- Name: config_package_auto_mine_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.config_package_auto_mine_id_seq', 8, true);


--
-- Name: config_ton_tasks_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.config_ton_tasks_id_seq', 1, false);


--
-- PostgreSQL database dump complete
--

\unrestrict 9VBpFg0Y70wGCAPokc0VyIE0PU5jpFfBJPA7QNom1zcWnMeSRr6cXys4bZ1RtST

