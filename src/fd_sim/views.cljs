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
         [:input {:type :text :value @amount-txt :on-change #(reset! amount-txt (.-value (.-target %)))}]
         [:button {:on-click #(dispatch [:collector/add-donation {:user-id @selected-donator
                                                                  :amount (js/parseFloat @amount-txt)}])}
          "Donate"]]]])))

(defn collector-panel []
  [:div "Collector"])

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
 
