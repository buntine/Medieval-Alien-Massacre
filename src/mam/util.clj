
; util.clj
; Defines utility functions.

(ns mam.util
  (:use [clojure.string :only (join)]))


(defn mam-pr
  ;([s] (mam-pr s (if (@game-options :retro) 30 0))) ; TODO: FIX THIS (game-options needs to be seen here)
  ([s] (mam-pr s 30))
  ([s i]
   "Prints a string like the ancient terminals used to, sleeping for i ms per character"
   (if (< i 5)
     (println s)
     (do
       (doseq [c s]
         (print c)
         (flush)
         (. Thread sleep i))
       (newline)))))
 
(defn print-with-newlines
  ([lines] (print-with-newlines lines ""))
  ([lines prepend]
   "Prints a sequence of strings, separated by newlines. Only useful for side-effects"
   (if (not (empty? prepend))
     (mam-pr prepend))
   (mam-pr (str " - " (join "\n - " lines)))))
