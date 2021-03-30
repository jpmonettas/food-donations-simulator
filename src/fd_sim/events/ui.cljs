(ns fd-sim.events.ui)

(defn select-role [db [_ role]]
  (assoc db :ui/selected-role role))

(defn select-donator [db [_ user-id]]
  (assoc db :ui/selected-donator user-id))
