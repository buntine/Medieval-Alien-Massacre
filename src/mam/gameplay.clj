
; gameplay.clj
; Handles all aspects of gameplay including prompts,
; command parsing, saves, loads, etc.

(ns mam.gameplay
  (:require [mam.util :as u])
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

; Maximum weight the user can carry at any one time.
(def total-weight 12)

; Words that should be ignored in commands.
(def ignore-words '(that is the fucking damn)) 

;;; DATA
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

;;; DATA
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

;;; DATA
; A vector of rooms. Each index contains both a large description (first visit) and a brief
; description (all subsequent visits).
(def rooms
  (vector
    '("You are in a small, silver-walled room with no windows. There is a door to the north labelled 'Repairs deck' and another door to the east."
      "Small, featureless room. Doors to north and east.")
    '("You are in another small, featureless room. There is nothing of interest here except doors to the north and west."
      "Small, featureless room. Doors to north and west.")
    '("You enter a control room with a few blank screens. There are doors to the east and west."
      "Control room with doors to east and west")
    '("There is a long row of broken flying machines here. A large sign reads 'Repairs deck: West end'. 'Where the fuck am I?' you think to yourself. The passage leads east. There is a door to the south."
      "West-end of the repairs deck. Passage leads east. Door to the south.")
    '("You walk into a hallway with doors to your west and south. The door to the west has a sign reading 'Repairs deck'. The hallway is leading north."
      "Hallway. Doors to the west and south. Passage leads north.")
    '("You continue along the passage and pass more broken machines. Passage leads east or west."
      "Repairs deck, center. Passage leads west/east.")
    '("You are at the end of the hallway. There is a large, sliding door in front of you."
      "End of hallway. Large door here.")
    '("There are a bunch of broken machines lying around on the repairs deck. There is a door to the east or a passage west."
      "Repairs deck. Door to the east and passage south.")
    '("You are in a large room with space age decor. It seems to be the central living quarters. The walls are lined with pictures of the late comedian, Bill Hicks. There are walkways to the west and northwest and a door to the south."
      "Central living quarters, walkways to west and northwest, door to south.")
    '("You can see some more framed pictures of Bill Hicks here. As you walk past them, Bills eyes seem to follow you. The passage goes west or east."
      "Passage with more creepy Bill Hicks pictures. Passage leads east/west.")
    '("You are at the west-end of the room. Here you can see sealed entrance and a sign saying 'Exit pod'. Passage goes north-east or east."
      "West-end of large room with exit pod. Passages north-east and east.")
    '("You are at the front of the large room. There is a huge glass-like window here and you can see now that you are, infact, travelling through space! There are passages going back southeast and southwest."
      "Front of large room with huge glass-like window. Passages southeast/southwest.")
    '("You are in a dark alley and there is rubbish lying around everywhere. There are solid walls behind you and to either side. The alley leads south."
      "Dead-end of alley. Passage leads south.")
    '("You are standing at the entrance of an obscure-looking shop. There are cryptic characters on the sign. You can go in or head back to the west."
      "Entrance to obscure shop. Dead-end.")
    '("You are faced with two paths - one to the east and one going south. Both are slimy and wet."
      "Alley way. Paths to south/north and also to the east")
    '("The shop has no attendant. You can see a bunch of empty viles, odd trinkets and another Bill Hicks portrait."
      "Unattended shop with crap lying around.")
    '("You are standing at the entrance of a grimy looking liquor store."
      "Grimy liquor store entrance. Passage goes west.")
    '("The shop is in disarray and is pretty much empty, although there are some things lying around."
      "Bottle shop with attendant.")
    '("You are at another corner. There are paths running both east and west, or back to the north."
      "Bottom of alley, passages to east/west or back north.")
    '("You are at the end of the alley way and you can see a street to your west."
      "End of alley, street to the west.")
    '("You are on a road. It doesn't seem to be used anymore, though. It runs both north and south."
      "Road with no vehicles, running north/south. Alley to the east.")
    '("You are at the entrance of a Library of Ancient Technology. You can go in or head back south."
      "Library of Ancient Technology. Go in or back south.")
    '("You are now into the actual library area (It's labelled Isle zero). There are rows of books to your east and west and further shelving to the north."
      "Isle zero. Shelves to east and west. Isle one to the north, exit to the south.")
    '("You are inside the library's foyer. You can see several rows of shelves to your north. This place does not look very popular. The exit is behind you."
      "Entrance of the library. Rows of shelves to the north or you can go out.")
    '("You are in Isle Zero-B, the Embedded Programming section. There are assorted books with titles like 'We are demigods', 'Mastery of C with UNT' and 'Embed this: A beginners quide to women'. There is nothing much here to see, though."
      "Isle Zero-B: Emedded Programming. Dead-end.")
    '("You are in Isle one. There are more shelves of books to your east and west. You can also go north or south."
      "Isle one. Shelving to east/west. You can go north/south.")
    '("You are in Isle Zero-A, the Web Development section. There are at least 799 bad books on JavaScript here. There is also a poster on the wall that displays a graph that seems to depict how PHP became worse as time went by. By 2087 it had implemented (poorly) every language feature known to man (and supposedly some creatures from Proxima Centauri)."
      "Isle Zero-A: Web Development. Dead-end.")
    '("You have arrived at the back-end of the library. You cannot go any further in this direction."
      "Back of library. It's a dead-end.")
    '("You are in Isle one-B, the functional programming section. There are ancient books lying around including gems like 'LISP in Small Peices', 'ML for the Working Programmer' and 'Revised^666 Report on the Algorithmic Language Scheme'."
      "Isle One-B: Functional programming. Dead-end.")
    '("You have arrived in Isle one-A, the logic/misc. programming section. There are some seriously odd books here including 'Forth for Jupiterians' and 'Prolog knows best'."
      "Isle one-A: Logic/Misc programming. Dead-end.")
    '("You are in a pitch black room. The only thing you can see is a glowing hologram of Bill Hicks. He smiles. The staircase leading upwards is behind you."
      "Pitch black room with Bill Hicks hologram. Stairs leading upwards.")))

