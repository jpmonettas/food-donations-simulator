(ns fd-sim.events.collector
  (:require [fd-sim.db :as db]
            [fd-sim.subs.collector :as subs.collector]))

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

(defn add-order [db [_ order]]
  (let [next-order-id (->> (keys (:collector/orders db))
                           (apply max)
                           inc)
        order (assoc order
                     :order/status :open
                     :order/id next-order-id)]
    (update db :collector/orders assoc next-order-id order)))

(defn register-consumer [db [_ consumer]]
  (update db :collector/consumers assoc (:consumer/id consumer) consumer))

(defn create-purchase-order [db [_]]
  (let [open-orders (->> (vals (:collector/orders db))
                         (filter (fn [o] (subs.collector/open-order? db (:order/id o)))))
        po-ingredients (->> open-orders
                            (map (fn [o]
                                   (let [dish-ingrs (get-in db [:collector/dishes (:dish/id o) :dish/ingredients])]
                                     ;; each order dish ingredients multiplied by the order requested dishes
                                     (->> dish-ingrs
                                          (map (fn [[ing-id ing-qty]]
                                                 [ing-id (* ing-qty (:order/quantity o))]))
                                          (into {})))))
                            (apply merge-with +))
        next-id (if-let [max-id (->> (keys (:collector/purchase-orders db))
                                     (apply max))]
                  (inc max-id)
                  0)        
        po {:purchase-order/id next-id
            :purchase-order/orders (into #{} (map :order/id open-orders))
            :purchase-order/ingredients po-ingredients}]
    (update db :collector/purchase-orders assoc next-id po)))

(defn fill-purchase-order [db [_ po-id order-fill]]  
  (assoc-in db [:collector/purchase-orders po-id :purchase-order/fill] order-fill))
