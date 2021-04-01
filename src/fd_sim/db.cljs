(ns fd-sim.db
  (:require [fd-sim.events.donators :as events.donators]
            [clojure.spec.alpha :as s]))

(s/def :donator/id int?)
(s/def :donator/name string?)
(s/def :food-service/id int?)
(s/def :food-service/name string?)

(s/def :ui/selected-role #{:donator :collector :food-service})
(s/def :ui/selected-donator :donator/id)
(s/def :ui/selected-food-service :food-service/id)

(s/def :sim/price number?)
(s/def :donation/id int?)
(s/def :donation/amount :sim/price)
(s/def :donation/usable-amount :sim/price)

(s/def :ingredient/id int?)
(s/def :ingredient/price :sim/price)
(s/def :ingredient/quantity int?)

(s/def :dish/id int?)
(s/def :dish/name string?)
(s/def :dish/ingredients (s/map-of :ingredient/id :ingredient/quantity))

(s/def :collector/donator (s/keys :req [:donator/id :donator/name]))
(s/def :collector/donation (s/keys :req [:donator/id
                                         :donation/id
                                         :donation/amount
                                         :donation/usable-amount]))
(s/def :collector/donations (s/coll-of :collector/donation :kind vector?))
(s/def :collector/donators (s/map-of :donator/id :collector/donator))
(s/def :collector/ingredient (s/keys :req [:ingredient/name :ingredient/price]))
(s/def :collector/market (s/map-of :ingredient/id :collector/ingredient))
(s/def :collector/dish (s/keys :req [:dish/name :dish/ingredients]))
(s/def :collector/dishes (s/map-of :dish/id :collector/dish))

(s/def ::db (s/keys :req [:ui/selected-role
                          :ui/selected-donator
                          :collector/donations
                          :collector/donators
                          :collector/dishes]))

(def initial-donators {1 {:donator/id 1 :donator/name "Juan"}
                       2 {:donator/id 2 :donator/name "Fede"}
                       3 {:donator/id 3 :donator/name "Gonza"}
                       4 {:donator/id 4 :donator/name "Nico"}})

(def initial-market {1 {:ingredient/name "Papas"
                        :ingredient/price 30.0}
                     2 {:ingredient/name "Muñato"
                        :ingredient/price 35.0}})

(def initial-dishes {1 {:dish/id 1
                        :dish/name "Ensopado"
                        :dish/ingredients {1 125
                                           2 200}}
                     2 {:dish/id 2
                        :dish/name "Papas al horno"
                        :dish/ingredients {1 500}}})

(def initial-food-services {1 {:food-service/id 1
                               :food-service/name "Olla Popular Palermo"}
                            2 {:food-service/id 2
                               :food-service/name "Olla Popular Cordon"}})

(def initial-db
  {:ui/selected-role :donator
   :ui/selected-donator (ffirst initial-donators)
   :ui/selected-food-service (ffirst initial-food-services)
   :collector/market initial-market
   :collector/donations []
   :collector/donators initial-donators
   :collector/dishes initial-dishes
   :collector/food-services initial-food-services})
