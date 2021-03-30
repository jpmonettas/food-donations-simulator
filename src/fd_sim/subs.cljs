(ns fd-sim.subs
  (:require [re-frame.core :refer [reg-sub]]
            [fd-sim.subs.collector :as subs.collector]
            [fd-sim.subs.donators :as subs.donators]
            [fd-sim.subs.ui :as subs.ui]))
         
(reg-sub :collector/balance subs.collector/balance)
(reg-sub :ui/selected-role subs.ui/selected-role)
(reg-sub :ui/selected-donator subs.ui/selected-donator)
(reg-sub :donators/donators subs.donators/donators)

