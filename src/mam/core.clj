
; MEDIEVAL ALIEN MASSACRE.
; By Andrew Buntine, 2010-2014
; http://www.andrewbuntine.com
;
; core.clj
; Initialises the game and gets things moving.
; If running in a REPL, execute: (-main)

(ns mam.core
  (:gen-class)
  (:use mam.gameplay))

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

(defn -main []
  "Game initializer. Welcomes user and starts loop."
  (play-file "media/opening.wav")
  (print-welcome-message)
  (messages))
