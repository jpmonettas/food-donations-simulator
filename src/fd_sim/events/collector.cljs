(ns fd-sim.events.collector
  (:require [fd-sim.events.donators :as events.donators]))

(defn initialize [_ _]
  {:collector/donations []
   :ui/selected-role :donator
   :ui/selected-donator (ffirst events.donators/donators)
   :collector/donators events.donators/donators})

(defn add-donation [db [_ donation]]
  (update db :collector/donations conj donation))

