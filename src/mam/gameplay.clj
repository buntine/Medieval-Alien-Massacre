
; gameplay.clj
; Handles all aspects of gameplay including prompts,
; command parsing, saves, loads, etc.

(ns mam.gameplay
  (:use mam.commands)
  (:use mam.rooms)
  (:use [clojure.contrib.string :only (split join)])
  (:use [clojure.contrib.duck-streams :only (spit)]))


(def current-room (ref 0))         ; The current room the player is in.
(def visited-rooms (ref []))       ; The rooms that the player has visited.
(def inventory (ref []))           ; The players inventory of items.
(def credits (ref 0))              ; The players credits (aka $$$$).
(def ignore-words '(the that is to ; Words that should be ignored in commands.
                    fucking damn)) 

(defn mam-pr [s]
  "Prints a string per-character like the ancient terminals used to"
  (if (empty? s)
    (newline)
    (do
      (print (first s))
      (flush)
      (. Thread sleep 30)
      (recur (rest s)))))

; Maps user commands to the appropriate function.
(def cmd-verbs
  {'go cmd-go 'n cmd-north 'e cmd-east 's cmd-south 'w cmd-west
   'ne cmd-northeast 'se cmd-southeast 'sw cmd-southwest 'nw cmd-northwest
   'north cmd-north 'east cmd-east 'south cmd-south 'west cmd-west
   'northeast cmd-northeast 'southeast cmd-southeast 'southwest cmd-southwest
   'drop cmd-drop 'throw cmd-drop 'inventory cmd-inventory 'pull cmd-pull
   'northwest cmd-northwest 'help cmd-help 'take cmd-take 'get cmd-take
   'examine cmd-inspect 'inspect cmd-inspect 'look cmd-look 'quit cmd-quit
   'suicide cmd-quit 'bed cmd-bed 'sleep cmd-bed 'eat cmd-eat 'fuck cmd-fuck
   'rape cmd-fuck 'talk cmd-talk 'speak cmd-talk 'inv cmd-inventory
   'save cmd-save 'load cmd-load})
   
; Declarations for some procedures I mention before they have been
; defined.
(declare messages)


(defmacro do-true [& body]
  "Executes body in a do form and returns true"
  (cons 'do (into '(true) body)))

(defmacro dosync-true [& body]
  "Executes body in a dosync form and returns true"
  (cons 'dosync (into '(true) body)))


(defn set-current-room! [room]
  (dosync
    (ref-set current-room room)))

