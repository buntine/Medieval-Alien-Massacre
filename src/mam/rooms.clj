
; rooms.clj
; Defines data structures for describing rooms,
; maps, objects and inventories.

(in-ns 'mam.gameplay)
(declare set-current-room! in-inventory?)

(ns mam.rooms
  (:use mam.gameplay))

(defn check-key [key-type room]
  "Checks if the player has the given type of security card. If they do, set the current
   room to 'room'. Otherwise, let them know"
  (let [keys {:green 4 :red 5 :silver 6}]
    (if (in-inventory? (keys key-type))
      (let [key-name (apply str (rest (str key-type)))]
        (set-current-room! room)
        (println (str " * Door unlocked with " key-name " keycard. *")))
      (println "You don't have security clearance for this door!"))))

; A vector of pairs. Each index contains both a large description (first visit) and a brief
; description (all subsequent visits).
(def rooms
  (vector
    '("You are in a small, silver-walled room with no windows. There are doors to the north and east."
      "Empty room with a bed. Doors to north and east.")
    '("You are in another small, featureless room. There is nothing of interest here except doors to the north and west."
      "Small, featureless room. Doors to north and west.")
    '("You enter a larger room with a few blank screens. There are doors to the east and west."
      "Control room with doors to east and west")
    '("You enter a large platform. There is a long row of broken flying machines here. A large sign reads 'Repairs deck: West end'. 'Where the fuck am I?' you think to yourself. The passage leads east. There is a door to the south."
      "West-end of the repairs deck. Passage leads east. Door to the south.")
    '("You walk into a hallway with doors to your west and south. The hallway is leading north."
      "Hallway. Doors to the west and south. Passage leads north.")
    '("You continue along the passage and pass more broken machines. Passage leads east or west."
      "Repairs desk, center. Passage leads west/east.")
    '("You are at the end of the hallway. There is a large, sliding door to the north."
      "End of hallway. Large door to north.")
    '("There are just more broken machines lying around on the repears deck. The passage ends with a door to the east."
      "Repairs deck. Door to the east and passage south.")
    '("You are in a large room with space age decor. It seems to be the central control room. The walls are lined with pictures of the late comedian, Bill Hicks. There are walkways to the east and northeast and a door to the south."
      "Central control room, walkways to east and northeast, door to south.")))

; Map to specify which rooms the player will enter on the given movement.
; A function indicates that something special needs to be done (check conditions, etc).
(def world-map
  (vector
;    north         east         south        west         northeast    southeast    southwest    northwest
    [3             2            nil          nil          nil          nil          nil          nil]
    [4             nil          nil          2            nil          nil          nil          nil]
    [nil           1            nil          0            nil          nil          nil          nil]
    [nil           5            0            nil          nil          nil          nil          nil]
    [6             nil          1            7            nil          nil          nil          nil]
    [nil           7            nil          3            nil          nil          nil          nil]
    [#(check-key
       :green 8)   nil          4            nil          nil          nil          nil          nil]
    [nil           4            nil          5            nil          nil          nil          nil]
    [nil           10           6            nil          9            nil          nil          nil]))

(def directions {'north 0 'east 1 'south 2 'west 3 'northeast 4
                 'southeast 5 'southwest 6 'northwest 7})

; Specifies the verbs that users can identify an object with (a gun might
; be "gun", "weapon", etc). Each index corresponds to the same index in room-objects.
(def object-identifiers
    {'candy 0 'bar 0 'bed 1 'lever 2 'mag 3 'magazine 3 'porno 3 'boy 7
     'teenager 7 'keycard 4 'key 4})

; A vector containing the objects that each room contains when the game starts. Each index
; corresponds to the room as defined in 'rooms'.
(def room-objects
  (ref (vector
         '(0 1)
         '()
         '(2)
         '()
         '()
         '()
         '()
         '(7)
         '())))

(defn make-dets [details]
  "A helper function to merge in some sane defaults for object details"
  (let [defaults {:inv nil, :weight nil, :edible false, :permanent false :living false
                  :speech nil}]
    (merge defaults details)))

; The details of all objects. Each object is assigned a number in object-identifiers, which
; corresponds to it's index here. Permanent object cannot be taken and thus don't require
; weights or inventory descriptions. Humans/Aliens can talk, the :speech symbol should contain
; their response (this can be a function, for checking conditions, etc).
(def object-details
  (vector
    (make-dets {:game "There is a tasty-looking candy bar here"
                :inv "A candy bar"
                :inspect "It's called 'Space hack bar' and there is a competition running according to the wrapper"
                :weight 1
                :edible true}),
    (make-dets {:game "There is a small bed here"
                :inspect "The bed is black and sorta' small looking. Perhaps for a child?"
                :permanent true}),
    (make-dets {:game "There is a large metal lever here"
                :inspect "There is no label, but it seems to have some wear from usage"
                :permanent true}),
    (make-dets {:game "There is a porno mag here"
                :inv "A porno mag"
                :inspect "The title is 'Humaniod Whores, vol 99239'."
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
                :speech "He mentions that he's looking for 'some ill pronz with Sasha Grey'. You nod, knowingly"
                :living true})))

(def *total-weight* 12)
