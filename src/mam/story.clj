
; story.clj
; Defines data structures for describing rooms,
; maps, objects and inventories. The entire story
; is/should be encoded here.

(in-ns 'mam.gameplay)
(declare set-current-room! current-room in-inventory? mam-pr can-afford?
         hit-milestone? add-milestone! credits take-object-from-room!
         drop-object-in-room! room-has-object? kill-player credits inventory
         inventory-weight play-file)

(ns mam.story
  (:use mam.gameplay))

(declare object-details)


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

; Functions to execute when player speaks to a given object.
(def speech-fn-for
  {:pod-manager
     #(cond
        (not (can-afford? 3))
          (mam-pr "The man says 'Hey, I can get your sorry ass off this ship, but it will cost you 3 credits. Come back when you can afford it, matey'.")
        (not (hit-milestone? :speak-to-captain))
          (mam-pr "The man says 'Hey matey, I can get your sorry ass off here, but I suggest you speak to the captain over there to our northeast first'.")
        :else
          (dosync
            (mam-pr "The man says 'Oky doke, matey, lets get your punk ass outta' here. I hope Syndal City on Jupiter 4 is alright'.")
            (mam-pr "\n... flying to Syndal City ..." 300)
            (alter credits - 3)
            (set-current-room! 12))),
   :repairs-captain
     #(if (hit-milestone? :speak-to-captain)
        (mam-pr "The captain says 'That is all the information I have. Now, fuck off before I get mad.'.")
        (do
          (mam-pr "The man says 'Ahh, you're up! I am Bob Benson, the captain of this grand model T102 repairs vessel. We found you floating out there on the oxygenated stretch of galactic highway 7. Anyway, you look a tad confused, so let me refresh your memory:")
          (mam-pr "It is the year 2843, you're currently travelling on a highway between two of the moons of Jupiter.")
          (mam-pr "\n** At this point you explain that you are infact from the year 2011 and the last thing you remember is driking coffee at home and writing some LISP code **\n")
          (mam-pr "The captain says 'Oh, yes, it makes sense now. A true LISP hacker and drinker of the finest bean can transcend both space and time. We've seen your type before. You should head over to see the Pod Manager to our southwest in order to get yourself off this ship'")
          (mam-pr "Good luck out there, young man...")
          (add-milestone! :speak-to-captain))),
   :homeless-bum
     #(mam-pr "He mutters 'Hey mystery man! Welcome to Syndal City, perhaps you can spare an old cyborg some whisky?'.")})

; Functions to execute when player gives a particular X to a Y.
(def give-fn-for
  {:porno-to-boy
     #(dosync
        (mam-pr "The teenagers eyes explode!! He quickly accepts the porno mag and runs away. He throws a green keycard in your general direction as he leaves the room.")
        (take-object-from-room! @current-room 7)
        (drop-object-in-room! @current-room 4)),
   :whisky-to-bum
     #(if (not (hit-milestone? :alcohol-to-bum))
        (dosync
          (mam-pr "The old bum accepts the whisky and says 'Wow!! Thank you, cobba! Please, take this small knife in return, It may help to 'cut' things that lay in your path'. You, in turn, take the knife.")
          (alter inventory conj 19)
          (add-milestone! :alcohol-to-bum))
        (mam-pr "He accepts the alcohol, but just grumbles something about Common LISP in response")),
   :becherovka-to-bum
     #(if (not (hit-milestone? :alcohol-to-bum))
        (dosync
          (mam-pr "The old bum accepts the whisky and says 'Holy fuck, Becherovka! My favourite! Please, take this small knife in return, It may help to 'cut' things that lay in your path'. You, in turn, take the knife.")
          (alter inventory conj 19)
          (add-milestone! :alcohol-to-bum))
        (mam-pr "He accepts the alcohol, but just grumbles something about Emacs LISP in response"))})

; Functions to execute when player eats particular objects.
(def eat-fn-for
  {:eats-candy
     #(dosync
        (mam-pr "You feel like you just ate crusty skin off Donald Trump's forehead. Although inside the wrapper there was an 'instant win' of 5 credits!")
        (alter credits + 5))})

; Functions to execute when player drinks particular objects.
(def drink-fn-for
  {:red-potion
     #(do
        (mam-pr "Wow, that tasted great. Unfortunately, it also physically melted your brain and ended your life...")
        (kill-player "Red potion")),
   :green-potion
     #(do
        (mam-pr "You drink the potion and instantly start to feel strange. Without warning, your eyes begin to glow green! Luckily, you feel no pain.")
        (add-milestone! :drinks-green-potion)
        true),
   :brown-potion
     #(do
        (mam-pr "Hmm... That was clearly a vile of human shit. And you just drank it! DUDE!")
        (mam-pr "YOU DRANK LIQUID SHIT!!!", 250)
        true)
   :salvika-whisky
     #(if (playey_has_becherovka)
        (do (mam-pr "Hiccup!") true)
        (do (mam-pr "Maybe you should give that to the dirty old hobo in the alley way? After all, he did ask for it kindly!") false))
   :becherovka
     #(if (playey_has_salvika)
        (do (mam-pr "Wow! That'll put hair on ya' chest!") true)
        (do (mam-pr "I think you should give that to the dirty old hobo in the alley way. Don't be so greedy!") false))})

; Functions to execute when player pulls particular objects.
(def pull-fn-for
  {:control-lever
     #(dosync
        (mam-pr "You pull the lever forwards and nothing much seems to happen. After about 10 seconds, 2 small creatures enter the room and you instantly pass out. You notice that one of the creatures drops something. You now find yourself back in the small room you started in.")
        (take-object-from-room! @current-room 2)
        (drop-object-in-room! @current-room 3)
        (set-current-room! 0))})

; Functions to execute when player cuts particular objects.
(def cut-fn-for
  {:spider-web
     #(dosync
        (mam-pr "You swing violently. The web gives way and falls into small peices, allowing you to marvel at it's fractal beauty. You are now free to continue west.")
        (take-object-from-room! @current-room 20))})
 
; Functions to execute when player takes particular objects.
(def take-fn-for
  {:salvika-whisky
     #(if (can-afford? 3)
        (dosync
          (alter credits - 3)
          true)
        (do
          (mam-pr "You try to take the whisky without paying, but the attendant swiftly thrusts a rusted knife into your jugular.")
          (kill-player "Rusty knife to the throat"))),
    :becherovka
      #(if (can-afford? 4)
        (dosync
          (alter credits - 4)
          true)
        (do
          (mam-pr "You try to take the whisky without paying, but the attendant displays a vile of acid and forcfully pours it into your eyeballs.")
          (kill-player "Acid to the brain")))
    :paper
      (fn []
        (mam-pr "As you take the paper, you notice that it's actually got a function in ML written on it. There is an obvious mistake in the source code, so you fix it up and then put it in your pocket.")
        true)})

(defn make-dets [details]
  "A helper function to merge in some sane defaults for object details"
  (let [defaults {:game nil, :inv nil, :weight 0, :edible false, :permanent false,
                  :living false, :events {}, :credits nil}]
    (merge defaults details)))

; The details of all objects. Each object is assigned a number in object-identifiers, which
; corresponds to it's index here. Permanent object cannot be taken and thus don't require
; weights or inventory descriptions. Events, such as :eat, :speak, :give and :put can be
; assigned and will be executed in the correct contexts.
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

; Maximum weight the user can carry at any one time.
(def *total-weight* 12)
