
; rooms.clj
; Defines data structures for describing rooms,
; maps and objects.

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
(def world-map
  (vector
;    no  ea  so  we  ne  se  sw  nw 
    [1   nil nil nil nil nil nil nil]
    [nil nil 0   nil nil nil nil nil]))

; Specifies the verbs that users can identify an object with (a gun might
; be "gun", "weapon", etc). Each index corresponds to the same index in room-objects.
(def object-identifiers
  (vector
    {'obj-longbow 0 'obj-bow 0}))

; Create a flattened map of all objects that the user can interact with.
; I sorta' stole/borrowed this pattern for representing objects from Dunnet, so thanks Ron.
(def objects (reduce conj object-identifiers))

; A vector containing the objects that each room contains when the game starts. Nil means
; the room is initially empty. Each index corresponds to the room as defined in 'rooms'.
(def room-objects
  (vector
    (list 'obj-longbow)
     '()))

; The descriptions of objects, as they appear in game and in the inventory. Each object is
; assigned a number above, which corresponds to it's index here.
(def object-descriptions
  (vector
    '("There is a wooden longbow here", "A longbow")))