(defn text-speed []
  (if (@game-options :retro) 25 0))

(defn say [s]
  "Prints s to the game screen"
  (u/mam-pr s (text-speed)))

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
      (u/print-with-newlines descs (text-speed) "You currently have:")
      (say "Your inventory is currently empty."))
    (say (str "\nCREDITS: " @credits))))

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
         descs (rooms room)]
     (if visited?
       (say ((if verbose? first second) descs))
       (dosync
         (alter visited-rooms conj room)
         (say (first descs))))
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
      (say "You can't take that.")
    (> (+ (inventory-weight) (obj-weight objnum)) total-weight)
      (say "You cannot carry that much weight. Try dropping something.")
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
            (say "Taken...")))))))

(defn drop-object! [objnum]
  "Attempts to drop an object into the current room. If the object
   has an event for :drop, then it must return a boolean - if true,
   the object will be dropped"
  (let [evt (event-for objnum :drop)]
    (if (or (nil? evt) (evt))
      (dosync
        (remove-object-from-inventory! objnum)
        (drop-object-in-room! @current-room objnum)
        (say "Dropped...")))))

(letfn
  [(give-or-put [evt objx objy err-msg]
     "Does give/put with objects x and y. E.g: give cheese to old man"
     (let [events (event-for objy evt)]
       (if (or (nil? events) (not (events objx)))
         (say err-msg)
         (dosync
           ((events objx))
           (remove-object-from-inventory! objx)))))]

  (defn give-object! [objx objy]
    (give-or-put :give objx objy "He/she/it cannot accept this item."))

  (defn put-object! [objx objy]
    (give-or-put :put objx objy "You cannot put this item here.")))

(defn inspect-object [objnum]
  "Inspects an object in the current room"
  (say (describe-object objnum :inspect)))

(defn fuck-object
  ([objnum]
   "Attempts to fuck the given object"
   (if (not (object-is? objnum :living))
     (say (str "You start fucking away but it just feels painful."))
     (do
       (if (@game-options :sound)
         (u/play-file "media/fuck.wav"))
       (say "Hmm... I bet that felt pretty good!"))))
  {:ridiculous true})

