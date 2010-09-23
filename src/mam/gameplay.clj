
; gameplay.clj
; Handles all aspects of gameplay including prompts,
; command parsing, saves, loads, etc.


(ns mam.gameplay
  (:use mam.rooms))

(defn describe-room [room]
  "Prints a description of the current room"
  (println (first (nth rooms room))))

(defn parse-input [s]
  "Parses the user input."
  )

(defn messages [room]
  "Describes current room and prompts for user input."
  (describe-room room)
  (print "> ")
  (flush)
  (parse-input (read-line)))
