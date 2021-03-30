(ns fd-sim.subs.collector)

(defn balance [db _]
  (->> (:collector/donations db)
       (map :amount)
       (reduce +)))

