(ns styles.main
  (:require [garden.stylesheet :refer [at-media at-keyframes]]))

(def screen
  [:.main {}
   [:.top-bar {:display :flex
               :flex-direction :row
               :justify-content :space-between}]])

(def tabs
  [[:.tabs {:display :flex
            :cursor :pointer
            :flex-direction :row
            :justify-content :space-between
            :min-width "250px"}
    [:.tab {:display :inline-block
            :border "1px solid #333"
            :padding "5px"}
     [:&.selected {:color :red}]]]

   [:.tab-content {:padding-top "10px"}]])

;; This creates resources/public/css/main.css
(def ^:garden main
  (list
   screen
   tabs
   ))


