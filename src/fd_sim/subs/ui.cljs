(ns fd-sim.subs.ui)

(defn selected-role [db _]
  (:ui/selected-role db))

(defn selected-donator [db _]
  (:ui/selected-donator db))
