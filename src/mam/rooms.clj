
; rooms.clj
; Defines data structures for describing rooms,
; maps, objects and inventories.

(ns mam.rooms)


; A vector of pairs. Each index contains both a large description (first visit) and a brief
; description (all subsequent visits).
(def rooms
  (vector
    '("You are in a dark cavern, standing in a pool of your own vomit. An arrow is protruding from your neck. A passage leads to the north."
      "Dark cavern, vomit is everywhere. Passage to north.")
    '("The cavern has opened up into a large hall, although it's still very dark. Blood is rushing from your neck."
      "A large, dim hall. Smells of blood.")))

; Map to specify which rooms the player will enter on the given movement.
; 999 indicates that something special needs to be done (check conditions, etc).
(def world-map
  (vector
;    no  ea  so  we  ne  se  sw  nw 
    [1   nil nil nil nil nil nil nil]
    [nil nil 0   nil nil nil nil nil]))

(def directions {'north 0 'east 1 'south 2 'west 3 'northeast 4
                 'southeast 5 'southwest 6 'northwest 7})

; Specifies the verbs that users can identify an object with (a gun might
; be "gun", "weapon", etc). Each index corresponds to the same index in room-objects.
(def object-identifiers
  {'longbow 0 'bow 0})

; A vector containing the objects that each room contains when the game starts. Each index
; corresponds to the room as defined in 'rooms'.
(def room-objects
  (ref (vector
         '(0)
         '())))

; The details of objects, incl. in-game and inventory descriptions, weight, etc. Each object
; is assigned a number in object-identifiers, which corresponds to it's index here.
(def object-details
  (vector
    ["There is a wooden longbow here", "A longbow", 3]))

(def *total-weight* 12)
