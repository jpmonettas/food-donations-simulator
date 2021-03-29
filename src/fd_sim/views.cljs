(ns fd-sim.views
  (:require [re-frame.core :refer [dispatch subscribe]]))

(defn main []
  [:div
   [:div "Food donations simulator"]
   [:div (str @(subscribe [:collector/money]))]
   [:button {:on-click #(dispatch [:collector/add-money 137])}
    "Add"]])
 
