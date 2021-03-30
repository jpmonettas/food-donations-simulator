(ns fd-sim.events
  (:require [re-frame.core :refer [reg-event-db]]
            [fd-sim.events.collector :as events.collector]
            [fd-sim.events.ui :as events.ui]))

(reg-event-db :collector/initialize events.collector/initialize)
(reg-event-db :collector/add-donation events.collector/add-donation)

(reg-event-db :ui/select-role events.ui/select-role)
(reg-event-db :ui/select-donator events.ui/select-donator)

 
