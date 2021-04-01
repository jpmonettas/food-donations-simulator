(ns fd-sim.events
  (:require [re-frame.core :refer [reg-event-db] :as re-frame]
            [fd-sim.events.collector :as events.collector]
            [fd-sim.events.ui :as events.ui]
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

(reg-event-db :ui/select-role [sc] events.ui/select-role)
(reg-event-db :ui/select-donator [sc] events.ui/select-donator)

 
