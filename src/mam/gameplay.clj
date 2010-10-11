
; gameplay.clj
; Handles all aspects of gameplay including prompts,
; command parsing, saves, loads, etc.

(ns mam.gameplay
  (:use mam.rooms)
  (:use [clojure.contrib.string :only (split)]))


(def current-room 0)                     ; The current room the player is in.
(def visited-rooms [])                   ; The rooms that the player has visited.
(def ignore-verbs '(the that is fucking  ; Verbs that should be ignored in commands.
                    damn)) 

; Declarations for some procedures I mention before they have been
; defined.
(declare messages)


(defn describe-object [obj]
  "Returns the string which describes the given object (symbol)"
  (str " - " (first (object-descriptions (objects obj)))))

(defn describe-objects-for-room [room]
  "Prints a description for each object that's in the given room"
  (let [objs (room-objects room)
        descs (map describe-object objs)]
    (if (not (empty? objs))
      (println
        (reduce (fn [t o]
                  (str t (str "\n" o))) descs)))))

(defn describe-room [room]
  "Prints a description of the current room"
  (let [visited? (some #{room} visited-rooms)
        descs (rooms room)]
    (if visited?
      (println (second descs))
      (do
        (def visited-rooms (conj visited-rooms room))
        (println (first descs))))
    (describe-objects-for-room room)))

(defn verb-parse [verb-lst]
  "Calls the procedure identified by the first usable verb. Returns
   false if the command is not understood"
  ; TODO: Implement.
  false)

(defn command->seq [s]
  "Translates the given string to a sequence, removing ignored words"
  (let [verbs (split #"\s+" s)]
    (filter (fn [v]
              (not (some #{(symbol v)} ignore-verbs)))
            verbs)))

(defn parse-input [s]
  "Parses the user input"
  (if (not (empty? s))
    (let [cmd (command->seq s)]
      (if (false? (verb-parse cmd))
        (println "I don't understand that."))
      (newline)
      (messages))))

(defn messages []
  "Describes current room and prompts for user input"
  (describe-room current-room)
  (newline)
  (print "> ")
  (flush)
  (parse-input (read-line)))
