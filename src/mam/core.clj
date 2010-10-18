
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
  (println "|---------------------------------------------------------|")
  (println "|                MEDIEVAL ALIEN MASSACRE                  |")
  (println "|---------------------------------------------------------|")
  (println "| A sadistic, pro-death text-based adventure for children |")
  (println "| By Andrew Buntine (http://www.andrewbuntine.com)        |")
  (println "|                                                         |")
  (println "| Rated X^3 (18+) for:                                    |")
  (println "|   * grotesque sexual violence                           |")
  (println "|   * murder without end                                  |")
  (println "|   * zombie goat sodomy                                  |")
  (println "|---------------------------------------------------------|\n"))

(defn -main []
  "Game initializer. Welcomes user and starts loop."
  (print-welcome-message)
  (messages))
