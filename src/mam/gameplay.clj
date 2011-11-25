
; gameplay.clj
; Handles all aspects of gameplay including prompts,
; command parsing, saves, loads, etc.

(ns mam.gameplay
  (:use mam.commands)
  (:use mam.story)
  (:use mam.compression)
  (:use [clojure.contrib.string :only (split join)])
  (:import (java.applet Applet))
  (:import (java.io File))
  (:import (java.net URL)))

; Declarations for some procedures I mention before they have been
; defined.
(declare messages)


(def current-room (ref 0))        ; Current room the player is in.
(def visited-rooms (ref []))      ; Rooms that the player has visited.
(def inventory (ref []))          ; Players inventory of items.
(def credits (ref 0))             ; Players credits (aka $$$).
(def milestones (ref #{}))        ; Players milestones. Used to track and manipulate story.
(def ignore-words '(that is the   ; Words that should be ignored in commands.
                    fucking damn)) 

(def game-options (ref {:retro true    ; Print to stdout with tiny pauses between characters.
                        :sound true})) ; Play sound during gameplay.

(defn set-option! [option value]
  "Sets one of the pre-defined game options. Assumes valid input."
  (dosync
    (alter game-options assoc option value)))

(defn valid-option? [option]
  "Returns true if option is valid game option."
  (let [opts (map key @game-options)]
    (boolean (some #{option} opts))))

; Plays audio from the specified URL.
(defn play-url [url-string]
  (.play (Applet/newAudioClip (URL. url-string))))

; Plays audio from the specified local file.
(defn play-file [file-name]
  (if (@game-options :sound)
    (let [absolute-name (.getAbsolutePath (File. file-name))
          url-string (str "file://" absolute-name)]
      (play-url url-string))))

(defn mam-pr
  ([s] (mam-pr s (if (@game-options :retro) 30 0)))
  ([s i]
   "Prints a string like the ancient terminals used to, sleeping for i ms per character"
   (if (< i 5)
     (println s)
     (do
       (doseq [c s]
         (print c)
         (flush)
         (. Thread sleep i))
       (newline)))))

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
   'save cmd-save 'load cmd-load 'give cmd-give 'put cmd-put 'in cmd-in
   'out cmd-out 'up cmd-up 'down cmd-down 'drink cmd-drink 'cut cmd-cut
   'stab cmd-cut 'set cmd-set 'settings cmd-set})
   
(defn set-current-room! [room]
  (dosync
    (ref-set current-room room)))

(defn in-inventory? [objnum]
  "Returns true if object assigned to 'objnum' is in players inventory"
  (boolean (some #{objnum} @inventory)))

(defn has-knife? []
  "Returns true if the player has a knife-like object"
  (some #((object-details %) :cutter) @inventory))

(defn obj-weight [objnum]
  "Returns the weight assigned to the given object"
  ((object-details objnum) :weight))

(defn inventory-weight []
  "Returns the current weight of the players inventory"
  (reduce + 0 (map obj-weight @inventory)))

(defn kill-player [reason]
  "Kills the player and ends the game"
  (play-file "media/kill.wav")
  (mam-pr (str "You were killed by: " reason))
  (cmd-quit false))

(defn event-for [objnum evt]
  "Returns either the value (usually a fn) assigned to the given event, or nil"
  (((object-details objnum) :events) evt))

(defn describe-object ([objnum] (describe-object objnum :game))
  ([objnum context]
    "Returns the string which describes the given object, or nil"
    (let [info ((object-details objnum) context)]
      (if info
        (str info)))))

(defn object-is? [objnum k]
  "Returns true is the object adheres to the given keyword"
  ((object-details objnum) k))

(defn objects-in-room ([] (objects-in-room @current-room))
  ([room]
   (nth @room-objects room)))

(defn room-has-object? [room objnum]
  "Returns true if the gien room currently houses the given object"
  (boolean (some #{objnum} (objects-in-room room))))

(letfn
  [(alter-room! [room changed]
     "Physically alters the contents of the given. Must be called from within
      a dosync form"
     (alter room-objects
            (fn [objs]
              (assoc-in objs [room] changed))))]

  (defn take-object-from-room! [room objnum]
    (alter-room! room (vec (remove #(= objnum %)
                                (objects-in-room room)))))

  (defn drop-object-in-room! [room objnum]
    (alter-room! room (conj (objects-in-room room) objnum))))

(defn remove-object-from-inventory! [objnum]
  "Physically removes an object from the players inventory. Must be called
   within a dosync form"
  (ref-set inventory (vec (remove #(= % objnum) @inventory))))
 
(defn take-object! [objnum]
  "Attempts to take an object from the current room. If the object
   has an event for :take, then it must return a boolean - if true,
   the object will be taken"
  (cond
    (object-is? objnum :permanent)
      (mam-pr "You can't take that.")
    (> (+ (inventory-weight) (obj-weight objnum)) *total-weight*)
      (mam-pr "You cannot carry that much weight. Try dropping something.")
    :else
      (let [evt (event-for objnum :take)]
        (if (or (nil? evt) (evt))
          (dosync
            (let [c ((object-details objnum) :credits)]
              ; If we are taking credits, just add them to the players wallet.
              (if (integer? c)
                (alter credits + c)
                (alter inventory conj objnum))
            (take-object-from-room! @current-room objnum)
            (mam-pr "Taken...")))))))

(defn drop-object! [objnum]
  "Attempts to drop an object into the current room. If the object
   has an event for :drop, then it must return a boolean - if true,
   the object will be dropped"
  (let [evt (event-for objnum :drop)]
    (if (or (nil? evt) (evt))
      (dosync
        (remove-object-from-inventory! objnum)
        (drop-object-in-room! @current-room objnum)
        (mam-pr "Dropped...")))))

(letfn
  [(give-or-put [evt objx objy err-msg]
     "Does give/put with objects x and y. E.g: give cheese to old man"
     (let [events (event-for objy evt)]
       (if (or (nil? events) (not (events objx)))
         (mam-pr err-msg)
         (dosync
           ((events objx))
           (remove-object-from-inventory! objx)))))]

  (defn give-object! [objx objy]
    (give-or-put :give objx objy "He/she/it cannot accept this item."))

  (defn put-object! [objx objy]
    (give-or-put :put objx objy "You cannot put this item here.")))

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
   "Attempts to fuck the given object"
   (if (not (object-is? objnum :living))
     (mam-pr (str "You start fucking away but it just feels painful."))
     (do
       (play-file "media/fuck.wav")
       (mam-pr "Hmm... I bet that felt pretty good!"))))
  {:ridiculous true})

(defn cut-object [objnum]
  "Attempts to cut the given object"
  (if (not (has-knife?))
    (mam-pr "You need a something sharp before you can cut this!")
    (let [evt (event-for objnum :cut)]
      (if (nil? evt)
        (mam-pr
          (if (object-is? objnum :living)
            (mam-pr "Wow, that must have hurt...")
            (mam-pr "Nothing seemed to happen.")))
        (if (string? evt) (mam-pr evt) (evt))))))

(defn eat-object! [objnum]
  "Attempts to eat the given object"
  (let [evt (event-for objnum :eat)]
    (if (nil? evt)
      (do
        (mam-pr (str "You force it into your throat and fucking die in pain."))
        (kill-player ((object-details objnum) :inv)))
      (dosync
        (play-file "media/eat.wav")
        (if (string? evt) (mam-pr evt) (evt))
        (remove-object-from-inventory! objnum)))))

(defn drink-object! [objnum]
  "Attempts to drink the given object"
  (let [evt (event-for objnum :drink)]
    (if (nil? evt)
      (mam-pr (str "It doesn't seem to be drinkable."))
      (dosync
        (play-file "media/drink.wav")
        (if (string? evt) (mam-pr evt) (evt))
        (remove-object-from-inventory! objnum)))))

(defn talk-to-object [objnum]
  "Attempts to talk to the given object"
  (if (not (object-is? objnum :living))
    (mam-pr (str "That item does not possess the ability to talk."))
    (let [evt (event-for objnum :speak)]
      (if (nil? evt)
        (mam-pr "Sorry, they have nothing to say at the moment.")
        (if (string? evt) (mam-pr evt) (evt))))))

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
  (let [objs (@room-objects room)]
    (if (not (empty? objs))
      (print-with-newlines
        (remove nil? (map describe-object objs))))))

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
  "Returns the prospective objects for the given verb.
   E.g: 'cheese' might mean objects 6 and 12 or object 9 or nothing."
  (let [objnums (object-identifiers verb)
        fns {:room #(room-has-object? @current-room %)
             :inventory in-inventory?}]
    (if (nil? objnums)
      '()
      (filter (fns context)
              (if (integer? objnums) #{objnums} objnums)))))

(defn highest-val [obj-counts]
  "Returns the key of the highest value in the given map. If no
   single highest value is available, returns a lazy seq of keys
   of the tied-highest. This is used during language parsing."
  (if (not (empty? obj-counts))
    (let [highest (apply max (vals obj-counts))
          matches (into {}
                        (filter #(-> % val (= highest))
                                obj-counts))]
      (if (= (count matches) 1)
        (key (first matches))
        (keys matches)))))

(defn deduce-object ([verbs context] (deduce-object verbs '() context))
  ([verbs realised context]
   "Attempts to realise a single object given a sequence of verbs and
    a context. This allows for the same term to identify multiple objects.
    Context must be either :room or :inventory"
   (if (empty? verbs)
     (highest-val (frequencies realised))
     (recur (rest verbs)
            (concat (prospects-for (first verbs) context) realised)
            context))))

(defn fn-for-command [cmd]
  "Returns the function for the given command verb, or nil"
  (if cmd (cmd-verbs cmd)))

(defn verb-parse [verb-lst]
  "Calls the procedure identified by the first usable verb. Returns
   false if the command is not understood"
  (let [f (fn-for-command (first verb-lst))
        verbs (rest verb-lst)]
    (if (empty? verb-lst)
      false
      (if f
        (and (f verbs) true)
        (recur verbs)))))

(defn command->seq [s]
  "Translates the given string to a sequence of symbols, removing ignored words"
  (let [verbs (split #"\s+" s)]
    (filter #(not (some #{%} ignore-words))
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
    (spit "savedata", (compress (str game-state)))))

(defn load-game! []
  "Loads all previously saved game data"
  (if (. (java.io.File. "savedata") exists)
    (let [game-state (load-string (decompress (slurp "savedata")))]
      (dosync
        (ref-set current-room (game-state :current-room))
        (ref-set inventory (game-state :inventory))
        (ref-set visited-rooms (game-state :visited-rooms))
        (ref-set credits (game-state :credits))
        (ref-set milestones (game-state :milestones))
        (ref-set room-objects (game-state :room-objects))))
    (mam-pr "No saved game data!")))
