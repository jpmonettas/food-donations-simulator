(ns fd-sim.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as reagent]
            [clojure.string :as str]))

(defn tabs [{:keys [event-key subs-key items]}]
  (let [selected @(subscribe [subs-key])]
    [:div.tabs
     (for [[label id] items]
       ^{:key (str id)}
       [:div.tab (cond-> {:on-click #(dispatch [event-key id])}
                   (= id selected) (assoc :class "selected"))
        label])]))

(defn donations-panel [{:keys [donator/id] :as filter}]
  (let [donations @(subscribe [:collector/donations filter])
        donators @(subscribe [:donators/donators])]
   [:div.donations.panel
    [:h2.title "Donations"]
    [:table
     [:thead
      [:tr [:th "Id"] [:th "Donator"] [:th "Amount"] [:th "Remaining"]]]
     [:tbody
      (for [d donations]
        ^{:key (str (:donation/id d))}
        [:tr
         [:td (str (:donation/id d))]
         [:td (:donator/name (get donators (:donator/id d)))]
         [:td (str (:donation/amount d))]
         [:td (str (:donation/usable-amount d))]])
      ]]]))

(defn donator-panel []
  (let [amount-txt (reagent/atom "")
        donators (subscribe [:donators/donators])
        selected-donator (subscribe [:ui/selected-donator])]
    (fn []
      [:div
       [:div
        [:button {} "Generate 100 random donations"]]
       [:div.user
        [:div
         [:label "Login as :"]
         [:select {:on-change (fn [e] (dispatch [:ui/select-donator (js/parseFloat (.-value (.-target e)))]))
                   :value @selected-donator}
          (for [d (vals @donators)]
            ^{:key (str (:donator/id d))}
            [:option {:value (:donator/id d)} (:donator/name d)])]]
        [:div
         [:input {:type :number :value @amount-txt :on-change #(reset! amount-txt (.-value (.-target %)))}]
         [:button {:on-click #(dispatch [:collector/add-donation {:donator/id @selected-donator
                                                                  :donation/amount (js/parseFloat @amount-txt)}])}
          "Donate"]]]
       [donations-panel {:donator/id @selected-donator}]])))

