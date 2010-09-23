
; rooms.clj
; Defines data structures for describing rooms
; and objects.


(ns mam.rooms)

(defn print-welcome-message []
  (println "---------------------------------------------------------")
  (println "                MEDIEVAL ALIEN MASSACRE")
  (println "A sadistic, pro-death text-based adventure for children")
  (println "---------------------------------------------------------\n"))

(def rooms
  (vector
    '("You are in a dark cavern, standing in a pool of your own vomit. An arrow is protruding from your neck."
      "Dark cavern, vomit is everywhere.")
    '("The cavern has opened up into a large hall, although it's still very dark. Blood is rushing from your neck."
      "A large, dim hall. Smells of blood.")))
