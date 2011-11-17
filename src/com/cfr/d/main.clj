(ns com.cfr.d.main
  (:gen-class)
  (:require [com.cfr.d.model :as model]))

(defn -main [main-class & args]
  (apply model/doit args))
