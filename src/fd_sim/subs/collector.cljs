(ns fd-sim.subs.collector)

(defn balance [db _]
  (->> (:collector/donations db)
       (map :donation/amount)
       (reduce +)))

(defn market [db _]
  (:collector/market db))

(defn donations [db [_ {:keys [user/id]}]]
  (cond->> (:collector/donations db)
    id (filter #(= (:user/id %) id))))
