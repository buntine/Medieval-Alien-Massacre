
; util.clj
; Defines utility functions.

(in-ns 'mam.gameplay)

(ns mam.util
  (:import (java.applet Applet))
  (:import (java.io File))
  (:import (java.net URL))
  (:use [clojure.string :only (join)]))

(defn play-url [url-string]
  "Plays audio from the specified URL"
  (.play (Applet/newAudioClip (URL. url-string))))

(defn play-file [file-name]
  "Plays audio from the specified local file"
  (let [absolute-name (.getAbsolutePath (File. file-name))
        url-string (str "file://" absolute-name)]
    (play-url url-string)))

(defn mam-pr [s i]
   "Prints a string like the ancient terminals used to, sleeping for i ms per character"
   (if (< i 5)
     (println s)
     (do
       (doseq [c s]
         (print c)
         (flush)
         (. Thread sleep i))
       (newline))))
 
(defn print-with-newlines
  ([lines] (print-with-newlines lines ""))
  ([lines prepend]
   "Prints a sequence of strings, separated by newlines. Only useful for side-effects"
   (if (not (empty? prepend))
     (mam-pr prepend))
   (mam-pr (str " - " (join "\n - " lines)))))

(defn print-welcome-message []
  (println "\033[31m|---------------------------------------------------------|\033[0m")
  (println "\033[31m|\033[0m                \033[4;33mMEDIEVAL ALIEN MASSACRE\033[0m                  \033[31m|\033[0m")
  (println "\033[31m|---------------------------------------------------------|\033[0m")
  (println "\033[31m|\033[0m \033[33mA sadistic, pro-death text-based adventure for children\033[0m \033[31m|\033[0m")
  (println "\033[31m|\033[0m \033[33mBy Andrew Buntine (http://www.andrewbuntine.com)\033[0m        \033[31m|\033[0m")
  (println "\033[31m|\033[0m                                                         \033[31m|\033[0m")
  (println "\033[31m|\033[0m \033[33mRated X^3 (18+) for:\033[0m                                    \033[31m|\033[0m")
  (println "\033[31m|\033[0m   \033[33m* grotesque sexual violence\033[0m                           \033[31m|\033[0m")
  (println "\033[31m|\033[0m   \033[33m* murder without end\033[0m                                  \033[31m|\033[0m")
  (println "\033[31m|\033[0m   \033[33m* zombie goat sodomy\033[0m                                  \033[31m|\033[0m")
  (println "\033[31m|\033[0m                                                         \033[31m|\033[0m")
  (println "\033[31m|\033[0m \033[33mType 'help' if you're a pussy.\033[0m                          \033[31m|\033[0m")
  (println "\033[31m|---------------------------------------------------------|\033[0m\n"))

(defn direction? [verb]
  (boolean
    (some #{verb}
          '(n e s w ne se sw nw north east south west northeast
            southeast southwest northwest in out up down))))