(defn in-inventory? [objnum]
  "Returns true if object assigned to 'objnum' is in players inventory"
  (boolean (some #{objnum} @inventory)))

(defn obj-weight [objnum]
  "Returns the weight assigned to the given object"
  ((object-details objnum) :weight))

(defn inventory-weight []
  "Returns the current weight of the players inventory"
  (if (empty? @inventory)
    0
    (reduce #(+ %1 (obj-weight %2)) @inventory)))

(defn kill-player []
  "Kills the player and ends the game"
  ; TODO: Implement.
  true)

(defn is-same-object? [objnum obj-sym]
  (= objnum (object-identifiers obj-sym)))

(defn describe-object ([objnum] (describe-object objnum :game))
  ([objnum context]
    "Returns the string which describes the given object"
    (str ((object-details objnum) context))))

(defn object-is? [objnum k]
  "Returns true is the object adheres to the given keyword"
  ((object-details objnum) k))

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
                                    (vec (filter #(not (= obj %)) (objects-in-room room))))))))

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
  (let [objnum (object-identifiers obj)]
    (if (or (not objnum) (not (room-has-object? @current-room objnum)))
      false
      (do-true
        (if (object-is? objnum :permanent)
          (mam-pr "You can't take that.")
          (if (> (+ (inventory-weight) (obj-weight objnum)) *total-weight*)
            (mam-pr "You cannot carry that much weight.")
            (dosync
              (alter inventory conj objnum)
              (take-object-from-room! @current-room objnum)
              (mam-pr "Taken..."))))))))

(defn drop-object! [obj]
  "Attempts to drop an object into the current room"
  (let [objnum (object-identifiers obj)]
    (if (or (not objnum) (not (in-inventory? objnum)))
      false
      (dosync-true
        (remove-object-from-inventory! objnum)
        (drop-object-in-room! @current-room objnum)
        (mam-pr "Dropped...")))))

(defn inspect-object [obj]
  "Attempts to inspect an object in the current room"
  (let [objnum (object-identifiers obj)]
    (if (or (not objnum) (not (room-has-object? @current-room objnum)))
      false
      (do-true
        (mam-pr (describe-object objnum :inspect))))))

(defn fuck-object
  ([obj]
   "Attempts to fuck the given object."
   (let [objnum (object-identifiers obj)]
     (cond
       (or (not objnum) (not (room-has-object? @current-room objnum)))
         false
       (not (object-is? objnum :living))
         (do-true 
           (mam-pr (str "You start fucking the " obj " but it just feels painful.")))
       :else
         (do-true
           (mam-pr "Hmm... I bet that felt pretty good!")))))
  {:ridiculous true})

(defn eat-object [obj]
  "Attempts to eat the given object"
  (let [objnum (object-identifiers obj)]
    (cond
      (or (not objnum) (not (in-inventory? objnum)))
        false
      (not (object-is? objnum :edible))
        (do
          (mam-pr (str "You force the " obj " into your throat and fucking die in pain."))
          (kill-player))
      :else
        (dosync-true
          (if (is-same-object? objnum 'candy)
            (do
              (mam-pr "You feel like you just ate crusty skin off Donald Trump's forehead. Although inside the wrapper there was an 'instant win' of 5 credits!")
              (alter credits + 5))
            (mam-pr "That tasted like a cold peice of shit..."))
          (remove-object-from-inventory! objnum)))))

(defn speech-for [objnum]
  "Some objects have things to say. This function will return the speech for
   the given object"
  (or ((object-details objnum) :speech)
      "Sorry, they have nothing to say at the moment."))

(defn talk-to-object [obj]
  "Attempts to talk to the given object"
  (let [objnum (object-identifiers obj)]
    (cond
      (or (not objnum) (not (room-has-object? @current-room objnum)))
        false
      (not (object-is? objnum :living))
        (do-true
          (mam-pr (str "The " obj " does not possess the ability to talk.")))
      :else
        (do-true
          (mam-pr (speech-for objnum))))))

(defn print-with-newlines
  ([lines] (print-with-newlines lines ""))
  ([lines prepend]
   "Prints a sequence of strings, separated by newlines. Only useful for side-effects"
   (if (not (empty? prepend))
     (mam-pr prepend))
   (mam-pr (str " - "
                 (join "\n - " lines)))))

(defn display-inventory []
  "Displays the players inventory"
  (let [descs (map #(describe-object % :inv) @inventory)]
    (if (not (empty? descs))
      (print-with-newlines descs "You currently have:")
      (mam-pr "Your inventory is currently empty."))
    (mam-pr (str "\nCREDITS: " @credits))))

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
       (mam-pr ((if verbose? first second) descs))
       (dosync
         (alter visited-rooms conj room)
         (mam-pr (first descs))))
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
        (do-true
          (f (rest verb-lst)))
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
        (mam-pr "I don't understand that."))
      (newline)
      (messages (not (= orig-room @current-room))))
    (messages false)))

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
  (let [game-state {:current-room @current-room
                    :inventory @inventory
                    :visited-rooms @visited-rooms
                    :credits @credits
                    :room-objects @room-objects}]
    (spit "savedata", game-state)))

(defn load-game! []
  "Loads all previously saved game data"
  (if (. (java.io.File. "savedata") exists)
    (let [game-state (load-file "savedata")]
      (dosync
        (ref-set current-room (game-state :current-room))
        (ref-set inventory (game-state :inventory))
        (ref-set visited-rooms (game-state :visited-rooms))
        (ref-set credits (game-state :credits))
        (ref-set room-objects (game-state :room-objects))))
    (mam-pr "No saved data data!")))
