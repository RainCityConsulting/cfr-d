(ns com.cfr.d.main
  (:gen-class)
  (:require [com.cfr.d.model :as model]))

(defn -main [& args]
  (model/doit))
