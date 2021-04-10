(ns fd-sim.events
  (:require [re-frame.core :refer [reg-event-db] :as re-frame]
            [fd-sim.events.collector :as events.collector]
            [fd-sim.events.ui :as events.ui]
            [fd-sim.events.food-service :as events.food-service]
            [fd-sim.db :as db]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as s]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (js/Error. (str "spec check failed: " (expound/expound-str a-spec db))))))

;; sc (spec check) interceptor using `after`
(def sc (re-frame/after (partial check-and-throw ::db/db)))


(reg-event-db :collector/initialize [sc] events.collector/initialize)
(reg-event-db :collector/add-donation [sc] events.collector/add-donation)
(reg-event-db :collector/add-market-ingredient [sc] events.collector/add-market-ingredient)
(reg-event-db :collector/add-order [sc] events.collector/add-order)
(reg-event-db :collector/register-consumer [sc] events.collector/register-consumer)
(reg-event-db :collector/create-purchase-order [sc] events.collector/create-purchase-order)
(reg-event-db :collector/fill-purchase-order [sc] events.collector/fill-purchase-order)
(reg-event-db :collector/report-dish-serve [sc] events.collector/report-dish-serve)

(reg-event-db :ui/select-tab [sc] events.ui/select-tab)
(reg-event-db :ui/select-donator [sc] events.ui/select-donator)
(reg-event-db :ui/select-food-service [sc] events.ui/select-food-service)


 
