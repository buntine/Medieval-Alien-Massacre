
; gameplay.clj
; Handles all aspects of gameplay including prompts,
; command parsing, saves, loads, etc.


(ns mam.gameplay
  (:use mam.rooms))


; The current room the player is in.
(def current-room 0)

; Declarations for some procedures I mention before they have been
; defined.
(declare messages)


(defn describe-room [room]
  "Prints a description of the current room"
  (println (first (nth rooms room))))

(defn verb-parse [verb-lst]
  "Calls the procedure identified by the first usable verb"
  false)

(defn command->list [s]
  "Translates the given string to a list"
  '())

(defn parse-input [s]
  "Parses the user input."
  (if (not (empty? s))
    (let [cmd (command->list s)]
      (println s)
      (if (false? (verb-parse cmd))
        (println "I don't understand that."))
      (messages))))

(defn messages []
  "Describes current room and prompts for user input."
  (describe-room current-room)
  (print "> ")
  (flush)
  (parse-input (read-line)))
