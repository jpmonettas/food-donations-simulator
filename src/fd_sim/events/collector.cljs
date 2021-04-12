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
                                  (sample 20 person-names)
                                  (repeatedly 20 #(random-ci))
                                  (shuffle (take 20 (cycle profiles-pictures)))
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
                                                                       (repeatedly #(rand-int 200)))]) ;; at some random price
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