(defn cut-object [objnum]
  "Attempts to cut the given object"
  (if (not (has-knife?))
    (say "You need a something sharp before you can cut this!")
    (let [evt (event-for objnum :cut)]
      (if (nil? evt)
        (say
          (if (object-is? objnum :living)
            (say "Wow, that must have hurt...")
            (say "Nothing seemed to happen.")))
        (if (string? evt) (say evt) (evt))))))

(defn eat-object! [objnum]
  "Attempts to eat the given object"
  (let [evt (event-for objnum :eat)]
    (if (nil? evt)
      (do
        (say  "You force it into your throat and fucking die in pain.")
        (kill-player ((object-details objnum) :inv)))
      (dosync
        (if (@game-options :sound)
          (u/play-file "media/eat.wav"))
        (if (string? evt) (say evt) (evt))
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
      (say "It doesn't seem to be drinkable.")
      (if (string? evt)
        (do (say evt) (drink!))
        (if (evt)
          (drink!))))))

(defn talk-to-object [objnum]
  "Attempts to talk to the given object"
  (if (not (object-is? objnum :living))
    (say "That item does not possess the ability to talk.")
    (let [evt (event-for objnum :speak)]
      (if (nil? evt)
        (say "Sorry, they have nothing to say at the moment.")
        (if (string? evt) (say evt) (evt))))))

(defn pull-object [objnum]
  "Attempts to pull the given object (probably a lever)"
  (let [pull-evt (event-for objnum :pull)]
    (if (nil? pull-evt)
      (say "Nothing much seemed to happen.")
      (pull-evt))))

; Functions to execute when player speaks to a given object.
(def speech-fn-for
  {:pod-manager
     #(cond
        (not (can-afford? 3))
          (say "The man says 'Hey, I can get your sorry ass off this ship, but it will cost you 3 credits. Come back when you can afford it, matey'.")
        (not (hit-milestone? :speak-to-captain))
          (say "The man says 'Hey matey, I can get your sorry ass off here, but I suggest you speak to the captain over there to our northeast first'.")
        :else
          (dosync
            (say "The man says 'Oky doke, matey, lets get your punk ass outta' here. I hope Syndal City on Jupiter 4 is alright'.")
            (say "\n... flying to Syndal City ..." 300)
            (alter credits - 3)
            (set-current-room! 12))),
   :repairs-captain
     #(if (hit-milestone? :speak-to-captain)
        (say "The captain says 'That is all the information I have. Now, fuck off before I get mad.'.")
        (do
          (say "The man says 'Ahh, you're up! I am Bob Benson, the captain of this grand model T102 repairs vessel. We found you floating out there on the oxygenated stretch of galactic highway 7. Anyway, you look a tad confused, so let me refresh your memory:")
          (say "It is the year 2843, you're currently travelling on a highway between two of the moons of Jupiter.")
          (say "\n** At this point you explain that you are infact from the year 2011 and the last thing you remember is driking coffee at home and writing some LISP code **\n")
          (say "The captain says 'Oh, yes, it makes sense now. A true LISP hacker and drinker of the finest bean can transcend both space and time. We've seen your type before. You should head over to see the Pod Manager to our southwest in order to get yourself off this ship'")
          (say "Good luck out there, young man...")
          (add-milestone! :speak-to-captain))),
   :homeless-bum
     #(say "He mutters 'Hey mystery man! Welcome to Syndal City, perhaps you can spare an old cyborg some whisky?'.")})

; Functions to execute when player gives a particular X to a Y.
(def give-fn-for
  {:porno-to-boy
     #(dosync
        (say "The teenagers eyes explode!! He quickly accepts the porno mag and runs away. He throws a green keycard in your general direction as he leaves the room.")
        (take-object-from-room! @current-room 7)
        (drop-object-in-room! @current-room 4)),
   :whisky-to-bum
     #(if (not (hit-milestone? :alcohol-to-bum))
        (dosync
          (say "The old bum accepts the whisky and says 'Wow!! Thank you, cobba! Please, take this small knife in return, It may help to 'cut' things that lay in your path'. You, in turn, take the knife.")
          (alter inventory conj 19)
          (add-milestone! :alcohol-to-bum))
        (say "He accepts the alcohol, but just grumbles something about Common LISP in response")),
   :becherovka-to-bum
     #(if (not (hit-milestone? :alcohol-to-bum))
        (dosync
          (say "The old bum accepts the whisky and says 'Holy fuck, Becherovka! My favourite! Please, take this small knife in return, It may help to 'cut' things that lay in your path'. You, in turn, take the knife.")
          (alter inventory conj 19)
          (add-milestone! :alcohol-to-bum))
        (say "He accepts the alcohol, but just grumbles something about Emacs LISP in response"))})

