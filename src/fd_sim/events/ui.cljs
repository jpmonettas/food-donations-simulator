(ns fd-sim.events.ui)

(defn select-role [db [_ role]]
  (assoc db :ui/selected-role role))

(defn select-donator [db [_ donator-id]]
  (assoc db :ui/selected-donator donator-id))

(defn select-food-service [db [_ food-service-id]]
  (assoc db :ui/selected-food-service food-service-id))
