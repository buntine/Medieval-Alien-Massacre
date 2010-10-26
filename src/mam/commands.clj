
; commands.clj
; Defines functions for handling user commands. Lots of dispatch
; to mam.gameplay going on here...

; I need this to allow 'mutual' interation between this namespace and
; mam.gameplay. There must be a better way of doing this!
(in-ns 'mam.gameplay)
(declare set-current-room! current-room take-object! inventory
         display-inventory drop-object! inspect-object parse-input
         describe-room room-has-object? drop-object-in-room!
         take-object-from-room! eat-object fuck-object talk-to-object
         save-game! load-game! mam-pr)

(ns mam.commands
  (:use mam.gameplay)
  (:use mam.rooms))

(declare cmd-inspect)


(defn direction? [verb]
  (boolean
    (some #{verb}
          '(n e s w ne se sw nw north east south west
            northeast southeast southwest northwest))))

(letfn
  [(move-room [dir]
     "Attempts to move in the given direction."
     (let [i (directions dir)]
       (if (not i)
         (mam-pr "I don't understand that direction.")
         (let [room ((world-map @current-room) i)]
           (if (nil? room)
             (mam-pr "You can't go that way.")
             (if (fn? room)
               (room)
               (set-current-room! room)))))))]

  (defn cmd-go [verbs]
    "Expects to be given direction. Dispatches to the 'move' command"
    (if (empty? verbs)
      (mam-pr "You need to supply a direction!")
      ; Catch commands like "go to bed", etc.
      (if (direction? (first verbs))
        (move-room (first verbs))
        (parse-input (reduce #(str %1 " " %2) "" verbs)))))

  (defn cmd-north [verbs] (move-room 'north))
  (defn cmd-east [verbs] (move-room 'east))
  (defn cmd-south [verbs] (move-room 'south))
  (defn cmd-west [verbs] (move-room 'west))
  (defn cmd-northeast [verbs] (move-room 'northeast))
  (defn cmd-southeast [verbs] (move-room 'southeast))
  (defn cmd-southwest [verbs] (move-room 'southwest))
  (defn cmd-northwest [verbs] (move-room 'northwest)))

(defn cmd-help [verbs]
  (println "  M-A-M HELP")
  (println "  ------------------------------")
  (println "   * Directions are north, east, south, west, northeaast, southeast, southwest, northeast.")
  (println "   * Or abbreviated n, e, s, w, ne, se, sw, nw.")
  (println "   * Keys automatically open the appropriate doors, so just walk in their direction.")
  (println "   * Credit is equivalent to our concept of money. Use it wisely!")
  (println "   * Check your items and credit with 'inventory' or 'inv'")
  (println "   * You can 'speak' to humans, aliens and robots, but some may be a tad vulgar...")
  (println "   * To end the game, type 'quit' or 'suicide' or forever dwell in green mess!")
  (println "   * You can 'save' and 'load' your game, mother fucker!")
  (println "   * Inspired by Dunnet, by Rob Schnell")
  (println "   * Don't forget: Life is a game and everything is pointless.")
  (println "  ------------------------------"))

(defn cmd-look ([verbs] (cmd-inspect verbs))
  ([]
   "Prints a long description of a room"
   (describe-room @current-room true)))

(defn drop-check [verbs]
  "Checks if the conditions are right for a special occurrence"
  (letfn
    [(teen-takes-porno []
       "Occurs when the player drops the porno in the teenagers room"
       (newline)
       (mam-pr "The teenagers eyes explode!! He quickly picks up the porno mag and runs away. He throws a green keycard in your general direction as he leaves the room.")
       (dosync
         (take-object-from-room! @current-room 'porno)
         (take-object-from-room! @current-room 'teenager)
         (drop-object-in-room! @current-room 'keycard)))]

    (if (and (= @current-room 7) (room-has-object? @current-room 'porno))
      (teen-takes-porno))))

(letfn
  [(try-interact
     [verbs no-verb not-here mod-fn]
     "Loops through the verbs trying to match one to a interactable object"
     (if (empty? verbs)
       (mam-pr no-verb)
       (loop [objs verbs]
         (if (empty? objs)
           (mam-pr not-here)
           (if (not (mod-fn (first objs)))
             (recur (rest objs)))))))]

  (defn cmd-take [verbs]
    (try-interact verbs
                  "You must supply an item to take!"
                  "I don't see that here..."
                  take-object!))
 (defn cmd-drop [verbs]
    (try-interact verbs
                  "You must supply an item to drop!"
                  "You don't have that item..."
                  drop-object!)
    (drop-check verbs))

  (defn cmd-inspect [verbs]
    (if (empty? verbs)
      (cmd-look)
      (try-interact verbs
                    "You must supply and item to inspect!"
                    "I don't see that here..."
                    inspect-object)))

  (defn cmd-eat [verbs]
    (try-interact verbs
                  "You must supply an item to eat!"
                  "You don't have that item..."
                  eat-object))

  (defn cmd-fuck [verbs]
    (cond
      (= (first verbs) 'you)
        (mam-pr "Mmm, sodomy...")
      (= (first verbs) 'me)
        (mam-pr "I probably would if I wasn't just a silly machine.")
      :else
        (try-interact verbs
                      "Fuck what exactly?"
                      "I don't see him/her/it here..."
                      fuck-object)))

  (defn cmd-talk [verbs]
    (try-interact verbs
                  "Talk to who exactly, dumbass?"
                  "I don't see him/her/it here..."
                  talk-to-object)))

(defn cmd-inventory [verbs]
  "Displays the players inventory"
  (display-inventory))

(defn cmd-quit [verbs]
  "Quits the game and returns user to terminal."
  (mam-pr "\033[0mThanks for playing, friend!")
  (flush)
  (. System exit 0))

(defn cmd-bed [verbs]
  (if (= @current-room 0)
    (mam-pr "You get into bed and slowly fall to sleep. You begin dreaming of a cruel medical examination. You wake up in a pool of sweat, feeling violated.")
    (mam-pr "There is no bed here. You try to sleep standing up and just get bored.")))

(defn cmd-pull [verbs]
  "Attempts to pull something."
  (if (and (= (first verbs) 'lever) (= @current-room 2) (room-has-object? @current-room 'lever))
    (dosync
      (mam-pr "You pull the lever forwards and nothing much seems to happen. After about 10 seconds, 2 small creatures enter the room and you instantly pass out. You notice that one of the creatures drops something. You now find yourself back in the small room you started in.")
      (take-object-from-room! @current-room 'lever)
      (drop-object-in-room! @current-room 'porno)
      (set-current-room! 0))
    (mam-pr "I don't see that here.")))

(defn cmd-save [verbs]
  (save-game!)
  (mam-pr " * Game saved *"))

(defn cmd-load [verbs]
  (load-game!)
  (mam-pr " * Game loaded *"))
