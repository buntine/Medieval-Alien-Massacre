
; text.clj
; Holds immutable game text. Rooms, objects, etc.

(in-ns 'mam.gameplay)

(ns mam.text)

; All game text, namespaced to allow for easy management (adding new rooms, etc).
(def game-text
  ; All rooms. Each index contains both a large description (first visit) and a brief
  ; description (all subsequent visits).
  {'rooms
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
        "Pitch black room with Bill Hicks hologram. Stairs leading upwards.")
    )
   'inventory
    {
      'have    "You currently have:"
      'empty   "Your inventory is currently empty."
      'credits "\nCREDITS: "
    }
   'commands
    {
      'cant-take   "You can't take that."
      'no-space    "You cannot carry that much weight. Try dropping something."
      'taken       "Taken..."
      'dropped     "Dropped..."
      'fuck-object (vector "You start fucking away but it just feels painful." "You try, but it just won't fit!" "...Dude")
      'fuck-living (vector "Hmm... I bet that felt pretty good!" "*Pump* *Pump* *Pump*" "The room is filled with a dank scent of deep musk. It's actually kinda' gross...")
      'give-error  "He/she/it cannot accept this item."
      'put-error   "You cannot put this item here."
      'cut-error   "You need a something sharp before you can cut this!"
      'cut-object  (vector "Nothing seemed to happen." "That achieved absolutely nothing..." "Stop wasting time!")
      'cut-living  (vector "Wow, that must have hurt..." "That's pretty rude..." "You really shouldn't be doing that!")
      'eat-error   (vector "You force it into your throat and fucking die in pain." "You force it into your mouth and swallow. Your life flashes before your eyes. And then your life ends... Fuck you.")
      'drink-error (vector "It doesn't seem to be drinkable." "Dude, you can't drink that!")
      'talk-error (vector "That item does not possess the ability to talk." "That doesn't even make sense" "Stop trying to fuck with me...")
      'speechless "Sorry, they have nothing to say at the moment."
      'pull-error "Nothing much seemed to happen."
    }
    'talk
    {
      'pod-manager
      {
        'broke     "The man says 'Hey, I can get your sorry ass off this ship, but it will cost you 3 credits. Come back when you can afford it, matey'."
        'not-ready "The man says 'Hey matey, I can get your sorry ass off here, but I suggest you speak to the captain over there to our northeast first'."
        'ready     "The man says 'Oky doke, matey, lets get your punk ass outta' here. I hope Syndal City on Jupiter 4 is alright'."
        'flying    "\n... flying to Syndal City ..."
      }
      'repairs-captain
      {
        'finished "The captain says 'That is all the information I have. Now, fuck off before I get mad.'."
        'spiel
        {
          'a "The man says 'Ahh, you're up! I am Bob Benson, the captain of this grand model T102 repairs vessel. We found you floating out there on the oxygenated stretch of galactic highway 7. Anyway, you look a tad confused, so let me refresh your memory:"
          'b "It is the year 2843, you're currently travelling on a highway between two of the moons of Jupiter."
          'c "\n** At this point you explain that you are infact from the year 2011 and the last thing you remember is driking coffee at home and writing some LISP code **\n"
          'd "The captain says 'Oh, yes, it makes sense now. A true LISP hacker and drinker of the finest bean can transcend both space and time. We've seen your type before. You should head over to see the Pod Manager to our southwest in order to get yourself off this ship'"
          'e "Good luck out there, young man..."
        }
      }
      'homeless-bum "He mutters 'Hey mystery man! Welcome to Syndal City, perhaps you can spare an old cyborg some whisky?'."
    }
    'give
    {
      'porno-to-boy "The teenagers eyes explode!! He quickly accepts the porno mag and runs away. He throws a green keycard in your general direction as he leaves the room."
      'whisky-to-bum "The old bum accepts the whisky and says 'Wow!! Thank you, cobba! Please, take this small knife in return, It may help to 'cut' things that lay in your path'. You, in turn, take the knife."
      'becherovka-to-bum "The old bum accepts the whisky and says 'Holy fuck, Becherovka! My favourite! Please, take this small knife in return, It may help to 'cut' things that lay in your path'. You, in turn, take the knife."
      'alcohol-to-bum "He accepts the alcohol, but just grumbles something about Common LISP in response"
    }
    'eat
    {
      'candy "You feel like you just ate crusty skin off Donald Trump's forehead. Although inside the wrapper there was an 'instant win' of 5 credits!"
    }
    'drink
    {
      'red-potion   "Wow, that tasted great. Unfortunately, it also physically melted your brain and ended your life..."
      'green-potion "You drink the potion and instantly start to feel strange. Without warning, your eyes begin to glow green! Luckily, you feel no pain."
      'brown-potion
      {
        'a "Hmm... That was clearly a vile of human shit. And you just drank it! DUDE!"
        'b "YOU DRANK LIQUID SHIT!!!"
      }
      'whisky
      {
        'success "Hiccup!"
        'fail    "Maybe you should give that to the dirty old hobo in the alley way?"
      }
      'becherovka
      {
        'success "Wow! That'll put hair on ya' chest!"
        'fail    "I think you should give that to the dirty old hobo in the alley way. Don't be so greedy!"
      }
    }
  })

(def rooms (game-text 'rooms))

(letfn
  [(deduce-text [path m]
     (let [v (m (first path))]
       (if (map? v)
         (deduce-text (rest path) v)
         v)))]

  (defn text [& path]
    "Returns a string of game text for the given path into game-text"
    (deduce-text
      (rest path)
      (game-text (first path)))))
