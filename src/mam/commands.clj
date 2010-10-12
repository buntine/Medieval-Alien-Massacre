
; commands.clj
; Defines functions for handling user commands.

(in-ns 'mam.gameplay)
(declare set-current-room)
(declare current-room)

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

(defn cmd-north [verbs]
  (move-room 'north))

(defn cmd-east [verbs]
  (move-room 'east))

(defn cmd-south [verbs]
  (move-room 'south))

(defn cmd-west [verbs]
  (move-room 'west))

(defn cmd-northeast [verbs]
  (move-room 'northeast))

(defn cmd-southeast [verbs]
  (move-room 'southeast))

(defn cmd-southwest [verbs]
  (move-room 'southwest))

(defn cmd-northwest [verbs]
  (move-room 'northwest))

(defn cmd-help [verbs]
  (println "  M-A-M HELP")
  (println "  ------------------------------")
  (println "   * Directions can be specified in full (north, southwest, etc) or abbreviated (n, sw, etc).")
  (println "   * If you're wondering why you keep dying, don't worry, it's just a game.")
  (println "  ------------------------------"))
