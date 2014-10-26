
; state.clj
; Holds refs and their mutators.

(in-ns 'mam.gameplay)

(ns mam.state)

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

(defn set-current-room! [room]
  (dosync
    (ref-set current-room room)))

(defn objects-in-room ([] (objects-in-room @current-room))
  ([room]
   (nth @room-objects room)))

(defn room-has-object? [room objnum]
  "Returns true if the gien room currently houses the given object"
  (boolean (some #{objnum} (objects-in-room room))))

(defn in-inventory? [objnum]
  "Returns true if object assigned to 'objnum' is in players inventory"
  (boolean (some #{objnum} @inventory)))

(letfn
  [(alter-room! [room changed]
     "Physically alters the contents of the given. Must be called from within
      a dosync form"
     (alter room-objects
            (fn [objs]
              (assoc-in objs [room] changed))))]

  (defn take-object-from-room!
    ([objnum] (take-object-from-room! @current-room objnum))
    ([room objnum]
     (alter-room! room
                  (vec (remove #(= objnum %)
                                 (objects-in-room room))))))

  (defn drop-object-in-room!
    ([objnum] (drop-object-in-room! @current-room objnum))
    ([room objnum]
     (alter-room! room
                  (conj (objects-in-room room) objnum)))))

(defn remove-object-from-inventory! [objnum]
  "Physically removes an object from the players inventory."
  (ref-set inventory (vec (remove #(= % objnum) @inventory))))
 
(defn add-object-to-inventory! [objnum]
  "Physically adds an object to the players inventory."
  (alter inventory conj objnum))

(defn pay-the-man! [c]
  "Physically adds/removes credits."
  (alter credits + c))

