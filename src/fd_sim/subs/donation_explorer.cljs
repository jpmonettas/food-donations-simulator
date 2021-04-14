(ns fd-sim.subs.donation-explorer
  (:require [goog.string :as gstr]))

(def dish-padding 2)
(def dish-width 10)
(def dish-cols 10)
(def order-rect-padding 30)
(def donation-start-x 0)
(def orders-start-x 2000)
(def dishes-start-x 4000)
(def layer-1-food-service-padding 400)
(def food-service-color {1 "#3428ca"
                         2 "#dd0808"
                         3 "#9e15e6"})

(defn reposition-rects
  "Recalculates rect y based on their heights so the don't overlap"
  [rects padding]
  (->> (reduce (fn [{:keys [y] :as r} order-rect]
                 (-> r
                     (update :positioned-rects conj (assoc order-rect :y y))
                     (assoc :y (+ y (:height order-rect) padding))))
               {:y 0
                :positioned-rects []}
               rects)
       :positioned-rects))

(defn build-orders-dishes-rects [serves orders consumers-map]
  (let [serves-by-order (group-by :order/id serves)        
        orders-rects-areas (->> serves-by-order
                                (map (fn [[order-id serves]]
                                       ;; each consumer dish is going to be 10x10
                                       ;; and each order dishes box will be 5 consumers wide grid, with a 2 padding
                                       (let [cnt (count serves)
                                             rows (js/Math.ceil (/ cnt dish-cols))
                                             cols (min dish-cols cnt)
                                             food-service-id (get-in orders [order-id :food-service/id])]
                                         {:order-id order-id
                                          :food-service-id food-service-id
                                          :color (food-service-color food-service-id)
                                          :serves (map-indexed (fn [i s]
                                                                 (let [grid-x-pos (inc (mod i dish-cols))
                                                                       grid-y-pos (inc (quot i dish-cols))]
                                                                   (cond-> (assoc s 
                                                                                  :cx (+ (* grid-x-pos dish-width)
                                                                                         (* (dec grid-x-pos) dish-padding))
                                                                                  :cy (+ (* grid-y-pos dish-width)
                                                                                         (* (dec grid-y-pos) dish-padding))
                                                                                  :r (/ dish-width 2))
                                                                     (:consumer/id s) (merge (get consumers-map (:consumer/id s))))))
                                                               serves)
                                          :width (max
                                                  (+ (* cols dish-width) ;; the width of the dishes
                                                     (* cols dish-padding) ;; the internal paddings
                                                     (* 2 dish-padding))
                                                  (* 10 (+ dish-width dish-padding))) 
                                          :height (max (+ (* rows dish-width) ;; the height of the dishes + paddings
                                                          (* cols dish-padding)
                                                          (* 2 dish-padding))
                                                       (* 12 (+ dish-width dish-padding)))
                                          :x dishes-start-x})))
                                (sort-by :food-service-id))]
    (reposition-rects orders-rects-areas order-rect-padding)))

