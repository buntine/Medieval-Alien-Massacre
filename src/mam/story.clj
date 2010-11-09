
; story.clj
; Defines data structures for describing rooms,
; maps, objects and inventories. The entire story
; is/should be encoded here.

(in-ns 'mam.gameplay)
(declare set-current-room! current-room in-inventory? mam-pr can-afford?
         hit-milestone? add-milestone! credits take-object-from-room!
         drop-object-in-room! room-has-object? kill-player credits inventory)

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
    '("There are a bunch broken machines lying around on the repairs deck. There is a door to the east or a passage south."
      "Repairs deck. Door to the east and passage south.")
    '("You are in a large room with space age decor. It seems to be the central living quarters. The walls are lined with pictures of the late comedian, Bill Hicks. There are walkways to the west and northwest and a door to the south."
      "Central living quarters, walkways to west and northwest, door to south.")
    '("You can see some more framed pictures of Bill Hicks here. As you walk past them, Bills eyes seem to follow you. The passage goes west or east."
      "Passage with more creepy Bill Hicks pictures. Passage leads east/west.")
    '("You are at the west-end of the room. Here you can see sealed entrance and a sign saying 'Exit pod'."
      "West-end of large room with exit pod.")
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
      "Grimy liquor store entrance. Passage goes south.")
    '("The shop is in disarray and is pretty much empty, although there are some things lying around."
      "Bottle shop with attendant.")
    '("You are at another corner. There are paths running both east and west, or back to the north."
      "Bottom of alley, passages to east/west or back north.")
    '("You are at the end of the alley way, but there is a giant spider web (must be some Jupiterian species) blocking the way out!."
      "End of alley, giant spider web blocking the way out.")
    '("You are on a road. It doesn't seem to be used anymore, though. It runs both north and south."
      "Road with no vehicles, running north/south.")))

(defn k [keynum room]
  "Returns a function that checks if the player has the given key. If they
   do, set the current room to 'room'. Otherwise, let them know"
  (fn []
    (if (in-inventory? keynum)
      (let [key-name (. ((object-details keynum) :inv) toLowerCase)]
        (set-current-room! room)
        (mam-pr (str " * Door unlocked with " key-name " *")))
      (mam-pr "You don't have security clearance for this door!"))))

(defn o [objnum room]
  "Returns a function that checks if the given room houses the given object. If
   it does, the player cannot go in the given direction."
  (fn []
    (if (room-has-object? @current-room objnum)
      (mam-pr "You can't go that way."
      (set-current-room! room)))))

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
    [nil       19        nil       nil       nil       nil       nil       nil       nil       nil       nil       nil])) ;20

(def directions {'north 0 'east 1 'south 2 'west 3 'northeast 4
                 'southeast 5 'southwest 6 'northwest 7 'up 8 'down 9
                 'in 10 'out 11})

; Specifies the verbs that users can identify an object with (a gun might
; be "gun", "weapon", etc). A set means that the given term may refer to
; multiple objects. The system will try to deduce the correct object when
; a command is entered. Each index corresponds to the same index in room-objects.
(def object-identifiers
    {'candy 0 'bar 0 'bed 1 'lever 2 'mag 3 'magazine 3 'porno 3 'boy 7
     'teenager 7 'keycard #{4 5 6} 'key #{4 5 6} 'man #{8 9} 'robot 10
     'green #{4 13} 'red #{5 12} 'brown 14 'silver 6 'bum 11 'potion #{12 13 14}
     'credits 18 'attendant 15 'woman 15 'salvika 16 'whisky 16 'becherovka 17
     'web 20 'knife 19 'small 19})

; A vector containing the objects that each room contains when the game starts. Each index
; corresponds to the room as defined in 'rooms'.
(def room-objects
  (ref (vector
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
         [20])))      ;19

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
          (mam-pr "\n** At this point you explain that you are infact from the year 2010 and the last thing you remember is driking coffee at home and writing some Lisp code **\n")
          (mam-pr "The captain says 'Oh, yes, it makes sense now. A true Lisp hacker and drinker of the finest bean can transcend both space and time. We've seen your type before. You should head over to see the Pod Manager to our southwest in order to get yourself off this ship'")
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
        (mam-pr "He accepts the alcohol, but just grumbles something about Common Lisp in response")),
   :becherovka-to-bum
     #(if (not (hit-milestone? :alcohol-to-bum))
        (dosync
          (mam-pr "The old bum accepts the whisky and says 'Holy fuck, Becherovka! My favourite! Please, take this small knife in return, It may help to 'cut' things that lay in your path'. You, in turn, take the knife.")
          (alter inventory conj 19)
          (add-milestone! :alcohol-to-bum))
        (mam-pr "He accepts the alcohol, but just grumbles something about Emacs Lisp in response"))})

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
        (mam-pr "You drink the potion and instantly start to feel strange. Without warning, a large hook protrudes from your left arm! Luckily, you feel no pain.")
        (add-milestone! :drinks-green-potion)),
   :brown-potion
     #(mam-pr "Hmm... That was clearly a vile of human shit. And you just drank it! DUDE!")})

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
        (mam-pr "You swing violently. The web gives way and falls into small peices. You are now free to continue west.")
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
          (kill-player "Acid to the brain")))})

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
                :inspect "It's black and sorta' small looking. Perhaps for a child?"
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
                :inspect "He smells like alcohol and blue cheese"
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
                :inspect "She is wearing an old cooking pot as a hat. It looks rather dumb."
                :permanent true
                :living true
                :events {:speak "She says 'Welcome, stranger. We don't get many customers these days. Anyway, the whisky is 3 credits and the Becharovka is 4 credits. Just take what you like.'. She also mentions that theft is punishable by a swift death."}}),
    (make-dets {:game "There is a bottle of 'Salvika' whisky here"
                :inspect "Looks OK. The price tag says 3 credits."
                :inv "Bottle of Salvika whisky"
                :events {:drink "* hiccup! *"
                         :take (take-fn-for :salvika-whisky)}
                :weight 2}),
    (make-dets {:game "There is a bottle of Becherovka (a Czech Liquer) here"
                :inspect "Looks great. The price tag says 4 credits."
                :inv "Bottle of Becherovka"
                :events {:drink "Wow, that was strong!"
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
    (make-dets {:inspect "It's tough. You'll need to find something sharp to cut through it."
                :events {:cut (cut-fn-for :spider-web)}
                :permanent true})))

(def *total-weight* 12)
