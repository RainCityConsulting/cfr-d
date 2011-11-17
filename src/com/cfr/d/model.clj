(ns com.cfr.d.model
  (:gen-class)
  (:require [clojure.java.jdbc :as   sql])
  (:use     [com.cfr.d.config  :only [config]]))

(def db {
    :classname "com.mysql.jdbc.Driver",
    :subprotocol "mysql",
    :subname (str "//" (:db-host config) "/" (:db-name config))
    :user (:db-user config)
    :password (:db-pass config)})

(defmacro wrap-connection [& body]
  `(if (sql/find-connection)
    ~@body
    (sql/with-connection db ~@body)))

(defmacro transaction [& body]
  `(if (sql/find-connection)
     (sql/transaction ~@body)
     (sql/with-connection db (sql/transaction ~@body))))

(defn- find-school-games
  "Returns vectors of distinct school-id/opp-school-id pairs"
  ([since]
    (wrap-connection
      (sql/with-query-results rs
          ["SELECT DISTINCT school_id, opp_school_id
            FROM games
            WHERE last_updated_date > ?
            ORDER BY school_id, opp_school_id" since]
        (doall (map #(vector (:school_id %) (:opp_school_id %)) rs)))))
  ([]
    (wrap-connection
      (sql/with-query-results rs
          ["SELECT DISTINCT school_id, opp_school_id
            FROM games
            ORDER BY school_id, opp_school_id"]
        (doall (map #(vector (:school_id %) (:opp_school_id %)) rs))))))

(defn- find-all-schools-with-games
  "Returns a sequence of every school-id that has a recorded game"
  ([since]
    (wrap-connection
      (sql/with-query-results rs
          ["SELECT DISTINCT school_id
            FROM games
            WHERE last_updated_date > ?
            ORDER BY school_id" since]
        (doall (map :school_id rs)))))
  ([]
    (wrap-connection
      (sql/with-query-results rs
          ["SELECT DISTINCT school_id FROM games ORDER BY school_id"]
        (doall (map :school_id rs))))))

(defn- find-all-game-seasons
  "Returns a sequence of all seasons in which games have been played"
  ([since]
    (wrap-connection
      (sql/with-query-results rs
          ["SELECT DISTINCT season
            FROM games
            WHERE last_updated_date > ?
            ORDER BY season DESC" since]
        (doall (map :season rs)))))
  ([]
    (wrap-connection
      (sql/with-query-results rs
          ["SELECT DISTINCT season FROM games ORDER BY season DESC"]
        (doall (map :season rs))))))

(defn rebuild-season-records-by-season [season]
  (transaction
    (do
      (sql/delete-rows :d_season_records ["season = ?" season])
      (sql/with-query-results rs [
          "SELECT
          school_id, season, SUM(win) AS wins, SUM(loss) AS losses, SUM(tie) AS ties,
          SUM(score) AS points_for, SUM(opp_score) AS points_against
          FROM games WHERE season = ?
          GROUP BY school_id, season
          ORDER BY school_id"
          season]
        (doseq [r rs]
          (sql/insert-record :d_season_records r))))))

(defn rebuild-all-season-records []
  (doseq [season (find-all-game-seasons)]
    (println season)
    (rebuild-season-records-by-season season)))

(defn rebuild-updated-season-records [since]
  (doseq [season (find-all-game-seasons since)]
    (println season)
    (rebuild-season-records-by-season season)))

(defn rebuild-season-records-vs-conference-by-season [season]
  (transaction
    (do
      (sql/delete-rows :d_season_school_vs_conference_records ["season = ?" season])
      (sql/with-query-results rs [
          "SELECT s.conference_id, g.school_id, g.season,
          SUM(g.win) AS wins, SUM(g.loss) AS losses, SUM(g.tie) AS ties,
          SUM(g.score) AS points_for, SUM(g.opp_score) AS points_against
          FROM games g
          INNER JOIN school_conference_seasons s ON g.opp_school_id = s.school_id
          AND g.season = s.season
          WHERE g.season = ?
          GROUP BY s.conference_id, g.school_id
          ORDER BY school_id"
          season]
        (doseq [r rs]
          (sql/insert-record :d_season_school_vs_conference_records r))))))

(defn rebuild-all-season-records-vs-conference []
  (doseq [season (find-all-game-seasons)]
    (println season)
    (rebuild-season-records-vs-conference-by-season season)))

(defn rebuild-updated-season-records-vs-conference [since]
  (doseq [season (find-all-game-seasons since)]
    (println season)
    (rebuild-season-records-vs-conference-by-season season)))

(defn- start-streak
  ([game break-schedule-id]
    (assoc game
           :streak 1
           :opp_school_id (:opp_school_id game)
           :end_season (:season game)
           :points_for (:score game)
           :points_against (:opp_score game)
           :is_win (:win game)
           :start_schedule_id (:schedule_id game)
           :break_schedule_id break-schedule-id))
  ([game]
    (assoc game
           :streak 1
           :opp_school_id (:opp_school_id game)
           :end_season (:season game)
           :points_for (:score game)
           :points_against (:opp_score game)
           :is_win (:win game)
           :start_schedule_id (:schedule_id game))))

(defn- first-streak [game]
  (dissoc (start-streak game) :end_season))

(defn- add-game-to-streak [streak game]
  (assoc streak
         :streak (inc (:streak streak))
         :points_for (+ (:points_for streak) (:score game))
         :points_against (+ (:points_against streak) (:opp_score game))
         :start_season (:season game)
         :start_schedule_id (:schedule_id game)))

(defn rebuild-h2h-streaks-by-school [school-id]
  (transaction
    (do
      (sql/delete-rows
          :d_h2h_game_streaks
          ["school_id = ?" school-id])
      (sql/with-query-results rs
          ["SELECT
                cds.division_id, g.opp_school_id, g.win, g.loss, g.tie,
                g.score, g.opp_score, g.season
           FROM games g
           INNER JOIN school_conference_seasons scs
               ON g.opp_school_id = scs.school_id AND g.season = scs.season
           INNER JOIN conference_division_seasons cds
               ON scs.conference_id = cds.conference_id AND scs.season = cds.season
           WHERE g.school_id = ?
           AND g.game_date IS NOT NULL
           ORDER BY g.opp_school_id ASC, g.game_date DESC" school-id]
        (letfn [(p [cur acc [game & games]]
                  (if (seq game)
                    (if (= (:opp_school_id cur) (:opp_school_id game))
                      (if (= (select-keys cur [:win :loss :tie]) (select-keys game [:win :loss :tie]))
                        (p (add-game-to-streak cur game) acc games)
                        (if (< 1 (:streak cur))
                          (p (start-streak game (:start_schedule_id cur)) (conj acc cur) games)
                          (p (start-streak game (:start_schedule_id cur)) acc games)))
                      (if (< 1 (:streak cur))
                        (p (first-streak game) (conj acc cur) games)
                        (p (first-streak game) acc games)))
                    (if (< 1 (:streak cur))
                      (conj acc cur)
                      acc)))]
          (doseq [streak (p (first-streak (first rs)) [] (rest rs))]
            (sql/insert-records
                :d_h2h_game_streaks
                (assoc
                    (select-keys streak [
                        :division_id :opp_school_id :is_win :streak :start_season :end_season :points_for :points_against])
                    :school_id school-id))))))))

(defn rebuild-all-h2h-streaks []
  (doseq [id (find-all-schools-with-games)]
    (println id)
    (rebuild-h2h-streaks-by-school id)))

(defn rebuild-updated-h2h-streaks [since]
  (doseq [id (find-all-schools-with-games since)]
    (println id)
    (rebuild-h2h-streaks-by-school id)))

(defn rebuild-game-streaks-by-school [school-id]
  (transaction
    (do
      (sql/delete-rows :d_game_streaks ["school_id = ?" school-id])
      (sql/with-query-results rs
          ["SELECT
                cds.division_id, g.schedule_id, g.win, g.loss, g.tie,
                g.score, g.opp_score, g.season
            FROM games g
            INNER JOIN school_conference_seasons scs
                ON g.opp_school_id = scs.school_id AND g.season = scs.season
            INNER JOIN conference_division_seasons cds
                ON scs.conference_id = cds.conference_id AND scs.season = cds.season
            WHERE g.school_id = ?
            AND g.game_date IS NOT NULL
            ORDER BY g.game_date DESC" school-id]
        (letfn [(p [cur acc [game & games]]
                  (if (seq game)
                    (if (= (select-keys cur [:win :loss :tie]) (select-keys game [:win :loss :tie]))
                      (p (add-game-to-streak cur game) acc games)
                      (if (< 1 (:streak cur))
                        (p (start-streak game (:start_schedule_id cur)) (conj acc cur) games)
                        (p (start-streak game (:start_schedule_id cur)) acc games)))
                    (if (< 1 (:streak cur))
                      (conj acc cur)
                      acc)))]
           (doseq [streak (p (first-streak (first rs)) [] (rest rs))]
             (sql/insert-record
                 :d_game_streaks
                 (assoc
                     (select-keys streak [
                         :division_id
                         :is_win
                         :streak
                         :start_season
                         :start_schedule_id
                         :end_season
                         :break_schedule_id
                         :points_for
                         :points_against])
                     :school_id school-id))))))))

(defn rebuild-all-game-streaks []
  (doseq [id (find-all-schools-with-games)]
    (println id)
    (rebuild-game-streaks-by-school id)))

(defn rebuild-updated-game-streaks [since]
  (doseq [id (find-all-schools-with-games since)]
    (println id)
    (rebuild-game-streaks-by-school id)))

(defn rebuild-h2h-games [school-id]
  (transaction
    (do
      (sql/delete-rows :d_h2h_games ["school_id = ?" school-id])
      (sql/with-query-results rs
          ["SELECT school_id, opp_school_id,
          SUM(win) AS wins, SUM(loss) AS losses, SUM(tie) AS ties,
          SUM(score) AS points_for, SUM(opp_score) AS points_against
          FROM games
          WHERE school_id = ?
          GROUP BY school_id, opp_school_id" school-id]
        (doseq [h2h rs]
          (sql/insert-record :d_h2h_games h2h))))))

(defn rebuild-all-h2h-games []
  (doseq [id (find-all-schools-with-games)]
    (println id)
    (rebuild-h2h-games id)))

(defn rebuild-updated-h2h-games [since]
  (doseq [id (find-all-schools-with-games since)]
    (println id)
    (rebuild-h2h-games id)))

(defn doit [& args] (apply rebuild-all-h2h-streaks args))
