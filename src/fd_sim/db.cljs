(ns fd-sim.db
  (:require [fd-sim.events.donators :as events.donators]
            [clojure.spec.alpha :as s]))

(s/def :user/id int?)

(s/def :collector/user (s/keys :req [:user/id :user/name]))

(s/def :collector/donator :collector/user)

(s/def :donation/amount float?)

(s/def :collector/donation (s/keys :req [:user/id :donation/amount]))

(s/def :collector/donations (s/coll-of :collector/donation))
(s/def :ui/selected-role #{:donator :collector :food-service})
(s/def :ui/selected-donator :user/id)
(s/def :collector/donators (s/map-of :user/id :collector/donator))

(s/def ::db (s/keys :req [:collector/donations
                          :ui/selected-role
                          :ui/selected-donator
                          :collector/donators]))

(def initial-db
  {:collector/donations []
   :ui/selected-role :donator
   :ui/selected-donator (ffirst events.donators/donators)
   :collector/donators events.donators/donators})