(defn build-orders-rects [orders market dishes orders-dishes-rects]
  (let [rects (->> orders-dishes-rects
                   (map (fn [{:keys [y width height order-id color order-id food-service-id serves]}]
                          (let [ingr-summary (->> serves
                                                  (mapcat :dish/paid-ingredients)
                                                  (reduce (fn [r [ing-id price don-id]]
                                                            (if don-id
                                                              (update r ing-id #(+ (or % 0) price))
                                                              r))
                                                          {}))
                                lines (map (fn [[ing-id paid]]
                                             (gstr/format "%s $U %d"
                                                          (:ingredient/name (get market ing-id))
                                                          paid))
                                           ingr-summary)
                                order (get orders order-id)
                                dish-name (:dish/name (get dishes (:dish/id order)))
                                title (gstr/format "Order %d - (%s)" order-id dish-name)] 
                            {:x orders-start-x
                             :y y                
                             :title title
                             :lines lines
                             :order-id order-id
                             :total-paid (reduce + (vals ingr-summary))
                             :total-serves (count serves)
                             :width (* 6 (apply max (into (map count lines) [(count title)])))
                             :height (max height (+ 30 (* (count lines) 12)))
                             :color color
                             :food-service-id food-service-id}))))]
    (reposition-rects rects 30)))

(defn build-donation-rect [donation orders-dishes-rects]
  (let [total-height (apply max (map :y orders-dishes-rects))]
    {:x donation-start-x
     :y (/ total-height 2)
     :width 100
     :height 100
     :text-size 20
     :text-line (gstr/format  "$U %d (%d)"
                              (:donation/amount donation)
                              (:donation/usable-amount donation))}))

(defn build-one->many-links [rect rects id-fn]
  (->> rects
       (map (fn [or]
              {:x1 (+ (:x rect) (:width rect))
               :y1 (+ (:y rect) (/ (:height rect) 2))
               :x2 (:x or)
               :y2 (+ (:y or) (/ (:height or) 2))
               :arrow-end? true
               :link-id (str "O-M-L" (id-fn or))}))))

(defn build-one->one-links [from-rects to-rects id-fn]
  (map (fn [from to]
         {:x1 (+ (:x from) (:width from))
          :y1 (+ (:y from) (/ (:height from) 2))
          :x2 (:x to)
          :y2 (+ (:y to) (/ (:height to) 2))
          :arrow-end? true
          :link-id (str "O-Dishes" (id-fn from))})
       from-rects
       to-rects))

(defn build-layer-2 [donation serves orders market dishes consumers]
  (let [orders-dishes-rects (build-orders-dishes-rects serves orders consumers)
        donation-rect (build-donation-rect donation orders-dishes-rects)
        orders-rects (build-orders-rects orders market dishes orders-dishes-rects)
        donation->orders-links (build-one->many-links donation-rect orders-rects :order-id)
        orders->dishes-links (build-one->one-links orders-rects orders-dishes-rects :order-id)]
    {:donation-rect donation-rect
     :orders-rects orders-rects
     :orders-dishes-rects orders-dishes-rects
     :links (into donation->orders-links orders->dishes-links)}))

(defn bounding-box [rects]
  (reduce (fn [bb {:keys [x y width height]}]
            (-> bb
                (update :min-x min x)
                (update :min-y min y)
                (update :max-x max (+ x width))
                (update :max-y max (+ y height))))
          {:min-x js/Number.MAX_VALUE :min-y js/Number.MAX_VALUE
           :max-x js/Number.MIN_VALUE :max-y js/Number.MIN_VALUE}
          rects))

(defn build-layer-1 [donation food-services layer-2]
  (let [donation-rect (-> (:donation-rect layer-2)
                          (update :x - 500)
                          (update :y - 500)
                          (update :width + 1000)
                          (update :height + 1000)
                          (assoc :text-size 130))
        food-services-rects (->> (:orders-rects layer-2)
                                 (group-by :food-service-id)
                                 (map (fn [[fsid rects]]
                                        (let [color (:color (first rects))
                                              {:keys [min-x min-y max-x max-y]} (bounding-box rects)]
                                          {:x (- min-x layer-1-food-service-padding)
                                           :y min-y
                                           :width (+ (- max-x min-x) (* 2 layer-1-food-service-padding))
                                           :height (- max-y min-y)
                                           :total-serves (->> rects
                                                              (map :total-serves)
                                                              (reduce +))
                                           :total-paid (->> rects
                                                            (map :total-paid)
                                                            (reduce +))
                                           :text (gstr/format "%s ($U %d)"
                                                              (:food-service/name (get food-services fsid))
                                                              (->> rects
                                                                   (map :total-paid)
                                                                   (reduce +)))
                                           :food-service-id fsid
                                           :color color}))))
        dishes-sum-rects (->> food-services-rects
                              (map (fn [r]
                                     (-> r
                                         (update :x + (- dishes-start-x orders-start-x))
                                         (assoc :text (gstr/format "%d Dishes ($U %d/each)"
                                                                   (:total-serves r)
                                                                   (/ (:total-paid r)
                                                                      (:total-serves r))))))))
        donation->food-services-links (build-one->many-links donation-rect food-services-rects :food-service-id)
        food-services->dishes-links (build-one->one-links food-services-rects dishes-sum-rects :food-service-id)]
    
    {:donation-rect donation-rect
     :food-services-rects food-services-rects
     :dishes-sum-rects dishes-sum-rects
     :links (into donation->food-services-links food-services->dishes-links)}))

(defn has-ingredient-paid-by [serve donation-id]
  (some (fn [[_ _ don-id]]
          (when (= don-id donation-id)
            true))
        (:dish/paid-ingredients serve)))

(defn layers [[donations orders market dish-serves purchase-orders consumers dishes food-services donation-id] _]
  ;; find all orders for a donation
  ;; find all dishes served to a order
  (let [donation (some (fn [d] (when (= (:donation/id d) donation-id) d)) donations)
        serves (filter #(has-ingredient-paid-by % donation-id) dish-serves)
        layer-2 (build-layer-2 donation serves orders market dishes consumers)
        layer-1 (build-layer-1 donation food-services layer-2)
        ]
    
    ;; for each order make a rectangle of w x h calculated from the number of consumers
    ;; and some area for a consumer dish
    ;; then calculate y coord for each order rectangle with some padding
    {:donation donation
     :layer-1 layer-1
     :layer-2 layer-2
     }))

(defn selected-donation [db _]
  (get-in db [:collector/donation-explorer :selected-donation]))

(defn map-state [donation-explorer _]
  (:map-state donation-explorer))
