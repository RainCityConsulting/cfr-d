(ns com.cfr.d.model
  (:require [clojure.java.jdbc :as sql]))

(def db {
    :classname "com.mysql.jdbc.Driver",
    :subprotocol "mysql",
    :subname "//localhost/cfr05jun2011"
    :user "cfr_user",
    :password "cfr"})

(defmacro wrap-connection [& body]
  `(if (sql/find-connection)
    ~@body
    (sql/with-connection db ~@body)))

(defn- find-all-game-seasons []
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
         :end_season (:season game)
         :points_for (:score game)
         :points_against (:opp_score game)
         :is_win (:win game)))

(defn- add-game-to-streak [streak game]
  (assoc streak
         :streak (inc (:streak streak))
         :points_for (+ (:points_for streak) (:score game))
         :points_against (+ (:points_against streak) (:opp_score game))
         :start_season (:season game)))

(defn- find-school-games []
  (wrap-connection
    (sql/with-query-results rs
        ["SELECT DISTINCT school_id, opp_school_id
        FROM games
        ORDER BY school_id, opp_school_id"]
      (doall (map #(vector (:school_id %) (:opp_school_id %)) rs)))))

(defn rebuild-head-to-head-streaks-by-schools [school-id opp-school-id]
  (wrap-connection
    (do
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
          (doseq [streak (p (start-streak (first rs)) [] (rest rs))]
            (println streak)
            (sql/insert-records
                :d_head_to_head_streaks
                (assoc
                    (select-keys streak [
                        :is_win
                        :streak
                        :start_season
                        :end_season
                        :points_for
                        :points_against])
                    :school_id school-id
                    :opp_school_id opp-school-id))))))))

(defn rebuild-all-head-to-head-streaks []
  (wrap-connection
    (do
      (sql/delete-rows :d_head_to_head_streaks [])
      (doseq [ids (find-school-games)]
        (println ids)
        (apply rebuild-head-to-head-streaks-by-schools ids)))))

(defn- find-all-schools-with-games []
  (wrap-connection
    (sql/with-query-results rs
        ["SELECT DISTINCT school_id FROM games ORDER BY school_id"]
      (doall (map :school_id rs)))))

(defn rebuild-all-streaks-by-school [school-id]
  (wrap-connection
    (do
      (sql/with-query-results rs
          ["SELECT win, loss, tie, score, opp_score, season
          FROM games
          WHERE school_id = ?
          ORDER BY game_date DESC" school-id]
        (letfn [(p [cur acc [game & games]]
                  (if (seq game)
                    (if (= (:win cur) (:win game))
                      (p (add-game-to-streak cur game) acc games)
                      (if (< 1 (:streak cur))
                        (p (start-streak game) (conj acc cur) games)
                        (p (start-streak game) acc games)))
                    acc))]
           (doseq [streak (p (start-streak (first rs)) [] (rest rs))]
             (println streak)
             (sql/insert-record
                 :d_game_streaks
                 (assoc
                     (select-keys streak [
                         :is_win
                         :streak
                         :start_season
                         :end_season
                         :points_for
                         :points_against])
                     :school_id school-id))))))))

(defn rebuild-all-streaks []
  (wrap-connection
    (do
      (sql/delete-rows :d_head_to_head_streaks [])
      (doseq [id (find-all-schools-with-games)]
        (println id)
        (rebuild-all-streaks-by-school id)))))

(defn doit [] (rebuild-all-streaks))
