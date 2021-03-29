(ns fd-sim.events
  (:require [re-frame.core :refer [reg-event-db]]
            [fd-sim.events.collector :as events.collector]))

(reg-event-db :collector/initialize events.collector/initialize)
(reg-event-db :collector/add-money events.collector/add-money)

 
