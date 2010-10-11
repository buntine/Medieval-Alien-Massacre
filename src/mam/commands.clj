
; commands.clj
; Defines functions for handling user commands.

(ns mam.commands)

(defn move-room [dir]
  "Attempts to move in the given direction."
  (println "You can't go that way."))

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
