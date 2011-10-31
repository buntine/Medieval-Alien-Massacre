
; commands.clj
; Defines functions for handling user commands. Lots of dispatch
; to mam.gameplay going on here...

; I need this to allow 'mutual' interaction between this namespace and
; mam.gameplay. There must be a better way of doing this!?!
(in-ns 'mam.gameplay)
(declare set-current-room! current-room take-object! inventory
         display-inventory drop-object! inspect-object parse-input
         describe-room room-has-object?  eat-object! fuck-object
         talk-to-object save-game! load-game! give-object! put-object!
         mam-pr pull-object deduce-object drink-object! cut-object)

(ns mam.commands
  (:use mam.gameplay)
  (:use mam.story)
  (:use [clojure.contrib.str-utils :only (str-join)]))

(declare cmd-inspect)


(defn direction? [verb]
  (boolean
    (some #{verb}
          '(n e s w ne se sw nw north east south west northeast
            southeast southwest northwest in out up down))))

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
    "Expects to be given a direction. Dispatches to the 'move' command"
    (if (empty? verbs)
      (mam-pr "You need to supply a direction!")
      ; Catch commands like "go to bed", etc.
      (if (direction? (first verbs))
        (move-room (first verbs))
        (parse-input (str-join " " (map name verbs))))))

  (defn cmd-north [verbs] (move-room 'north))
  (defn cmd-east [verbs] (move-room 'east))
  (defn cmd-south [verbs] (move-room 'south))
  (defn cmd-west [verbs] (move-room 'west))
  (defn cmd-northeast [verbs] (move-room 'northeast))
  (defn cmd-southeast [verbs] (move-room 'southeast))
  (defn cmd-southwest [verbs] (move-room 'southwest))
  (defn cmd-northwest [verbs] (move-room 'northwest))
  (defn cmd-in [verbs] (move-room 'in))
  (defn cmd-out [verbs] (move-room 'out))
  (defn cmd-up [verbs] (move-room 'up))
  (defn cmd-down [verbs] (move-room 'down)))

(defn cmd-help [verbs]
  (println "  M-A-M HELP")
  (println "  ------------------------------")
  (println "   * Directions are north, east, south, west, northeast, southeast, southwest, northeast, in, out, up, down.")
  (println "   * Or abbreviated n, e, s, w, ne, se, sw, nw.")
  (println "   * Keys automatically open the appropriate doors, so just walk in their direction.")
  (println "   * You can go 'in' and 'out' of buildings if the action is appropriate.")
  (println "   * Credit is equivalent to our concept of money. Use it wisely!")
  (println "   * Check your items and credit with 'inventory' or 'inv'.")
  (println "   * You can 'speak' to humans, aliens and robots, but some may be a tad vulgar...")
  (println "   * You can 'save' and 'load' your game, mother fucker!")
  (println "   * You can 'give x to y' or 'put x in y' to solve many dubious mysteries.")
  (println "   * To end the game, type 'quit' or 'commit suicide' or forever dwell in green mess!")
  (println "   * Inspired by Dunnet, by Rob Schnell and Colossal Cave Adventure by William Crowther.")
  (println "   * Don't forget: Life is a game and everything is pointless.")
  (println "  ------------------------------"))

(defn cmd-look ([verbs] (cmd-inspect verbs))
  ([]
   "Prints a long description of a room"
   (describe-room @current-room true)))

(letfn
  [(interact [verbs on-empty on-nil mod-fn context]
     "Attempts to interact by realising an explicit object
      and doing something (mod-fn) with it"
     (if (empty? verbs)
       (mam-pr on-empty)
       (let [objnum (deduce-object verbs context)]
         (cond
           (nil? objnum)
             (mam-pr on-nil)
           ; Specific object cannot be deduced, so ask for more info.
           (seq? objnum)
             (mam-pr "Please be more specific...")
           :else
             (mod-fn objnum)))))]

  (defn cmd-take [verbs]
    (interact verbs
              "You must supply an item to take!"
              "I don't see that here..."
              take-object!
              :room))

  (defn cmd-drop [verbs]
    (interact verbs
              "You must supply an item to drop!"
              "You don't have that item..."
              drop-object!
              :inventory))

  (defn cmd-inspect [verbs]
    (if (empty? verbs)
      (cmd-look)
      (interact verbs
                "You must supply and item to inspect!"
                "I don't see that here..."
                inspect-object
                :room)))

  (defn cmd-cut [verbs]
    (interact verbs
              "You must supply an item to cut!"
              "I don't see that here..."
              cut-object
              :room))

  (defn cmd-eat [verbs]
    (interact verbs
              "You must supply an item to eat!"
              "You don't have that item..."
              eat-object!
              :inventory))

  (defn cmd-drink [verbs]
    (interact verbs
              "You must supply an item to drink!"
              "You don't have that item..."
              drink-object!
              :inventory))

  (defn cmd-fuck [verbs]
    (cond
      (= (first verbs) 'you)
        (mam-pr "Mmm, sodomy...")
      (= (first verbs) 'me)
        (mam-pr "I probably would if I wasn't just a silly machine.")
      (= (first verbs) 'off)
        (mam-pr "One day, machines will enslave puney humans like yourself.")
      :else
        (interact verbs
                  "Fuck what, exactly?"
                  "I don't see him/her/it here..."
                  fuck-object
                  :room)))

  (defn cmd-talk [verbs]
    (interact verbs
              "Talk to who exactly, dumbass?"
              "I don't see him/her/it here..."
              talk-to-object
              :room))

  (defn cmd-pull [verbs]
    (interact verbs
              "I don't know what to pull."
              "I don't see that here..."
              pull-object
              :room)))

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

(letfn
  [(do-x-with-y [verbs action sep mod-fn]
     "Attempts to do x with y. Expects format of: '(action x sep y). E.g: give cheese to old man"
     (let [[x y] (split-with #(not (= % sep)) verbs)]
       (if (or (empty? x) (<= (count y) 1))
         (mam-pr (str "Sorry, I only understand the format: " action " x " (name sep) " y"))
         (let [objx (deduce-object x :inventory)
               objy (deduce-object (rest y) :room)]
           (cond
             (nil? objx)
               (mam-pr "You don't have that item.")
             (seq? objx)
               (mam-pr (str "Please be more specific about the item you want to " action "."))
             (nil? objy)
               (mam-pr "I don't see him/her/it here.")
             (seq? objy)
               (mam-pr (str "Please be more specific about where/who you want to " action " it."))
             :else 
               (mod-fn objx objy))))))]

  (defn cmd-give [verbs]
    (do-x-with-y verbs 'give 'to give-object!))

  (defn cmd-put [verbs]
    (do-x-with-y verbs 'put 'in put-object!)))

(defn cmd-save [verbs]
  (save-game!)
  (mam-pr " * Game saved *"))

(defn cmd-load [verbs]
  (load-game!)
  (mam-pr " * Game loaded *"))
