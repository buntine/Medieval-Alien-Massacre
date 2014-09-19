
; story.clj
; Defines data structures for describing rooms,
; maps, objects and inventories. The entire story
; is/should be encoded here.

(in-ns 'mam.gameplay)
;(declare set-current-room! current-room in-inventory? mam-pr can-afford?
;         hit-milestone? add-milestone! credits take-object-from-room!
;         drop-object-in-room! room-has-object? kill-player credits inventory
;         inventory-weight play-file)

(ns mam.story
  (:use mam.util)
  (:use mam.state))

;;(declare object-details)


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

(defn k [keynum room]
  "Returns a function that checks if the player has the given key. If they
   do, set the current room to 'room'. Otherwise, let them know"
  (fn []
    (if (in-inventory? keynum)
      (let [key-name (. ((object-details keynum) :inv) toLowerCase)]
        (set-current-room! room)
        (play-file "media/door.wav")
        (mam-pr (str " * Door unlocked with " key-name " *")))
      (do
        (play-file "media/fail.wav")
        (mam-pr "You don't have security clearance for this door!")))))

(defn o [objnum room]
  "Returns a function that checks if the given room houses the given object. If
   it does, the player cannot go in the given direction."
  (fn []
    (if (room-has-object? @current-room objnum)
      (mam-pr "You can't go that way.")
      (set-current-room! room))))

(letfn
  [(library-trapdoor []
     (if (> (inventory-weight) 7)
       (dosync
         (mam-pr "As you walk into this area, the floorboards below you give way because of your weight! The hole reveals a hidden staircase. You can now go down.")
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

(def directions {'north 0 'east 1 'south 2 'west 3 'northeast 4
                 'southeast 5 'southwest 6 'northwest 7 'up 8 'down 9
                 'in 10 'out 11})

; Maximum weight the user can carry at any one time.
(def *total-weight* 12)
