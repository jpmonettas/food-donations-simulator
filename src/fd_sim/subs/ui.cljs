(ns fd-sim.subs.ui)

(defn selected-tab [db [_ tab-id]]
  (get-in db [:ui/selected-tab tab-id]))

(defn selected-donator [db _]
  (:ui/selected-donator db))

(defn selected-food-service [db _]
  (:ui/selected-food-service db))
