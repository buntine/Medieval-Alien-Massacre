
; gameplay.clj
; Handles all aspects of gameplay including prompts,
; command parsing, saves, loads, etc.

(ns mam.gameplay
  (:use mam.util)
  (:use mam.state)
  (:use mam.state)
  (:use mam.commands)
  (:use mam.story)
  (:use mam.compression)
  (:use [clojure.string :only (split)])
  (:import (java.applet Applet))
  (:import (java.io File))
  (:import (java.net URL)))

; Declarations for some procedures I mention before they have been
; defined.
(declare messages)

(def ignore-words '(that is the   ; Words that should be ignored in commands.
                    fucking damn)) 
  
(defn verb-parse [verb-lst]
  "Calls the procedure identified by the first usable verb. Returns
   false if the command is not understood"
  (let [f (fn-for-command (first verb-lst))
        verbs (rest verb-lst)]
    (if (empty? verb-lst)
      false
      (if f
        (and (f verbs) true)
        (recur verbs)))))

(defn command->seq [s]
  "Translates the given string to a sequence of symbols, removing ignored words"
  (let [verbs (split #"\s+" s)]
    (filter #(not (some #{%} ignore-words))
            (map symbol verbs))))

(defn parse-input [s]
  "Parses the user input"
  (if (not (empty? s))
    (let [cmd (command->seq s)
          orig-room @current-room]
      (if (false? (verb-parse cmd))
        (mam-pr "I don't understand that."))
      (newline)
      (messages (not (= orig-room @current-room))))
    (messages false)))

(defn request-command []
  "Sends a terminal escape sequence (green text), reads a command, and then resets the colour"
  (print "\033[1;32m")
  (flush)
  (let [cmd (read-line)]
    (flush)
    (print "\033[0m")
    cmd))

(defn messages ([] (messages true))
  ([verbose]
   "Describes current room and prompts for user input"
   (when verbose
     (describe-room @current-room)
     (newline))
   (print "> ")
   (flush)
   (parse-input (request-command))))
