(ns fd-sim.subs.collector)

(defn balance [db _]
  (->> (:collector/donations db)
       (map :donation/amount)
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

(defn orders [db _]
  (let [dishes (:collector/dishes db)]
    (->> (vals (:collector/orders db))
         (map (fn [o] (assoc o :dish/name (:dish/name (get dishes (:dish/id o)))))))))

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