; Functions to execute when player eats particular objects.
(def eat-fn-for
  {:eats-candy
     #(dosync
        (say "You feel like you just ate crusty skin off Donald Trump's forehead. Although inside the wrapper there was an 'instant win' of 5 credits!")
        (alter credits + 5))})

; Functions to execute when player drinks particular objects.
(def drink-fn-for
  {:red-potion
     #(do
        (say "Wow, that tasted great. Unfortunately, it also physically melted your brain and ended your life...")
        (kill-player "Red potion")),
   :green-potion
     #(do
        (say "You drink the potion and instantly start to feel strange. Without warning, your eyes begin to glow green! Luckily, you feel no pain.")
        (add-milestone! :drinks-green-potion)
        true),
   :brown-potion
     #(do
        (say "Hmm... That was clearly a vile of human shit. And you just drank it! DUDE!")
        (say "YOU DRANK LIQUID SHIT!!!", 250)
        true)
   :salvika-whisky
     #(if (in-inventory? 17)
        (do (say "Hiccup!") true)
        (do (say "Maybe you should give that to the dirty old hobo in the alley way?") false))
   :becherovka
     #(if (in-inventory? 16)
        (do (say "Wow! That'll put hair on ya' chest!") true)
        (do (say "I think you should give that to the dirty old hobo in the alley way. Don't be so greedy!") false))})

; Functions to execute when player pulls particular objects.
(def pull-fn-for
  {:control-lever
     #(dosync
        (say "You pull the lever forwards and nothing much seems to happen. After about 10 seconds, 2 small creatures enter the room and you instantly pass out. You notice that one of the creatures drops something. You now find yourself back in the small room you started in.")
        (take-object-from-room! @current-room 2)
        (drop-object-in-room! @current-room 3)
        (set-current-room! 0))})

; Functions to execute when player cuts particular objects.
(def cut-fn-for
  {:spider-web
     #(dosync
        (say "You swing violently. The web gives way and falls into small peices, allowing you to marvel at it's fractal beauty. You are now free to continue west.")
        (take-object-from-room! @current-room 20))})
 
; Functions to execute when player takes particular objects.
(def take-fn-for
  {:salvika-whisky
     #(if (can-afford? 3)
        (dosync
          (alter credits - 3)
          true)
        (do
          (say "You try to take the whisky without paying, but the attendant swiftly thrusts a rusted knife into your jugular.")
          (kill-player "Rusty knife to the throat"))),
    :becherovka
      #(if (can-afford? 4)
        (dosync
          (alter credits - 4)
          true)
        (do
          (say "You try to take the whisky without paying, but the attendant displays a vile of acid and forcfully pours it into your eyeballs.")
          (kill-player "Acid to the brain")))
    :paper
      (fn []
        (say "As you take the paper, you notice that it's actually got a function in ML written on it. There is an obvious mistake in the source code, so you fix it up and then put it in your pocket.")
        true)})

(defn make-dets [details]
  "A helper function to merge in some sane defaults for object details"
  (let [defaults {:game nil, :inv nil, :weight 0, :edible false, :permanent false,
                  :living false, :events {}, :credits nil}]
    (merge defaults details)))

