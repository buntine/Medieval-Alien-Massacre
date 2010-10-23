
; gameplay.clj
; Handles all aspects of gameplay including prompts,
; command parsing, saves, loads, etc.

(ns mam.gameplay
  (:use mam.commands)
  (:use mam.rooms)
  (:use [clojure.contrib.string :only (split join)]))


(def current-room (ref 0))         ; The current room the player is in.
(def visited-rooms (ref []))       ; The rooms that the player has visited.
(def inventory (ref []))           ; The players inventory of items.
(def credits (ref 0))              ; The players credits (aka $$$$).
(def ignore-words '(the that is to ; Words that should be ignored in commands.
                    fucking damn)) 

; Maps user commands to the appropriate function.
(def cmd-verbs
  {'go cmd-go 'n cmd-north 'e cmd-east 's cmd-south 'w cmd-west
   'ne cmd-northeast 'se cmd-southeast 'sw cmd-southwest 'nw cmd-northwest
   'north cmd-north 'east cmd-east 'south cmd-south 'west cmd-west
   'northeast cmd-northeast 'southeast cmd-southeast 'southwest cmd-southwest
   'drop cmd-drop 'throw cmd-drop 'inventory cmd-inventory 'pull cmd-pull
   'northwest cmd-northwest 'help cmd-help 'take cmd-take 'get cmd-take
   'examine cmd-inspect 'inspect cmd-inspect 'look cmd-look 'quit cmd-quit
   'suicide cmd-quit 'bed cmd-bed 'sleep cmd-bed 'eat cmd-eat})
   
; Declarations for some procedures I mention before they have been
; defined.
(declare messages)


(defn set-current-room! [room]
  (dosync
    (ref-set current-room room)))

