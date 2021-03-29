(ns fd-sim.events.collector)

(defn initialize [_ _]
  {:money 0})

(defn add-money [db [_ amount]]
  (update db :money #(+ % amount)))