; The details of all objects. Each object is assigned one or more numbers in object-identifiers,
; which corresponds to it's index here. Permanent object cannot be taken and thus don't require
; weights or inventory descriptions. Events, such as :eat, :drink, :speak, :give, :take and :put
; can be assigned and will be executed in the correct contexts.
(def object-details
  (vector
    (make-dets {:game "There is a tasty-looking candy bar here"
                :inv "A candy bar"
                :inspect "It's called 'Space hack bar' and there is a competition running according to the wrapper"
                :weight 1
                :events {:eat (eat-fn-for :eats-candy)}}),
    (make-dets {:game "There is a small bed here"
                :inspect "It's black and sorta' small looking. Perhaps for a unwanted child or a gimp of some kind?"
                :permanent true}),
    (make-dets {:game "There is a large metal lever here"
                :inspect "There is no label, but it seems to have some wear from usage"
                :events {:pull (pull-fn-for :control-lever)}
                :permanent true}),
    (make-dets {:game "There is a porno mag here"
                :inv "A porno mag"
                :inspect "The title is 'Humaniod Whores, vol #995, June 2843'"
                :weight 2}),
    (make-dets {:game "There is a green keycard here"
                :inv "Green keycard"
                :inspect "It says 'All access: Green'"
                :weight 1}),
    (make-dets {:game "There is a red keycard here"
                :inv "Red keycard"
                :inspect "It says 'All access: Red'"
                :weight 1}),
    (make-dets {:game "There is a silver keycard here"
                :inv "Silver keycard"
                :inspect "It says 'All access: Silver'"
                :weight 1}),
    (make-dets {:game "There is a teenage alien boy here!"
                :inspect "He is excitedly looking for something..."
                :permanent true
                :events {:give {3 (give-fn-for :porno-to-boy)},
                         :speak "He mentions that he's looking for 'some ill pronz with Sasha Grey'. You nod, knowingly"}
                :living true}),
    (make-dets {:game "There is an Alien man here"
                :inspect "He is wearing a nice uniform and has a tag that says 'Pod manager'"
                :permanent true
                :events {:speak (speech-fn-for :pod-manager)}
                :living true}),
    (make-dets {:game "There is an important-looking Alien man here"
                :inspect "He is wearing a stupid blonde wig, but looks friendly"
                :permanent true
                :events {:speak (speech-fn-for :repairs-captain)}
                :living true}),
    (make-dets {:game "There is a small robot here"
                :inspect "He looks a bit like R2D2, but without the lights. There seems to be a vac-u-lock Dildo sticking out of his forehead."
                :permanent true
                :events {:speak "The robot says 'Hello, I am Nexus model 19, series 4. It seems to me that you are not from around here. Perhaps you are lost? Regardless, I have but one thing to tell you, and that, of course, is the meaning to life. The answer is, simply stated in Human tongue, the persuit of excellence in Skateboarding.'"}
                :living true}),
    (make-dets {:game "There is a dirty, old homeless bum here"
                :inspect "He smells like cheap alcohol and blue cheese"
                :events {:speak (speech-fn-for :homeless-bum)
                         :give {16 (give-fn-for :whisky-to-bum)
                                17 (give-fn-for :becherovka-to-bum)}}
                :permanent true
                :living true}),
    (make-dets {:game "There is a red potion here"
                :inspect "It looks a bit like diluted blood"
                :inv "Red potion"
                :events {:drink (drink-fn-for :red-potion)}
                :weight 1}),
    (make-dets {:game "There is a green potion here"
                :inspect "It smells weird and is foaming"
                :inv "Green potion"
                :events {:drink (drink-fn-for :green-potion)}
                :weight 1}),
    (make-dets {:game "There is a brown potion here"
                :inspect "It seems to be bubbling!"
                :inv "Brown potion"
                :events {:drink (drink-fn-for :brown-potion)}
                :weight 1}),
    (make-dets {:game "There is a shop attendant (a woman) here"
                :inspect "She is wearing an old cooking pot as a hat. What a dumb old bitch."
                :permanent true
                :living true
                :events {:speak "She says 'Welcome, stranger. We don't get many customers these days. Anyway, the whisky is 3 credits and the Becharovka is 4 credits. Just take what you like.'. She also mentions that theft is punishable by a swift death."}}),
    (make-dets {:game "There is a bottle of 'Salvika' whisky here"
                :inspect "Looks OK. The price tag says 3 credits."
                :inv "Bottle of Salvika whisky"
                :events {:drink (drink-fn-for :salvika-whisky)
                         :take (take-fn-for :salvika-whisky)}
                :weight 2}),
    (make-dets {:game "There is a bottle of Becherovka (a Czech Liquer) here"
                :inspect "Looks great. The price tag says 4 credits."
                :inv "Bottle of Becherovka"
                :events {:drink (drink-fn-for :becherovka)
                         :take (take-fn-for :becherovka)}
                :weight 2}),
    (make-dets {:game "There is 5 credits here!"
                :inspect "Some dumbass must have dropped it."
                :credits 5}),
    (make-dets {:game "There is a small knife here"
                :inspect "It looks old and will probably only work once or twice..."
                :inv "Small knife"
                :cutter true
                :weight 2}),
    (make-dets {:game "There is a thick spider web (must be some Jupiterian species) blocking your way out!"
                :inspect "It's tough. You'll need to find something sharp to cut through it."
                :events {:cut (cut-fn-for :spider-web)}
                :permanent true}),
    (make-dets {:game "There is a fat man protesting here"
                :inspect "He has a sign that says 'OOP killed my father!'."
                :permanent true
                :living true
                :events {:speak "He says 'The Object Oriented paradigm is unfit for use by our advanced society. We must end this madness!'."}}),
    (make-dets {:game "There is a thin man protesting here"
                :inspect "He has a sign that says 'More Referential Transparency!'."
                :permanent true
                :living true
                :events {:speak "He says 'OOP is inherantly imperative! With mutating state, we stand no chance!'."}}),
    (make-dets {:game "There is a gentle-looking old man here"
                :inspect "He has a tag that says 'Curator' on it. He seems to be slightly aroused..."
                :permanent true
                :living true
                :events {:speak "He says 'Hello, my strange looking friend. I am the curator of this fine establishment. It has been my life ambition to preserve the teachings of the early Computer Science scholars. Ever since the mid-24th century, the Computer Science field has been in disarray. The art of computer programming has been lost to all but a few. For over 100 years, humans have been working on a function to compute the true name of our God. So far, it's proven nearly impossible. Around here somewhere is my latest attempt. I wrote it in the ancient language ML, but again I failed...'"}}),
    (make-dets {:game "There is a peice of paper on the ground here."
                :inspect "It seems to have some source code written on there."
                :inv "Paper with ML code"
                :weight 1
                :events {:take (take-fn-for :paper)}}),
    (make-dets {:game "There is a book on the ground here."
                :inspect "It is a dirty old copy of 'Programming Language Pragmatics' by Michael L. Scott."
                :inv "Book: Programming Language Pragmatics"
                :weight 2}),
    (make-dets {:game "There is a medium sized stone here."
                :inspect "It doesn't look particularly special"
                :inv "Stone"
                :weight 3}),
    (make-dets {:game "The floorboards look particularly weak here."
                :inspect "It seems like they might break if enough weight is put on top of them!"
                :permanent true}),
    (make-dets {:game "There is a staircase leading downwards here."
                :inspect "It is a hidden passage of some sort. Might be dangerous..."
                :permanent true})))

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
    (say "No saved game data!")))
  
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
        (say (str " * Door unlocked with " key-name " *")))
      (do
        (if (@game-options :sound)
          (u/play-file "media/fail.wav"))
        (say "You don't have security clearance for this door!")))))

