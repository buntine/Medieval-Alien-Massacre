
; rooms.clj
; Defines data structures for describing rooms,
; maps and objects.


(ns mam.rooms)

; A vector of pairs. Each index contains both a large description (first visit) and a brief
; description (all subsequent visits).
(def rooms
  (vector
    '("You are in a dark cavern, standing in a pool of your own vomit. An arrow is protruding from your neck."
      "Dark cavern, vomit is everywhere.")
    '("The cavern has opened up into a large hall, although it's still very dark. Blood is rushing from your neck."
      "A large, dim hall. Smells of blood.")))

; Specifies the verbs that users can identify an object with.
(def obj-identifiers
  (vector
    {'longbow 0 'bow 0}))

; Create a variable for each object that the user can interact with.
; I stole this pattern for representing objects from Dunnet, so thanks Ron.
; TODO: Implement.
'()

; A vector containing the objects that each room contains when the game starts. Nil means
; the room is initially empty.
(def room-objects
  (vector
    (list obj-longbow)
     nil))

; The descriptions of objects, as they appear in game and in the inventory.
(def object-descriptions
  (vector
    '("There is a wooden Longbow here", "A longbow")))
