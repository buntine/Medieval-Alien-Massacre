
; MEDIEVAL ALIEN MASSACRE.
; By Andrew Buntine, 2010
; http://www.andrewbuntine.com
;
; core.clj
; Initialises the game and gets things moving.
; If running in a REPL, execute: (-main)

(ns mam.core
  (:gen-class)
  (:use mam.gameplay))

(defn print-welcome-message []
  (println "---------------------------------------------------------")
  (println "                MEDIEVAL ALIEN MASSACRE")
  (println "A sadistic, pro-death text-based adventure for children")
  (println "   By Andrew Buntine (http://www.andrewbuntine.com/)")
  (println "---------------------------------------------------------\n"))

(defn -main []
  "Game initializer. Welcomes user and starts loop."
  (print-welcome-message)
  (messages))
