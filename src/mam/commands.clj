
; commands.clj
; Defines functions for handling user commands.

; I need this to allow 'mutual' interation between this namespace and
; mam.gameplay. There must be a better way of doing this!
(in-ns 'mam.gameplay)
(declare set-current-room current-room take-object
         inventory display-inventory)


(ns mam.commands
  (:use mam.gameplay)
  (:use mam.rooms))


(defn move-room [dir]
  "Attempts to move in the given direction."
  (let [i (directions (symbol dir))]
    (if (not i)
      (println "I don't understand that direction.")
      (let [room ((world-map @current-room) i)]
        (if (nil? room)
          (println "You can't go that way.")
          (set-current-room room))))))

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
(defn cmd-northwest [verbs] (move-room 'northwest))

(defn cmd-help [verbs]
  (println "  M-A-M HELP")
  (println "  ------------------------------")
  (println "   * Directions are north, east, south, west, northeaast, southeast, southwest, northeast.")
  (println "   * Or abbreviated n, e, s, w, ne, se, sw, nw.")
  (println "   * Keys automatically open the appropriate doors, so you don't need to 'unlock' them.")
  (println "   * Inspired by Dunnet, by Rob Schnell")
  (println "   * If you're wondering why you keep dying, don't worry, it's just a game.")
  (println "  ------------------------------"))

(defn cmd-take [verbs]
  "Loops through the verbs trying to match one to a takeable object"
  (if (empty? verbs)
    (println "You must supply an object to take!")
    ; Check all the remaining verbs for a match.
    (loop [objs verbs]
      (if (empty? objs)
        (println "I don't see that here...")
        (if (not (take-object (symbol (first objs))))
          (recur (rest objs)))))))

(defn cmd-drop [verbs]
  "Loops through the verbs trying to match one to a droppable object"
  (if (empty? verbs)
    (println "You must supply an object to drop!")
    (println "You can't drop that item.")))

(defn cmd-inventory [verbs]
  "Displays the players inventory"
  (if (empty? @inventory)
    (println "Your inventory is currently empty.")
    (display-inventory)))
