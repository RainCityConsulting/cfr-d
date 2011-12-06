CREATE TABLE d_home_game_streaks (
  school_id INTEGER UNSIGNED NOT NULL,
  division_id INTEGER UNSIGNED NULL,
  is_win TINYINT(1) UNSIGNED NOT NULL DEFAULT 0,
  streak INTEGER UNSIGNED NOT NULL,
  start_season INTEGER UNSIGNED NOT NULL,
  start_schedule_id INTEGER UNSIGNED NOT NULL,
  end_season INTEGER UNSIGNED NULL,
  break_schedule_id INTEGER UNSIGNED NULL,
  points_for INTEGER UNSIGNED NOT NULL,
  points_against INTEGER UNSIGNED NOT NULL,
  FOREIGN KEY (division_id) REFERENCES divisions (id),
  FOREIGN KEY (start_schedule_id) REFERENCES schedules (id),
  FOREIGN KEY (break_schedule_id) REFERENCES schedules (id)
) ENGINE = InnoDB;

CREATE TABLE d_away_game_streaks (
  school_id INTEGER UNSIGNED NOT NULL,
  division_id INTEGER UNSIGNED NULL,
  is_win TINYINT(1) UNSIGNED NOT NULL DEFAULT 0,
  streak INTEGER UNSIGNED NOT NULL,
  start_season INTEGER UNSIGNED NOT NULL,
  start_schedule_id INTEGER UNSIGNED NOT NULL,
  end_season INTEGER UNSIGNED NULL,
  break_schedule_id INTEGER UNSIGNED NULL,
  points_for INTEGER UNSIGNED NOT NULL,
  points_against INTEGER UNSIGNED NOT NULL,
  FOREIGN KEY (division_id) REFERENCES divisions (id),
  FOREIGN KEY (start_schedule_id) REFERENCES schedules (id),
  FOREIGN KEY (break_schedule_id) REFERENCES schedules (id)
) ENGINE = InnoDB;
