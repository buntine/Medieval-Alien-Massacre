
; MEDIEVAL ALIEN MASSACRE. v0.1
; By Andrew Buntine, 2010
; http://www.andrewbuntine.com
;
; core.clj
; Initialises the game and gets things moving.


(ns mam.core
  (:gen-class)
  (:use mam.gameplay))

(defn print-welcome-message []
  (println "---------------------------------------------------------")
  (println "                MEDIEVAL ALIEN MASSACRE")
  (println "A sadistic, pro-death text-based adventure for children")
  (println "---------------------------------------------------------\n"))

(defn mam []
  "Game initializer. Welcomes user and starts loop."
  (print-welcome-message)
  (messages 0))
