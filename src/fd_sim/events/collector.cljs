(ns fd-sim.events.collector
  (:require [fd-sim.db :as db]))

(defn initialize [_ _]
  db/initial-db)

(defn add-donation [db [_ donation]]
  (update db :collector/donations conj donation))

