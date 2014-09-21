
; MEDIEVAL ALIEN MASSACRE.
; By Andrew Buntine, 2010-2014
; http://www.andrewbuntine.com
;
; core.clj
; Initialises the game and gets things moving.
; If running in a REPL, execute: (-main)

(ns mam.core
  (:gen-class)
  (:require [mam.util :as u])
  (:use mam.gameplay))

(defn -main []
  "Game initializer. Welcomes user and starts loop."
  (u/play-file "media/opening.wav")
  (u/print-welcome-message)
  (messages))
