(ns com.cfr.d.config
  (:use
      [clojure.java.io :only [file]]
      [clj-config.core :only [safely read-config]]))

(def config-file (file (System/getProperty "cfr.config")))

(def config (safely read-config config-file))

;; Defs both for convenience and compile-time verification of simple settings
(def db-name (or
  (:db-name config)
  (throw (Exception. "config.clj needs a :db-name key"))))
