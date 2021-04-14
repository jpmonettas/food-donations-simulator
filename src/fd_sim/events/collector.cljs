(ns fd-sim.events.collector
  (:require [fd-sim.db :as db]
            [fd-sim.subs.collector :as subs.collector]
            [clojure.string :as str]
            [goog.string :as gstr]))

(defn initialize [_ _]
  db/initial-db)

(defn add-donation [db [_ donator-id donation-amount]]
  (let [next-id (if-not (empty? (:collector/donations db))
                  (inc (:donation/id (last (:collector/donations db))) )
                  0)]
    (update db :collector/donations conj {:donator/id donator-id
                                          :donation/amount donation-amount
                                          :donation/usable-amount donation-amount
                                          :donation/id next-id})))

(defn add-market-ingredient [db [_ ing]]
  (let [next-id (inc (apply max (keys (:collector/market db))))]
    (update db :collector/market assoc next-id ing)))

(defn add-order [db [_ order]]
  (let [next-order-id (if-not (empty? (:collector/orders db))
                        (->> (keys (:collector/orders db))
                             (apply max)
                             inc)
                        0)
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

(defn pay-dish-ingredients
  "Try to pay as much or the `serve` as we can with `donation`, return a map with
  updated :serve and :donation"
  [serve donation]
  (loop [[ing & rem-ing :as to-process-ing] (:dish/paid-ingredients serve)
         d donation
         processed-ing []]
    (if (and ing (pos? (:donation/usable-amount d)))

      ;; if we still have money in d and ingredients to try to pay
      (let [[_ price paid-by] ing]        
        (if (and (not paid-by)
                 (>= (:donation/usable-amount d) price))
          ;; if we can pay this ingredient price and hasn't been already paid
          (recur rem-ing
                 (update d :donation/usable-amount #(- % price))
                 (conj processed-ing (assoc ing 2 (:donation/id d)))) ;; set this was paid by this donation

          ;; if we can't pay this ing, or has been already paid skip to the next
          (recur rem-ing
                 d
                 (conj processed-ing ing))))
      
      ;; no more ingredients or no more money in d
      {:serve (assoc serve :dish/paid-ingredients (into [] (into to-process-ing processed-ing)))
       :donation d})))

(defn pay-dish-serves
  "Try to pay as much `serves` as we can with `donation`, return a map with
  updated :serves and :donation"
  [serves donation]
  (loop [[s & rem-serves :as to-proc-serves] serves
         d donation
         proc-serves []]
    (if (and s (pos? (:donation/usable-amount d)))
      
      ;; if we still have money in d and serves to try to pay
      (let [{updated-serve :serve updated-donation :donation} (pay-dish-ingredients s d)]
        (recur rem-serves
               updated-donation
               (conj proc-serves updated-serve)))
      
      ;; no more serves to process or no more money in d
      {:donation d :serves (into [] (into to-proc-serves proc-serves))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; - grab one donation and process as much dishes_ingredients as it can
;; - do the same for each donation until we have paid the purchase-order
;; - join the resulting dishes collection into :collector/dish-serves
;; - when reporting serves, add the consumer/id to the next free :served-to for the order-id
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pay-dishes-with-donations
  "Try to pay as much unpayed :collector/dish-serves as we can with usable donations."
  [db]

  (reduce (fn [rdb {:keys [donation/usable-amount] :as donation}]
            (if (zero? usable-amount)
              ;; if this donation has no money do nothing
              rdb

              ;; if we have some usable amount, try to pay with it as much serves as we can
              (let [{updated-serves :serves updated-donation :donation} (pay-dish-serves (:collector/dish-serves rdb)
                                                                                         donation)]
                (-> rdb
                    (assoc :collector/dish-serves updated-serves)
                    (assoc-in [:collector/donations (:donation/id donation)] updated-donation)))))
   db
   (:collector/donations db)))
 
;; - generate all dishes for a purchase-order
;; [{:order/id 1
;;   :dish/paid-ingredients [[1 $10 nil] <- ingr_id, price_ingr_this dish paid_by_donation_id
;;                           [2 $20 nil]]
;;   :consumer/id nil}
;;  ...]
(defn purchase-orders-dishes [db purchase-order-id purchase-order-fill]
  (let [dishes-map (get db :collector/dishes)
        all-orders-map (get db :collector/orders)
        purchase-order (get-in db [:collector/purchase-orders purchase-order-id])
        po-ingredients-qty (:purchase-order/ingredients purchase-order) 
        orders (->> (:purchase-order/orders purchase-order)
                    (map all-orders-map))]
    (->> orders
         (mapcat (fn [{order-qty :order/quantity
                       dish-id :dish/id
                       order-id :order/id}]
                   (let [order-dish-ings (:dish/ingredients (get dishes-map dish-id))
                         dish-template {:order/id order-id
                                        :dish/paid-ingredients (->> order-dish-ings
                                                                    (mapv (fn [[ing-id ing-qty]]
                                                                            (let [total-spent-on-ing (get purchase-order-fill ing-id)
                                                                                  total-ing-qty (get po-ingredients-qty ing-id)
                                                                                  ing-price-for-dish (/ (* ing-qty total-spent-on-ing) total-ing-qty)]
                                                                              [ing-id ing-price-for-dish nil]))))}]
                     (repeat order-qty dish-template))))
         (into []))))

(defn fill-purchase-order [db [_ po-id purchase-order-fill]]
  (try
    (-> db
        (update :collector/dish-serves into (purchase-orders-dishes db po-id purchase-order-fill))
        (assoc-in [:collector/purchase-orders po-id :purchase-order/fill] purchase-order-fill)
        pay-dishes-with-donations)
    (catch js/Error e
      (js/console.error (.-message e))
      db)))

(defn report-dish-serve [db [_ order-id consumer-id]]
  ;; add customer id to the next free dish-serve for order-id
  (println "REPORTING SERVE" [order-id consumer-id])
  (let [free-serve-idx (->> (:collector/dish-serves db)
                            (map-indexed vector)
                            (some (fn [[idx dish-serve]]
                                    (when (and (= order-id (:order/id dish-serve))
                                               (nil? (:consumer/id dish-serve)))
                                      idx))))]
   (update-in db [:collector/dish-serves free-serve-idx] assoc :consumer/id consumer-id)))


(def person-names
  ["Isabella" "Daniel" "Olivia" "David" "Alexis" "Gabriel" "Sofía" "Benjamín" "Victoria" "Samuel" "Amelia" "Lucas" "Alexa" "Ángel"
   "Julia" "José" "Camila" "Adrián" "Alexandra" "Sebastián" "Maya" "Xavier" "Andrea" "Juan" "Ariana" "Luis" "María" "Diego" "Eva" "Óliver"
   "Angelina" "Carlos" "Valeria" "Jesús" "Natalia" "Alex" "Isabel" "Max" "Sara" "Alejandro" "Liliana" "Antonio" "Adriana" "Miguel" "Juliana" "Víctor"
   "Gabriela" "Joel" "Daniela" "Santiago" "Valentina" "Elías" "Lila" "Iván" "Vivian" "Óscar" "Nora" "Leonardo" "Ángela" "Eduardo" "Elena" "Alan"
   "Clara" "Nicolás" "Eliana" "Jorge" "Alana" "Omar" "Miranda" "Paúl" "Amanda" "Andrés" "Diana" "Julián" "Ana" "Josué" "Penélope" "Román" "Aurora" "Fernando"
   "Alexandría" "Javier" "Lola" "Abraham" "Alicia" "Ricardo" "Amaya" "Francisco" "Alexia" "César" "Jazmín" "Mario" "Mariana" "Manuel" "Alina" "Édgar"
   "Lucía" "Alexis" "Fátima" "Israel" "Ximena" "Mateo" "Laura" "Héctor" "Cecilia" "Sergio" "Alejandra" "Emiliano" "Esmeralda" "Simón" "Verónica" "Rafael"
   "Daniella" "Martín" "Miriam" "Marco" "Carmen" "Roberto" "Iris" "Pedro" "Guadalupe" "Emanuel" "Selena" "Abel" "Fernanda" "Rubén" "Angélica" "Fabián"
   "Emilia" "Emilio" "Lía" "Joaquín" "Tatiana" "Marcos" "Mónica" "Lorenzo" "Carolina" "Armando" "Jimena" "Adán" "Dulce" "Raúl"
   "Talía" "Julio" "Estrella" "Enrique" "Brenda" "Gerardo" "Lilian" "Pablo" "Paola" "Jaime" "Serena" "Saúl" "Celeste" "Esteban" "Viviana" "Gustavo"
   "Elisa" "Rodrigo" "Melina" "Arturo" "Gloria" "Mauricio" "Claudia" "Orlando" "Sandra" "Hugo" "Marisol" "Salvador" "Asia" "Alfredo" "Ada" "Maximiliano"
   "Rosa" "Ramón" "Isabela" "Ernesto" "Regina" "Tobías" "Elsa" "Abram" "Perla" "Noé" "Raquel" "Guillermo" "Virginia" "Ezequiel"
   "Patricia" "Lucián" "Linda" "Alonzo" "Marina" "Felipe" "Leila" "Matías" "América" "Tomás" "Mercedes" "Jairo"])

(def profiles-pictures
  ["https://images.pexels.com/photos/220453/pexels-photo-220453.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500"
   "https://media.istockphoto.com/photos/portrait-of-a-girl-picture-id938709362?k=6&m=938709362&s=612x612&w=0&h=mUQAOuFjTUhvykTbkpXXERePajEWvVqOM2tR3gwS3II="
   "https://images.pexels.com/photos/2078265/pexels-photo-2078265.jpeg?auto=compress&cs=tinysrgb&dpr=1&w=500"])

(defn random-ci []
  (gstr/format "%d.%d%d%d.%d%d%d-%d"
               (inc (rand-int 3))
               (rand-int 10)
               (rand-int 10)
               (rand-int 10)
               (rand-int 10)
               (rand-int 10)
               (rand-int 10)
               (rand-int 10)))

(defn sample [n coll]
  (take n (shuffle coll)))

(defn generate-random-data [db _]
  (let [food-services (:collector/food-services db)
        dishes (:collector/dishes db)
        market (:collector/market db)
        all-donators (->> (sample 20 person-names)
                          (map-indexed (fn [pid pname]
                                         [pid {:donator/id pid
                                               :donator/name pname}]))
                          (into {}))
        gen-orders (fn [n]
                     (map (fn [order-id food-service-id dish-id]
                            {:food-service/id food-service-id
                             :dish/id dish-id
                             :order/quantity (rand-int 100)})
                          (range n)
                          (cycle (keys food-services))
                          (cycle (keys dishes))))
        random-donator-id (fn [] (rand-int (inc (apply max (keys all-donators)))))
        random-donation-amount (fn [] (rand-int 10000))
        
        ;; add donators
        db-0 (let []
               (assoc db :collector/donators all-donators))
        ;; add some donations associated to random donators
        db-1 (reduce (fn [rdb donation-id]
                       (add-donation rdb [nil (random-donator-id) (random-donation-amount)])) 
                     db-0
                     (range 200))
        ;; generate and register some random consumers
        db-2 (let [consumers (map (fn [pname pci pp fsid]
                                    [pci {:consumer/id pci
                                          :consumer/name pname
                                          :profile/picture pp
                                          :food-service/id fsid}])
                                  person-names
                                  (repeatedly #(random-ci))
                                  (shuffle (take (count person-names) (cycle profiles-pictures)))
                                  (cycle (keys food-services)))]
               (reduce (fn [rdb consumer]
                         (register-consumer rdb consumer))
                       db-1
                       consumers))
        ;; generate and add some orders
        db-3 (reduce (fn [rdb order]
                       (add-order rdb [nil order]))                     
                     db-2
                     (gen-orders 50))
        ;; create a purchase-order for the orders so far
        db-4 (create-purchase-order db-3 nil)
        created-purchase-order-id 0
        ;; fill the created purchase order with ingredients at some random prices
        ;; WARNING, we aren't calculating the ingredients needed for this fill because
        ;; we know all ingredients are needed probably after 50 orders of random dishes 
        db-5 (fill-purchase-order db-4 [nil created-purchase-order-id (zipmap
                                                                       (keys market) ;; ingredient id
                                                                       (repeat 20000))]) ;; TODO: fix this to something that makes sense
        ;; add some more orders, just to have some open orders
        db-6 (reduce (fn [rdb order]
                       (add-order rdb [nil order]))                     
                     db-5
                     (gen-orders 10))
        ;; serve some dishes
        ;; for each food-service grab the filled orders and simulate some of it's registered consumers (not all)
        ;; comming for dishes
        db-7 (let [filled-orders (->> (vals (:collector/orders db-6))
                                      (filter #(not (subs.collector/open-order? db-6 (:order/id %)))))
                   dish-serves (->> filled-orders
                                    (mapcat (fn [order]
                                              (let [fsid (:food-service/id order)
                                                    fs-consumers (->> (vals (:collector/consumers db-6))
                                                                      (filter #(= fsid (:food-service/id %))))
                                                    ;; can't serve more dishes than we have registered consumers
                                                    ;; and ordered dishes
                                                    to-serve-consumers (take (max (rand-int (count fs-consumers))
                                                                                  (:order/quantity order))
                                                                             fs-consumers)]
                                                (map (fn [consumer]
                                                       {:order/id (:order/id order)
                                                        :consumer/id (:consumer/id consumer)})
                                                     to-serve-consumers)))))]
               (reduce (fn [rdb dish-serve]
                         (report-dish-serve rdb [nil (:order/id dish-serve) (:consumer/id dish-serve)]))
                db-6
                dish-serves))]
    
    db-7
    ))
