(ns fd-sim.subs.collector)

(defn balance [db _]
  (->> (:collector/donations db)
       (map :donation/usable-amount)
       (reduce +)))

(defn market [db _]
  (:collector/market db))

(defn donations [db [_ {:keys [donator/id]}]]
  (cond->> (:collector/donations db)
    id (filter #(= (:donator/id %) id))))

(defn dishes [db [_ _]]
  (:collector/dishes db))

(defn food-services [db [_ _]]
  (:collector/food-services db))

(defn open-order?
  "A order is open if there isn't any purchase-order that includes it and is also filled"
  [db order-id]
  (empty? (some (fn [o]
                  (and (contains? (:purchase-order/orders o) order-id)
                       (:purchase-order/fill o)))
                (vals (:collector/purchase-orders db)))))

(defn orders [db _]
  (let [dishes (:collector/dishes db)]
    (->> (vals (:collector/orders db))
         (map (fn [o]
                (-> o
                    (assoc :dish/name (:dish/name (get dishes (:dish/id o)))
                           :order/status (if (open-order? db (:order/id o))
                                           :open
                                           :filled))))))))

(defn selected-food-service-orders [[orders dishes food-service-id] _]
  (->> orders
       (filter #(= food-service-id (:food-service/id %)))
       (map (fn [o] (assoc o :dish/name (:dish/name (get dishes (:dish/id o))))))))

(defn consumers [db _]
  (let [food-services (:collector/food-services db)]
   (->> (vals (:collector/consumers db))
        (map (fn [c]
               (assoc c :food-service/name (:food-service/name (get food-services (:food-service/id c)))))))))

(defn selected-food-service-consumers [[consumers food-services food-service-id]]
  (->> consumers
       (map (fn [c]
              (assoc c :food-service/name (:food-service/name (get food-services (:food-service/id c))))))))

(defn purchase-orders [db _]
  (vals (:collector/purchase-orders db)))

(defn dish-serves [db _]
  (->> (:collector/dish-serves db)
       (map (fn [ds]
              (let [order (get-in db [:collector/orders (:order/id ds)])
                    dish (get-in db [:collector/dishes (:dish/id order)])
                    consumer (get-in db [:collector/consumers (:consumer/id ds)])]
                (assoc ds
                       :consumer/name   (:consumer/name consumer)
                       :profile/picture (:profile/picture consumer)
                       :food-service/id (:food-service/id order)
                       :dish/name (:dish/name dish)))))))

(defn selected-food-service-dish-serves [[dish-serves selected-food-service] _]
  (filter #(= (:food-service/id %) selected-food-service) dish-serves))
