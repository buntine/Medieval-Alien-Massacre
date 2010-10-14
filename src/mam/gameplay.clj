
; gameplay.clj
; Handles all aspects of gameplay including prompts,
; command parsing, saves, loads, etc.

(ns mam.gameplay
  (:use mam.rooms)
  (:use mam.commands)
  (:use [clojure.contrib.string :only (split)]))


(def current-room (ref 0))         ; The current room the player is in.
(def visited-rooms (ref []))       ; The rooms that the player has visited.
(def inventory (ref []))           ; The players inventory of items.
(def ignore-words '(the that is to ; Words that should be ignored in commands.
                    fucking damn)) 

; Maps user commands to the appropriate function.
(def cmd-verbs
  {'go cmd-go 'n cmd-north 'e cmd-east 's cmd-south 'w cmd-west
   'ne cmd-northeast 'se cmd-southeast 'sw cmd-southwest 'nw cmd-northwest
   'north cmd-north 'east cmd-east 'south cmd-south 'west cmd-west
   'northeast cmd-northeast 'southeast cmd-southeast 'southwest cmd-southwest
   'northwest cmd-northwest 'help cmd-help 'take cmd-take 'get cmd-take
   'inventory cmd-inventory})
   
; Declarations for some procedures I mention before they have been
; defined.
(declare messages)


(defn set-current-room [room]
  (dosync
    (ref-set current-room room)))

(defn take-object [obj]
  "Attempts to take an object from the current room."
  (let [opts (nth room-objects @current-room)
        obj-index (object-identifiers (symbol obj))]
    (if (or (not obj-index) (not (some #{obj-index} opts)))
      false
      (dosync
        (alter inventory conj obj-index)
        ; TODO: Remove object from room...
        (println "Taken...")
        true))))

(defn describe-object [objnum]
  "Returns the string which describes the given object (symbol)"
  (str " - " (first (object-descriptions objnum))))

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
  (let [visited? (some #{room} @visited-rooms)
        descs (rooms room)]
    (if visited?
      (println (second descs))
      (dosync
        (alter visited-rooms conj room)
        (println (first descs))))
    (describe-objects-for-room room)))

(defn fn-for-command [cmd]
  "Returns the function for the given command verb, or nil"
  (if cmd
    (cmd-verbs (symbol cmd))
    nil))

(defn verb-parse [verb-lst]
  "Calls the procedure identified by the first usable verb. Returns
   false if the command is not understood"
  (let [f (fn-for-command (first verb-lst))]
    (if (empty? verb-lst)
      false
      (if f
        (do
          (f (rest verb-lst))
           true)
        (verb-parse (rest verb-lst))))))

(defn command->seq [s]
  "Translates the given string to a sequence, removing ignored words"
  (let [verbs (split #"\s+" s)]
    (filter (fn [v]
              (not (some #{(symbol v)} ignore-words)))
            verbs)))

(defn parse-input [s]
  "Parses the user input"
  (if (not (empty? s))
    (let [cmd (command->seq s)
          orig-room @current-room]
      (if (false? (verb-parse cmd))
        (println "I don't understand that."))
      (newline)
      (messages (not (= orig-room @current-room))))))

(defn messages ([] (messages true))
  ([verbose]
   "Describes current room and prompts for user input"
   (when verbose
     (describe-room @current-room)
     (newline))
   (print "> ")
   (flush)
   (parse-input (read-line))))
