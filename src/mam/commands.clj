
; commands.clj
; Defines functions for handling user commands.

(ns mam.commands)


(defn cmd-go [verbs]
  "Expects to be given direction. Dispatches to the correct function"
  (println "You can't go that way.")
  true)

(defn cmd-north [verbs]
  false)

(defn cmd-east [verbs]
  false)

(defn cmd-south [verbs]
  false)

(defn cmd-west [verbs]
  false)

(defn cmd-northeast [verbs]
  false)

(defn cmd-southeast [verbs]
  false)

(defn cmd-southwest [verbs]
  false)

(defn cmd-northwest [verbs]
  false)
