(ns fd-sim.subs
  (:require [re-frame.core :refer [reg-sub]]
            [fd-sim.subs.collector :as subs.collector]))
         
(reg-sub :collector/money subs.collector/money)

