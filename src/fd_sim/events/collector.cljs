(ns fd-sim.events.collector
  (:require [fd-sim.db :as db]
            [fd-sim.subs.collector :as subs.collector]))

(defn initialize [_ _]
  db/initial-db)

(defn add-donation [db [_ donation]]
  (let [next-id (if-not (empty? (:collector/donations db))
                  (inc (:donation/id (last (:collector/donations db))) )
                  0)]
    (update db :collector/donations conj (assoc donation
                                                :donation/usable-amount (:donation/amount donation)
                                                :donation/id next-id))))

(defn add-market-ingredient [db [_ ing]]
  (let [next-id (inc (apply max (keys (:collector/market db))))]
    (update db :collector/market assoc next-id ing)))

(defn add-order [db [_ order]]
  (let [next-order-id (->> (keys (:collector/orders db))
                           (apply max)
                           inc)
        order (assoc order
                     :order/status :open
                     :order/id next-order-id)]
    (update db :collector/orders assoc next-order-id order)))

(defn register-consumer [db [_ consumer]]
  (update db :collector/consumers assoc (:consumer/id consumer) consumer))

(defn create-purchase-order [db [_]]
  (let [open-orders (->> (vals (:collector/orders db))
                         (filter (fn [o] (subs.collector/open-order? db (:order/id o)))))
        po-ingredients (->> open-orders
                            (map (fn [o]
                                   (let [dish-ingrs (get-in db [:collector/dishes (:dish/id o) :dish/ingredients])]
                                     ;; each order dish ingredients multiplied by the order requested dishes
                                     (->> dish-ingrs
                                          (map (fn [[ing-id ing-qty]]
                                                 [ing-id (* ing-qty (:order/quantity o))]))
                                          (into {})))))
                            (apply merge-with +))
        next-id (if-let [max-id (->> (keys (:collector/purchase-orders db))
                                     (apply max))]
                  (inc max-id)
                  0)        
        po {:purchase-order/id next-id
            :purchase-order/orders (into #{} (map :order/id open-orders))
            :purchase-order/ingredients po-ingredients}]
    (update db :collector/purchase-orders assoc next-id po)))

(defn consume-donations [donations spent-amount]
  ;; this is inefficient but will work for the simulator
  (let [{:keys [processed-donations remaining-amount]}
        (->> donations
             (reduce (fn [{:keys [processed-donations remaining-amount] :as r} {:keys [donation/usable-amount] :as donation}]
                       (if (zero? remaining-amount)
                         (update r :processed-donations conj donation)
                         (if (<= usable-amount remaining-amount)
                           ;; if this donation is not enough
                           {:processed-donations (conj processed-donations (assoc donation :donation/usable-amount 0))
                            :remaining-amount (- remaining-amount usable-amount)}

                           ;; else this donation is enough to pay the rest, pay and leave the change in the donation
                           {:processed-donations (conj processed-donations (update donation :donation/usable-amount #(- % remaining-amount)))
                            :remaining-amount 0})))
                     {:processed-donations []
                      :remaining-amount spent-amount}))]
    
    (if (zero? remaining-amount)
      ;; we were able to pay everything, return the update donations
      processed-donations
      
      ;; not enough donations to pay for spent-amount
      (throw (ex-info "Not enough donations to pay for spent-amount" {})))))

(defn fill-purchase-order [db [_ po-id order-fill]]
  (try 
   (let [spent-amount (reduce + (vals order-fill))]
     (-> db
         (update :collector/donations consume-donations spent-amount)
         (assoc-in [:collector/purchase-orders po-id :purchase-order/fill] order-fill)))
   (catch js/Error e
     (js/console.error (.-message e))
     db)))

(defn report-dish-serve [db [_ order-id consumer-id]]
  (update db :collector/dish-serves conj {:order/id order-id
                                          :consumer/id consumer-id}))
