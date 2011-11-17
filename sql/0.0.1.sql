/*
ALTER TABLE games
ADD school_division_id INTEGER UNSIGNED NULL AFTER school_id,
ADD opp_school_division_id INTEGER UNSIGNED NULL AFTER opp_school_id,
ADD FOREIGN KEY (school_division_id) REFERENCES divisions (id),
ADD FOREIGN KEY (opp_school_division_id) REFERENCES divisions (id);

UPDATE games g
INNER JOIN school_conference_seasons scs ON g.school_id = scs.school_id AND g.season = scs.season
INNER JOIN conference_division_seasons cds ON scs.conference_id = cds.conference_id AND scs.season = cds.season
SET school_division_id = cds.division_id;

UPDATE games g
INNER JOIN school_conference_seasons scs ON g.opp_school_id = scs.school_id AND g.season = scs.season
INNER JOIN conference_division_seasons cds ON scs.conference_id = cds.conference_id AND scs.season = cds.season
SET opp_school_division_id = cds.division_id;
*/

ALTER TABLE d_season_records
ADD division_id INTEGER UNSIGNED NULL AFTER school_id,
ADD FOREIGN KEY (division_id) REFERENCES divisions (id),
ADD KEY (division_id, season, school_id);

ALTER TABLE d_season_school_vs_conference_records
ADD division_id INTEGER UNSIGNED NULL AFTER school_id,
ADD FOREIGN KEY (division_id) REFERENCES divisions (id),
ADD KEY (division_id, school_id, season, conference_id);

ALTER TABLE d_h2h_game_streaks
ADD division_id INTEGER UNSIGNED NULL AFTER school_id,
ADD FOREIGN KEY (division_id) REFERENCES divisions (id),
ADD KEY (division_id, school_id, opp_school_id);

TRUNCATE TABLE d_game_streaks;

ALTER TABLE d_game_streaks
ADD division_id INTEGER UNSIGNED NULL AFTER school_id,
ADD start_schedule_id INTEGER UNSIGNED NOT NULL AFTER start_season,
ADD break_schedule_id INTEGER UNSIGNED NULL AFTER end_season,
ADD FOREIGN KEY (division_id) REFERENCES divisions (id),
ADD FOREIGN KEY (start_schedule_id) REFERENCES schedules (id),
ADD FOREIGN KEY (break_schedule_id) REFERENCES schedules (id);

ALTER TABLE d_h2h_games
ADD division_id INTEGER UNSIGNED NULL AFTER school_id,
ADD FOREIGN KEY (division_id) REFERENCES divisions (id);
