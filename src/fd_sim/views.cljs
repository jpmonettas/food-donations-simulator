(ns fd-sim.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as reagent]))

(defn tabs [{:keys [event-key subs-key items]}]
  (let [selected @(subscribe [subs-key])]
    [:div.tabs
     (for [[label id] items]
       ^{:key (str id)}
       [:div.tab (cond-> {:on-click #(dispatch [event-key id])}
                   (= id selected) (assoc :class "selected"))
        label])]))

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
                   :selected @selected-donator}
          (for [d (vals @donators)]
            ^{:key (str (:user/id d))}
            [:option {:value (:user/id d)} (:user/name d)])]]
        [:div
         [:input {:type :number :value @amount-txt :on-change #(reset! amount-txt (.-value (.-target %)))}]
         [:button {:on-click #(dispatch [:collector/add-donation {:user/id @selected-donator
                                                                  :donation/amount (js/parseFloat @amount-txt)}])}
          "Donate"]]]])))

(defn collector-panel []
  (let [market (subscribe [:collector/market])
        new-ing-txt (reagent/atom "")
        new-ing-price-txt (reagent/atom "")
        update-ings-txt (reagent/atom {})
        ]
    (fn []
      (let [update-ings @update-ings-txt]
       [:div
        [:div.market.panel
         [:h2.title "Market"]
         [:table
          [:thead
           [:tr
            [:th "Ingredient"]
            [:th "Price"]]]
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
                  "Add"]]]]]]]))))

(defn food-service-panel []
  [:div "Food service"])


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
 
