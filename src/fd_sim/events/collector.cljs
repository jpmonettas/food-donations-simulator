(ns fd-sim.events.collector
  (:require [fd-sim.db :as db]))

(defn initialize [_ _]
  db/initial-db)

(defn add-donation [db [_ donation]]
  (update db :collector/donations conj donation))

(defn add-market-ingredient [db [_ ing]]
  (let [next-id (inc (apply max (keys (:collector/market db))))]
    (update db :collector/market assoc next-id ing)))

(defn update-market-ingredient-price [db [_ ing-id price]]
  (assoc-in db [:collector/market ing-id :ingredient/price] price))