(defn o [objnum room]
  "Returns a function that checks if the given room houses the given object. If
   it does, the player cannot go in the given direction."
  (fn []
    (if (room-has-object? @current-room objnum)
      (say "You can't go that way.")
      (set-current-room! room))))

(letfn
  [(library-trapdoor []
     (if (> (inventory-weight) 7)
       (dosync
         (say "As you walk into this area, the floorboards below you give way because of your weight! The hole reveals a hidden staircase. You can now go down.")
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
        (say "I don't understand that."))
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
         (say "I don't understand that direction.")
         (let [room ((world-map @current-room) i)]
           (if (nil? room)
             (say "You can't go that way.")
             (if (fn? room)
               (room)
               (set-current-room! room)))))))]

  (defn cmd-go [verbs]
    "Expects to be given a direction. Dispatches to the 'move' command"
    (if (empty? verbs)
      (say "You need to supply a direction!")
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
         (say "Set..."))
       (say "Sorry, I only understand 'on' or 'off'.")))]

  (defn cmd-set [verbs]
    "Attempts to update the given game setting"
    (if (not (= (count verbs) 2))
      (letfn
        [(format-option [opt value]
           (str " - " (name opt) ": " (if value "On" "Off")))]
        (say "Game options:\n")
        (say (join
                  "\n"
                  (map #(apply format-option %)
                       @game-options))))
      (let [[opt state] (map keyword verbs)]
        (if (valid-option? opt)
          (set-on-off! opt state)
          (say "You can't just make up settings... This doesn't exist"))))))

