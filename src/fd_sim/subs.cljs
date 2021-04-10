(ns fd-sim.subs
  (:require [re-frame.core :refer [reg-sub]]
            [fd-sim.subs.collector :as subs.collector]
            [fd-sim.subs.donators :as subs.donators]
            [fd-sim.subs.ui :as subs.ui]))
         
(reg-sub :collector/balance subs.collector/balance)
(reg-sub :collector/market subs.collector/market)
(reg-sub :collector/donations subs.collector/donations)
(reg-sub :collector/dishes subs.collector/dishes) 
(reg-sub :collector/food-services subs.collector/food-services)
(reg-sub :collector/orders subs.collector/orders)
(reg-sub :collector/purchase-orders subs.collector/purchase-orders)
(reg-sub :collector/consumers subs.collector/consumers)
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


(reg-sub :ui/selected-tab subs.ui/selected-tab)
(reg-sub :ui/selected-donator subs.ui/selected-donator)
(reg-sub :ui/selected-food-service subs.ui/selected-food-service)
(reg-sub :donators/donators subs.donators/donators)


