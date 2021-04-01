(ns fd-sim.subs.collector)

(defn balance [db _]
  (->> (:collector/donations db)
       (map :donation/amount)
       (reduce +)))

(defn market [db _]
  (:collector/market db))