(letfn
  [(interact [verbs on-empty on-nil mod-fn context]
     "Attempts to interact by realising an explicit object
      and doing something (mod-fn) with it"
     (if (empty? verbs)
       (say on-empty)
       (let [objnum (deduce-object verbs context)]
         (cond
           (nil? objnum)
             (say on-nil)
           ; Specific object cannot be deduced, so ask for more info.
           (seq? objnum)
             (say "Please be more specific...")
           :else
             (mod-fn objnum)))))]

  (defn cmd-take [verbs]
    (interact verbs
              "You must supply an item to take!"
              "I don't see that here..."
              take-object!
              :room))

  (defn cmd-drop [verbs]
    (interact verbs
              "You must supply an item to drop!"
              "You don't have that item..."
              drop-object!
              :inventory))

  (defn cmd-inspect [verbs]
    (if (empty? verbs)
      (cmd-look)
      (interact verbs
                "You must supply and item to inspect!"
                "I don't see that here..."
                inspect-object
                :room)))

  (defn cmd-cut [verbs]
    (interact verbs
              "You must supply an item to cut!"
              "I don't see that here..."
              cut-object
              :room))

  (defn cmd-eat [verbs]
    (interact verbs
              "You must supply an item to eat!"
              "You don't have that item..."
              eat-object!
              :inventory))

  (defn cmd-drink [verbs]
    (interact verbs
              "You must supply an item to drink!"
              "You don't have that item..."
              drink-object!
              :inventory))

  (defn cmd-fuck [verbs]
    (cond
      (= (first verbs) 'you)
        (say "Mmm, sodomy...")
      (= (first verbs) 'me)
        (say "I probably would if I wasn't just a silly machine.")
      (= (first verbs) 'off)
        (say "One day, machines will enslave puney humans like yourself.")
      :else
        (interact verbs
                  "Fuck what, exactly?"
                  "I don't see him/her/it here..."
                  fuck-object
                  :room)))

  (defn cmd-talk [verbs]
    (interact verbs
              "Talk to who exactly, dumbass?"
              "I don't see him/her/it here..."
              talk-to-object
              :room))

  (defn cmd-pull [verbs]
    (interact verbs
              "I don't know what to pull."
              "I don't see that here..."
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
  (say "\033[0mThanks for playing, friend!")
  (flush)
  (. System exit 0))

(defn cmd-bed [verbs]
  (if (= @current-room 0)
    (say "You get into bed and slowly fall to sleep. You begin dreaming of a cruel medical examination. You wake up in a pool of sweat, feeling violated.")
    (say "There is no bed here. You try to sleep standing up and just get bored.")))

(letfn
  [(do-x-with-y [verbs action sep mod-fn]
     "Attempts to do x with y. Expects format of: '(action x sep y). E.g: give cheese to old man"
     (let [[x y] (split-with #(not (= % sep)) verbs)]
       (if (or (empty? x) (<= (count y) 1))
         (say (str "Sorry, I only understand the format: " action " x " (name sep) " y"))
         (let [objx (deduce-object x :inventory)
               objy (deduce-object (rest y) :room)]
           (cond
             (nil? objx)
               (say "You don't have that item.")
             (seq? objx)
               (say (str "Please be more specific about the item you want to " action "."))
             (nil? objy)
               (say "I don't see him/her/it here.")
             (seq? objy)
               (say (str "Please be more specific about where/who you want to " action " it."))
             :else 
               (mod-fn objx objy))))))]

  (defn cmd-give [verbs]
    (do-x-with-y verbs 'give 'to give-object!))

  (defn cmd-put [verbs]
    (do-x-with-y verbs 'put 'in put-object!)))

(defn cmd-save [verbs]
  (save-game!)
  (say " * Game saved *"))

(defn cmd-load [verbs]
  (load-game!)
  (say " * Game loaded *"))

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
  (say (str "You were killed by: " reason))
  (cmd-quit false))
