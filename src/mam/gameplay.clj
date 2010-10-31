
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
(def credits (ref 0))              ; The players credits (aka $$$).
(def milestones (ref #{}))         ; The players milestones. Used to track and manipulate story.
(def ignore-words '(the that is to ; Words that should be ignored in commands.
                    fucking damn in)) 

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
   'save cmd-save 'load cmd-load 'give cmd-give 'put cmd-put})
   
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

(defn object-identifier [obj]
  "Returns the number that identifies the given object symbol. Some objects
   can be identified differently depending on which room the player is in"
  (let [ident (object-identifiers obj)]
    (if (map? ident)
      (ident @current-room)
      ident)))

(defn in-inventory? [objnum]
  "Returns true if object assigned to 'objnum' is in players inventory"
  (boolean (some #{objnum} @inventory)))

(defn is-keycard? [objnum]
  (boolean (some #{objnum} (vals keycards))))

(defn obj-weight [objnum]
  "Returns the weight assigned to the given object"
  ((object-details objnum) :weight))

(defn inventory-weight []
  "Returns the current weight of the players inventory"
  (if (empty? @inventory)
    0
    (reduce + (map obj-weight @inventory))))

(defn kill-player [reason]
  "Kills the player and ends the game"
  (mam-pr (str "You were killed by: " reason))
  (cmd-quit false))

(defn is-same-object? [objnum obj-sym]
  (= objnum (object-identifier obj-sym)))

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
    (room-has-object? room (object-identifier obj))
    (boolean (some #{obj} (objects-in-room room)))))

(defn take-object-from-room! [room obj]
  "Physically removes an object from the given room. Must be called from within
   a dosync form"
  (if (symbol? obj)
    (take-object-from-room! room (object-identifier obj))
    (alter room-objects (fn [objs]
                          (assoc-in objs [room]
                                    (vec (remove #(= obj %) (objects-in-room room))))))))

(defn drop-object-in-room! [room obj]
  "Physically adds an object to the given room. Must be called from within
   a dosync form"
  (if (symbol? obj)
    (drop-object-in-room! room (object-identifier obj))
    (alter room-objects (fn [objs]
                          (assoc-in objs [room]
                                    (conj (objects-in-room room) obj))))))

(defn remove-object-from-inventory! [obj]
  "Physically removes an object from the players inventory. Must be called
   within a dosync form"
  (if (symbol? obj)
    (remove-object-from-inventory! (object-identifier obj))
    (ref-set inventory (vec (remove #(= % obj) @inventory)))))
 
(defn take-object! [objnum]
  "Attempts to take an object from the current room"
  (cond
    (object-is? objnum :permanent)
      (mam-pr "You can't take that.")
    (> (+ (inventory-weight) (obj-weight objnum)) *total-weight*)
      (mam-pr "You cannot carry that much weight.")
    :else
      (dosync
        (alter inventory conj objnum)
        (take-object-from-room! @current-room objnum)
        (mam-pr "Taken..."))))

(defn drop-object! [objnum]
  "Attempts to drop an object into the current room"
  (dosync
    (remove-object-from-inventory! objnum)
    (drop-object-in-room! @current-room objnum)
    (mam-pr "Dropped...")))

(defn event-for [objnum evt]
  "Returns either the value (usually a fn) assigned to the given event, or nil"
  (((object-details objnum) :events) evt))

(letfn
  [(do-x-with-y [x y evt err-msg]
     "Attempts to do something with x for y. E.g: give dildo to wizard, put dildo in wizard"
     (let [x-obj (object-identifier x) y-obj (object-identifier y)]
       (cond
         (or (not x-obj) (not (in-inventory? x-obj)))
           (mam-pr "Sorry, you don't have that item.")
         (or (not y-obj) (not (room-has-object? @current-room y-obj)))
           (mam-pr (str "I don't see " y " here."))
         :else
           (let [event-fn ((event-for y-obj evt) x-obj)]
             (if (nil? event-fn)
               (mam-pr err-msg)
               (dosync
                 (event-fn)
                 (remove-object-from-inventory! x)))))))]

  (defn give-object! [x y]
    (do-x-with-y x y :give (str "The " y " cannot accept this item.")))

  (defn put-object! [x y]
    (do-x-with-y x y :put (str "You cannot put the " y " here."))))

(defn inspect-object [objnum]
  "Inspects an object in the current room"
  (mam-pr (describe-object objnum :inspect)))

(defn can-afford? [n]
  "Returns true if the player can afford the given price"
  (>= @credits n))

(defn hit-milestone? [m]
  "Returns true if the player has hit the given, named milestone"
  (contains? @milestones m))

(defn add-milestone! [m]
  "Adds the given milestone to the players list"
  (dosync
    (alter milestones conj m)))

(defn fuck-object
  ([objnum]
   "Attempts to fuck the given object."
   (if (not (object-is? objnum :living))
     (mam-pr (str "You start fucking away but it just feels painful."))
     (mam-pr "Hmm... I bet that felt pretty good!")))
  {:ridiculous true})

(defn eat-object! [objnum]
  "Attempts to eat the given object"
  (let [eat-fn (event-for objnum :eat)]
    (if (nil? eat-fn)
      (do
        (mam-pr (str "You force it into your throat and fucking die in pain."))
        (kill-player (str "Trying to eat: " ((object-details objnum) :inv))))
      (dosync
        (eat-fn)
        (remove-object-from-inventory! objnum)))))

(defn talk-to-object [objnum]
  "Attempts to talk to the given object"
  (if (not (object-is? objnum :living))
    (mam-pr (str "That item does not possess the ability to talk."))
    (let [speech (event-for objnum :speak)]
      (cond
        (nil? speech)
          (mam-pr "Sorry, they have nothing to say at the moment.")
        (fn? speech)
          (speech)
        :else
          (mam-pr speech)))))

(defn pull-object [objnum]
  "Attempts to pull the given object (probably a lever)"
  (let [pull-evt (event-for objnum :pull)]
    (if (nil? pull-evt)
      (mam-pr "Nothing much seemed to happen.")
      (pull-evt))))

(defn print-with-newlines
  ([lines] (print-with-newlines lines ""))
  ([lines prepend]
   "Prints a sequence of strings, separated by newlines. Only useful for side-effects"
   (if (not (empty? prepend))
     (mam-pr prepend))
   (mam-pr (str " - " (join "\n - " lines)))))

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

(defn prospects-for [verb context]
  (let [objnums (object-identifiers verb)
        fns {:room #(room-has-object? @current-room %)
             :inventory in-inventory?}]
    (cond
      (nil? objnums)
        '()
      (integer? objnums)
        (list objnums)
      :else
        (filter (fns context) objnums))))

(defn highest-val [obj-counts]
  "Returns the key of the highest value in the given map. If no
   single highest value is available, returns a lazy seq of keys
   of the tied-highest"
  (if (not (empty? obj-counts))
    (let [highest (apply max (vals obj-counts))
          matches (filter #(= highest (second %)) obj-counts)]
      (if (= (count matches) 1)
        (first (first matches))
        (keys matches)))))

(defn add-or-inc [m k]
  "Either increments a keys value in m or create a key/value pair for it"
  (let [new-val (or (m k) 0)]
    (merge m {k (+ 1 new-val)})))

(defn most-popular ([objnums] (most-popular objnums {}))
  ([objnums obj-counts]
   "Given a list of object numbers, returns the one that occurs the most"
   (if (empty? objnums)
     (highest-val obj-counts)
     (recur (rest objnums)
            (add-or-inc obj-counts (first objnums))))))
    
(defn deduce-object ([verbs context] (deduce-object verbs '() context))
  ([verbs realised context]
   "Attempts to realise a single object given a sequence of verbs and
    a context. This allows for the same term to identify multiple objects.
    Context must be either :room or :inventory"
   (if (empty? verbs)
     (most-popular realised)
     (recur (rest verbs)
            (concat (prospects-for (first verbs) context) realised)
            context))))

(defn fn-for-command [cmd]
  "Returns the function for the given command verb, or nil"
  (if cmd (cmd-verbs cmd) nil))

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
                    :milestones @milestones
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
        (ref-set milestones (game-state :milestones))
        (ref-set room-objects (game-state :room-objects))))
    (mam-pr "No saved data data!")))
