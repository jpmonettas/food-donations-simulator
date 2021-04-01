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
