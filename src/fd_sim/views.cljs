(ns fd-sim.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as reagent]
            [clojure.string :as str]
            [goog.string :as gstr]
            [fd-sim.donation-explorer-views :as donation-explorer-views]))

(defn tabs [{:keys [tab-id items extra-class]}]
  (let [selected @(subscribe [:ui/selected-tab tab-id])]
    [:div.tabs (when extra-class {:class extra-class})
     (for [[label id] items]
       ^{:key (str tab-id "->" id)}
       [:div.tab (cond-> {:on-click #(dispatch [:ui/select-tab tab-id id])}
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
        [:tr.clickable {:on-click #(dispatch [:donation-explorer/select-donation (:donation/id d)])}
         [:td (str (:donation/id d))]
         [:td (:donator/name (get donators (:donator/id d)))]
         [:td (gstr/format "%d" (:donation/amount d))]
         [:td (gstr/format "%d" (:donation/usable-amount d))]])
      ]]]))

(defn donator-tab []
  (let [amount-txt (reagent/atom "")
        donators (subscribe [:donators/donators])
        selected-donator (subscribe [:ui/selected-donator])]
    (fn []
      [:div       
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
         [:button {:on-click #(do
                                (dispatch [:collector/add-donation @selected-donator (js/parseFloat @amount-txt)])
                                (reset! amount-txt ""))}
          "Donate"]
         [:span "Visa, Mastercard, PayPal, Bitcoin"]]]
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
  [:div.panel.orders
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
  [:div.panel.consumers {}
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

(defn purchase-orders-panel []
  (let [purchase-orders (subscribe [:collector/purchase-orders])
        market (subscribe [:collector/market])
        selected-porder (reagent/atom nil)
        ing-prices (reagent/atom nil)]
    (fn []
      (let [prices @ing-prices
            ing-names @market
            selected-purchase-order-val @selected-porder]
        [:div.panel {}
         [:h2.title "Purchase Orders"]
         (when (seq (vals @purchase-orders))
           [:div
            
            [:table
             [:thead
              [:tr [:th "ID"] [:th "Orders"] [:th ""]]]
             [:tbody
              (for [po (vals @purchase-orders)]
                ^{:key (str (:purchase-order/id po))}
                [:tr
                 [:td (:purchase-order/id po)]
                 [:td (str/join "," (:purchase-order/orders po))]
                 [:td (when-not (:purchase-order/fill po)
                        [:button {:on-click #(do
                                               (reset! selected-porder po)
                                               (reset! ing-prices (zipmap (keys (:purchase-order/ingredients po))
                                                                          (repeat nil))))}
                         "Fill"])]])]]
            (when selected-purchase-order-val
              [:table
               [:thead
                [:tr [:th "Ingredient"] [:th "Qty"] [:th "Deal price"]]]
               [:tbody
                (for [i (keys prices)]
                  ^{:key (str i)}
                  [:tr
                   [:td (:ingredient/name (get ing-names i))]
                   [:td (str (get-in selected-purchase-order-val [:purchase-order/ingredients i]) " grs")]
                   [:td [:input {:type :number
                                 :value (get prices i)
                                 :on-change #(swap! ing-prices assoc i (js/parseFloat (.-value (.-target %))))}]]])
                [:tr [:td] [:td]
                 [:td [:button
                       {:on-click #(do
                                     (dispatch [:collector/fill-purchase-order
                                                (:purchase-order/id selected-purchase-order-val)
                                                @ing-prices])
                                     (reset! selected-porder nil)
                                     (reset! ing-prices nil))}
                       "Finish"]]]]])])]))))

(defn dish-serves-panel [serves]
  [:div.panel.dish-serves
   [:h2.title "Dish serves"]
   [:table
    [:thead
     [:tr [:th "CI"] [:th "Name"] [:th "Pic"] [:th "Order"] [:th "Dish"]]]
    [:tbody
     (for [s serves]
       ^{:key (str (:order/id s) (:consumer/id s))}
       [:tr
        [:td (:consumer/id s)]
        [:td (:consumer/name s)]
        [:td [:img.profile {:src (:profile/picture s)}]]
        [:td (:order/id s)]
        [:td (:dish/name s)]])]]])

(defn collector-tab []
  (let [orders @(subscribe [:collector/orders])
        consumers @(subscribe [:collector/consumers])
        serves @(subscribe [:collector/dish-serves true])
        selected-collector-tab @(subscribe [:ui/selected-tab :collector])]
    [:div.collector     
     [tabs {:tab-id :collector
            :extra-class "sub-tabs"
            :items [["Market and Dishes" :market-dishes]
                    ["Orders" :orders]
                    ["Serves" :serves]
                    ["Consumers" :consumers]
                    ["Donations" :donations]]}]
     [:div.tab-content
      (case selected-collector-tab
        :market-dishes [:div
                        [collector-market-panel]
                        [dishes-panel]]
        :orders [:div
                 [orders-panel {:orders orders}]
                 [:button {:on-click #(dispatch [:collector/create-purchase-order])}
                  "Create purchase order"]
                 [purchase-orders-panel]]
        :serves [dish-serves-panel serves]
        :consumers [consumers-panel {:consumers consumers}]
        :donations [donations-panel {}])]]))

(defn serve-new-dish-panel []
  (let [consumers (subscribe [:collector/selected-food-service-consumers])
        orders (subscribe [:collector/selected-food-service-orders])
        selected-consumer-id (reagent/atom nil)
        selected-order-id (reagent/atom nil)]
    (fn []
      (let [filled-orders (filter #(= (:order/status %) :filled) @orders)
            selected-order-id-val (or @selected-order-id (:order/id (first @orders)))
            selected-consumer-id-val (or @selected-consumer-id (:consumer/id (first @consumers)))]
        [:div.panel
         [:h2 "Serve a dish"]
         (when (seq filled-orders)
           [:div
            [:select {:on-change (fn [e] (reset! selected-consumer-id (.-value (.-target e))))
                      :value selected-consumer-id-val}
             (for [c @consumers]
               ^{:key (str (:consumer/id c))}
               [:option {:value (:consumer/id c)} (str (:consumer/name c) "(" (:consumer/id c) ")")])]
            [:select {:on-change (fn [e] (reset! selected-order-id (.-value (.-target e))))
                      :value selected-order-id-val}
             (for [o filled-orders]
               ^{:key (str (:order/id o))}
               [:option {:value (:order/id o)} (:order/id o)])]
            [:button {:on-click #(dispatch [:collector/report-dish-serve selected-order-id-val selected-consumer-id-val])}
             "Serve"]])]))))

(defn food-service-tab []
  (let [selected-food-service (subscribe [:ui/selected-food-service])
        services (subscribe [:collector/food-services])
        serves (subscribe [:collector/selected-food-service-dish-serves true])
        orders (subscribe [:collector/selected-food-service-orders])
        consumers (subscribe [:collector/selected-food-service-consumers])
        selected-food-service-tab (subscribe [:ui/selected-tab :food-service])
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
         [tabs {:tab-id :food-service
                :extra-class "sub-tabs"
                :items [["Orders and Serves" :orders-serves]
                        ["Consumers" :consumers]]}]
         [:div.tab-content
          (case @selected-food-service-tab
            :orders-serves [:div
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
                            [dish-serves-panel @serves]
                            [serve-new-dish-panel]]
            :consumers [consumers-panel {:consumers @consumers
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
                                                                  "Register"]]]}])]]))))

(defn transparency-tab []
  (let [selected-transparency-tab @(subscribe [:ui/selected-tab :transparency])]
    [:div.transparency
     [tabs {:tab-id :transparency
            :extra-class "sub-tabs"
            :items [["Donation Explorer" :donation-explorer]
                    ["Donators Ranking" :donators-ranking]
                    ["Companies Marketing" :companies-marketing]]}]
     [:div.tab-content
      (case selected-transparency-tab
        :donation-explorer [donation-explorer-views/donation-explorer]
        :donators-ranking [:div]
        :companies-marketing [:div
                              [:img {:src "./logos.png"}]])]]))

(defn company-tab []
  [:div "Companies donations comming soon..."])

(defn main []
  (let [selected-main-tab @(subscribe [:ui/selected-tab :main])]
    [:div.main
     
     [:div.top-bar
      [tabs {:tab-id :main
             :items [["Donator" :donator]
                     ["Collector" :collector]
                     ["Food service" :food-service]
                     ["Company" :company]
                     ["Transparency" :transparency]]}]
      [:button {:on-click #(dispatch [:collector/generate-random-data])}
       "Generate random data"]
      [:div.balance (gstr/format "$U %d" @(subscribe [:collector/balance]))]]

     [:div.tab-content
      (case selected-main-tab
        :donator      [donator-tab]
        :collector    [collector-tab]
        :food-service [food-service-tab]
        :transparency [transparency-tab]
        :company      [company-tab])]]))
 


                                             
