
; rooms.clj
; Defines data structures for describing rooms,
; maps, objects and inventories.

(in-ns 'mam.gameplay)
(declare set-current-room)

(ns mam.rooms
  (:use mam.gameplay))


; A vector of pairs. Each index contains both a large description (first visit) and a brief
; description (all subsequent visits).
(def rooms
  (vector
    '("You are in a small, silver-walled room with no windows. There are doors to the north and east."
      "Empty room with a bed. Doors to north and east.")
    '("You enter a larger room with a few blank screens. There are doors to the east and west."
      "Control room with doors to east and west")
    '("You enter a large platform. There is a long row of broken flying machines here. A large sign reads 'Repairs deck: West end'. 'Where the fuck am I?' you think to yourself. The passage leads east."
      "West-end of the repairs deck. Passage leads east.")))

(defn doroom [func room]
  "Returns a curried function that executes the given fn and then sets the current room"
  (fn [] (func)
         (set-current-room room)))

; Map to specify which rooms the player will enter on the given movement.
; A function indicates that something special needs to be done (check conditions, etc).
(def world-map
  (vector
;    north        east        south       west        northeast   southeast   southwest   northwest
    [2            1           nil         nil         nil         nil         nil         nil]
    [nil          3           nil         0           nil         nil         nil         nil]
    [nil          nil         0           nil         nil         nil         nil         nil]))

(def directions {'north 0 'east 1 'south 2 'west 3 'northeast 4
                 'southeast 5 'southwest 6 'northwest 7})

; Specifies the verbs that users can identify an object with (a gun might
; be "gun", "weapon", etc). Each index corresponds to the same index in room-objects.
(def object-identifiers
  (vector
    {'longbow 0 'bow 0 'bed 1}
    {'lever 2}))

; A vector containing the objects that each room contains when the game starts. Each index
; corresponds to the room as defined in 'rooms'.
(def room-objects
  (ref (vector
         '(0 1)
         '(2)
         '())))

; The details of objects: [game desc, inventory name, inspect desc, weight, permanent?]. Each
; object is assigned a number in object-identifiers, which corresponds to it's index here.
; Permanent object cannot be taken.
(def object-details
  (vector
    ["There is a wooden longbow here" "A longbow"
     "The longbow seems in working order" 3 false],
    ["There is a small bed here" nil
     "The bed is black and sorta' small looking. Perhaps for a child?" nil true],
    ["There is a large metal lever here" nil
     "There is no label, but it seems to have some wear from usage" nil true]))

(def *total-weight* 12)
