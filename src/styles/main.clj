(ns styles.main
  (:require [garden.stylesheet :refer [at-media at-keyframes]]))

(def screen
  [:div {:color :blue}])

;; This creates resources/public/css/main.css
(def ^:garden main
  (list
   screen
   ))


