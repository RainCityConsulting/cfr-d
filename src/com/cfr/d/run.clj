(ns com.cfr.d.run
  (:gen-class)
  (:require [com.cfr.d.model :as model]))

(defn -main [& args]
  (model/doit))