(defn in-inventory? [obj-index]
  "Returns true if object assigned to 'obj-index' is in players inventory"
  (boolean (some #{obj-index} @inventory)))

(defn obj-weight [obj-index]
  "Returns the weight assigned to the given object"
  ((object-details obj-index) :weight))

(defn inventory-weight []
  "Returns the current weight of the players inventory"
  (if (empty? @inventory)
    0
    (reduce #(+ %1 (obj-weight %2)) @inventory)))

(defn kill-player []
  "Kills the player and ends the game"
  ; TODO: Implement.
  true)

(defn is-same-object? [obj-index obj-sym]
  (= obj-index (object-identifiers obj-sym)))

(defn describe-object ([obj-index] (describe-object obj-index :game))
  ([obj-index context]
    "Returns the string which describes the given object"
    (str ((object-details obj-index) context))))

(defn permanent-object? [obj-index]
  (true? ((object-details obj-index) :permanency)))

(defn edible-object? [obj-index]
  (true? ((object-details obj-index) :edible)))

(defn objects-in-room ([] (objects-in-room @current-room))
  ([room]
   (nth @room-objects room)))

(defn room-has-object? [room obj]
  "Returns true if the gien room currently houses the given object"
  (if (symbol? obj)
    (room-has-object? room (object-identifiers obj))
    (boolean (some #{obj} (objects-in-room room)))))

(defn take-object-from-room! [room obj]
  "Physically removes an object from the given room. Must be called from within
   a dosync form"
  (if (symbol? obj)
    (take-object-from-room! room (object-identifiers obj))
    (alter room-objects (fn [objs]
                          (assoc-in objs [room]
                                    (filter #(not (= obj %)) (objects-in-room room)))))))

(defn drop-object-in-room! [room obj]
  "Physically adds an object to the given room. Must be called from within
   a dosync form"
  (if (symbol? obj)
    (drop-object-in-room! room (object-identifiers obj))
    (alter room-objects (fn [objs]
                          (assoc-in objs [room]
                                    (conj (objects-in-room room) obj))))))

(defn remove-object-from-inventory! [obj]
  "Physically removes an object from the players inventory. Must be called
   within a dosync form"
  (if (symbol? obj)
    (remove-object-from-inventory! (object-identifiers obj))
    (alter inventory (fn [i] (filter #(not (= % obj)) i)))))
 
(defn take-object! [obj]
  "Attempts to take an object from the current room"
  (let [obj-index (object-identifiers obj)]
    (if (or (not obj-index) (not (room-has-object? @current-room obj-index)))
      false
      (do
        (if (permanent-object? obj-index)
          (println "You can't take that.")
          (if (> (+ (inventory-weight) (obj-weight obj-index)) *total-weight*)
            (println "You cannot carry that much weight.")
            (dosync
              (alter inventory conj obj-index)
              (take-object-from-room! @current-room obj-index)
              (println "Taken..."))))
        true))))

(defn drop-object! [obj]
  "Attempts to drop an object into the current room"
  (let [obj-index (object-identifiers obj)]
    (if (or (not obj-index) (not (in-inventory? obj-index)))
      false
      (dosync
        (remove-object-from-inventory! obj-index)
        (drop-object-in-room! @current-room obj-index)
        (println "Dropped...")
        true))))

(defn inspect-object [obj]
  "Attempts to inspect an object in the current room"
  (let [obj-index (object-identifiers obj)]
    (if (or (not obj-index) (not (room-has-object? @current-room obj-index)))
      false
      (do
        (println (describe-object obj-index :inspect))
        true))))

(defn eat-object [obj]
  "Attempts to eat the given object"
  (let [obj-index (object-identifiers obj)]
    (cond
      (or (not obj-index) (not (in-inventory? obj-index)))
        false
      (not (edible-object? obj-index))
        (do
          (println (str "You force the " obj " into your throat and fucking die in pain."))
          (kill-player))
      :else
        (dosync
          (if (is-same-object? obj-index 'candy)
            (do
              (println "You feel like you just ate crusty skin off Donald Trump's forehead. Although inside the wrapper there was an 'instant win' of 5 credits!")
              (alter credits + 5))
            (println "That tasted like a cold peice of shit..."))
          (remove-object-from-inventory! obj-index)
          true))))

(defn print-with-newlines
  ([lines] (print-with-newlines lines ""))
  ([lines prepend]
   "Prints a sequence of strings, separated by newlines. Only useful for side-effects"
   (if (not (empty? prepend))
     (println prepend))
   (println (str " - "
                 (join "\n - " lines)))))

(defn display-inventory []
  "Displays the players inventory"
  (let [descs (map #(describe-object % :inv) @inventory)]
    (if (not (empty? descs))
      (print-with-newlines descs "You currently have:")
      (println "Your inventory is currently empty."))
    (println (str "\nCREDITS: " @credits))))

(defn describe-objects-for-room [room]
  "Prints a description for each object that's in the given room"
  (let [objs (@room-objects room)
        descs (map describe-object objs)]
    (if (not (empty? objs))
      (print-with-newlines descs))))

(defn describe-room ([room] (describe-room room false))
  ([room verbose?]
   "Prints a description of the current room"
   (let [visited? (some #{room} @visited-rooms)
         descs (rooms room)]
     (if visited?
       (println ((if verbose? first second) descs))
       (dosync
         (alter visited-rooms conj room)
         (println (first descs))))
     (describe-objects-for-room room))))

(defn fn-for-command [cmd]
  "Returns the function for the given command verb, or nil"
  (if cmd
    (cmd-verbs cmd)
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
  "Translates the given string to a sequence of symbols, removing ignored words"
  (let [verbs (split #"\s+" s)]
    (filter (fn [v] (not (some #{v} ignore-words)))
            (map symbol verbs))))

(defn parse-input [s]
  "Parses the user input"
  (if (not (empty? s))
    (let [cmd (command->seq s)
          orig-room @current-room]
      (if (false? (verb-parse cmd))
        (println "I don't understand that."))
      (newline)
      (messages (not (= orig-room @current-room))))))

(defn request-command []
  "Sends a terminal escape sequence (green text), reads a command, and then resets the colour"
  (print "\033[1;32m")
  (flush)
  (let [cmd (read-line)]
    (flush)
    (print "\033[0m")
    cmd))

(defn messages ([] (messages true))
  ([verbose]
   "Describes current room and prompts for user input"
   (when verbose
     (describe-room @current-room)
     (newline))
   (print "> ")
   (flush)
   (parse-input (request-command))))

(defn save-game! []
  "Saves the current game data into a file on the disk"
  ; TODO: Implement
  ; State: current-room, inventory, visited-rooms, credits, room-objects
  )

(defn load-game! []
  "Loads all previously saved game data"
  ; TODO: Implement
  )
