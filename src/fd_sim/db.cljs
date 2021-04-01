(ns fd-sim.db
  (:require [fd-sim.events.donators :as events.donators]
            [clojure.spec.alpha :as s]))

(s/def :user/id int?)

(s/def :ui/selected-role #{:donator :collector :food-service})
(s/def :ui/selected-donator :user/id)

(s/def :sim/price float?)
(s/def :donation/amount :sim/price)
(s/def :ingredient/price :sim/price)

(s/def :collector/user (s/keys :req [:user/id :user/name]))
(s/def :collector/donator :collector/user)
(s/def :collector/donation (s/keys :req [:user/id :donation/amount]))
(s/def :collector/donations (s/coll-of :collector/donation))
(s/def :collector/donators (s/map-of :user/id :collector/donator))
(s/def :ingredient/id int?)
(s/def :collector/ingredient (s/keys :req [:ingredient/name :ingredient/price]))
(s/def :collector/market (s/map-of :ingredient/id :collector/ingredient))
(s/def ::db (s/keys :req [:collector/donations
                          :ui/selected-role
                          :ui/selected-donator
                          :collector/donators]))

(def initial-market {1 {:ingredient/name "Papas"
                        :ingredient/price 30.0}
                     2 {:ingredient/name "Muñato"
                        :ingredient/price 35.0}})

(def initial-db
  {:ui/selected-role :donator
   :ui/selected-donator (ffirst events.donators/donators)
   :collector/market initial-market
   :collector/donations []
   :collector/donators events.donators/donators})
