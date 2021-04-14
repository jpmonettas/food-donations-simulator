(ns fd-sim.subs
  (:require [re-frame.core :refer [reg-sub]]
            [fd-sim.subs.collector :as subs.collector]
            [fd-sim.subs.donators :as subs.donators]
            [fd-sim.subs.donation-explorer :as subs.donation-explorer]
            [fd-sim.subs.ui :as subs.ui]))
         
(reg-sub :collector/balance subs.collector/balance)
(reg-sub :collector/market subs.collector/market)
(reg-sub :collector/donations subs.collector/donations)
(reg-sub :collector/dishes subs.collector/dishes) 
(reg-sub :collector/food-services subs.collector/food-services)
(reg-sub :collector/orders subs.collector/orders)
(reg-sub :collector/orders-map subs.collector/orders-map)
(reg-sub :collector/purchase-orders subs.collector/purchase-orders)
(reg-sub :collector/consumers subs.collector/consumers)
(reg-sub :collector/consumers-map subs.collector/consumers-map)
(reg-sub :collector/selected-food-service-orders
         :<- [:collector/orders]
         :<- [:collector/dishes]
         :<- [:ui/selected-food-service]
         subs.collector/selected-food-service-orders)
(reg-sub :collector/selected-food-service-consumers
         :<- [:collector/consumers]
         :<- [:collector/food-services]
         :<- [:ui/selected-food-service]
         subs.collector/selected-food-service-consumers)

(reg-sub :collector/dish-serves subs.collector/dish-serves)
(reg-sub :collector/selected-food-service-dish-serves
         :<- [:collector/dish-serves]
         :<- [:ui/selected-food-service]
         subs.collector/selected-food-service-dish-serves)

(reg-sub :collector/donation-explorer subs.collector/donation-explorer)

(reg-sub :donation-explorer/selected-donation subs.donation-explorer/selected-donation)
(reg-sub :donation-explorer/layers
         :<- [:collector/donations]
         :<- [:collector/orders-map]
         :<- [:collector/market]
         :<- [:collector/dish-serves false]
         :<- [:collector/purchase-orders]
         :<- [:collector/consumers-map]
         :<- [:collector/dishes]
         :<- [:collector/food-services]
         :<- [:donation-explorer/selected-donation]
         subs.donation-explorer/layers)

(reg-sub :donation-explorer/map-state
         :<- [:collector/donation-explorer] 
         subs.donation-explorer/map-state)


(reg-sub :ui/selected-tab subs.ui/selected-tab)
(reg-sub :ui/selected-donator subs.ui/selected-donator)
(reg-sub :ui/selected-food-service subs.ui/selected-food-service)
(reg-sub :donators/donators subs.donators/donators)


