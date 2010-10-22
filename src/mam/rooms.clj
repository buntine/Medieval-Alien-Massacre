
; rooms.clj
; Defines data structures for describing rooms,
; maps, objects and inventories.

(in-ns 'mam.gameplay)
(declare set-current-room! in-inventory?)

(ns mam.rooms
  (:use mam.gameplay))


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
      "West-end of the repairs deck. Passage leads east.")
    '("You walk into a hallway with doors to your west and south. The hallway is leading north."
      "Hallway. Doors to the west and south. Passage leads north.")
    '("You continue along the passage and pass more broken machines. Passage leads east or west."
      "Repairs desk, center. Passage leads west/east.")
    '("You are at the end of the hallway. There is a large, sliding door to the north."
      "End of hallway. Large door to north.")
    '("There are just more broken machines lying around on the repears deck. The passage ends with a door to the east."
      "Repairs deck. Door to the east and passage south.")))

(defn check-key [key-type room]
  "Checks if the player has the given type of security card. If they do, set the current
   room to 'room'. Otherwise, let them know"
  (let [keys {:green 4 :red 5 :silver 6}]
    (if (in-inventory? (keys key-type))
      (set-current-room! room)
      (println "You don't have security clearance for this door!"))))

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
    [nil           4            nil          5            nil          nil          nil          nil]))

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
         '(7))))

; The details of objects: [game desc, inventory name, inspect desc, weight, permanent?]. Each
; object is assigned a number in object-identifiers, which corresponds to it's index here.
; Permanent object cannot be taken and thus don't require weights or inventory descriptions.
(def object-details
  (vector
    ["There is a tasty-looking candy bar here" "A candy bar"
     "It's called 'Space hack bar'" 1 false],
    ["There is a small bed here" nil
     "The bed is black and sorta' small looking. Perhaps for a child?" nil true],
    ["There is a large metal lever here" nil
     "There is no label, but it seems to have some wear from usage" nil true]
    ["There is a porno mag here" "A porno mag"
     "The title is 'Humaniod Whores, vol 99239'." 1 false]
    ["There is a green keycard here" "Green keycard"
     "It says 'All access: Green'" 1 false]
    ["There is a red keycard here" "Red keycard"
     "It says 'All access: Red'" 1 false]
    ["There is a silver keycard here" "Silver keycard"
     "It says 'All access: Silver'" 1 false]
    ["There is a teenage alien boy here!" nil
     "He is excitedly looking for something..." nil true]))

(def *total-weight* 12)
