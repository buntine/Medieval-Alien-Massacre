
; state.clj
; Handles all current game state mutation.

(ns mam.state
  (:use mam.util))

(def current-room (ref 0))             ; Current room the player is in.
(def visited-rooms (ref []))           ; Rooms that the player has visited.
(def inventory (ref []))               ; Players inventory of items.
(def credits (ref 0))                  ; Players credits (aka $$$).
(def milestones (ref #{}))             ; Players milestones. Used to track and manipulate story.
(def game-options (ref {:retro true    ; Print to stdout with tiny pauses between characters.
                        :sound true})) ; Play sound during gameplay.

; Specifies the verbs that users can identify an object with (a gun might
; be "gun", "weapon", etc). A set means that the given term may refer to
; multiple objects. The system will try to deduce the correct object when
; a command is entered. Each index corresponds to the same index in room-objects.
(def object-identifiers
    {'candy 0 'bar 0 'bed 1 'lever 2 'mag 3 'magazine 3 'porno 3 'boy 7
     'teenager 7 'keycard #{4 5 6} 'key #{4 5 6} 'man #{8 9 21 22 23} 'robot 10
     'green #{4 13} 'red #{5 12} 'brown 14 'silver 6 'bum 11 'potion #{12 13 14}
     'credits 18 'attendant 15 'woman 15 'salvika 16 'whisky 16 'becherovka 17
     'web 20 'knife 19 'small 19 'thin 22 'skinny 22 'fat 21 'paper 24 'book 25
     'stone 26 'rock 26})

; A vector containing the objects that each room contains when the game starts. Each index
; corresponds to the room as defined in 'rooms'.
(def room-objects
  (ref
    (vector
      [0 1]        ;0
      []           ;1
      [2]          ;2
      []           ;3
      []           ;4
      []           ;5
      []           ;6
      [7]          ;7
      []           ;8
      []           ;9
      [8]          ;10
      [9 10]       ;11
      [11]         ;12
      []           ;13
      []           ;14
      [12 13 14]   ;15
      [18]         ;16
      [15 16 17]   ;17
      []           ;18
      [20]         ;19
      []           ;20
      [21 22 26]   ;21
      []           ;22
      [23]         ;23
      []           ;24
      []           ;25
      [27]         ;26
      [25]         ;27
      [24]         ;28
      []           ;29
      [])))        ;30

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

(defn set-option! [option value]
  "Sets one of the pre-defined game options. Assumes valid input."
  (dosync
    (alter game-options assoc option value)))

(defn valid-option? [option]
  "Returns true if option is valid game option."
  (let [opts (map key @game-options)]
    (boolean (some #{option} opts))))

(defn set-current-room! [room]
  (dosync
    (ref-set current-room room)))

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
        (mam-pr  "You force it into your throat and fucking die in pain.")
        (kill-player ((object-details objnum) :inv)))
      (dosync
        (play-file "media/eat.wav")
        (if (string? evt) (mam-pr evt) (evt))
        (remove-object-from-inventory! objnum)))))

(defn drink-object! [objnum]
  "Attempts to drink the given object. The event must return a boolean value, if
   false then the side-effect will not occur (removal of item from game)."
  (let [evt (event-for objnum :drink)
        drink! #(dosync
                  (play-file "media/drink.wav")
                  (remove-object-from-inventory! objnum))]
    (if (nil? evt)
      (mam-pr "It doesn't seem to be drinkable.")
      (if (string? evt)
        (do (mam-pr evt) (drink!))
        (if (evt)
          (drink!))))))

(defn talk-to-object [objnum]
  "Attempts to talk to the given object"
  (if (not (object-is? objnum :living))
    (mam-pr "That item does not possess the ability to talk.")
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

(defn save-game! []
  "Saves the current game data into a file on the disk"
  (let [game-state {:current-room @current-room
                    :inventory @inventory
                    :visited-rooms @visited-rooms
                    :credits @credits
                    :milestones @milestones
                    :game-options @game-options
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
        (ref-set game-options (game-state :game-options))
        (ref-set room-objects (game-state :room-objects))))
    (mam-pr "No saved game data!")))

; Plays audio from the specified URL.
(defn play-url [url-string]
  (.play (Applet/newAudioClip (URL. url-string))))

; Plays audio from the specified local file.
(defn play-file [file-name]
  (if (@game-options :sound)
    (let [absolute-name (.getAbsolutePath (File. file-name))
          url-string (str "file://" absolute-name)]
      (play-url url-string))))

(defn kill-player [reason]
  "Kills the player and ends the game"
  (play-file "media/kill.wav")
  (mam-pr (str "You were killed by: " reason))
  (cmd-quit false))


