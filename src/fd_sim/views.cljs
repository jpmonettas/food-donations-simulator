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
      [:tr [:th "Id"] [:th "Donator"] [:th "Amount"]]]
     [:tbody
      (for [d donations]
        ^{:key (str (:donation/id d))}
        [:tr
         [:td (str (:donation/id d))]
         [:td (:donator/name (get donators (:donator/id d)))]
         [:td (str (:donation/amount d))]])
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
           [:tr [:th "Ingredient"] [:th "Price"]]]
          [:tbody
           (for [[ing-id ing] @market]
             ^{:key (str ing-id)}
             [:tr
              [:td (:ingredient/name ing)]
              [:td [:input {:type :number
                            :value (or (get update-ings ing-id) (:ingredient/price ing))
                            :on-change #(swap! update-ings-txt assoc ing-id (js/parseFloat (.-value (.-target %))))}]]
              [:td [:button {:on-click #(dispatch [:collector/update-market-ingredient-price ing-id (get update-ings ing-id)])}
                    "Update"]]])
           [:tr
            [:td [:input {:type :text
                          :value @new-ing-txt
                          :on-change #(reset! new-ing-txt (.-value (.-target %)))}]]
            [:td [:input {:type :number
                          :value @new-ing-price-txt
                          :on-change #(reset! new-ing-price-txt (.-value (.-target %)))}]]
            [:td [:button {:on-click (fn [_]
                                       (dispatch [:collector/add-market-ingredient {:ingredient/name @new-ing-txt
                                                                                    :ingredient/price (js/parseFloat @new-ing-price-txt)}])
                                       (reset! new-ing-txt "")
                                       (reset! new-ing-price-txt ""))}
                  "Add"]]]]]]))))

(defn collector-panel []
  [:div.collector
   [collector-market-panel]
   [donations-panel {}]])

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
                    (str/join ", "))
           ]])]]])) 

(defn food-service-panel []
  (let [selected-food-service (subscribe [:ui/selected-food-service])
        services (subscribe [:collector/food-services])]
    [:div
     [dishes-panel]
     [:div
      [:label "Login as food service:"]
      [:select {:on-change (fn [e] (dispatch [:ui/select-food-service (js/parseFloat (.-value (.-target e)))]))
                :value @selected-food-service}
       (for [s (vals @services)]
         ^{:key (str (:food-service/id s))}
         [:option {:value (:food-service/id s)} (:food-service/name s)])]]]))


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
 
