CREATE TABLE d_season_records (
  school_id INTEGER UNSIGNED NOT NULL,
  season INTEGER UNSIGNED NOT NULL,
  wins INTEGER UNSIGNED NOT NULL,
  losses INTEGER UNSIGNED NOT NULL,
  ties INTEGER UNSIGNED NOT NULL,
  points_for INTEGER UNSIGNED NOT NULL,
  points_against INTEGER UNSIGNED NOT NULL,
  FOREIGN KEY (school_id) REFERENCES schools (id),
  KEY (season, school_id)
) ENGINE = InnoDB;

CREATE TABLE d_season_school_vs_conference_records (
  school_id INTEGER UNSIGNED NOT NULL,
  conference_id INTEGER UNSIGNED NULL,
  season INTEGER UNSIGNED NOT NULL,
  wins INTEGER UNSIGNED NOT NULL,
  losses INTEGER UNSIGNED NOT NULL,
  ties INTEGER UNSIGNED NOT NULL,
  points_for INTEGER UNSIGNED NOT NULL,
  points_against INTEGER UNSIGNED NOT NULL,
  FOREIGN KEY (school_id) REFERENCES schools (id),
  FOREIGN KEY (conference_id) REFERENCES conferences (id),
  KEY (school_id, season, conference_id)
) ENGINE = InnoDB;

CREATE TABLE d_h2h_game_streaks (
  school_id INTEGER UNSIGNED NOT NULL,
  opp_school_id INTEGER UNSIGNED NOT NULL,
  is_win TINYINT(1) UNSIGNED NOT NULL DEFAULT 0,
  streak INTEGER UNSIGNED NOT NULL,
  start_season INTEGER UNSIGNED NOT NULL,
  end_season INTEGER UNSIGNED NULL,
  points_for INTEGER UNSIGNED NOT NULL,
  points_against INTEGER UNSIGNED NOT NULL,
  FOREIGN KEY (school_id) REFERENCES schools (id),
  FOREIGN KEY (opp_school_id) REFERENCES schools (id),
  KEY (school_id, opp_school_id),
  KEY (streak),
  KEY (points_for),
  KEY (points_against)
) ENGINE = InnoDB;

CREATE TABLE d_game_streaks (
  school_id INTEGER UNSIGNED NOT NULL,
  is_win TINYINT(1) UNSIGNED NOT NULL DEFAULT 0,
  streak INTEGER UNSIGNED NOT NULL,
  start_season INTEGER UNSIGNED NOT NULL,
  end_season INTEGER UNSIGNED NULL,
  points_for INTEGER UNSIGNED NOT NULL,
  points_against INTEGER UNSIGNED NOT NULL
) ENGINE = InnoDB;

CREATE TABLE d_h2h_games (
  school_id INTEGER UNSIGNED NOT NULL,
  opp_school_id INTEGER UNSIGNED NOT NULL,
  games INTEGER UNSIGNED NULL DEFAULT 0,
  wins INTEGER UNSIGNED NOT NULL,
  losses INTEGER UNSIGNED NOT NULL,
  ties INTEGER UNSIGNED NOT NULL,
  points_for INTEGER UNSIGNED NOT NULL,
  points_against INTEGER UNSIGNED NOT NULL,
  FOREIGN KEY (school_id) REFERENCES schools (id),
  FOREIGN KEY (opp_school_id) REFERENCES schools (id),
  UNIQUE KEY (school_id, opp_school_id)
) ENGINE = InnoDB;


DELIMITER ;;

CREATE TRIGGER d_h2h_games_bi_trig BEFORE INSERT ON d_h2h_games
FOR EACH ROW BEGIN
SET NEW.games = NEW.wins + NEW.losses + NEW.ties;
END ;;

CREATE TRIGGER d_h2h_games_bu_trig BEFORE UPDATE ON d_h2h_games
FOR EACH ROW BEGIN
SET NEW.games = NEW.wins + NEW.losses + NEW.ties;
END ;;

DELIMITER ;