(defn collector-market-panel []
  (let [market (subscribe [:collector/market])
        new-ing-txt (reagent/atom "")
        new-ing-price-txt (reagent/atom "")
        update-ings-txt (reagent/atom {})
        ]
    (fn []
      (let [update-ings @update-ings-txt]
        [:div.market.panel
         [:h2.title "Market"]
         [:table
          [:thead
           [:tr [:th "Ingredient"]]]
          [:tbody
           (for [[ing-id ing] @market]
             ^{:key (str ing-id)}
             [:tr
              [:td (:ingredient/name ing)]])
           [:tr
            [:td [:input {:type :text
                          :value @new-ing-txt
                          :on-change #(reset! new-ing-txt (.-value (.-target %)))}]]            
            [:td [:button {:on-click (fn [_]
                                       (dispatch [:collector/add-market-ingredient {:ingredient/name @new-ing-txt
                                                                                    :ingredient/price (js/parseFloat @new-ing-price-txt)}])
                                       (reset! new-ing-txt "")
                                       (reset! new-ing-price-txt ""))}
                  "Add"]]]]]]))))

(defn dishes-panel []
  (let [dishes (vals @(subscribe [:collector/dishes]))
        market @(subscribe [:collector/market])]
    [:div.dishes.panel
     [:h2.title "Dishes"]
     [:table
      [:thead
       [:tr [:th "Id"] [:th "Dish name"] [:th "Ingredients"]]]
      [:tbody
       (for [d dishes]
         ^{:key (str (:dish/id d))}
         [:tr
          [:td (str (:dish/id d))]
          [:td (:dish/name d)]
          [:td (->> (:dish/ingredients d)
                    (map (fn [[iid iq]]
                           (str (:ingredient/name (get market iid)) "(" iq "grs)")))
                    (str/join ", "))]])]]]))

(defn orders-panel [{:keys [orders new-order-row]}]
  [:div
   [:h2.title "Orders"]
   [:table
    [:thead
     [:tr [:th "Id"] [:th "Dish name"] [:th "Quantity"] [:th "Status"] [:th ""]]]
    [:tbody
     (for [o orders]
       ^{:key (str (:order/id o))}
       [:tr
        [:td (:order/id o)]
        [:td (:dish/name o)]
        [:td (:order/quantity o)]
        [:td (:order/status o)]])
     (when new-order-row
       new-order-row)]]])

(defn consumers-panel [{:keys [consumers new-consumer-row]}]
  [:div {}
   [:h2.title "Consumers"]
   [:table
    [:thead
     [:tr [:th "CI"] [:th "Name"] [:th "Pic"] [:th "Food service"]]]
    [:tbody
     (for [c consumers]
       ^{:key (:consumer/id c)}
       [:tr
        [:td (:consumer/id c)]
        [:td (:consumer/name c)]
        [:td [:img.profile {:src (:profile/picture c)}]]
        [:td (:food-service/name c)]])
     (when new-consumer-row
       new-consumer-row)]]])

(defn purchase-orders []
  (let [purchase-orders (subscribe [:collector/purchase-orders])
        market (subscribe [:collector/market])
        selected-order (reagent/atom nil)
        ing-prices (reagent/atom nil)]
    (fn []
      (let [prices @ing-prices
            ing-names @market]
       [:div {}
        (when (seq @purchase-orders)
          [:div 
           [:h2.title "Purchase Orders"]
           [:table
            [:thead
             [:tr [:th "ID"] [:th "Orders"] [:th ""]]]
            [:tbody
             (for [po @purchase-orders]
               ^{:key (str (:purchase-order/id po))}
               [:tr
                [:td (:purchase-order/id po)]
                [:td (str/join "," (:purchase-order/orders po))]
                [:td (when-not (:purchase-order/fill po)
                       [:button {:on-click #(do
                                             (reset! selected-order po)
                                             (reset! ing-prices (zipmap (keys (:purchase-order/ingredients po))
                                                                        (repeat nil))))}
                       "Fill"])]])]]
           (when @selected-order
             [:table
              [:thead
               [:tr [:th "Ingredient"] [:th "Deal price"]]]
              [:tbody
               (for [i (keys prices)]
                 ^{:key (str i)}
                 [:tr
                  [:td (:ingredient/name (get ing-names i))]
                  [:td [:input {:type :number
                                :value (get prices i)
                                :on-change #(swap! ing-prices assoc i (js/parseFloat (.-value (.-target %))))}]]])
               [:tr [:td] [:td]
                [:td [:button
                      {:on-click #(do
                                    (dispatch [:collector/fill-purchase-order
                                               (:purchase-order/id @selected-order)
                                               @ing-prices])
                                    (reset! selected-order nil)
                                    (reset! ing-prices nil))}
                      "Finish"]]]]])])]))))

(defn collector-panel []
  (let [orders @(subscribe [:collector/orders])
        consumers @(subscribe [:collector/consumers])]
   [:div.collector
    [collector-market-panel]
    [dishes-panel]
    [orders-panel {:orders orders}]
    [:button {:on-click #(dispatch [:collector/create-purchase-order])}
     "Create purchase order"]
    [purchase-orders]
    [consumers-panel {:consumers consumers}]
    [donations-panel {}]]))

(defn food-service-panel []
  (let [selected-food-service (subscribe [:ui/selected-food-service])
        services (subscribe [:collector/food-services])
        orders (subscribe [:collector/selected-food-service-orders])
        consumers (subscribe [:collector/selected-food-service-consumers])
        dishes (subscribe [:collector/dishes])
        selected-dish (reagent/atom (first (keys @dishes)))
        new-order-qty-txt (reagent/atom "")
        new-cons-ci-txt (reagent/atom "")
        new-cons-name-txt (reagent/atom "")
        new-cons-pic-txt (reagent/atom "")]
    (fn []
      (let [all-dishes @dishes]
       [:div
        [:div
         [:label "Login as food service:"]
         [:select {:on-change (fn [e] (dispatch [:ui/select-food-service (js/parseFloat (.-value (.-target e)))]))
                   :value @selected-food-service}
          (for [s (vals @services)]
            ^{:key (str (:food-service/id s))}
            [:option {:value (:food-service/id s)} (:food-service/name s)])]]
        [orders-panel {:orders @orders
                       :new-order-row [:tr
                                       [:td ""]
                                       [:td [:select {:on-change #(reset! selected-dish (.-value (.-target %)))
                                                      :value @selected-dish}
                                             (for [d (vals all-dishes)]
                                               ^{:key (str (:dish/id d))}
                                               [:option {:value (:dish/id d)} (:dish/name d)])]]
                                       [:td [:input {:type :number
                                                     :value @new-order-qty-txt
                                                     :on-change #(reset! new-order-qty-txt (.-value (.-target %)))}]]
                                       [:td [:button {:on-click (fn [_]
                                                                  (dispatch [:collector/add-order {:food-service/id @selected-food-service
                                                                                                   :dish/id (js/parseFloat @selected-dish)
                                                                                                   :order/quantity (js/parseFloat @new-order-qty-txt)}])
                                                                  (reset! new-order-qty-txt ""))}
                                             "Add"]]]}]
        [consumers-panel {:consumers @consumers
                          :new-consumer-row [:tr
                                             [:td [:input {:type :text
                                                           :value @new-cons-ci-txt
                                                           :on-change #(reset! new-cons-ci-txt (.-value (.-target %)))}]]
                                             [:td [:input {:type :text
                                                           :value @new-cons-name-txt
                                                           :on-change #(reset! new-cons-name-txt (.-value (.-target %)))}]]
                                             [:td [:input {:type :text
                                                           :value @new-cons-pic-txt
                                                           :on-change #(reset! new-cons-pic-txt (.-value (.-target %)))}]]
                                             [:td [:button {:on-click (fn [_]
                                                                        (dispatch [:collector/register-consumer {:food-service/id @selected-food-service
                                                                                                                 :consumer/id @new-cons-ci-txt
                                                                                                                 :profile/picture @new-cons-pic-txt
                                                                                                                 :consumer/name @new-cons-name-txt}])
                                                                        (reset! new-cons-ci-txt "")
                                                                        (reset! new-cons-pic-txt "")
                                                                        (reset! new-cons-name-txt ""))}
                                                   "Register"]]]}]
        ]))))


(defn main []
  (let [selected-role @(subscribe [:ui/selected-role])]
    [:div.main
     
     [:div.top-bar
      [tabs {:event-key :ui/select-role
             :subs-key :ui/selected-role
             :items [["Donator" :donator]
                     ["Collector" :collector]
                     ["Food service" :food-service]]}]      
      [:div.balance (str "$U " @(subscribe [:collector/balance]))]]

     [:div.tab-content
      (case selected-role
        :donator      [donator-panel]
        :collector    [collector-panel]
        :food-service [food-service-panel])]]))
 


                                             
