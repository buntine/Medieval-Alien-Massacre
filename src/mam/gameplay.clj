
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
   'drop cmd-drop 'dump cmd-drop 'inventory cmd-inventory 'inspect cmd-inspect
   'examine cmd-inspect})
   
; Declarations for some procedures I mention before they have been
; defined.
(declare messages)


(defn set-current-room [room]
  (dosync
    (ref-set current-room room)))

(defn in-inventory? [obj-index]
  "Returns true if object assigned to 'obj-index' is in players inventory"
  (boolean (some #{obj-index} @inventory)))

(defn obj-weight [obj-index]
  "Returns the weight assigned to the given object"
  (nth (nth object-details obj-index) 3))

(defn inventory-weight []
  "Returns the current weight of the players inventory"
  (if (empty? @inventory)
    0
    (reduce #(+ %1 (obj-weight %2)) @inventory)))

(defn describe-object ([objnum] (describe-object objnum 'game))
  ([objnum context]
    "Returns the string which describes the given object (symbol)"
    (let [f ({'game first
              'inventory second
              'inspect #(second (rest %))} context)]
      (str (f (object-details objnum))))))

(defn take-object [obj]
  "Attempts to take an object from the current room"
  (let [opts (nth @room-objects @current-room)
        obj-index (object-identifiers obj)
        dotake (fn [objs]
                 (assoc-in objs [@current-room]
                           (filter #(not (= obj-index %)) opts)))]
    (if (or (not obj-index) (not (some #{obj-index} opts)))
      false
      (do
        (if (> (+ (inventory-weight) (obj-weight obj-index)) *total-weight*)
          (println "You cannot carry that much weight.")
          (dosync
            (alter inventory conj obj-index)
            (alter room-objects dotake)
            (println "Taken...")))
        true))))

(defn drop-object [obj]
  "Attempts to drop an object into the current room"
  (let [opts (nth @room-objects @current-room)
        obj-index (object-identifiers obj)
        dodrop (fn [objs]
                 (assoc-in objs [@current-room]
                           (conj opts obj-index)))]
    (if (or (not obj-index) (not (in-inventory? obj-index)))
      false
      (dosync
        (alter inventory (fn [i] (filter #(not (= % obj-index)) i)))
        (alter room-objects dodrop)
        (println "Dropped...")
        true))))

(defn inspect-object [obj]
  "Attempts to inspect an object in the current room"
  (let [opts (nth @room-objects @current-room)
        obj-index (object-identifiers obj)]
    (if (or (not obj-index) (not (some #{obj-index} opts)))
      false
      (do
        (println (describe-object obj-index 'inspect))
        true))))

(defn print-with-newlines [lines]
  "Prints a sequence of strings, separated by newlines. Only useful for side-effects"
  (println (str " - "
    (reduce #(str " - " %1 "\n" %2) lines))))

(defn display-inventory []
  "Displays the players inventory"
  (let [descs (map #(describe-object % 'inventory) @inventory)]
    (when (not (empty? descs))
      (println "You currently have:")
      (print-with-newlines descs))))

(defn describe-objects-for-room [room]
  "Prints a description for each object that's in the given room"
  (let [objs (@room-objects room)
        descs (map describe-object objs)]
    (if (not (empty? objs))
      (print-with-newlines descs))))

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

(defn messages ([] (messages true))
  ([verbose]
   "Describes current room and prompts for user input"
   (when verbose
     (describe-room @current-room)
     (newline))
   (print "> ")
   (flush)
   (parse-input (read-line))))
