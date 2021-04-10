(ns fd-sim.events.ui)

(defn select-tab [db [_ tab-id tab]]
  (assoc-in db [:ui/selected-tab tab-id] tab))

(defn select-donator [db [_ donator-id]]
  (assoc db :ui/selected-donator donator-id))

(defn select-food-service [db [_ food-service-id]]
  (assoc db :ui/selected-food-service food-service-id)) 
