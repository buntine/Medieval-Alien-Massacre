
; commands.clj
; Defines functions for handling user commands.

; I need this to allow 'mutual' interation between this namespace and
; mam.gameplay. There must be a better way of doing this!
(in-ns 'mam.gameplay)
(declare set-current-room current-room take-object
         inventory display-inventory drop-object)

(ns mam.commands
  (:use mam.gameplay)
  (:use mam.rooms))


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
                 (set-current-room room)))))))]

  (defn cmd-go [verbs]
    "Expects to be given direction. Dispatches to the 'move' command"
    (if (empty? verbs)
      (println "You need to supply a direction!")
      (move-room (first verbs))))

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
  (println "   * Inspired by Dunnet, by Rob Schnell")
  (println "   * If you're wondering why you keep dying, don't worry, it's just a game.")
  (println "  ------------------------------"))

(let [try-take-drop
      (fn [verbs no-verb not-here mod-fn]
        "Loops through the verbs trying to match one to a takeable/droppable object"
        (if (empty? verbs)
          (println no-verb)
          (loop [objs verbs]
            (if (empty? objs)
              (println not-here)
              (if (not (mod-fn (first objs)))
                (recur (rest objs)))))))]
  (defn cmd-take [verbs]
    (try-take-drop verbs
                   "You must supply an item to take!"
                   "I don't see that here..."
                   take-object))
 (defn cmd-drop [verbs]
    (try-take-drop verbs
                   "You must supply an item to drop!"
                   "You don't have that item..."
                   drop-object)))

(defn cmd-inventory [verbs]
  "Displays the players inventory"
  (if (empty? @inventory)
    (println "Your inventory is currently empty.")
    (display-inventory)))
