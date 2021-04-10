(ns fd-sim.db
  (:require [fd-sim.events.donators :as events.donators]
            [clojure.spec.alpha :as s]))

(s/def :consumer/id string?)
(s/def :consumer/name string?)
(s/def :profile/picture string?)

(s/def :donator/id int?)
(s/def :donator/name string?)

(s/def :food-service/id int?)
(s/def :food-service/name string?)

(s/def :order/id int?)
(s/def :order/quantity int?)
(s/def :order/status #{:open :filled})
(s/def :food-service/order (s/keys :req [:order/id :dish/id :order/quantity :food-service/id]))

(s/def :ui.tabs/main #{:donator :collector :food-service :transparency})
(s/def :ui.tabs/collector #{:market-dishes :orders :serves :consumers :donations})
(s/def :ui.tabs/food-service #{:orders-serves :consumers})
(s/def :ui.tabs/transparency #{:donation-explorer})
(s/def :ui/selected-tab (s/keys :req-un [:ui.tabs/main
                                         :ui.tabs/collector
                                         :ui.tabs/food-service
                                         :ui.tabs/transparency]))

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
(s/def :collector/ingredient (s/keys :req [:ingredient/name]))
(s/def :collector/market (s/map-of :ingredient/id :collector/ingredient))
(s/def :collector/dish (s/keys :req [:dish/name :dish/ingredients]))
(s/def :collector/dishes (s/map-of :dish/id :collector/dish))
(s/def :collector/orders (s/map-of :order/id :food-service/order))
(s/def :food-service/consumer (s/keys :req [:consumer/id :consumer/name :profile/picture :food-service/id]))
(s/def :collector/consumers (s/map-of :consumer/id :food-service/consumer))

(s/def :purchase-order/id int?)
(s/def :purchase-order/orders (s/coll-of :order/id))
(s/def :purchase-order/ingredients (s/map-of :ingredient/id :ingredient/quantity))
(s/def :purchase-order/fill (s/map-of :ingredient/id :ingredient/price))
(s/def :collector/purchase-order (s/keys :req [:purchase-order/id
                                               :purchase-order/orders
                                               :purchase-order/ingredients]
                                         :opt [:purchase-order/fill]))
(s/def :collector/purchase-orders (s/map-of :purchase-order/id :collector/purchase-order))
(s/def :collector/dish-serve (s/keys :req [:order/id :consumer/id]))
(s/def :collector/dish-serves (s/coll-of :collector/dish-serve))

(s/def ::db (s/keys :req [:ui/selected-tab
                          :ui/selected-donator
                          :ui/selected-food-service
                          :collector/market
                          :collector/donations
                          :collector/donators
                          :collector/dishes
                          :collector/food-services
                          :collector/orders
                          :collector/consumers
                          :collector/dish-serves
                          :food-service/current-serves]))

(def initial-donators {1 {:donator/id 1 :donator/name "Juan"}
                       2 {:donator/id 2 :donator/name "Fede"}
                       3 {:donator/id 3 :donator/name "Gonza"}
                       4 {:donator/id 4 :donator/name "Nico"}})

(def initial-market {1 {:ingredient/name "Papas"}
                     2 {:ingredient/name "Muñato"}})

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

(def initial-orders {1 {:order/id 1
                        :food-service/id 1
                        :dish/id 1
                        :order/quantity 10
                        :order/status :open}})

(def initial-consumers {"4.875.532-2" {:consumer/id "4.875.532-2"
                                       :consumer/name "Martin"
                                       :profile/picture "https://images.pexels.com/photos/220453/pexels-photo-220453.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500"
                                       :food-service/id 1}
                        "2.875.534-9" {:consumer/id "2.875.534-9"
                                       :consumer/name "Maria"
                                       :profile/picture "https://media.istockphoto.com/photos/portrait-of-a-girl-picture-id938709362?k=6&m=938709362&s=612x612&w=0&h=mUQAOuFjTUhvykTbkpXXERePajEWvVqOM2tR3gwS3II="
                                       :food-service/id 1}
                        "3.975.534-2" {:consumer/id "3.975.534-2"
                                       :consumer/name "Jorge"
                                       :profile/picture "https://images.pexels.com/photos/2078265/pexels-photo-2078265.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500"
                                       :food-service/id 2}})

(def initial-donations [{:donator/id 1
                         :donation/id 1
                         :donation/amount 100
                         :donation/usable-amount 100}
                        {:donation/id 2
                         :donator/id 2                         
                         :donation/amount 55
                         :donation/usable-amount 55}
                        {:donation/id 3
                         :donator/id 3                        
                         :donation/amount 177
                         :donation/usable-amount 177}])

(def initial-db
  {:ui/selected-tab {:main :donator
                     :collector :market-dishes
                     :food-service :orders-serves
                     :transparency :donation-explorer}
   :ui/selected-donator (ffirst initial-donators)
   :ui/selected-food-service (ffirst initial-food-services)
   :collector/market initial-market
   :collector/donations initial-donations
   :collector/donators initial-donators
   :collector/dishes initial-dishes
   :collector/food-services initial-food-services
   :collector/orders initial-orders
   :collector/consumers initial-consumers
   :collector/dish-serves []
   :food-service/current-serves {}})
