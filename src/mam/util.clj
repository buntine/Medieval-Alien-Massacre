
; util.clj
; Defines utility functions.

(ns mam.util)


(defn mam-pr
  ([s] (mam-pr s (if (@game-options :retro) 30 0)))
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
 