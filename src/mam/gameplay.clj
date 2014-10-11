
; gameplay.clj
; Handles all aspects of gameplay including prompts,
; command parsing, saves, loads, etc.

(ns mam.gameplay
  (:require [mam.util :as u])
  (:require [mam.text :as t])
  (:use [clojure.string :only (join split)]))

; Declarations for some procedures I mention before they have been
; defined.
(declare messages object-details kill-player cmd-verbs cmd-look)

(def current-room (ref 0))             ; Current room the player is in.
(def visited-rooms (ref []))           ; Rooms that the player has visited.
(def inventory (ref []))               ; Players inventory of items.
(def credits (ref 0))                  ; Players credits (aka $$$).
(def milestones (ref #{}))             ; Players milestones. Used to track and manipulate story.
(def game-options (ref {:retro true    ; Print to stdout with tiny pauses between characters.
                        :sound true})) ; Play sound during gameplay.

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

; Maximum weight the user can carry at any one time.
(def total-weight 12)

; Words that should be ignored in commands.
(def ignore-words '(that is the fucking damn)) 

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

(defn text-speed []
  (if (@game-options :retro) 25 0))

(defn say 
  "Prints s to the game screen. If given a vector of strings, a random one will be chosen."
  [& {:keys [raw path speed]
       :or {speed (text-speed)}}]
   (if (nil? raw)
     (say
       :raw (apply t/text path) :speed speed)
     (if (vector? raw)
       (say
         :raw (rand-nth raw) :speed speed)
       (u/mam-pr raw speed))))

(defn objects-in-room ([] (objects-in-room @current-room))
  ([room]
   (nth @room-objects room)))

(defn room-has-object? [room objnum]
  "Returns true if the gien room currently houses the given object"
  (boolean (some #{objnum} (objects-in-room room))))

(defn in-inventory? [objnum]
  "Returns true if object assigned to 'objnum' is in players inventory"
  (boolean (some #{objnum} @inventory)))

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

(defn display-inventory []
  "Displays the players inventory"
  (let [descs (map #(describe-object % :inv) @inventory)]
    (if (not (empty? descs))
      (u/print-with-newlines descs (text-speed) (t/text 'inventory 'have))
      (say :path '(inventory empty)))
    (say :raw (str (t/text 'inventory 'credits) @credits))))

(defn describe-objects-for-room [room]
  "Prints a description for each object that's in the given room"
  (let [objs (@room-objects room)]
    (if (not (empty? objs))
      (u/print-with-newlines
        (remove nil? (map describe-object objs)) (text-speed)))))

(defn describe-room ([room] (describe-room room false))
  ([room verbose?]
   "Prints a description of the current room"
   (let [visited? (some #{room} @visited-rooms)
         descs (t/rooms room)]
     (if visited?
       (say :raw ((if verbose? first second) descs))
       (dosync
         (alter visited-rooms conj room)
         (say :raw (first descs))))
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
      (say :path '(commands cant-take))
    (> (+ (inventory-weight) (obj-weight objnum)) total-weight)
      (say :path '(commands no-space))
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
            (say :path '(commands taken))))))))

(defn drop-object! [objnum]
  "Attempts to drop an object into the current room. If the object
   has an event for :drop, then it must return a boolean - if true,
   the object will be dropped"
  (let [evt (event-for objnum :drop)]
    (if (or (nil? evt) (evt))
      (dosync
        (remove-object-from-inventory! objnum)
        (drop-object-in-room! @current-room objnum)
        (say :path '(commands dropped))))))

(letfn
  [(give-or-put [evt objx objy err-msg]
     "Does give/put with objects x and y. E.g: give cheese to old man"
     (let [events (event-for objy evt)]
       (if (or (nil? events) (not (events objx)))
         (say :raw err-msg)
         (dosync
           ((events objx))
           (remove-object-from-inventory! objx)))))]

  (defn give-object! [objx objy]
    (give-or-put :give objx objy (t/text 'commands 'give-error)))

  (defn put-object! [objx objy]
    (give-or-put :put objx objy (t/text 'commands 'put-error))))

(defn inspect-object [objnum]
  "Inspects an object in the current room"
  (say :raw (describe-object objnum :inspect)))

(defn fuck-object
  ([objnum]
   "Attempts to fuck the given object"
   (if (not (object-is? objnum :living))
     (say :path '(commands fuck-object))
     (do
       (if (@game-options :sound)
         (u/play-file "media/fuck.wav"))
       (say :path '(commands fuck-living)))))
  {:ridiculous true})

(defn cut-object [objnum]
  "Attempts to cut the given object"
  (if (not (has-knife?))
    (say :path '(commands no-knife))
    (let [evt (event-for objnum :cut)]
      (if (nil? evt)
        (if (object-is? objnum :living)
          (say :path '(commands cut-living))
          (say :path '(commands cut-object)))
        (if (string? evt) (say :raw evt) (evt))))))

(defn eat-object! [objnum]
  "Attempts to eat the given object"
  (let [evt (event-for objnum :eat)]
    (if (nil? evt)
      (do
        (say :path '(commands do-not-eat))
        (kill-player ((object-details objnum) :inv)))
      (dosync
        (if (@game-options :sound)
          (u/play-file "media/eat.wav"))
        (if (string? evt) (say :raw evt) (evt))
        (remove-object-from-inventory! objnum)))))

(defn drink-object! [objnum]
  "Attempts to drink the given object. The event must return a boolean value, if
   false then the side-effect will not occur (removal of item from game)."
  (let [evt (event-for objnum :drink)
        drink! #(dosync
                  (if (@game-options :sound)
                    (u/play-file "media/drink.wav"))
                  (remove-object-from-inventory! objnum))]
    (if (nil? evt)
      (say :path '(commands cannot-drink))
      (if (string? evt)
        (do (say :raw evt) (drink!))
        (if (evt)
          (drink!))))))

(defn talk-to-object [objnum]
  "Attempts to talk to the given object"
  (if (not (object-is? objnum :living))
    (say :path '(commands cannot-talk))
    (let [evt (event-for objnum :speak)]
      (if (nil? evt)
        (say :path '(commands speechless))
        (if (string? evt) (say :raw evt) (evt))))))

(defn pull-object [objnum]
  "Attempts to pull the given object (probably a lever)"
  (let [pull-evt (event-for objnum :pull)]
    (if (nil? pull-evt)
      (say :path '(commands cannot-pull))
      (pull-evt))))

; Functions to execute when player speaks to a given object.
(def speech-fn-for
  {:pod-manager
     #(cond
        (not (can-afford? 3))
          (say :path '(talk pod-manager broke))
        (not (hit-milestone? :speak-to-captain))
          (say :path '(talk pod-manager not-ready))
        :else
          (dosync
            (say :path '(talk pod-manager ready))
            (say :path '(talk pod-manager flying)
                 :speed 300)
            (alter credits - 3)
            (set-current-room! 12))),
   :repairs-captain
     #(if (hit-milestone? :speak-to-captain)
        (say :path '(talk repairs-captain finished))
        (do
          (doseq [x '(a b c d e)]
            (say :path (concat '(talk repairs-captain spiel) [x])))
          (add-milestone! :speak-to-captain))),
   :homeless-bum
     #(say :path '(talk homeless-bum))})

; Functions to execute when player gives a particular X to a Y.
(def give-fn-for
  {:porno-to-boy
     #(dosync
        (say :path '(give porno-to-boy))
        (take-object-from-room! @current-room 7)
        (drop-object-in-room! @current-room 4)),
   :whisky-to-bum
     #(if (not (hit-milestone? :alcohol-to-bum))
        (dosync
          (say :path '(give whisky-to-bum))
          (alter inventory conj 19)
          (add-milestone! :alcohol-to-bum))
        (say :path '(give alcohol-to-bum))),
   :becherovka-to-bum
     #(if (not (hit-milestone? :alcohol-to-bum))
        (dosync
          (say :path '(give becherovka-to-bum))
          (alter inventory conj 19)
          (add-milestone! :alcohol-to-bum))
        (say :path '(give alcohol-to-bum)))})

; Functions to execute when player eats particular objects.
(def eat-fn-for
  {:eats-candy
     #(dosync
        (say :path '(eat candy))
        (alter credits + 5))})

; Functions to execute when player drinks particular objects.
(def drink-fn-for
  {:red-potion
     #(do
        (say :path '(drink red-potion))
        (kill-player "Red potion")),
   :green-potion
     #(do
        (say :path '(drink green-potion))
        (add-milestone! :drinks-green-potion)
        true),
   :brown-potion
     #(do
        (say :path '(drink brown-potion a))
        (say :path '(drink brown-potion b) :speed 250)
        true)
   :salvika-whisky
     #(if (in-inventory? 17)
        (do (say :path '(drink whisky success)) true)
        (do (say :path '(drink whisky fail)) false))
   :becherovka
     #(if (in-inventory? 16)
        (do (say :path '(drink becherovka success)) true)
        (do (say :path '(drink becherovka fail)) false))})

; Functions to execute when player pulls particular objects.
(def pull-fn-for
  {:control-lever
     #(dosync
        (say :path '(pull control-lever))
        (take-object-from-room! @current-room 2)
        (drop-object-in-room! @current-room 3)
        (set-current-room! 0))})

; Functions to execute when player cuts particular objects.
(def cut-fn-for
  {:spider-web
     #(dosync
        (say :path '(cut spider-web))
        (take-object-from-room! @current-room 20))})
 
; Functions to execute when player takes particular objects.
(def take-fn-for
  {:salvika-whisky
     #(if (can-afford? 3)
        (dosync
          (alter credits - 3)
          true)
        (do
          (say :path '(take whisky))
          (kill-player "Rusty knife to the throat"))),
    :becherovka
      #(if (can-afford? 4)
        (dosync
          (alter credits - 4)
          true)
        (do
          (say :path '(take becherovka))
          (kill-player "Acid to the brain")))
    :paper
      (fn []
        (say :pth '(take paper))
        true)})

;(defn make-dets [id details]
;  "A helper function to merge in some sane defaults for object details"
;  (let [defaults {:game nil, :inv nil, :weight 0, :edible false, :permanent false,
;                  :living false, :events {}, :credits nil}]
;    (merge defaults details)))

(defn make-dets [id details]
  "A helper function to merge in some sane defaults for object details"
  (letfn
    [(text-path [basename]
       (t/text 'objects id basename))]

    (let [defaults {:weight 0, :edible false, :permanent false,
                    :living false, :events {}, :credits nil,
                    :game (text-path 'game), :inv (text-path 'inv),
                    :inspect (text-path 'inspect)}]
      (merge defaults details))))

; The details of all objects. Each object is assigned one or more numbers in object-identifiers,
; which corresponds to it's index here. Permanent object cannot be taken and thus don't require
; weights or inventory descriptions. Events, such as :eat, :drink, :speak, :give, :take and :put
; can be assigned and will be executed in the correct contexts.
; TODO: Refector. Do not use indexes to identify objects.
(def object-details
  (vec (map
    #(apply make-dets %)
    (vector
      ['candy-bar
       {:weight 1
        :events {:eat (eat-fn-for :eats-candy)}}]
      ['small-bed
       {:permanent true}]
      ['large-lever
       {:events {:pull (pull-fn-for :control-lever)}
         :permanent true}]
      ['porno-mag
       {:weight 2}]
      ['green-keycard
       {:weight 1}]
      ['red-keycard
       {:weight 1}]
      ['silver-keycard
       {:weight 1}]
      ['alien-boy 
       {:permanent true
        :events {:give {3 (give-fn-for :porno-to-boy)},
                 :speak (t/text 'objects 'alien-boy 'speak)}
        :living true}]
      ['pod-manager
       {:permanent true
        :events {:speak (speech-fn-for :pod-manager)}
        :living true}]
      ['repairs-captain
       {:permanent true
        :events {:speak (speech-fn-for :repairs-captain)}
        :living true}]
      ['small-robot
       {:permanent true
        :events {:speak (t/text 'objects 'small-robot 'speak)}
        :living true}]
      ['homeless-bum 
       {:events {:speak (speech-fn-for :homeless-bum)
                 :give {16 (give-fn-for :whisky-to-bum)
                        17 (give-fn-for :becherovka-to-bum)}}
        :permanent true
        :living true}]
      ['red-potion
       {:events {:drink (drink-fn-for :red-potion)}
        :weight 1}]
      ['green-potion
       {:events {:drink (drink-fn-for :green-potion)}
        :weight 1}]
      ['brown-potion 
       {:events {:drink (drink-fn-for :brown-potion)}
        :weight 1}]
      ['shop-att-a
       {:permanent true
        :events {:speak (t/text 'objects 'shop-att-a 'speak)}
        :living true}]
      ['salvika 
       {:events {:drink (drink-fn-for :salvika-whisky)
                 :take (take-fn-for :salvika-whisky)}
        :weight 2}]
      ['becherovka
       {:events {:drink (drink-fn-for :becherovka)
                 :take (take-fn-for :becherovka)}
        :weight 2}]
      ['five-credits
       {:credits 5}]
      ['small-knife
       {:cutter true
        :weight 2}]
      ['spider-web
       {:events {:cut (cut-fn-for :spider-web)}
        :permanent true}]
      ['fat-protester
       {:permanent true
        :living true
        :events {:speak (t/text 'objects 'fat-protester 'speak)}}]
      ['thin-protester
       {:permanent true
        :living true
        :events {:speak (t/text 'objects 'thin-protester 'speak)}}]
      ['gentle-old-man
       {:permanent true
        :living true
        :events {:speak (t/text 'objects 'gentle-old-man 'speak)}}]
      ['paper-a
       {:weight 1
        :events {:take (take-fn-for :paper)}}]
      ['book-a
       {:weight 2}]
      ['medium-stone
       {:weight 3}]
      ['wet-floor
       {:permanent true}]
      ['staircase-a
       {:permanent true}]))))

(print object-details)

(defn save-game! []
  "Saves the current game data into a file on the disk"
  (let [game-state {:current-room @current-room
                    :inventory @inventory
                    :visited-rooms @visited-rooms
                    :credits @credits
                    :milestones @milestones
                    :game-options @game-options
                    :room-objects @room-objects}]
    (spit "savedata", (with-out-str (pr game-state)))))

(defn load-game! []
  "Loads all previously saved game data"
  (if (. (java.io.File. "savedata") exists)
    (let [game-state (read-string (slurp "savedata"))]
      (dosync
        (ref-set current-room (game-state :current-room))
        (ref-set inventory (game-state :inventory))
        (ref-set visited-rooms (game-state :visited-rooms))
        (ref-set credits (game-state :credits))
        (ref-set milestones (game-state :milestones))
        (ref-set game-options (game-state :game-options))
        (ref-set room-objects (game-state :room-objects))))
    (say :raw "No saved game data!")))
  
(def directions {'north 0 'east 1 'south 2 'west 3 'northeast 4
                 'southeast 5 'southwest 6 'northwest 7 'up 8 'down 9
                 'in 10 'out 11})

(defn k [keynum room]
  "Returns a function that checks if the player has the given key. If they
   do, set the current room to 'room'. Otherwise, let them know"
  (fn []
    (if (in-inventory? keynum)
      (let [key-name (. ((object-details keynum) :inv) toLowerCase)]
        (set-current-room! room)
        (if (@game-options :sound)
          (u/play-file "media/door.wav"))
        (say :raw (str " * Door unlocked with " key-name " *")))
      (do
        (if (@game-options :sound)
          (u/play-file "media/fail.wav"))
        (say :raw "You don't have security clearance for this door!")))))

(defn o [objnum room]
  "Returns a function that checks if the given room houses the given object. If
   it does, the player cannot go in the given direction."
  (fn []
    (if (room-has-object? @current-room objnum)
      (say :raw "You can't go that way.")
      (set-current-room! room))))

(letfn
  [(library-trapdoor []
     (if (> (inventory-weight) 7)
       (dosync
         (say :path '(secret trapdoor))
         (take-object-from-room! @current-room 27)
         (drop-object-in-room! @current-room 28))))]

  (defn rc [i room]
    "Returns a function that performs the 'room check' (a named function) identified by i. The function should either return a number indicating the room to move the player to, or a false value, in which case the player will be sent to 'room'"
    (fn []
      (let [fnvec [library-trapdoor]
            new-room (or ((fnvec i)) room)]
        (set-current-room! new-room)))))

; Map to specify which rooms the player will enter on the given movement.
; A function indicates that something special needs to be done (check conditions, etc).
(def world-map
  (vector
;    north     east      south     west      ntheast   stheast   sthwest   nthwest   up        down      in        out
    [3         2         nil       nil       nil       nil       nil       nil       nil       nil       nil       nil]   ;0
    [4         nil       nil       2         nil       nil       nil       nil       nil       nil       nil       nil]   ;1
    [nil       1         nil       0         nil       nil       nil       nil       nil       nil       nil       nil]   ;2
    [nil       5         0         nil       nil       nil       nil       nil       nil       nil       nil       nil]   ;3
    [6         nil       1         7         nil       nil       nil       nil       nil       nil       nil       nil]   ;4
    [nil       7         nil       3         nil       nil       nil       nil       nil       nil       nil       nil]   ;5
    [(k 4 8)   nil       4         nil       nil       nil       nil       nil       nil       nil       (k 4 8)   nil]   ;6
    [nil       4         nil       5         nil       nil       nil       nil       nil       nil       nil       nil]   ;7
    [nil       nil       6         9         nil       nil       nil       11        nil       nil       nil       nil]   ;8
    [nil       8         nil       10        nil       nil       nil       nil       nil       nil       nil       nil]   ;9
    [nil       9         nil       nil       11        nil       nil       nil       nil       nil       nil       nil]   ;10
    [nil       nil       nil       nil       nil       8         10        nil       nil       nil       nil       nil]   ;11
    [nil       nil       14        nil       nil       nil       nil       nil       nil       nil       nil       nil]   ;12
    [nil       nil       nil       14        nil       nil       nil       nil       nil       nil       15        nil]   ;13
    [12        13        18        nil       nil       nil       nil       nil       nil       nil       nil       nil]   ;14
    [nil       nil       nil       nil       nil       nil       nil       nil       nil       nil       nil       13]    ;15
    [nil       nil       nil       18        nil       nil       nil       nil       nil       nil       17        nil]   ;16
    [nil       nil       nil       nil       nil       nil       nil       nil       nil       nil       nil       16]    ;17
    [14        16        nil       19        nil       nil       nil       nil       nil       nil       nil       nil]   ;18
    [nil       18        nil       (o 20 20) nil       nil       nil       nil       nil       nil       nil       nil]   ;19
    [21        19        nil       nil       nil       nil       nil       nil       nil       nil       nil       nil]   ;20
    [nil       nil       20        nil       nil       nil       nil       nil       nil       nil       23        nil]   ;21
    [25        24        23        (rc 0 26) nil       nil       nil       nil       nil       nil       nil       nil]   ;22
    [22        nil       nil       nil       nil       nil       nil       nil       nil       nil       nil       21]    ;23
    [nil       nil       nil       22        nil       nil       nil       nil       nil       nil       nil       nil]   ;24
    [27        28        22        29        nil       nil       nil       nil       nil       nil       nil       nil]   ;25
    [nil       22        nil       nil       nil       nil       nil       nil       nil       (o 27 30) nil       nil]   ;26
    [nil       nil       25        nil       nil       nil       nil       nil       nil       nil       nil       nil]   ;27
    [nil       nil       nil       25        nil       nil       nil       nil       nil       nil       nil       nil]   ;28
    [nil       25        nil       nil       nil       nil       nil       nil       nil       nil       nil       nil]   ;29
    [nil       nil       nil       nil       nil       nil       nil       nil       26        nil       nil       nil])) ;30

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
  (let [verbs (split s #"\s+")]
    (filter #(not (some #{%} ignore-words))
            (map symbol verbs))))

(defn parse-input [s]
  "Parses the user input"
  (if (not (empty? s))
    (let [cmd (command->seq s)
          orig-room @current-room]
      (if (false? (verb-parse cmd))
        (say :path '(parsing unknown)))
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

(letfn
  [(move-room [dir]
     "Attempts to move in the given direction."
     (let [i (directions dir)]
       (if (not i)
         (say :path '(parsing unknown-dir))
         (let [room ((world-map @current-room) i)]
           (if (nil? room)
             (say :path '(parsing wrong-dir))
             (if (fn? room)
               (room)
               (set-current-room! room)))))))]

  (defn cmd-go [verbs]
    "Expects to be given a direction. Dispatches to the 'move' command"
    (if (empty? verbs)
      (say :path '(parsing no-dir))
      ; Catch commands like "go to bed", etc.
      (if (u/direction? (first verbs))
        (move-room (first verbs))
        (parse-input (join " " (map name verbs))))))

  (defn cmd-north [verbs] (move-room 'north))
  (defn cmd-east [verbs] (move-room 'east))
  (defn cmd-south [verbs] (move-room 'south))
  (defn cmd-west [verbs] (move-room 'west))
  (defn cmd-northeast [verbs] (move-room 'northeast))
  (defn cmd-southeast [verbs] (move-room 'southeast))
  (defn cmd-southwest [verbs] (move-room 'southwest))
  (defn cmd-northwest [verbs] (move-room 'northwest))
  (defn cmd-in [verbs] (move-room 'in))
  (defn cmd-out [verbs] (move-room 'out))
  (defn cmd-up [verbs] (move-room 'up))
  (defn cmd-down [verbs] (move-room 'down)))

(defn cmd-commands [verbs]
  "Prints a line-delimited list of the commands the system understands."
  (let [commands (sort (map str (keys cmd-verbs)))]
    (doseq [c commands]
      (println c))))

(letfn
  [(set-on-off! [option state]
     (if (or (= state :on) (= state :off))
       (do
         (set-option! option (= state :on))
         (say :raw "Set..."))
       (say :path '(options error))))]

  (defn cmd-set [verbs]
    "Attempts to update the given game setting"
    (if (not (= (count verbs) 2))
      (letfn
        [(format-option [opt value]
           (str " - " (name opt) ": " (if value "On" "Off")))]
        (say :raw "Game options:\n")
        (say :raw (join
                  "\n"
                  (map #(apply format-option %)
                       @game-options))))
      (let [[opt state] (map keyword verbs)]
        (if (valid-option? opt)
          (set-on-off! opt state)
          (say :path '(options unknown)))))))

(letfn
  [(interact [verbs base mod-fn context]
     "Attempts to interact by realising an explicit object
      and doing something (mod-fn) with it"
     (letfn
       [(say-for-path [qualifier]
          (say :path
               (map symbol
                    ['commands (str base "-" qualifier)])))]

       (if (empty? verbs)
         (say-for-path "error")
         (let [objnum (deduce-object verbs context)]
           (cond
             (nil? objnum)
               (say-for-path "unknown")
             ; Specific object cannot be deduced, so ask for more info.
             (seq? objnum)
               (say :path '(commands interact-error))
             :else
               (mod-fn objnum))))))]

  (defn cmd-take [verbs]
    (interact verbs
              'take
              take-object!
              :room))

  (defn cmd-drop [verbs]
    (interact verbs
              'drop
              drop-object!
              :inventory))

  (defn cmd-inspect [verbs]
    (if (empty? verbs)
      (cmd-look)
      (interact verbs
                'inspect
                inspect-object
                :room)))

  (defn cmd-cut [verbs]
    (interact verbs
              'cut
              cut-object
              :room))

  (defn cmd-eat [verbs]
    (interact verbs
              'eat
              eat-object!
              :inventory))

  (defn cmd-drink [verbs]
    (interact verbs
              'drink
              drink-object!
              :inventory))

  (defn cmd-fuck [verbs]
    (let [v (first verbs)]
      (if (some #(= v %) '(you me off))
        (say :path (map symbol ['commands (str "fuck-" v)]))
        (interact verbs
                  'fuck
                  fuck-object
                  :room))))

  (defn cmd-talk [verbs]
    (interact verbs
              'talk
              talk-to-object
              :room))

  (defn cmd-pull [verbs]
    (interact verbs
              'pull
              pull-object
              :room)))

(defn cmd-look ([verbs] (cmd-inspect verbs))
  ([]
   "Prints a long description of a room"
   (describe-room @current-room true)))

(defn cmd-inventory [verbs]
  "Displays the players inventory"
  (display-inventory))

(defn cmd-quit [verbs]
  "Quits the game and returns user to terminal."
  (say :path '(commands quit))
  (flush)
  (. System exit 0))

(defn cmd-bed [verbs]
  (if (= @current-room 0)
    (say :path '(bed a))
    (say :path '(bed unknown))))

(letfn
  [(do-x-with-y [verbs action sep mod-fn]
     "Attempts to do x with y. Expects format of: '(action x sep y). E.g: give cheese to old man"
     (let [[x y] (split-with #(not (= % sep)) verbs)]
       (if (or (empty? x) (<= (count y) 1))
         (say :raw (str "Sorry, I only understand the format: " action " x " (name sep) " y"))
         (let [objx (deduce-object x :inventory)
               objy (deduce-object (rest y) :room)]
           (cond
             (nil? objx)
               (say :path '(commands do-error))
             (seq? objx)
               (say :raw (str "Please be more specific about the item you want to " action "."))
             (nil? objy)
               (say :path '(commands do-unknown))
             (seq? objy)
               (say :raw (str "Please be more specific about where/who you want to " action " it."))
             :else 
               (mod-fn objx objy))))))]

  (defn cmd-give [verbs]
    (do-x-with-y verbs 'give 'to give-object!))

  (defn cmd-put [verbs]
    (do-x-with-y verbs 'put 'in put-object!)))

(defn cmd-save [verbs]
  (save-game!)
  (say :raw " * Game saved *"))

(defn cmd-load [verbs]
  (load-game!)
  (say :raw " * Game loaded *"))

(defn cmd-help [verbs]
  (println "  M-A-M HELP")
  (println "  ------------------------------")
  (println "   * Directions are north, east, south, west, northeast, southeast, southwest, northeast, in, out, up, down.")
  (println "   * Or abbreviated n, e, s, w, ne, se, sw, nw.")
  (println "   * Keys automatically open the appropriate doors, so just walk in their direction.")
  (println "   * Type 'commands' to see a fat-ass list of the things I understand.")
  (println "   * You can go 'in' and 'out' of buildings if the action is appropriate.")
  (println "   * Credit is equivalent to our concept of money. Use it wisely!")
  (println "   * Check your items and credit with 'inventory' or 'inv'.")
  (println "   * You can 'speak' to humans, aliens and robots, but some may be a tad vulgar...")
  (println "   * You can 'save' and 'load' your game, mother fucker!")
  (println "   * You can 'give x to y' or 'put x in y' to solve many dubious mysteries.")
  (println "   * To end the game, type 'quit' or 'commit suicide' or forever dwell in green mess!")
  (println "   * Inspired by Dunnet, by Rob Schnell and Colossal Cave Adventure by William Crowther.")
  (println "   * Don't forget: Life is a game and everything is pointless.")
  (println "  ------------------------------"))

; Maps user commands to the appropriate function.
(def cmd-verbs
  {'go cmd-go 'n cmd-north 'e cmd-east 's cmd-south 'w cmd-west
   'ne cmd-northeast 'se cmd-southeast 'sw cmd-southwest 'nw cmd-northwest
   'north cmd-north 'east cmd-east 'south cmd-south 'west cmd-west
   'northeast cmd-northeast 'southeast cmd-southeast 'southwest cmd-southwest
   'drop cmd-drop 'throw cmd-drop 'inventory cmd-inventory 'pull cmd-pull
   'northwest cmd-northwest 'help cmd-help 'take cmd-take 'get cmd-take 'buy cmd-take
   'examine cmd-inspect 'inspect cmd-inspect 'look cmd-look 'quit cmd-quit
   'suicide cmd-quit 'bed cmd-bed 'sleep cmd-bed 'eat cmd-eat 'fuck cmd-fuck
   'rape cmd-fuck 'talk cmd-talk 'speak cmd-talk 'inv cmd-inventory
   'save cmd-save 'load cmd-load 'give cmd-give 'put cmd-put 'in cmd-in
   'out cmd-out 'enter cmd-in 'leave cmd-out 'up cmd-up 'down cmd-down
   'drink cmd-drink 'cut cmd-cut 'stab cmd-cut 'set cmd-set 'settings cmd-set
   'commands cmd-commands})

(defn messages ([] (messages true))
  ([verbose]
   "Describes current room and prompts for user input"
   (when verbose
     (describe-room @current-room)
     (newline))
   (print "> ")
   (flush)
   (parse-input (request-command))))

(defn kill-player [reason]
  "Kills the player and ends the game"
  (if (@game-options :sound)
    (u/play-file "media/kill.wav"))
  (say :raw (str "You were killed by: " reason))
  (cmd-quit false))
