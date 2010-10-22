
; commands.clj
; Defines functions for handling user commands.

; I need this to allow 'mutual' interation between this namespace and
; mam.gameplay. There must be a better way of doing this!
(in-ns 'mam.gameplay)
(declare set-current-room! current-room take-object! inventory
         display-inventory drop-object! inspect-object parse-input
         describe-room room-has-object? drop-object-in-room!
         take-object-from-room!)

(ns mam.commands
  (:use mam.gameplay)
  (:use mam.rooms))

(declare cmd-inspect)


(defn direction? [verb]
  (boolean
    (some #{verb}
          '(n e s w ne se sw nw north east south west
            northeast southeast southwest northwest))))

(let [move-room
     (fn [dir]
       "Attempts to move in the given direction."
       (let [i (directions dir)]
         (if (not i)
           (println "I don't understand that direction.")
           (let [room ((world-map @current-room) i)]
             (if (nil? room)
               (println "You can't go that way.")
               (if (fn? room)
                 (room)
                 (set-current-room! room)))))))]

  (defn cmd-go [verbs]
    "Expects to be given direction. Dispatches to the 'move' command"
    (if (empty? verbs)
      (println "You need to supply a direction!")
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
  (println "   * Keys automatically open the appropriate doors, so you don't need to 'unlock' them.")
  (println "   * Credit is equivalent to our concept of money. Use it wisely!")
  (println "   * To end the game, type 'quit' or 'suicide' or forever dwell in green mess!")
  (println "   * You can 'save' and 'load' your game, mother fucker!")
  (println "   * Inspired by Dunnet, by Rob Schnell")
  (println "   * If you're wondering why you keep dying, don't worry, it's just a game.")
  (println "  ------------------------------"))

(defn cmd-look ([verbs] (cmd-inspect verbs))
  ([]
   "Prints a long description of a room"
   (describe-room @current-room true)))

(defn teen-takes-porno []
  "Occurs when the player drops the porno in the teenagers room"
  (dosync
    (newline)
    (println "The teenagers eyes explode!! He quickly picks up the porno mag and runs away. He throws a green keycard in your general direction as he leaves the room.")
    (take-object-from-room! @current-room 'porno)
    (take-object-from-room! @current-room 'teenager)
    (drop-object-in-room! @current-room 'keycard)))

(defn drop-check [verbs]
  "Checks if the conditions are right for a special occurrence"
  (if (and (= @current-room 7) (room-has-object? @current-room 'porno))
    (teen-takes-porno)))

(let [try-interact
      (fn [verbs no-verb not-here mod-fn]
        "Loops through the verbs trying to match one to a interactable object"
        (if (empty? verbs)
          (println no-verb)
          (loop [objs verbs]
            (if (empty? objs)
              (println not-here)
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
                    inspect-object))))

(defn cmd-inventory [verbs]
  "Displays the players inventory"
  (display-inventory))

(defn cmd-quit [verbs]
  "Quits the game and returns user to terminal."
  (println "\033[0m Thanks for playing, friend!")
  (flush)
  (. System exit 0))

(defn cmd-bed [verbs]
  (if (= @current-room 0)
    (println "You get into bed and slowly fall to sleep. You begin dreaming of a cruel medical examination. You wake up in a pool of sweat, feeling violated.")
    (println "There is no bed here. You try to sleep standing up and just get bored.")))

(defn cmd-pull [verbs]
  "Attempts to pull something."
  (if (and (= (first verbs) 'lever) (= @current-room 2) (room-has-object? @current-room 'lever))
    (dosync
      (println "You pull the lever forwards and nothing much seems to happen. After about 10 seconds, 2 small creatures enter the room and you instantly pass out. You notice that one of the creatures drops something. You now find yourself back in the small room you started in.")
      (take-object-from-room! @current-room 'lever)
      (drop-object-in-room! @current-room 'porno)
      (set-current-room! 0))
    (println "I don't see that here.")))
