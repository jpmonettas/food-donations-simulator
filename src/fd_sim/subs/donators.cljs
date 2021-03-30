(ns fd-sim.subs.donators)

(defn donators [db _]
  (:collector/donators db))
