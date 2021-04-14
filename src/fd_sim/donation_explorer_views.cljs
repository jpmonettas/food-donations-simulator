(ns fd-sim.donation-explorer-views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [fd-sim.subs.donation-explorer :as subs.donation-explorer]
            [goog.string :as gstr]))

(def left-button  0)
(def wheel-button 1)
(def right-button 2)

(defn donation-rectangle [donation-rect scale]
  (let [{:keys [x y width height text-line text-size]} donation-rect]
    [:g {}     
     [:rect {:x x :y y :width width :height height :fill :transparent :stroke "#079dab" :stroke-width "5"}]
     [:text {:x x :y (+ y (/ height 2)) :font-size text-size :fill "#079dab" :stroke :transparent}
      text-line]
     ]))

(defn layer-2-opacity [scale]
  (let [line-fn (fn [s] (+ (* 0.625 s) -0.25))]
    (cond
      (<= scale 0.4)  0
      (< 0.4 scale 2) (line-fn scale)
      (>= scale 2)    1)))

(defn donation-explorer-layer-2 [scale {:keys [donation-rect orders-dishes-rects orders-rects links]}]
  [:g {:opacity (layer-2-opacity scale)}
   [donation-rectangle donation-rect scale]
   
   [:g ;; orders 
    (for [{:keys [order-id title lines x y width height color]} orders-rects]
      ^{:key (str "ORDER-" order-id)}
      [:g {:transform (gstr/format "translate(%f %f)" x y)}
       [:rect {:x 0 :y 0 :width width :height height :fill :transparent :stroke color :stroke-width "3"}]
       [:text {:x 5 :y 10 :font-size "10" :fill :black :stroke :transparent} title]
       [:g
        (for [[l ly] (map vector lines (iterate #(+ % 12) 30))]
          ^{:key (str ly)}
          [:text {:x 5 :y ly :font-size "10" :fill :black :stroke :transparent} l]
          )]
       ])]
   
   [:g ;; orders dishes
    (for [{:keys [order-id x y width height color serves]} orders-dishes-rects]
      ^{:key (str "ORDER-DISHES-" order-id)}
      [:g {:transform (gstr/format "translate(%f %f)" x y)}
       [:rect {:x 0 :y 0 :width width :height height :fill :transparent :stroke color :stroke-width "3"}]
       (for [[idx {:keys [cx cy r] :as s}] (map-indexed vector serves)]
         ^{:key (str "SERVE-" order-id "-" idx)}
         [:g {}
          (if (:consumer/id s)
            [:g                       
             [:image {:x (- cx r) :y (- cy r)
                      :width (* 2 r) :height (* 2 r)
                      :href (:profile/picture s)}]
             [:text {:x (- cx r) :y (+ cy r) :r r
                     :font-size "1.8" :fill color :stroke :transparent
                     ;;:text-anchor "middle"
                     }
              (:consumer/id s)]]
            [:circle {:cx cx :cy cy :r r :fill :transparent :stroke "#266c08"}])])])]

   [:g ;;links 
    (for [{:keys [x1 y1 x2 y2 arrow-start? arrow-end? link-id]} links]
      ^{:key link-id}
      [:line (cond-> {:x1 x1 :y1 y1 :x2 x2 :y2 y2
                      :stroke :grey
                      :stroke-width 3}
               arrow-start?  (assoc :marker-start "url(#arrow-start)")
               arrow-end?    (assoc :marker-end "url(#arrow-end)"))])]])

(defn layer-1-opacity [scale]
  (let [line-fn (fn [s] (+ (* -0.83333 s) 1.083333))]
    (cond
      (<= scale 0.1)    1
      (< 0.1 scale 1.3) (line-fn scale)
      (>= scale 1.3)    0)))

(defn donation-explorer-layer-1 [scale {:keys [donation-rect food-services-rects dishes-sum-rects links]}]
  [:g {:opacity (layer-1-opacity scale)}
   
   [donation-rectangle donation-rect scale]
   
   [:g ;; food-services
    (for [{:keys [food-service-id text x y width height color]} food-services-rects]
      ^{:key (str "FS-" food-service-id)}
      [:g {:transform (gstr/format "translate(%f %f)" x y)}
       [:rect {:x 0 :y 0 :width width :height height :fill :transparent :stroke color :stroke-width "5"}]
       [:text {:x 10 :y 60 :font-size "60" :fill :black :stroke :transparent} text]
       ])]
   
   [:g ;; dishes summary
    (for [{:keys [food-service-id text x y width height color]} dishes-sum-rects]
      ^{:key (str "DS-" food-service-id)}
      [:g {:transform (gstr/format "translate(%f %f)" x y)}
       [:rect {:x 0 :y 0 :width width :height height :fill :transparent :stroke color :stroke-width "5"}]       
       [:text {:x 10 :y 60
               :font-size "60" :fill :black :stroke :transparent
               ;;:text-anchor "middle"
               }
        text]
       ])]

   [:g ;;links 
    (for [{:keys [x1 y1 x2 y2 arrow-start? arrow-end? link-id]} links]
      ^{:key link-id}
      [:line (cond-> {:x1 x1 :y1 y1 :x2 x2 :y2 y2
                      :stroke :black
                      :stroke-width 3}
               arrow-start?  (assoc :marker-start "url(#arrow-start)")
               arrow-end?    (assoc :marker-end "url(#arrow-end)"))])]])

(defn donation-explorer-layers [scale]
  (let [{:keys [layer-1 layer-2]} @(subscribe [:donation-explorer/layers])]
    [:g
     [donation-explorer-layer-2 scale layer-2]
     [donation-explorer-layer-1 scale layer-1]]
    ))

(defn donation-explorer []
  (let [{:keys [translate scale grab]} @(subscribe [:donation-explorer/map-state])        
        [translate-x translate-y] translate]
    [:div.donation-explorer
     #_[:div (str @(subscribe [:donation-explorer/map-state]))]
     #_[:div (str @(subscribe [:donation-explorer/layers]))]     
     [:div {:on-wheel (fn [evt]
                        (let [x (-> evt .-nativeEvent .-offsetX)
                              y (-> evt .-nativeEvent .-offsetY)]
                          (dispatch [:donation-explorer.map/zoom {:delta (.-deltaY evt)
                                                                  :x x
                                                                  :y y}])))
            :on-mouse-down (fn [evt]
                             (.stopPropagation evt)
                             (.preventDefault evt)
                             (let [x (-> evt .-nativeEvent .-offsetX)
                                   y (-> evt .-nativeEvent .-offsetY)]
                               (cond

                                 ;; left button pressed
                                 (= left-button (.-button evt))                                  
                                 (dispatch [:donation-explorer.map/grab {:x x :y y}]))))
            
            :on-mouse-move (fn [evt]
                             (let [x (-> evt .-nativeEvent .-offsetX)
                                   y (-> evt .-nativeEvent .-offsetY)]
                               (cond
                                 grab
                                 (dispatch [:donation-explorer.map/drag {:x x :y y}])
                                 )))
            :on-mouse-up (fn [evt]
                           (.stopPropagation evt)
                           (.preventDefault evt)
                           (cond
                             (= left-button (.-button evt))
                             (dispatch [:donation-explorer.map/grab-release])
                             ))}
      [:svg {:width "100%"
             :height "800px"}

       [:defs
        [:marker {:id "arrow-end" :marker-width 10 :marker-height 10 :ref-x 5 :ref-y 2 :orient "auto" :marker-units "strokeWidth"}
         [:path {:d "M0,0 L0,4 L6,2 z" :fill "gray"}]]
        [:marker {:id "arrow-start" :marker-width 10 :marker-height 10 :ref-x 0 :ref-y 2 :orient "auto" :marker-units "strokeWidth"}
         [:path {:d "M0,2 L6,4 L6,0 z" :fill "gray"}]]]      
       
       ;; background
       [:rect {:x "0" :y "0" :width "100%" :height "100%" :fill "#eceff8"}]

       [:g {:transform (gstr/format "translate(%f %f) scale(%f %f)"
                                    (or translate-x 0)
                                    (or translate-y 0)
                                    scale scale)}
        [donation-explorer-layers scale]]]]]))
