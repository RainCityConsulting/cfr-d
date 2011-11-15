(ns com.cfr.d.model
  (:require [clojure.java.jdbc :as sql]))

(def db {
    :classname "com.mysql.jdbc.Driver",
    :subprotocol "mysql",
    :subname "//localhost/cfr_dev"
    :user "cfr_user",
    :password "cfr"})

(defmacro wrap-connection [& body]
  `(if (sql/find-connection)
    ~@body
    (sql/with-connection db ~@body)))

(defn find-all-game-seasons []
  (wrap-connection
    (sql/with-query-results rs [
        "SELECT DISTINCT season FROM games ORDER BY season DESC"]
      (doall (map :season rs)))))

(defn rebuild-season-records-by-season [season]
  (wrap-connection
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
    (rebuild-season-records-by-season season)))

(defn- start-streak [game]
  (assoc game
         :streak 1
         :end_season (:season game)))

(defn- add-game-to-streak [streak game]
  (assoc streak
         :streak (inc (:streak streak))
         :score (+ (:score streak) (:score game))
         :opp_score (+ (:opp_score streak) (:opp_score game))
         :start_season (:season game)))

(defn rebuild-head-to-head-streaks [school-id opp-school-id]
  (wrap-connection
    (do
      (sql/delete-rows :d_head_to_head_streaks [])
      (sql/with-query-results rs
          ["SELECT win, loss, tie, score, opp_score, season
           FROM games
           WHERE school_id = ?
           AND opp_school_id = ?
           ORDER BY game_date DESC" school-id opp-school-id]
        (letfn [(p [cur acc [game & games]]
                  (if (seq game)
                    (if (= (:win cur) (:win game))
                      (p (add-game-to-streak cur game) acc games)
                      (if (< 1 (:streak cur))
                        (p (start-streak game) (conj acc cur) games)
                        (p (start-streak game) acc games)))
                    acc))]
          (p (start-streak (first rs)) [] (rest rs)))))))

(defn doit [] (rebuild-head-to-head-streaks 27 37))
