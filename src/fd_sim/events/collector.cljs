(ns fd-sim.events.collector
  (:require [fd-sim.db :as db]))

(defn initialize [_ _]
  db/initial-db)

(defn add-donation [db [_ donation]]
  (let [next-id (if-not (empty? (:collector/donations db))
                  (inc (:donation/id (last (:collector/donations db))) )
                  0)]
    (update db :collector/donations conj (assoc donation
                                                :donation/usable-amount (:donation/amount donation)
                                                :donation/id next-id))))

(defn add-market-ingredient [db [_ ing]]
  (let [next-id (inc (apply max (keys (:collector/market db))))]
    (update db :collector/market assoc next-id ing)))

(defn update-market-ingredient-price [db [_ ing-id price]]
  (assoc-in db [:collector/market ing-id :ingredient/price] price))

(defn add-order [db [_ order]]
  (let [next-order-id (->> (keys (:collector/orders db))
                           (apply max)
                           inc)
        order (assoc order
                     :order/status :waiting
                     :order/id next-order-id)]
    (update db :collector/orders assoc next-order-id order)))

(defn register-consumer [db [_ consumer]]
  (update db :collector/consumers assoc (:consumer/id consumer) consumer))
